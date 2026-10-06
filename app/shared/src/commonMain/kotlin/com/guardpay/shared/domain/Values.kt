package com.guardpay.shared.domain

import kotlin.jvm.JvmInline

/** USDC in its smallest unit (7 decimals on Stellar). Never negative. */
@JvmInline
value class UsdcAmount(val units: Long) : Comparable<UsdcAmount> {
    init {
        require(units >= 0) { "amount must not be negative" }
    }

    val isPositive: Boolean get() = units > 0

    override fun compareTo(other: UsdcAmount): Int = units.compareTo(other.units)

    /** Null on overflow, mirroring the contract's checked sums. */
    fun plusOrNull(other: UsdcAmount): UsdcAmount? {
        val sum = units + other.units
        return if (sum < 0) null else UsdcAmount(sum)
    }

    companion object {
        const val UNITS_PER_USDC = 10_000_000L
        val ZERO = UsdcAmount(0)
        fun ofWhole(usdc: Long) = UsdcAmount(usdc * UNITS_PER_USDC)
    }
}

/**
 * A Stellar strkey: a `G…` account or a `C…` contract. Muxed (`M…`) addresses are
 * not accepted: the contract only matches plain addresses against trusted contacts.
 * This is a shape check only; the SDK and the contract are the real validators.
 */
@JvmInline
value class StellarAddress(val value: String) {
    init {
        require(isValid(value)) { "not a G or C strkey" }
    }

    val isContract: Boolean get() = value[0] == 'C'

    /** "GABC…WXYZ": first and last 4 characters, for the review sheet. */
    fun short(): String = "${value.take(4)}…${value.takeLast(4)}"

    companion object {
        private val shape = Regex("^[GC][A-Z2-7]{55}$")
        fun isValid(value: String): Boolean = shape.matches(value)
    }
}

/** Ledger close time in Unix seconds. All hold times come from the ledger, never from the phone. */
@JvmInline
value class LedgerTime(val epochSeconds: Long) : Comparable<LedgerTime> {
    override fun compareTo(other: LedgerTime): Int = epochSeconds.compareTo(other.epochSeconds)
}

/** A transaction hash as returned by the RPC (64 hex characters). Never made up. */
@JvmInline
value class TxHash(val hex: String) {
    init {
        require(Regex("^[0-9a-f]{64}$").matches(hex)) { "not a 32-byte hex hash" }
    }
}

/** One short neutral phrase from the AI card, e.g. "Pide urgencia". Display only. */
@JvmInline
value class RiskSignal(val text: String)

data class TrustedContact(val name: String, val address: StellarAddress)

/** Who stopped a held payment. The guardian can stop money but never move it. */
enum class Party { Owner, Guardian }

/** What the owner wants to pay. Built by the form, never by the AI. */
data class PaymentIntent(val destination: StellarAddress, val amount: UsdcAmount) {
    init {
        require(amount.isPositive) { "amount must be > 0" }
    }
}
