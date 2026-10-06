package com.guardpay.shared.ui.owner

import com.guardpay.shared.ai.Analysis
import com.guardpay.shared.domain.HeldPayment
import com.guardpay.shared.domain.HoldStatus
import com.guardpay.shared.domain.LanePrediction
import com.guardpay.shared.domain.LaneRules
import com.guardpay.shared.domain.LedgerTime
import com.guardpay.shared.domain.PaymentIntent
import com.guardpay.shared.domain.PaymentState
import com.guardpay.shared.domain.SigningOrigin
import com.guardpay.shared.domain.StellarAddress
import com.guardpay.shared.domain.TrustedContact
import com.guardpay.shared.domain.TxHash
import com.guardpay.shared.domain.UsdcAmount
import com.guardpay.shared.domain.predictLane
import com.guardpay.shared.domain.stateOf
import com.guardpay.shared.signing.Signer
import com.guardpay.shared.stellar.AccountRules
import com.guardpay.shared.stellar.AccountShape
import com.guardpay.shared.stellar.StellarGateway
import com.guardpay.shared.stellar.SubmissionOutcomeUnknownException
import com.guardpay.shared.stellar.SubmitResult
import com.guardpay.shared.stellar.ed25519AccountId
import com.guardpay.shared.ui.MessageReader
import com.guardpay.shared.ui.Navigator
import com.guardpay.shared.ui.PaymentKey
import com.guardpay.shared.ui.Screen
import com.guardpay.shared.ui.model.DataSource
import com.guardpay.shared.ui.model.KnownTx
import com.guardpay.shared.ui.model.ProofLinks
import com.guardpay.shared.ui.model.RejectedAttempt
import com.guardpay.shared.ui.model.parseUsdc
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.TimeZone
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/** Fixed per install: there is no settings screen. Names are local; the chain only has addresses. */
data class OwnerConfig(
    val account: StellarAddress,
    val ownerName: String,
    val guardianName: String,
    val source: DataSource,
    val links: ProofLinks = ProofLinks(),
    val contactNames: Map<StellarAddress, String> = emptyMap(),
    val tz: TimeZone = TimeZone.currentSystemDefault(),
    /**
     * Whether "Detener mi pago" is offered. The deployed `hold_registry` lets only
     * the guardian cancel, so real wiring sets false; the chain would refuse it anyway.
     */
    val ownerMayStop: Boolean = true,
)

/** The button moment of Firmando…: "Esperando tu huella o PIN…", then "Enviando a Stellar…". */
enum class SigningPhase { Idle, WaitingPasskey, Sending }

/** A payment only this app knows about: a direct transfer it sent, or a refusal it received. */
data class LocalPayment(
    val key: PaymentKey.Local,
    val state: PaymentState,
    val at: LedgerTime?,
    val txs: List<KnownTx>,
    val rejected: List<RejectedAttempt>,
    val analysis: Analysis?,
)

/** What this app adds to a registry record: its own transactions, and a state the record cannot show. */
data class HoldExtra(
    val txs: List<KnownTx> = emptyList(),
    val rejected: List<RejectedAttempt> = emptyList(),
    /** Rechazado or Error de red on a release attempt. */
    val overlay: PaymentState? = null,
    val sentAt: LedgerTime? = null,
    val releaseTx: TxHash? = null,
    val analysis: Analysis? = null,
)

data class PayForm(
    val contact: StellarAddress? = null,
    val otherOpen: Boolean = false,
    val otherAddress: String = "",
    val amountText: String = "",
    val ownerChoseHold: Boolean = false,
    val message: String = "",
    val analysis: Analysis? = null,
    val analyzing: Boolean = false,
    val reviewOpen: Boolean = false,
    /** The last signature never reached Stellar: the sheet offers "Reintentar". */
    val networkError: Boolean = false,
)

