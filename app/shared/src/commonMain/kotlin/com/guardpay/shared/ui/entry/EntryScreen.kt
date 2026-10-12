package com.guardpay.shared.ui.entry

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.guardpay.shared.ui.components.Body
import com.guardpay.shared.ui.components.ButtonKind
import com.guardpay.shared.ui.components.GpButton
import com.guardpay.shared.ui.components.GpIcon
import com.guardpay.shared.ui.components.GpText
import com.guardpay.shared.ui.components.Note
import com.guardpay.shared.ui.components.ScreenScaffold
import com.guardpay.shared.ui.components.SimulationBanner
import com.guardpay.shared.ui.theme.GpColor
import com.guardpay.shared.ui.theme.GpIcons
import com.guardpay.shared.ui.theme.GpSize
import com.guardpay.shared.ui.theme.GpSpace
import com.guardpay.shared.ui.theme.LocalGpType

/**
 * A. Entrada. [onOwner] / [onGuardian] are null when this build has nothing to
 * connect to (release without contract addresses): the screen says so instead of
 * pretending to sign in. Only one null is a phone that holds one role (debug, two
 * phones): its other button is off and there is no note.
 */
@Composable
fun EntryScreen(simulation: Boolean, onOwner: (() -> Unit)?, onGuardian: (() -> Unit)?) {
    val type = LocalGpType.current
    ScreenScaffold(
        top = { if (simulation) SimulationBanner() },
        bottom = {
            GpButton("Entrar a mi cuenta", onOwner ?: {}, enabled = onOwner != null)
            GpButton("Soy guardián de alguien", onGuardian ?: {}, kind = ButtonKind.Secondary, enabled = onGuardian != null)
        },
    ) {
        GpText("GuardPay", type.cardTitle)
        Column(
            Modifier
                .fillMaxWidth()
                .background(GpColor.Navy, RoundedCornerShape(GpSize.cardRadius))
                .padding(GpSpace.s24),
        ) {
            GpText(
                "Paga al instante a quien conoces. A los demás, con una espera que tu guardián puede detener.",
                type.title, Modifier.semantics { heading() }, color = GpColor.White,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(GpSpace.s16)) {
            Point(GpIcons.Check, "Tus contactos: sale ahora")
            Point(GpIcons.Hourglass, "Desconocidos: espera y tu guardián puede detenerlo")
            Point(GpIcons.Hand, "Tu guardián no puede mover tu dinero")
        }
        if (onOwner == null && onGuardian == null) Note("Esta versión todavía no está conectada a Stellar.")
    }
}

@Composable
private fun Point(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(GpSpace.s12)) {
        GpIcon(icon, GpColor.Navy)
        Body(text)
    }
}
