package com.guardpay.shared.domain

/**
 * The nine payment states of the visual proposal (section 8). Only states the
 * contract can confirm are shown; "Retenido" exists only with an on-chain record,
 * which [Held] enforces by requiring a [HeldPayment] read from the registry.
 *
 * The "En Stellar" seal is not a state: it is [verifiedOnChain].
 */
sealed interface PaymentState {
    /** True when this state was read from the chain or comes with a transaction. */
    val verifiedOnChain: Boolean

    /** Sent, Stopped, Expired and Rejected never change again. */
    val isFinal: Boolean get() = false

    /** Borrador: only in the app. */
    data class Draft(val intent: PaymentIntent) : PaymentState {
        override val verifiedOnChain get() = false
    }

    /** Firmando…: the moment of the button, not a stored state. Remembers where to go back. */
    data class Signing(val origin: SigningOrigin) : PaymentState {
        override val verifiedOnChain get() = false
    }

    /** Retenido: a registry record in HELD, before readyAt. */
    data class Held(val hold: HeldPayment) : PaymentState {
        init {
            require(hold.status == HoldStatus.Held) { "Held needs a HELD record" }
        }
        override val verifiedOnChain get() = true
    }

    /** Listo para enviar: derived, now >= readyAt. Needs a new owner signature to leave. */
    data class ReadyToSend(val hold: HeldPayment) : PaymentState {
        init {
            require(hold.status == HoldStatus.Held) { "ReadyToSend needs a HELD record" }
        }
        override val verifiedOnChain get() = true
    }

    /**
     * Enviado. [txHash] may be unknown when the state is rebuilt from an EXECUTED
     * record (the registry does not store it); [hold] is null for a direct transfer.
     */
    data class Sent(val intent: PaymentIntent, val txHash: TxHash?, val hold: HeldPayment?) : PaymentState {
        override val verifiedOnChain get() = true
        override val isFinal get() = true
    }

    /** Detenido: a person decided. No retry. */
    data class Stopped(val hold: HeldPayment, val by: Party, val at: LedgerTime?) : PaymentState {
        override val verifiedOnChain get() = true
        override val isFinal get() = true
    }

    /** Vencido (SHOULD): matured but not sent before expiresAt. */
    data class Expired(val hold: HeldPayment) : PaymentState {
        override val verifiedOnChain get() = true
        override val isFinal get() = true
    }

    /** Rechazado por el contrato: the rule acted. Carries the seal only with a tx hash. */
    data class Rejected(val intent: PaymentIntent, val txHash: TxHash?) : PaymentState {
        override val verifiedOnChain get() = txHash != null
        override val isFinal get() = true
    }

    /** Error de red: it never reached Stellar, nothing changed. Retry goes back to signing. */
    data class NetworkError(val origin: SigningOrigin) : PaymentState {
        override val verifiedOnChain get() = false
    }
}

/** What a signature is for: a new payment (direct transfer or queue), or releasing a matured hold. */
sealed interface SigningOrigin {
    val intent: PaymentIntent

    data class NewPayment(override val intent: PaymentIntent) : SigningOrigin
    data class Release(val hold: HeldPayment) : SigningOrigin {
        override val intent get() = hold.intent
    }
}

/** Rebuilds the state of a hold from the registry and the ledger clock. */
fun stateOf(hold: HeldPayment, now: LedgerTime): PaymentState = when (hold.status) {
    HoldStatus.Executed -> PaymentState.Sent(hold.intent, txHash = null, hold = hold)
    HoldStatus.Cancelled -> PaymentState.Stopped(hold, hold.cancelledBy!!, hold.cancelledAt)
    HoldStatus.Held -> when {
        hold.isExpired(now) -> PaymentState.Expired(hold)
        hold.isMatured(now) -> PaymentState.ReadyToSend(hold)
        else -> PaymentState.Held(hold)
    }
}