data class OwnerUi(
    val balance: UsdcAmount? = null,
    val rules: AccountRules? = null,
    val contacts: List<TrustedContact> = emptyList(),
    val spentToday: UsdcAmount? = null,
    val shape: AccountShape? = null,
    val shapeFailed: Boolean = false,
    val holds: List<HeldPayment> = emptyList(),
    /** Ledger time of the last read that worked. States are derived from this, never from the phone. */
    val ledgerNow: LedgerTime? = null,
    val readMark: TimeMark? = null,
    val lastReadOk: Boolean = false,
    val local: List<LocalPayment> = emptyList(),
    val extras: Map<Long, HoldExtra> = emptyMap(),
    val form: PayForm = PayForm(),
    val phase: SigningPhase = SigningPhase.Idle,
    /** The hold whose "Detener mi pago" confirmation is open. */
    val stopSheetFor: Long? = null,
    val notice: String? = null,
) {
    val waiting: List<HeldPayment> get() = holds.filter { it.status == HoldStatus.Held }.sortedByDescending { it.createdAt }

    fun stateOf(key: PaymentKey): PaymentState? = when (key) {
        is PaymentKey.Local -> local.firstOrNull { it.key == key }?.state
        is PaymentKey.Hold -> {
            val hold = holds.firstOrNull { it.id == key.id }
            val extra = extras[key.id]
            val overlay = extra?.overlay
            when {
                hold == null -> null
                overlay is PaymentState.Rejected -> overlay
                overlay is PaymentState.NetworkError && hold.status == HoldStatus.Held -> overlay
                ledgerNow == null -> null
                else -> when (val s = stateOf(hold, ledgerNow)) {
                    is PaymentState.Sent -> s.copy(txHash = extra?.releaseTx)
                    else -> s
                }
            }
        }
    }

    fun holdOf(key: PaymentKey): HeldPayment? = (key as? PaymentKey.Hold)?.let { k -> holds.firstOrNull { it.id == k.id } }

    /** The contract refused a release because the payment had been stopped. */
    fun rejectedAfterStop(key: PaymentKey): Boolean =
        stateOf(key) is PaymentState.Rejected && holdOf(key)?.status == HoldStatus.Cancelled

    /** Final payments, newest first, at most 5. */
    fun recent(): List<PaymentKey> {
        val fromHolds = holds.filter { it.status != HoldStatus.Held }.map { PaymentKey.Hold(it.id) to it.createdAt.epochSeconds }
        val fromLocal = local.map { it.key to (it.at?.epochSeconds ?: 0L) }
        return (fromHolds + fromLocal).sortedByDescending { it.second }.map { it.first }.take(5)
    }

    fun formDestination(): StellarAddress? = form.contact
        ?: form.otherAddress.trim().takeIf { form.otherOpen && StellarAddress.isValid(it) }?.let(::StellarAddress)

    fun formAmount(): UsdcAmount? = parseUsdc(form.amountText)

    fun formIntent(): PaymentIntent? {
        val d = formDestination() ?: return null
        val a = formAmount() ?: return null
        return PaymentIntent(d, a)
    }

    /** Informative: the contract decides when she signs. Null until the rules were read. */
    fun prediction(): LanePrediction? {
        val intent = formIntent() ?: return null
        val rules = rules ?: return null
        val spent = spentToday ?: return null
        return predictLane(intent, LaneRules(contacts, rules.dailyCap, spent), form.ownerChoseHold)
    }

    /** What is left of today's cap with trusted contacts. */
    fun capLeft(): UsdcAmount? {
        val cap = rules?.dailyCap ?: return null
        val spent = spentToday ?: return null
        return UsdcAmount((cap.units - spent.units).coerceAtLeast(0))
    }
}

/**
 * The owner's side of the app. Every number on screen comes from [gateway]; this
 * class only reads, asks for a signature and reports what the chain answered. The
 * contract decides; nothing here enforces a rule.
 */
