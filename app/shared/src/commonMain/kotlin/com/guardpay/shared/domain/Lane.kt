package com.guardpay.shared.domain

/**
 * Which way a payment will go. Informative only: the app predicts it so the owner
 * knows before signing; the contract is the authority and decides when she signs.
 */
enum class Lane { Immediate, MustHold }

/** The on-chain config the prediction uses, read from guardian_hold. */
data class LaneRules(
    val trustedContacts: List<TrustedContact>,
    val dailyCap: UsdcAmount,
    /** Spent today (UTC day by ledger time), as the contract counts it. */
    val spentToday: UsdcAmount,
)

sealed interface LanePrediction {
    val lane: Lane

    /** To a trusted contact within the cap. [remainingAfter] is what is left of today's cap. */
    data class Immediate(val contact: TrustedContact, val remainingAfter: UsdcAmount) : LanePrediction {
        override val lane get() = Lane.Immediate
    }

    data class MustHold(val reason: HoldReason) : LanePrediction {
        override val lane get() = Lane.MustHold
    }
}

enum class HoldReason {
    /** Not in the trusted list: the contract only lets it out after a hold. */
    NotTrustedContact,

    /** A trusted contact, but over today's cap: a direct transfer would be rejected. */
    OverDailyCap,

    /** The owner chose "Retener este pago" for a trusted contact. */
    OwnerChoseHold,
}

/**
 * Mirrors guardian_hold's trusted-contact rule: destination in the list, amount > 0
 * (guaranteed by [PaymentIntent]) and spentToday + amount <= dailyCap with a checked sum.
 */
fun predictLane(intent: PaymentIntent, rules: LaneRules, ownerChoseHold: Boolean = false): LanePrediction {
    val contact = rules.trustedContacts.firstOrNull { it.address == intent.destination }
        ?: return LanePrediction.MustHold(HoldReason.NotTrustedContact)
    if (ownerChoseHold) return LanePrediction.MustHold(HoldReason.OwnerChoseHold)

    val total = rules.spentToday.plusOrNull(intent.amount)
    if (total == null || total > rules.dailyCap) return LanePrediction.MustHold(HoldReason.OverDailyCap)

    return LanePrediction.Immediate(contact, UsdcAmount(rules.dailyCap.units - total.units))
}
