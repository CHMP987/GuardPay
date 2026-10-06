package com.guardpay.shared.ui.guardian

import com.guardpay.shared.domain.HeldPayment
import com.guardpay.shared.domain.HoldStatus
import com.guardpay.shared.domain.LedgerTime
import com.guardpay.shared.domain.Party
import com.guardpay.shared.domain.StellarAddress
import com.guardpay.shared.domain.TrustedContact
import com.guardpay.shared.stellar.SubmissionOutcomeUnknownException
import com.guardpay.shared.stellar.SubmitResult
import com.guardpay.shared.ui.model.DataSource
import com.guardpay.shared.ui.model.KnownTx
import com.guardpay.shared.ui.model.ProofLinks
import com.guardpay.shared.ui.model.RejectedAttempt
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

/**
 * Everything the guardian's screens can do: read the owner's holds and stop one.
 * There is no transfer, no queue and no signer here, so the guardian mode cannot
 * offer what the guardian cannot do. A test checks this surface.
 */
interface GuardianChain {
    suspend fun latestLedgerTime(): LedgerTime
    suspend fun readHolds(): List<HeldPayment>
    suspend fun readTrustedContacts(): List<TrustedContact>

    /** Cancels [holdId] on the registry with the guardian's key. [onSigned] runs once the key signed. */
    suspend fun submitCancel(holdId: Long, onSigned: () -> Unit): SubmitResult
}

data class GuardianConfig(
    val ownerName: String,
    val guardianName: String,
    val source: DataSource,
    val links: ProofLinks = ProofLinks(),
    val contactNames: Map<StellarAddress, String> = emptyMap(),
    val tz: TimeZone = TimeZone.currentSystemDefault(),
)

enum class GuardianPhase { Idle, WaitingKey, Sending }

data class GuardianUi(
    val holds: List<HeldPayment> = emptyList(),
    val contacts: List<TrustedContact> = emptyList(),
    val ledgerNow: LedgerTime? = null,
    val readMark: TimeMark? = null,
    val lastReadOk: Boolean = false,
    val phase: GuardianPhase = GuardianPhase.Idle,
    val confirmFor: Long? = null,
    val txs: Map<Long, List<KnownTx>> = emptyMap(),
    val rejected: Map<Long, List<RejectedAttempt>> = emptyMap(),
    val notice: String? = null,
) {
    val waiting: List<HeldPayment> get() = holds.filter { it.status == HoldStatus.Held }.sortedBy { it.readyAt }

    /** "Ya detuviste": the last 5 this guardian stopped. */
    val stoppedByYou: List<HeldPayment>
        get() = holds.filter { it.status == HoldStatus.Cancelled && it.cancelledBy == Party.Guardian }
            .sortedByDescending { it.cancelledAt ?: it.createdAt }.take(5)

    fun isContact(a: StellarAddress) = contacts.any { it.address == a }

    /**
     * "Cuenta nueva": not a trusted contact and no executed hold to it. A non-contact
     * can only be paid through a hold, so the registry is the whole history.
     */
    fun isNewAccount(a: StellarAddress) =
        !isContact(a) && holds.none { it.destination == a && it.status == HoldStatus.Executed }
}

/** The guardian's side. Reads the chain every 15 s while something waits; one verb: stop. */
class GuardianSession(
    private val chain: GuardianChain,
    val config: GuardianConfig,
    private val scope: CoroutineScope,
    private val pollMillis: Long = 15_000,
) {
    private val _ui = MutableStateFlow(GuardianUi())
    val ui: StateFlow<GuardianUi> = _ui.asStateFlow()

    private val readLock = Mutex()
    private var poller: Job? = null

    fun nameOf(a: StellarAddress): String =
        config.contactNames[a] ?: _ui.value.contacts.firstOrNull { it.address == a }?.name ?: a.short()

    /** Starts polling; when already running, just reads again (the person came back to this side). */
    fun start() {
        if (poller?.isActive == true) {
            scope.launch { refresh() }
            return
        }
        poller = scope.launch {
            refresh()
            while (isActive) {
                delay(pollMillis)
                if (_ui.value.waiting.isNotEmpty() || !_ui.value.lastReadOk) refresh()
            }
        }
    }

    suspend fun refresh() = readLock.withLock {
        try {
            val now = chain.latestLedgerTime()
            val holds = chain.readHolds()
            val contacts = chain.readTrustedContacts()
            _ui.update {
                it.copy(holds = holds, contacts = contacts, ledgerNow = now, readMark = TimeSource.Monotonic.markNow(), lastReadOk = true)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _ui.update { it.copy(lastReadOk = false) }
        }
    }

    fun askStop(id: Long) {
        if (_ui.value.waiting.any { it.id == id }) _ui.update { it.copy(confirmFor = id, notice = null) }
    }

    /** System back: true when the confirmation was open (it closes, unless a signature is in flight). */
    fun closeSheet(): Boolean {
        if (_ui.value.confirmFor == null) return false
        closeStop()
        return true
    }

    fun closeStop() {
        if (_ui.value.phase == GuardianPhase.Idle) _ui.update { it.copy(confirmFor = null) }
    }

    /** "Detener el pago". */
    fun confirmStop() {
        val id = _ui.value.confirmFor ?: return
        if (_ui.value.phase != GuardianPhase.Idle) return
        _ui.update { it.copy(phase = GuardianPhase.WaitingKey) }
        scope.launch {
            val outcome = try {
                chain.submitCancel(id) { _ui.update { it.copy(phase = GuardianPhase.Sending) } }
            } catch (e: CancellationException) {
                throw e
            } catch (e: SubmissionOutcomeUnknownException) {
                null
            } catch (e: Exception) {
                SubmitResult.NetworkFailure(e.message ?: "submit failed")
            }
            _ui.update { s ->
                val base = s.copy(phase = GuardianPhase.Idle, confirmFor = null)
                when (outcome) {
                    is SubmitResult.Confirmed -> base.copy(txs = base.txs + (id to (base.txs[id].orEmpty() + KnownTx("Detención", outcome.txHash))))
                    is SubmitResult.Rejected -> base.copy(
                        rejected = base.rejected + (id to (base.rejected[id].orEmpty() + RejectedAttempt(outcome.txHash, outcome.code, s.ledgerNow))),
                        notice = "El contrato no permitió detenerlo. Mira su estado abajo.",
                    )
                    is SubmitResult.NetworkFailure -> base.copy(notice = "No llegó a Stellar. Nada cambió.")
                    SubmitResult.SigningCancelled -> s.copy(phase = GuardianPhase.Idle)
                    null -> base.copy(notice = "No sabemos si llegó a Stellar. Se está leyendo de nuevo.")
                }
            }
            refresh()
        }
    }

    fun dismissNotice() = _ui.update { it.copy(notice = null) }
}
