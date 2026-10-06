package com.guardpay.android

import com.guardpay.shared.domain.HeldPayment
import com.guardpay.shared.domain.HoldStatus
import com.guardpay.shared.domain.LedgerTime
import com.guardpay.shared.domain.Party
import com.guardpay.shared.domain.PaymentIntent
import com.guardpay.shared.domain.StellarAddress
import com.guardpay.shared.domain.TrustedContact
import com.guardpay.shared.domain.TxHash
import com.guardpay.shared.domain.UsdcAmount
import com.guardpay.shared.signing.Signer
import com.guardpay.shared.signing.SigningCancelledException
import com.guardpay.shared.stellar.AccountRules
import com.guardpay.shared.stellar.AccountShape
import com.guardpay.shared.stellar.StellarGateway
import com.guardpay.shared.stellar.SubmitResult
import com.guardpay.shared.stellar.ed25519AccountId
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * DEBUG BUILDS ONLY: an in-memory stand-in for the three contracts, so the screens
 * can be walked on the phone before testnet addresses exist. Same rules as the
 * test fake (security spike, "Modelo de estados") on the phone's wall clock.
 * Nothing here reaches Stellar. Its tx hashes are synthetic: the UI hides every
 * hash and proof while the source is Simulation, and they are never evidence.
 */
class SimulatedStellarGateway(
    private val account: StellarAddress,
    private val ownerKey: ByteArray,
    private val guardianKey: ByteArray,
    private val policy: StellarAddress,
    private val rules: AccountRules,
    private val contacts: List<TrustedContact>,
    startBalance: UsdcAmount,
) : StellarGateway {
    private val lock = Mutex()
    private var balance = startBalance
    private var spentToday = UsdcAmount.ZERO
    private var spentDay = day(now())
    private val holds = mutableListOf<HeldPayment>()
    private var txCounter = 0L

    private fun now() = LedgerTime(System.currentTimeMillis() / 1000)

    override suspend fun latestLedgerTime() = settle { now() }
    override suspend fun readAccountRules(account: StellarAddress) = settle { requireAccount(account); rules }
    override suspend fun readBalance(account: StellarAddress) = settle { requireAccount(account); balance }
    override suspend fun readTrustedContacts(account: StellarAddress) = settle { requireAccount(account); contacts }
    override suspend fun readDailySpent(account: StellarAddress) = settle {
        requireAccount(account)
        rollDay()
        spentToday
    }
    override suspend fun readHolds(account: StellarAddress) = settle { requireAccount(account); holds.toList() }
    override suspend fun readAccountShape(account: StellarAddress) = settle {
        requireAccount(account)
        AccountShape(1, listOf(ed25519AccountId(ownerKey)), listOf(policy))
    }

    override suspend fun submitQueue(account: StellarAddress, intent: PaymentIntent, owner: Signer) =
        submit(owner) {
            if (account != this.account || !owner.publicKey.contentEquals(ownerKey)) return@submit reject(1)
            val now = now()
            val duplicate = holds.any { it.status == HoldStatus.Held && it.intent == intent && !it.isExpired(now) }
            if (duplicate) return@submit reject(2)
            val readyAt = LedgerTime(now.epochSeconds + rules.holdDurationSeconds)
            holds += HeldPayment(
                id = holds.size.toLong(),
                account = account,
                destination = intent.destination,
                amount = intent.amount,
                createdAt = now,
                readyAt = readyAt,
                expiresAt = rules.expiryWindowSeconds?.let { LedgerTime(readyAt.epochSeconds + it) },
                status = HoldStatus.Held,
            )
            confirm()
        }

    override suspend fun submitCancel(account: StellarAddress, holdId: Long, signer: Signer) =
        submit(signer) {
            val by = when {
                signer.publicKey.contentEquals(guardianKey) -> Party.Guardian
                signer.publicKey.contentEquals(ownerKey) -> Party.Owner
                else -> return@submit reject(3)
            }
            val i = holds.indexOfFirst { it.id == holdId && it.account == account }
            if (i < 0 || holds[i].status != HoldStatus.Held) return@submit reject(4)
            holds[i] = holds[i].copy(status = HoldStatus.Cancelled, cancelledBy = by, cancelledAt = now())
            confirm()
        }

    override suspend fun submitTransfer(account: StellarAddress, intent: PaymentIntent, owner: Signer) =
        submit(owner) {
            // Only the owner's key is an account signer; the guardian's never is.
            if (account != this.account || !owner.publicKey.contentEquals(ownerKey)) return@submit reject(1)
            if (intent.amount > balance) return@submit reject(5)
            rollDay()
            val now = now()

            val isContact = contacts.any { it.address == intent.destination }
            val total = spentToday.plusOrNull(intent.amount)
            if (isContact && total != null && total <= rules.dailyCap) {
                spentToday = total
                balance = UsdcAmount(balance.units - intent.amount.units)
                return@submit confirm()
            }

            val i = holds.indexOfFirst {
                it.status == HoldStatus.Held && it.intent == intent && it.isMatured(now) && !it.isExpired(now)
            }
            if (i < 0) return@submit reject(6)
            holds[i] = holds[i].copy(status = HoldStatus.Executed)
            balance = UsdcAmount(balance.units - intent.amount.units)
            confirm()
        }

    /** A short pause so "Leyendo…" and "Enviando a Stellar…" are visible, as they would be on a real RPC. */
    private suspend fun <T> settle(body: () -> T): T {
        delay(READ_DELAY_MS)
        return lock.withLock { body() }
    }

    private suspend fun submit(signer: Signer, body: () -> SubmitResult): SubmitResult {
        try {
            signer.signAuthDigest(ByteArray(32))
        } catch (e: SigningCancelledException) {
            return SubmitResult.SigningCancelled
        }
        delay(SUBMIT_DELAY_MS)
        return lock.withLock { body() }
    }

    private fun nextHash(): TxHash = TxHash((++txCounter).toString(16).padStart(64, '0'))
    private fun confirm() = SubmitResult.Confirmed(nextHash())
    private fun reject(code: Int) = SubmitResult.Rejected(nextHash(), code)

    private fun requireAccount(a: StellarAddress) = require(a == account) { "unknown account" }

    private fun rollDay() {
        val today = day(now())
        if (today != spentDay) {
            spentDay = today
            spentToday = UsdcAmount.ZERO
        }
    }

    private fun day(t: LedgerTime) = t.epochSeconds / 86_400

    private companion object {
        const val READ_DELAY_MS = 250L
        const val SUBMIT_DELAY_MS = 900L
    }
}

/**
 * DEBUG BUILDS ONLY: stands in for the passkey/Keystore prompt (P6). It holds a
 * public key and nothing else; the "signature" is zeros and no secret exists.
 */
class SimulatedSigner(override val publicKey: ByteArray) : Signer {
    override suspend fun signAuthDigest(digest: ByteArray): ByteArray {
        delay(600)
        return ByteArray(64)
    }
}
