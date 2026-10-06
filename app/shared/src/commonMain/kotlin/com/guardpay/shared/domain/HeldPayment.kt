package com.guardpay.shared.domain

/** The three statuses the hold registry stores. Only moves from [Held] to a final one. */
enum class HoldStatus { Held, Executed, Cancelled }

/**
 * A hold record as read from the registry. Every field comes from the chain:
 * `createdAt`, `readyAt` and `expiresAt` are computed by the registry, never inputs.
 *
 * Field names follow the security spike; exact types are provisional until
 * INTERFACES.md is frozen (Sync 1).
 */
data class HeldPayment(
    val id: Long,
    val account: StellarAddress,
    val destination: StellarAddress,
    val amount: UsdcAmount,
    val createdAt: LedgerTime,
    val readyAt: LedgerTime,
    /** SHOULD in the contract design; null if the registry has no expiry. */
    val expiresAt: LedgerTime?,
    val status: HoldStatus,
    /** Set only when [status] is [HoldStatus.Cancelled]. */
    val cancelledBy: Party? = null,
    val cancelledAt: LedgerTime? = null,
) {
    init {
        require(amount.isPositive) { "held amount must be > 0" }
        require(readyAt > createdAt) { "readyAt must be after createdAt" }
        require(expiresAt == null || expiresAt > readyAt) { "expiresAt must be after readyAt" }
        require((status == HoldStatus.Cancelled) == (cancelledBy != null)) { "cancelledBy only on cancelled records" }
    }

    val intent: PaymentIntent get() = PaymentIntent(destination, amount)

    fun isMatured(now: LedgerTime): Boolean = now >= readyAt

    fun isExpired(now: LedgerTime): Boolean = expiresAt != null && now > expiresAt
}
