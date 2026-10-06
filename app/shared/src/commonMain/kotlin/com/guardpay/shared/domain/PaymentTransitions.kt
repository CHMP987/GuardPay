package com.guardpay.shared.domain

/** Things that can happen to a payment. Chain events carry what was read from the chain. */
sealed interface PaymentEvent {
    /** The owner pressed "Firmar y …", or "Reintentar" after a network error. */
    data object SignRequested : PaymentEvent

    /** The owner dismissed the system signing dialog. Nothing was sent. */
    data object SigningCancelled : PaymentEvent

    /** A queue landed and the registry record was read back. */
    data class QueueConfirmed(val hold: HeldPayment) : PaymentEvent

    /** A transfer landed (direct to a contact, or the release of a matured hold). */
    data class TransferConfirmed(val txHash: TxHash) : PaymentEvent

    /** The contract refused the transaction. */
    data class ContractRejected(val txHash: TxHash?) : PaymentEvent

    /** The transaction never reached Stellar. */
    data object SubmissionFailed : PaymentEvent

    /** The owner closed the network error without retrying. */
    data object ErrorDismissed : PaymentEvent

    /** New ledger time: may mature or expire a hold. */
    data class ClockAdvanced(val now: LedgerTime) : PaymentEvent

    /** The registry shows the hold cancelled. */
    data class HoldCancelled(val by: Party, val at: LedgerTime?) : PaymentEvent
}

sealed interface Transition {
    data class Moved(val to: PaymentState) : Transition
    data class Invalid(val from: PaymentState, val event: PaymentEvent) : Transition
}

/**
 * The app-side state machine. It only allows what the contract allows; in particular
 * nothing leaves Sent or Stopped (or any other final state), and only an explicit
 * queue enters Held.
 */
fun PaymentState.on(event: PaymentEvent): Transition {
    val next: PaymentState? = when (this) {
        is PaymentState.Draft -> when (event) {
            PaymentEvent.SignRequested -> PaymentState.Signing(SigningOrigin.NewPayment(intent))
            else -> null
        }

        is PaymentState.Signing -> when (event) {
            PaymentEvent.SigningCancelled -> origin.resumeState()
            PaymentEvent.SubmissionFailed -> PaymentState.NetworkError(origin)
            is PaymentEvent.ContractRejected -> PaymentState.Rejected(origin.intent, event.txHash)
            is PaymentEvent.TransferConfirmed -> PaymentState.Sent(
                origin.intent, event.txHash, (origin as? SigningOrigin.Release)?.hold,
            )
            is PaymentEvent.QueueConfirmed -> {
                // Only a new payment can be queued, and the record must match what was signed.
                val o = origin
                if (o is SigningOrigin.NewPayment && event.hold.status == HoldStatus.Held && event.hold.intent == o.intent) {
                    PaymentState.Held(event.hold)
                } else {
                    null
                }
            }
            else -> null
        }

        is PaymentState.Held -> when (event) {
            is PaymentEvent.ClockAdvanced -> when {
                hold.isExpired(event.now) -> PaymentState.Expired(hold)
                hold.isMatured(event.now) -> PaymentState.ReadyToSend(hold)
                else -> this
            }
            is PaymentEvent.HoldCancelled -> PaymentState.Stopped(hold, event.by, event.at)
            else -> null
        }

        is PaymentState.ReadyToSend -> when (event) {
            PaymentEvent.SignRequested -> PaymentState.Signing(SigningOrigin.Release(hold))
            is PaymentEvent.ClockAdvanced -> if (hold.isExpired(event.now)) PaymentState.Expired(hold) else this
            is PaymentEvent.HoldCancelled -> PaymentState.Stopped(hold, event.by, event.at)
            else -> null
        }

        is PaymentState.NetworkError -> when (event) {
            PaymentEvent.SignRequested -> PaymentState.Signing(origin)
            // "Nada cambió": the payment goes back to where it was.
            PaymentEvent.ErrorDismissed -> origin.resumeState()
            else -> null
        }

        is PaymentState.Sent,
        is PaymentState.Stopped,
        is PaymentState.Expired,
        is PaymentState.Rejected -> null
    }
    return if (next == null) Transition.Invalid(this, event) else Transition.Moved(next)
}

private fun SigningOrigin.resumeState(): PaymentState = when (this) {
    is SigningOrigin.NewPayment -> PaymentState.Draft(intent)
    is SigningOrigin.Release -> PaymentState.ReadyToSend(hold)
}
