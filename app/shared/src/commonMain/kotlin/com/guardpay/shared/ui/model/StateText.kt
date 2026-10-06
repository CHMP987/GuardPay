package com.guardpay.shared.ui.model

import androidx.compose.ui.graphics.vector.ImageVector
import com.guardpay.shared.domain.Party
import com.guardpay.shared.domain.PaymentState
import com.guardpay.shared.domain.StellarAddress
import com.guardpay.shared.ui.theme.GpIcons
import kotlinx.datetime.TimeZone

/** How a chip is drawn. Each state also has its own icon and word, never color alone. */
enum class ChipTone { Warning, Success, DangerFilled, DangerOutline, Neutral }

data class ChipModel(val label: String, val tone: ChipTone, val icon: ImageVector)

/** Section 8 of the visual proposal. Borrador and Firmando… have no chip. */
fun chipFor(state: PaymentState): ChipModel? = when (state) {
    is PaymentState.Draft, is PaymentState.Signing -> null
    is PaymentState.Held -> ChipModel("Retenido", ChipTone.Warning, GpIcons.Hourglass)
    is PaymentState.ReadyToSend -> ChipModel("Listo para enviar", ChipTone.Warning, GpIcons.ClockCheck)
    is PaymentState.Sent -> ChipModel("Enviado", ChipTone.Success, GpIcons.Check)
    is PaymentState.Stopped -> ChipModel("Detenido", ChipTone.DangerFilled, GpIcons.Hand)
    is PaymentState.Expired -> ChipModel("Vencido", ChipTone.Neutral, GpIcons.TimerOff)
    is PaymentState.Rejected -> ChipModel("Rechazado por el contrato", ChipTone.DangerOutline, GpIcons.Ban)
    is PaymentState.NetworkError -> ChipModel("Error de red", ChipTone.Neutral, GpIcons.WifiOff)
}

/** Who is looking: the same state reads differently to the owner and to the guardian. */
data class Viewpoint(
    val viewer: Party,
    val ownerName: String,
    val guardianName: String,
    val tz: TimeZone,
    val nameOf: (StellarAddress) -> String,
)

/**
 * The one-line phrase under the chip. [sentAt] is known only for transfers this app
 * confirmed (the registry does not store it). [rejectedAfterStop] is true when the
 * registry shows the payment was stopped before the contract refused it.
 */
fun phraseFor(
    state: PaymentState,
    v: Viewpoint,
    sentAt: String? = null,
    rejectedAfterStop: Boolean = false,
): String = when (state) {
    is PaymentState.Draft -> "Este pago aún no se envió"
    is PaymentState.Signing -> "Esperando tu huella o PIN…"
    is PaymentState.Held -> {
        val by = if (v.viewer == Party.Guardian) "Puedes detenerlo." else "${v.guardianName} puede detenerlo."
        "Retenido hasta las ${state.hold.readyAt.clock(v.tz)}. $by"
    }
    is PaymentState.ReadyToSend ->
        if (v.viewer == Party.Owner) "Listo. Necesita tu firma para salir."
        else "Listo. Puedes detenerlo hasta que ${v.ownerName} lo envíe."
    is PaymentState.Sent -> "Enviado a ${v.nameOf(state.intent.destination)}" + (sentAt?.let { " · $it" } ?: "")
    is PaymentState.Stopped -> {
        val who = when {
            state.by == v.viewer -> "ti"
            state.by == Party.Guardian -> v.guardianName
            else -> v.ownerName
        }
        "Detenido por $who" + (state.at?.let { " a las ${it.clock(v.tz)}" } ?: "")
    }
    is PaymentState.Expired -> "Venció sin enviarse"
    is PaymentState.Rejected ->
        "El contrato no permitió este envío" + if (rejectedAfterStop) ": el pago fue detenido" else "."
    is PaymentState.NetworkError -> "No llegó a Stellar. Nada cambió."
}

/** Where the data on screen came from. The "En Stellar" seal needs [Chain]. */
enum class DataSource {
    /** Read from Stellar Testnet through KmpStellarGateway. */
    Chain,

    /** The in-memory simulation of the debug build. Nothing is on Stellar. */
    Simulation,
}

/** The seal rule of the proposal: only chain data, only when the last read worked. */
fun showSeal(state: PaymentState, source: DataSource, lastReadOk: Boolean): Boolean =
    state.verifiedOnChain && source == DataSource.Chain && lastReadOk
