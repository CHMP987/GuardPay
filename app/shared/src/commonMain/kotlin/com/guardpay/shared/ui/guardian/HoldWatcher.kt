package com.guardpay.shared.ui.guardian

import com.guardpay.shared.domain.HeldPayment
import com.guardpay.shared.domain.HoldStatus
import com.guardpay.shared.domain.LedgerTime
import com.guardpay.shared.domain.StellarAddress
import com.guardpay.shared.ui.model.clock
import com.guardpay.shared.ui.model.usdc
import kotlinx.datetime.TimeZone

/**
 * A system notification for one held payment. Built only from the record read from
 * the registry in that same [HoldWatcher.check]: never from a payload or a cache.
 */
data class HoldAlert(
    val holdId: Long,
    val title: String,
    val text: String,
    /** The registry's own creation time, to measure how late the alert is. */
    val createdAt: LedgerTime,
)

/**
 * P8: tells the guardian that a payment is waiting, with no backend. The platform
 * calls [check] on a timer and posts each [HoldAlert] as a local notification.
 *
 * It only reads ([GuardianChain.readHolds]); it cannot sign or cancel. A failed
 * read throws, so it never looks like "nothing new".
 */
class HoldWatcher(
    private val chain: GuardianChain,
    alreadyAlerted: Set<Long> = emptySet(),
    private val tz: TimeZone = TimeZone.currentSystemDefault(),
) {
    private val alerted = alreadyAlerted.toMutableSet()

    /** Ids already alerted, for the platform to keep across restarts. */
    val alertedIds: Set<Long> get() = alerted.toSet()

    /** Reads the registry once and returns an alert for each `Held` record not alerted before. */
    suspend fun check(): List<HoldAlert> {
        val fresh = chain.readHolds().filter { it.status == HoldStatus.Held && it.id !in alerted }.sortedBy { it.id }
        alerted += fresh.map { it.id }
        return fresh.map(::alertFor)
    }

    private fun alertFor(h: HeldPayment) = HoldAlert(
        holdId = h.id,
        title = "Pago retenido: ${h.amount.usdc()}",
        text = "A ${full(h.destination)}. Puedes detenerlo hasta las ${h.readyAt.clock(tz)}.",
        createdAt = h.createdAt,
    )

    /** The whole address, as on Detalle: a short form would let a look-alike pass. */
    private fun full(a: StellarAddress) = a.value
}
