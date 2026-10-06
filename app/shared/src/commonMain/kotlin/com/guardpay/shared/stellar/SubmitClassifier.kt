package com.guardpay.shared.stellar

import com.guardpay.shared.domain.TxHash
import com.soneso.stellar.sdk.rpc.responses.GetTransactionStatus
import com.soneso.stellar.sdk.rpc.responses.SendTransactionStatus

/**
 * Pure mapping from RPC answers to [SubmitResult]. The rule throughout: report
 * "nothing changed" only when it is provably true. Once a transaction has been
 * handed to the network, only a final status or a closed validity window decides.
 */
internal object SubmitClassifier {
    private val contractError = Regex("""Error\(Contract, #(\d+)\)""")

    fun contractErrorCode(error: String?): Int? =
        error?.let { contractError.find(it)?.groupValues?.get(1)?.toIntOrNull() }

    /** The host ran the call in simulation and refused it. Nothing was sent. */
    fun simulationFailed(error: String): SubmitResult = SubmitResult.Rejected(null, contractErrorCode(error))

    sealed interface AfterSend {
        data class Poll(val hash: TxHash) : AfterSend
        data class Done(val result: SubmitResult) : AfterSend
    }

    fun afterSend(status: SendTransactionStatus, hash: TxHash): AfterSend = when (status) {
        SendTransactionStatus.PENDING, SendTransactionStatus.DUPLICATE -> AfterSend.Poll(hash)
        // Refused before reaching a ledger (fee, sequence, malformed): retryable, nothing changed.
        SendTransactionStatus.ERROR -> AfterSend.Done(SubmitResult.NetworkFailure("transaction refused by the network"))
        SendTransactionStatus.TRY_AGAIN_LATER -> AfterSend.Done(SubmitResult.NetworkFailure("network busy, try again"))
    }

    sealed interface PollStep {
        data object Again : PollStep
        data class Done(val result: SubmitResult) : PollStep
    }

    /**
     * [validUntil] is the transaction's maxTime. Ledger close times only grow, so
     * once the latest closed ledger is past it and the transaction is still not
     * found, it can never land.
     */
    fun afterPoll(status: GetTransactionStatus, hash: TxHash, latestCloseTime: Long?, validUntil: Long): PollStep =
        when (status) {
            GetTransactionStatus.SUCCESS -> PollStep.Done(SubmitResult.Confirmed(hash))
            // TODO(day 4): read the contract error code from the result meta.
            GetTransactionStatus.FAILED -> PollStep.Done(SubmitResult.Rejected(hash, null))
            GetTransactionStatus.NOT_FOUND ->
                if (latestCloseTime != null && latestCloseTime > validUntil) {
                    PollStep.Done(SubmitResult.NetworkFailure("not included before its time limit; it can no longer land"))
                } else {
                    PollStep.Again
                }
        }
}
