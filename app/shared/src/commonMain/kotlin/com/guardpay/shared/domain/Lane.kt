package com.guardpay.shared.domain

/**
 * Lane the app expects a payment to take, computed before signing so the user
 * knows what will happen. Informative only: the contract is the authority.
 */
enum class Lane {
    /** Trusted contact under the daily cap: leaves in seconds. */
    Immediate,

    /** Any other destination: must be queued in HoldRegistry and wait. */
    MustHold,
}
