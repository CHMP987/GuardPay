package com.guardpay.shared.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * The closed icon set of the visual proposal (section 5), plus the two navigation
 * glyphs every screen needs (back, expand). Path data generated from
 * lucide-static 1.52.0 (ISC, see app/shared/licenses/Lucide-ISC.txt); circles and
 * rects converted to paths. Stroke 1.75 on the 24 grid, as the proposal asks.
 * Forbidden by the proposal and absent here: shields, locks, robots, sparkles.
 */
object GpIcons {
    /** lucide `hourglass` */
    val Hourglass: ImageVector by lazy { lucide("hourglass", "M5 22h14", "M5 2h14", "M17 22v-4.172a2 2 0 0 0-.586-1.414L12 12l-4.414 4.414A2 2 0 0 0 7 17.828V22", "M7 2v4.172a2 2 0 0 0 .586 1.414L12 12l4.414-4.414A2 2 0 0 0 17 6.172V2") }
    /** lucide `clock-check` */
    val ClockCheck: ImageVector by lazy { lucide("clock-check", "M21.95 13a10 10 0 1 0-8.685 8.92", "M12 6v6l4 2", "m16 19 2 2 4-4") }
    /** lucide `check` */
    val Check: ImageVector by lazy { lucide("check", "M20 6 9 17l-5-5") }
    /** lucide `hand` */
    val Hand: ImageVector by lazy { lucide("hand", "M18 11V6a2 2 0 0 0-2-2a2 2 0 0 0-2 2", "M14 10V4a2 2 0 0 0-2-2a2 2 0 0 0-2 2v2", "M10 10.5V6a2 2 0 0 0-2-2a2 2 0 0 0-2 2v8", "M18 8a2 2 0 1 1 4 0v6a8 8 0 0 1-8 8h-2c-2.8 0-4.5-.86-5.99-2.34l-3.6-3.6a2 2 0 0 1 2.83-2.82L7 15") }
    /** lucide `ban` */
    val Ban: ImageVector by lazy { lucide("ban", "M2 12a10 10 0 1 0 20 0a10 10 0 1 0 -20 0", "M4.929 4.929 19.07 19.071") }
    /** lucide `wifi-off` */
    val WifiOff: ImageVector by lazy { lucide("wifi-off", "M12 20h.01", "M8.5 16.429a5 5 0 0 1 7 0", "M5 12.859a10 10 0 0 1 5.17-2.69", "M19 12.859a10 10 0 0 0-2.007-1.523", "M2 8.82a15 15 0 0 1 4.177-2.643", "M22 8.82a15 15 0 0 0-11.288-3.764", "m2 2 20 20") }
    /** lucide `user` */
    val User: ImageVector by lazy { lucide("user", "M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2", "M8 7a4 4 0 1 0 8 0a4 4 0 1 0 -8 0") }
    /** lucide `message-square-text` */
    val MessageSquareText: ImageVector by lazy { lucide("message-square-text", "M22 17a2 2 0 0 1-2 2H6.828a2 2 0 0 0-1.414.586l-2.202 2.202A.71.71 0 0 1 2 21.286V5a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2z", "M7 11h10", "M7 15h6", "M7 7h8") }
    /** lucide `external-link` */
    val ExternalLink: ImageVector by lazy { lucide("external-link", "M15 3h6v6", "M10 14 21 3", "M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6") }
    /** lucide `copy` */
    val Copy: ImageVector by lazy { lucide("copy", "M10 8h10a2 2 0 0 1 2 2v10a2 2 0 0 1 -2 2h-10a2 2 0 0 1 -2 -2v-10a2 2 0 0 1 2 -2z", "M4 16c-1.1 0-2-.9-2-2V4c0-1.1.9-2 2-2h10c1.1 0 2 .9 2 2") }
    /** lucide `timer-off` */
    val TimerOff: ImageVector by lazy { lucide("timer-off", "M10 2h4", "M4.6 11a8 8 0 0 0 1.7 8.7 8 8 0 0 0 8.7 1.7", "M7.4 7.4a8 8 0 0 1 10.3 1 8 8 0 0 1 .9 10.2", "m2 2 20 20", "M12 12v-2") }
    /** lucide `chevron-down` */
    val ChevronDown: ImageVector by lazy { lucide("chevron-down", "m6 9 6 6 6-6") }
    /** lucide `arrow-left` */
    val ArrowLeft: ImageVector by lazy { lucide("arrow-left", "m12 19-7-7 7-7", "M19 12H5") }
}

private fun lucide(name: String, vararg paths: String): ImageVector =
    ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
        paths.forEach { d ->
            addPath(
                pathData = addPathNodes(d),
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.75f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
    }.build()
