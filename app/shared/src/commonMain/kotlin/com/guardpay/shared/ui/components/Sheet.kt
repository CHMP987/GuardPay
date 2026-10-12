package com.guardpay.shared.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.guardpay.shared.ui.theme.GpColor
import com.guardpay.shared.ui.theme.GpSize
import com.guardpay.shared.ui.theme.GpSpace

/**
 * Bottom sheet on phones (radius 20 on top, the only shadow in the app), centered
 * dialog from 768. The scrim does not dismiss it: a signature or a stop must be
 * left through a visible button. Draw it last inside the screen's Box.
 */
@Composable
fun GpSheet(title: String, content: @Composable ColumnScope.() -> Unit) {
    val layout = LocalGpLayout.current
    Box(Modifier.fillMaxSize(), contentAlignment = if (layout.column) Alignment.Center else Alignment.BottomCenter) {
        // The scrim swallows taps so nothing behind the sheet can be pressed. It is a sibling
        // of the sheet, not its parent: a clickable merges its descendants' semantics, and
        // merging paneTitle throws on iOS once accessibility (VoiceOver, XCUITest) reads the tree.
        Box(
            Modifier
                .matchParentSize()
                .background(GpColor.Scrim)
                .clickable(remember { MutableInteractionSource() }, indication = null) {},
        )
        val shape = if (layout.column) RoundedCornerShape(GpSize.sheetRadius)
        else RoundedCornerShape(topStart = GpSize.sheetRadius, topEnd = GpSize.sheetRadius)
        Column(
            Modifier
                .then(if (layout.column) Modifier.widthIn(max = GpSize.column) else Modifier.fillMaxWidth())
                .shadow(8.dp, shape, ambientColor = GpColor.Navy, spotColor = GpColor.Navy)
                .background(GpColor.White, shape)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = layout.margin, vertical = GpSpace.s24)
                .semantics { paneTitle = title },
            verticalArrangement = Arrangement.spacedBy(GpSpace.s16),
        ) {
            Title(title)
            content()
        }
    }
}
