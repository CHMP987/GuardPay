package com.guardpay.shared.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.guardpay.shared.ui.res.Res
import com.guardpay.shared.ui.res.ibm_plex_mono_regular
import com.guardpay.shared.ui.res.ibm_plex_sans_medium
import com.guardpay.shared.ui.res.ibm_plex_sans_regular
import com.guardpay.shared.ui.res.ibm_plex_sans_semibold
import org.jetbrains.compose.resources.Font

/** Type scale of the visual proposal. Nothing is smaller than 13. */
@Immutable
data class GpType(
    /** 40/48, 48/56 from 390 px; tabular figures. */
    val amount: TextStyle,
    /** Screen title, phrased as a question or a state: 24/32. */
    val title: TextStyle,
    val cardTitle: TextStyle,
    val body: TextStyle,
    val label: TextStyle,
    /** Verifiable data (addresses, hashes, ledger times). */
    val mono: TextStyle,
)

val LocalGpType = staticCompositionLocalOf<GpType> { error("GpTheme not set") }

@Composable
fun rememberGpType(wide: Boolean): GpType {
    val sans = FontFamily(
        Font(Res.font.ibm_plex_sans_regular, FontWeight.Normal),
        Font(Res.font.ibm_plex_sans_medium, FontWeight.Medium),
        Font(Res.font.ibm_plex_sans_semibold, FontWeight.SemiBold),
    )
    val mono = FontFamily(Font(Res.font.ibm_plex_mono_regular, FontWeight.Normal))
    fun s(size: Int, line: Int, w: FontWeight, f: FontFamily = sans, features: String? = null) = TextStyle(
        fontFamily = f,
        fontWeight = w,
        fontSize = size.sp,
        lineHeight = line.sp,
        color = GpColor.Navy,
        fontFeatureSettings = features,
        letterSpacing = TextUnit.Unspecified,
    )
    return GpType(
        amount = if (wide) s(48, 56, FontWeight.SemiBold, features = "tnum") else s(40, 48, FontWeight.SemiBold, features = "tnum"),
        title = s(24, 32, FontWeight.SemiBold),
        cardTitle = s(17, 24, FontWeight.SemiBold),
        body = s(16, 24, FontWeight.Normal),
        label = s(13, 18, FontWeight.Medium),
        mono = s(13, 20, FontWeight.Normal, mono),
    )
}
