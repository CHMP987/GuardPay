package com.guardpay.shared.stellar

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
import com.guardpay.shared.signing.signWith

/**
 * In-memory stand-in for the three contracts, test-only. It applies the same rules
 * as the contract design (security spike, section "Modelo de estados") so the
 * app's state machine can be exercised end to end before testnet addresses exist.
 * Single account, single token. Its tx hashes are synthetic and never evidence.
 */
class FakeStellarGateway(
    private val account: StellarAddress,
    private val ownerKey: ByteArray,
    private val guardianKey: ByteArray,
    private val rules: AccountRules,
    private val contacts: List<TrustedContact>,
    startBalance: UsdcAmount,
    var now: LedgerTime = LedgerTime(1_000_000),
) : StellarGateway {
    private var balance = startBalance
    private var spentToday = UsdcAmount.ZERO
    private var spentDay = day(now)
    private val holds = mutableListOf<HeldPayment>()
    private var txCounter = 0L

    /** Set to simulate a dead network on the next submit. */
    var networkDown = false

    fun advance(seconds: Long) {
        now = LedgerTime(now.epochSeconds + seconds)
    }

    override suspend fun latestLedgerTime() = now
    override suspend fun readAccountRules(account: StellarAddress) = rules.also { requireAccount(account) }
    override suspend fun readBalance(account: StellarAddress) = balance.also { requireAccount(account) }
    override suspend fun readTrustedContacts(account: StellarAddress) = contacts.also { requireAccount(account) }
    override suspend fun readDailySpent(account: StellarAddress): UsdcAmount {
        requireAccount(account)
        rollDay()
        return spentToday
    }
    override suspend fun readHolds(account: StellarAddress) = holds.toList().also { requireAccount(account) }
    override suspend fun readAccountShape(account: StellarAddress) =
        AccountShape(1, listOf(ed25519AccountId(ownerKey)), emptyList()).also { requireAccount(account) }

    override suspend fun submitQueue(account: StellarAddress, intent: PaymentIntent, owner: Signer): SubmitResult =
        submit(owner) {
            if (account != this.account || !owner.publicKey.contentEquals(ownerKey)) return@submit reject(1)
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

    override suspend fun submitCancel(account: StellarAddress, holdId: Long, signer: Signer): SubmitResult =
        submit(signer) {
            val by = when {
                signer.publicKey.contentEquals(guardianKey) -> Party.Guardian
                signer.publicKey.contentEquals(ownerKey) -> Party.Owner
                else -> return@submit reject(3)
            }
            val i = holds.indexOfFirst { it.id == holdId && it.account == account }
            if (i < 0 || holds[i].status != HoldStatus.Held) return@submit reject(4)
            holds[i] = holds[i].copy(status = HoldStatus.Cancelled, cancelledBy = by, cancelledAt = now)
            confirm()
        }

    override suspend fun submitTransfer(account: StellarAddress, intent: PaymentIntent, owner: Signer): SubmitResult =
        submit(owner) {
            // Only the owner's key is an account signer; the guardian's never is.
            if (account != this.account || !owner.publicKey.contentEquals(ownerKey)) return@submit reject(1)
            if (intent.amount > balance) return@submit reject(5)
            rollDay()

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

    private suspend fun submit(signer: Signer, body: () -> SubmitResult): SubmitResult {
        try {
            signWith(signer, ByteArray(32)) ?: return SubmitResult.SigningFailed
        } catch (e: SigningCancelledException) {
            return SubmitResult.SigningCancelled
        }
        if (networkDown) return SubmitResult.NetworkFailure("fake network down")
        return body()
    }

    private fun nextHash(): TxHash = TxHash((++txCounter).toString(16).padStart(64, '0'))
    private fun confirm() = SubmitResult.Confirmed(nextHash())
    private fun reject(code: Int) = SubmitResult.Rejected(nextHash(), code)

    private fun requireAccount(a: StellarAddress) = require(a == account) { "unknown account" }

    private fun rollDay() {
        if (day(now) != spentDay) {
            spentDay = day(now)
            spentToday = UsdcAmount.ZERO
        }
    }

    private fun day(t: LedgerTime) = t.epochSeconds / 86_400
}

/** Test signer: returns a dummy signature, or "cancels" like a dismissed prompt. */
class FakeSigner(override val publicKey: ByteArray, var cancels: Boolean = false, var breaks: Boolean = false) : Signer {
    override suspend fun signHash(digest: ByteArray): ByteArray {
        if (cancels) throw SigningCancelledException()
        if (breaks) throw IllegalStateException("key invalidated")
        return ByteArray(64)
    }
}