class OwnerSession(
    private val gateway: StellarGateway,
    private val owner: Signer,
    val config: OwnerConfig,
    private val reader: MessageReader,
    private val nav: Navigator,
    private val scope: CoroutineScope,
    private val pollMillis: Long = 15_000,
) {
    private val _ui = MutableStateFlow(OwnerUi())
    val ui: StateFlow<OwnerUi> = _ui.asStateFlow()

    /** The owner's key as a G strkey, to recognize it among the account's signers. */
    val ownerId: StellarAddress = ed25519AccountId(owner.publicKey)

    private val readLock = Mutex()
    private var poller: Job? = null
    private var localCounter = 0L

    fun nameOf(a: StellarAddress): String = config.contactNames[a] ?: a.short()

    /** Reads once, then every 15 s while a payment is held. When already running, just reads again. */
    fun start() {
        if (poller?.isActive == true) {
            scope.launch { refresh() }
            return
        }
        poller = scope.launch {
            refresh()
            while (isActive) {
                delay(pollMillis)
                if (_ui.value.waiting.isNotEmpty()) refresh()
            }
        }
    }

    suspend fun refresh() = readLock.withLock {
        try {
            val now = gateway.latestLedgerTime()
            val rules = gateway.readAccountRules(config.account)
            val balance = gateway.readBalance(config.account)
            val contacts = gateway.readTrustedContacts(config.account)
            val spent = gateway.readDailySpent(config.account)
            val holds = gateway.readHolds(config.account)
            _ui.update {
                it.copy(
                    rules = rules, balance = balance, contacts = contacts, spentToday = spent, holds = holds,
                    ledgerNow = now, readMark = TimeSource.Monotonic.markNow(), lastReadOk = true,
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Keep what was shown; the screens say "sin actualizar desde las HH:MM" and drop the seal.
            _ui.update { it.copy(lastReadOk = false) }
        }
        try {
            val shape = gateway.readAccountShape(config.account)
            _ui.update { it.copy(shape = shape, shapeFailed = false) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _ui.update { it.copy(shapeFailed = true) }
        }
    }

    fun refreshNow() {
        scope.launch { refresh() }
    }

    fun dismissNotice() = _ui.update { it.copy(notice = null) }

    // ---- Pagar ----

    fun newPayment() {
        _ui.update { it.copy(form = PayForm(), notice = null) }
        nav.go(Screen.Pay)
    }

    fun selectContact(a: StellarAddress) =
        editForm { it.copy(contact = a, otherOpen = false, otherAddress = "", ownerChoseHold = false) }

    fun openOther() = editForm { it.copy(contact = null, otherOpen = true, ownerChoseHold = false) }
    fun setOther(text: String) = editForm { it.copy(otherAddress = text.trim()) }
    fun setAmount(text: String) = editForm { it.copy(amountText = text) }
    fun toggleOwnerHold() = editForm { it.copy(ownerChoseHold = !it.ownerChoseHold) }
    fun chooseHold() = editForm { it.copy(ownerChoseHold = true) }
    fun setMessage(text: String) = editForm { it.copy(message = text, analysis = null) }

    /** Sends the pasted text to the AI. The result is shown; it never changes the form. */
    fun analyzeMessage() {
        val text = _ui.value.form.message
        if (text.isBlank() || _ui.value.form.analyzing) return
        editForm { it.copy(analyzing = true) }
        scope.launch {
            val analysis = try {
                reader.read(text)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Analysis.Unavailable
            }
            _ui.update { s -> if (s.form.message == text) s.copy(form = s.form.copy(analysis = analysis, analyzing = false)) else s }
        }
    }

    fun openReview() {
        val s = _ui.value
        if (s.formIntent() == null || s.prediction() == null || s.formDestination() == config.account) return
        editForm { it.copy(reviewOpen = true, networkError = false) }
        if (_ui.value.notice == SIGNING_FAILED) _ui.update { it.copy(notice = null) }
    }

    /** System back: true when a sheet was open (it closes, unless a signature is in flight). */
    fun closeSheet(): Boolean {
        val s = _ui.value
        if (!s.form.reviewOpen && s.stopSheetFor == null) return false
        closeReview()
        closeStop()
        return true
    }

    /** "Cambiar": back to the form, nothing signed. */
    fun closeReview() {
        if (_ui.value.phase != SigningPhase.Idle) return
        editForm { it.copy(reviewOpen = false, networkError = false) }
    }

    /** "Firmar y enviar" / "Firmar y retener" / "Reintentar". */
    fun sign() {
        val s = _ui.value
        if (s.phase != SigningPhase.Idle) return
        val intent = s.formIntent() ?: return
        val prediction = s.prediction() ?: return
        val analysis = s.form.analysis?.takeUnless { it.isUnavailable }
        scope.launch {
            val queue = prediction is LanePrediction.MustHold
            val before = _ui.value.holds.map { it.id }.toSet()
            val outcome = submit {
                if (queue) gateway.submitQueue(config.account, intent, it) else gateway.submitTransfer(config.account, intent, it)
            }
            when (outcome) {
                is SubmitResult.Confirmed -> {
                    refresh()
                    if (queue) {
                        val hold = _ui.value.holds
                            .filter { it.id !in before && it.status == HoldStatus.Held && it.intent == intent }
                            .maxByOrNull { it.id }
                        if (hold != null) {
                            putExtra(hold.id) { it.copy(txs = it.txs + KnownTx("Creación", outcome.txHash), analysis = analysis) }
                            leaveForm(Screen.Detail(PaymentKey.Hold(hold.id)))
                        } else {
                            // Never show "Retenido" without the record: say what is known instead.
                            _ui.update { it.copy(notice = "Stellar aceptó el pago, pero todavía no se pudo leer su registro. Se actualizará solo.") }
                            leaveForm(Screen.Home, toRoot = true)
                        }
                    } else {
                        val key = addLocal(
                            PaymentState.Sent(intent, outcome.txHash, null),
                            txs = listOf(KnownTx("Envío", outcome.txHash)), analysis = analysis,
                        )
                        leaveForm(Screen.Detail(key))
                    }
                }
                is SubmitResult.Rejected -> {
                    val key = addLocal(
                        PaymentState.Rejected(intent, outcome.txHash),
                        rejected = listOf(RejectedAttempt(outcome.txHash, outcome.code, _ui.value.ledgerNow)), analysis = analysis,
                    )
                    refresh()
                    leaveForm(Screen.Detail(key))
                }
                is SubmitResult.NetworkFailure -> editForm { it.copy(networkError = true) }
                SubmitResult.SigningCancelled -> Unit
                SubmitResult.SigningFailed -> _ui.update { it.copy(notice = SIGNING_FAILED) }
                null -> {
                    _ui.update { it.copy(notice = UNKNOWN_OUTCOME) }
                    leaveForm(Screen.Home, toRoot = true)
                }
            }
        }
    }

    // ---- Detalle ----

    /** "Firmar y enviar" on a matured hold: a new owner signature, the contract checks the record. */
    fun release(id: Long) {
        val s = _ui.value
        if (s.phase != SigningPhase.Idle) return
        val hold = s.holds.firstOrNull { it.id == id } ?: return
        val state = s.stateOf(PaymentKey.Hold(id))
        if (state !is PaymentState.ReadyToSend && !(state is PaymentState.NetworkError && state.origin is SigningOrigin.Release)) return
        scope.launch {
            when (val outcome = submit { gateway.submitTransfer(config.account, hold.intent, it) }) {
                is SubmitResult.Confirmed -> {
                    putExtra(id) {
                        it.copy(
                            txs = it.txs + KnownTx("Envío", outcome.txHash), overlay = null,
                            releaseTx = outcome.txHash, sentAt = _ui.value.ledgerNow,
                        )
                    }
                    refresh()
                }
                is SubmitResult.Rejected -> {
                    putExtra(id) {
                        it.copy(
                            overlay = PaymentState.Rejected(hold.intent, outcome.txHash),
                            rejected = it.rejected + RejectedAttempt(outcome.txHash, outcome.code, _ui.value.ledgerNow),
                        )
                    }
                    refresh()
                }
                is SubmitResult.NetworkFailure ->
                    putExtra(id) { it.copy(overlay = PaymentState.NetworkError(SigningOrigin.Release(hold))) }
                SubmitResult.SigningCancelled -> Unit
                SubmitResult.SigningFailed -> _ui.update { it.copy(notice = SIGNING_FAILED) }
                null -> {
                    _ui.update { it.copy(notice = UNKNOWN_OUTCOME) }
                    refresh()
                }
            }
        }
    }

    /** Closes "Error de red" without retrying: nothing changed, back to Listo para enviar. */
    fun dismissError(id: Long) = putExtra(id) { if (it.overlay is PaymentState.NetworkError) it.copy(overlay = null) else it }

    fun askStop(id: Long) = _ui.update { it.copy(stopSheetFor = id) }

    fun closeStop() {
        if (_ui.value.phase == SigningPhase.Idle) _ui.update { it.copy(stopSheetFor = null) }
    }

    /** "Detener mi pago": the owner cancels her own held payment on the registry. */
    fun confirmStop() {
        val id = _ui.value.stopSheetFor ?: return
        if (_ui.value.phase != SigningPhase.Idle) return
        scope.launch {
            when (val outcome = submit { gateway.submitCancel(config.account, id, it) }) {
                is SubmitResult.Confirmed -> {
                    putExtra(id) { it.copy(txs = it.txs + KnownTx("Detención", outcome.txHash)) }
                    _ui.update { it.copy(stopSheetFor = null) }
                    refresh()
                }
                is SubmitResult.Rejected -> {
                    putExtra(id) { it.copy(rejected = it.rejected + RejectedAttempt(outcome.txHash, outcome.code, _ui.value.ledgerNow)) }
                    _ui.update { it.copy(stopSheetFor = null, notice = "El contrato no permitió detenerlo. Mira su estado abajo.") }
                    refresh()
                }
                is SubmitResult.NetworkFailure ->
                    _ui.update { it.copy(stopSheetFor = null, notice = "No llegó a Stellar. Nada cambió.") }
                SubmitResult.SigningCancelled -> Unit
                SubmitResult.SigningFailed -> _ui.update { it.copy(stopSheetFor = null, notice = SIGNING_FAILED) }
                null -> {
                    _ui.update { it.copy(stopSheetFor = null, notice = UNKNOWN_OUTCOME) }
                    refresh()
                }
            }
        }
    }

    // ---- internals ----

    /** Runs a submit with the phase-tracking signer. Null means the outcome is unknown. */
    private suspend fun submit(block: suspend (Signer) -> SubmitResult): SubmitResult? {
        _ui.update { it.copy(phase = SigningPhase.WaitingPasskey) }
        val tracking = object : Signer {
            override val publicKey: ByteArray get() = owner.publicKey
            override suspend fun signHash(digest: ByteArray): ByteArray =
                owner.signHash(digest).also { _ui.update { it.copy(phase = SigningPhase.Sending) } }
        }
        return try {
            block(tracking)
        } catch (e: CancellationException) {
            throw e
        } catch (e: SubmissionOutcomeUnknownException) {
            null
        } catch (e: Exception) {
            SubmitResult.NetworkFailure(e.message ?: "submit failed")
        } finally {
            _ui.update { it.copy(phase = SigningPhase.Idle) }
        }
    }

    private fun editForm(f: (PayForm) -> PayForm) = _ui.update { it.copy(form = f(it.form)) }

    private fun putExtra(id: Long, f: (HoldExtra) -> HoldExtra) =
        _ui.update { it.copy(extras = it.extras + (id to f(it.extras[id] ?: HoldExtra()))) }

    private fun addLocal(
        state: PaymentState,
        txs: List<KnownTx> = emptyList(),
        rejected: List<RejectedAttempt> = emptyList(),
        analysis: Analysis?,
    ): PaymentKey.Local {
        val key = PaymentKey.Local(++localCounter)
        _ui.update { it.copy(local = it.local + LocalPayment(key, state, it.ledgerNow, txs, rejected, analysis)) }
        return key
    }

    private fun leaveForm(to: Screen, toRoot: Boolean = false) {
        _ui.update { it.copy(form = PayForm()) }
        if (toRoot) nav.popTo(Screen.Home) else nav.replace(to)
    }

    companion object {
        /** After [SubmissionOutcomeUnknownException]: never "nada cambió", never a blind retry. */
        const val SIGNING_FAILED = "No se pudo firmar en este teléfono. No se envió nada."
        const val UNKNOWN_OUTCOME =
            "No sabemos si llegó a Stellar. Antes de intentarlo otra vez, revisa tu saldo y tus pagos: se están leyendo de nuevo."
    }
}
