package com.guardpay.shared.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.guardpay.shared.ui.theme.GpColor
import com.guardpay.shared.ui.theme.GpSize
import com.guardpay.shared.ui.theme.GpSpace
import com.guardpay.shared.ui.theme.LocalGpType
import com.guardpay.shared.ui.theme.rememberGpType

/** Breakpoints of section 10: <390, 390–767, 768–1023, ≥1024. */
@Immutable
data class GpLayout(
    val width: Dp,
    /** Side margin: 16 below 390, 20 from 390. */
    val margin: Dp,
    /** From 768: a 480 column, the main button is not fixed and sheets become dialogs. */
    val column: Boolean,
    /** From 1024: a 360 side panel shows "Ver en Stellar" expanded. */
    val sidePanel: Boolean,
)

val LocalGpLayout = staticCompositionLocalOf { layoutFor(360.dp) }

fun layoutFor(width: Dp) = GpLayout(
    width = width,
    margin = if (width < 390.dp) 16.dp else 20.dp,
    column = width >= 768.dp,
    sidePanel = width >= 1024.dp,
)

/** Measures the window and provides type and layout. Ice background. */
@Composable
fun GpTheme(content: @Composable () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize().background(GpColor.Ice)) {
        val layout = layoutFor(maxWidth)
        CompositionLocalProvider(
            LocalGpLayout provides layout,
            LocalGpType provides rememberGpType(wide = maxWidth >= 390.dp),
        ) { content() }
    }
}

/**
 * One screen: optional fixed top (simulation banner, guardian band), a scrolling body,
 * and an optional bottom action that is fixed on phones and inline from 768. From
 * 1024 [side] is shown in a 360 panel next to the column.
 */
@Composable
fun ScreenScaffold(
    top: @Composable ColumnScope.() -> Unit = {},
    bottom: (@Composable ColumnScope.() -> Unit)? = null,
    side: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val layout = LocalGpLayout.current
    Column(Modifier.fillMaxSize().background(GpColor.Ice)) {
        top()
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Column(
                Modifier
                    .then(if (layout.column) Modifier.widthIn(max = GpSize.column) else Modifier)
                    .weight(1f, fill = !layout.column)
                    .fillMaxHeight(),
            ) {
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = layout.margin, vertical = GpSpace.s24),
                    verticalArrangement = Arrangement.spacedBy(GpSpace.s24),
                ) {
                    content()
                    if (bottom != null && layout.column) {
                        Column(Modifier.padding(top = GpSpace.s24), verticalArrangement = Arrangement.spacedBy(GpSpace.s12), content = bottom)
                    }
                }
                if (bottom != null && !layout.column) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .background(GpColor.Ice)
                            .padding(horizontal = layout.margin, vertical = GpSpace.s16),
                        verticalArrangement = Arrangement.spacedBy(GpSpace.s12),
                        content = bottom,
                    )
                }
            }
            if (side != null && layout.sidePanel) {
                Column(
                    Modifier
                        .widthIn(max = GpSize.sidePanel)
                        .weight(1f, fill = false)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .padding(GpSpace.s24),
                    verticalArrangement = Arrangement.spacedBy(GpSpace.s16),
                    content = side,
                )
            }
        }
    }
}

/** Back arrow and screen title. */
@Composable
fun ScreenHeader(title: String, onBack: (() -> Unit)?) {
    Column(verticalArrangement = Arrangement.spacedBy(GpSpace.s8)) {
        if (onBack != null) {
            GpLink("Volver", onBack, icon = null)
        }
        Title(title)
    }
}

@Composable
fun CenteredBox(modifier: Modifier = Modifier, content: @Composable () -> Unit) =
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { content() }
