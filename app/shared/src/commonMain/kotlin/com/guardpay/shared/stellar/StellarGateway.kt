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
 *
 * Reads throw on failure (`ChainReadException`, `ChainDecodeException`); they
 * never return a guessed value. Submits return a [SubmitResult], except when the
 * outcome cannot be known: then [SubmissionOutcomeUnknownException].
 */
interface StellarGateway {
    suspend fun latestLedgerTime(): LedgerTime

    suspend fun readAccountRules(account: StellarAddress): AccountRules
    suspend fun readBalance(account: StellarAddress): UsdcAmount
    suspend fun readTrustedContacts(account: StellarAddress): List<TrustedContact>
    suspend fun readDailySpent(account: StellarAddress): UsdcAmount
    suspend fun readHolds(account: StellarAddress): List<HeldPayment>

    /**
     * PROVISIONAL (no ABI yet): the account's context rules, signers and policies,
     * for "Reglas de esta cuenta". Which OZ view functions return them is decided
     * by INTERFACES.md; until then the real gateway throws [ChainReadException].
     */
    suspend fun readAccountShape(account: StellarAddress): AccountShape

    /** `HoldRegistry.queue(account, token, destination, amount)`, authorized by the account, signed by the owner. */
    suspend fun submitQueue(account: StellarAddress, intent: PaymentIntent, owner: Signer): SubmitResult

    /**
     * `HoldRegistry.cancel(caller, id)`: caller is the guardian's G account when
     * [signer] holds the guardian key (classic auth), otherwise the account itself.
     */
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

/**
 * What the account contract itself says about its rules. [signers] are the ed25519
 * signer keys as G strkeys (see [ed25519AccountId]), so the UI can compare them with
 * the owner's key and the guardian's address without decoding anything.
 */
data class AccountShape(
    val contextRuleCount: Int,
    val signers: List<StellarAddress>,
    val policies: List<StellarAddress>,
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

/**
 * The transaction was handed to the network and its fate could not be confirmed
 * (the RPC stopped answering). It may still land. The caller must NOT retry or
 * report "nothing changed": re-read balance and holds from the chain instead,
 * since a blind retry could pay twice. PROVISIONAL: may become a [SubmitResult]
 * variant once INTERFACES.md and the UI states settle it.
 */
class SubmissionOutcomeUnknownException(val txHash: TxHash, cause: Throwable?) :
    Exception("outcome of ${txHash.hex} unknown", cause)
