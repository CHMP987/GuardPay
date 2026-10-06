package com.guardpay.shared.stellar

import com.guardpay.shared.domain.hash
import com.guardpay.shared.stellar.SubmitClassifier.AfterSend
import com.guardpay.shared.stellar.SubmitClassifier.PollStep
import com.soneso.stellar.sdk.rpc.responses.GetTransactionStatus
import com.soneso.stellar.sdk.rpc.responses.SendTransactionStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class SubmitClassifierTest {
    private val h = hash(1)

    @Test
    fun contractErrorCodeIsReadFromHostError() {
        assertEquals(7, SubmitClassifier.contractErrorCode("HostError: Error(Contract, #7)\nEvent log: ..."))
        assertNull(SubmitClassifier.contractErrorCode("HostError: Error(Auth, InvalidAction)"))
        assertNull(SubmitClassifier.contractErrorCode(null))
    }

    @Test
    fun simulationFailureIsARejectionWithoutHash() {
        assertEquals(SubmitResult.Rejected(null, 3), SubmitClassifier.simulationFailed("Error(Contract, #3)"))
        assertEquals(SubmitResult.Rejected(null, null), SubmitClassifier.simulationFailed("Error(Auth, InvalidAction)"))
    }

    @Test
    fun sendStatus() {
        assertEquals(AfterSend.Poll(h), SubmitClassifier.afterSend(SendTransactionStatus.PENDING, h))
        assertEquals(AfterSend.Poll(h), SubmitClassifier.afterSend(SendTransactionStatus.DUPLICATE, h))
        val error = SubmitClassifier.afterSend(SendTransactionStatus.ERROR, h)
        assertIs<SubmitResult.NetworkFailure>((error as AfterSend.Done).result)
        val busy = SubmitClassifier.afterSend(SendTransactionStatus.TRY_AGAIN_LATER, h)
        assertIs<SubmitResult.NetworkFailure>((busy as AfterSend.Done).result)
    }

    @Test
    fun finalStatusesCarryTheRealHash() {
        assertEquals(PollStep.Done(SubmitResult.Confirmed(h)), SubmitClassifier.afterPoll(GetTransactionStatus.SUCCESS, h, 10, 100))
        assertEquals(PollStep.Done(SubmitResult.Rejected(h, null)), SubmitClassifier.afterPoll(GetTransactionStatus.FAILED, h, 10, 100))
    }

    @Test
    fun notFoundIsFailureOnlyOnceTheWindowHasClosed() {
        assertEquals(PollStep.Again, SubmitClassifier.afterPoll(GetTransactionStatus.NOT_FOUND, h, 99, 100))
        assertEquals(PollStep.Again, SubmitClassifier.afterPoll(GetTransactionStatus.NOT_FOUND, h, 100, 100), "maxTime itself is still valid")
        assertEquals(PollStep.Again, SubmitClassifier.afterPoll(GetTransactionStatus.NOT_FOUND, h, null, 100))
        val closed = SubmitClassifier.afterPoll(GetTransactionStatus.NOT_FOUND, h, 101, 100)
        assertIs<SubmitResult.NetworkFailure>((closed as PollStep.Done).result)
    }
}
