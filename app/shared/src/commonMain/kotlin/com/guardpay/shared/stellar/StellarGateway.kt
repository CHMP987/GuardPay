package com.guardpay.shared.stellar

import com.guardpay.shared.domain.HeldPayment
import com.guardpay.shared.domain.LedgerTime
import com.guardpay.shared.domain.PaymentIntent
import com.guardpay.shared.domain.StellarAddress
import com.guardpay.shared.domain.TrustedContact
import com.guardpay.shared.domain.TxHash
import com.guardpay.shared.domain.UsdcAmount
import com.guardpay.shared.signing.Signer

/**
 * Every chain access goes through here, so the SDK can be swapped without touching
 * domain or UI. Production: `KmpStellarGateway` (stellar-sdk 1.14.0). Tests:
 * `FakeStellarGateway`, which exists only in commonTest.
 *
 * PROVISIONAL until INTERFACES.md is frozen at Sync 1: argument and return types
 * may change to match the contracts' exact signatures, reads and error codes.
 */
interface StellarGateway {
    suspend fun latestLedgerTime(): LedgerTime

    suspend fun readAccountRules(account: StellarAddress): AccountRules
    suspend fun readBalance(account: StellarAddress): UsdcAmount
    suspend fun readTrustedContacts(account: StellarAddress): List<TrustedContact>
    suspend fun readDailySpent(account: StellarAddress): UsdcAmount
    suspend fun readHolds(account: StellarAddress): List<HeldPayment>

    /** `HoldRegistry.queue(account, destination, amount)`, signed by the owner. */
    suspend fun submitQueue(account: StellarAddress, intent: PaymentIntent, owner: Signer): SubmitResult

    /** `HoldRegistry.cancel(id)`, signed by the guardian (or the owner). */
    suspend fun submitCancel(account: StellarAddress, holdId: Long, signer: Signer): SubmitResult

    /** `USDC.transfer(account, destination, amount)` authorized by the account, signed by the owner. */
    suspend fun submitTransfer(account: StellarAddress, intent: PaymentIntent, owner: Signer): SubmitResult
}

/** The account's fixed config, set once at deploy. The app has no settings screen. */
data class AccountRules(
    val guardian: StellarAddress,
    val dailyCap: UsdcAmount,
    val holdDurationSeconds: Long,
    /** SHOULD: seconds after readyAt before a matured hold expires; null if none. */
    val expiryWindowSeconds: Long?,
)

sealed interface SubmitResult {
    data class Confirmed(val txHash: TxHash) : SubmitResult

    /** The contract refused it. [code] is the contract error, once INTERFACES.md lists them. */
    data class Rejected(val txHash: TxHash?, val code: Int?) : SubmitResult

    /** Never reached Stellar (timeout, no connection, RPC error). Nothing changed. */
    data class NetworkFailure(val message: String) : SubmitResult

    /** The person dismissed the signing prompt. Nothing was sent. */
    data object SigningCancelled : SubmitResult
}
