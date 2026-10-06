package com.guardpay.shared.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.guardpay.shared.ui.theme.GpColor
import com.guardpay.shared.ui.theme.GpSize
import com.guardpay.shared.ui.theme.GpSpace
import com.guardpay.shared.ui.theme.LocalGpType

@Composable
fun GpText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = GpColor.Navy,
    maxLines: Int = Int.MAX_VALUE,
    align: TextAlign? = null,
) {
    BasicText(
        text = text,
        modifier = modifier,
        style = if (align != null) style.copy(color = color, textAlign = align) else style.copy(color = color),
        maxLines = maxLines,
        overflow = if (maxLines == Int.MAX_VALUE) TextOverflow.Clip else TextOverflow.Ellipsis,
    )
}

@Composable
fun Title(text: String, modifier: Modifier = Modifier) =
    GpText(text, LocalGpType.current.title, modifier.semantics { heading() })

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) =
    GpText(text, LocalGpType.current.cardTitle, modifier.semantics { heading() })

@Composable
fun Body(text: String, modifier: Modifier = Modifier, color: Color = GpColor.Navy) =
    GpText(text, LocalGpType.current.body, modifier, color)

/** Secondary gray text: Navy 64 %, 5.5:1 on Ice. */
@Composable
fun Note(text: String, modifier: Modifier = Modifier) =
    GpText(text, LocalGpType.current.label.copy(fontWeight = FontWeight.Normal), modifier, GpColor.Navy64)

@Composable
fun GpIcon(vector: ImageVector, tint: Color, size: Dp = GpSize.icon, description: String? = null) {
    Image(vector, description, Modifier.size(size), colorFilter = ColorFilter.tint(tint))
}

/** White on Ice, 1 px Navy 10 % border, radius 12, no shadow. [leftBar] draws the lane or proof bar. */
@Composable
fun GpCard(
    modifier: Modifier = Modifier,
    leftBar: Color? = null,
    leftBarWidth: Dp = 4.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(GpSize.cardRadius)
    Row(
        modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .background(GpColor.White, shape)
            .border(1.dp, GpColor.Navy10, shape),
    ) {
        if (leftBar != null) {
            Box(
                Modifier
                    .width(leftBarWidth)
                    .fillMaxHeight()
                    .background(leftBar, RoundedCornerShape(topStart = GpSize.cardRadius, bottomStart = GpSize.cardRadius)),
            )
        }
        Column(Modifier.weight(1f).padding(GpSpace.s16), verticalArrangement = Arrangement.spacedBy(GpSpace.s8), content = content)
    }
}

enum class ButtonKind { Primary, Secondary, DangerOutline, DangerFilled }

/** Height 52, radius 12. Disabled: Navy 38 %. A 2 px focus ring for keyboards and switch access. */
@Composable
fun GpButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: ButtonKind = ButtonKind.Primary,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    val shape = RoundedCornerShape(GpSize.buttonRadius)
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val (fill, fg, border) = when {
        !enabled && kind == ButtonKind.Primary -> Triple(GpColor.Navy10, GpColor.Navy64, null)
        !enabled -> Triple(GpColor.White, GpColor.Navy64, GpColor.Navy38)
        kind == ButtonKind.Primary -> Triple(GpColor.Navy, GpColor.White, null)
        kind == ButtonKind.Secondary -> Triple(GpColor.White, GpColor.Navy, GpColor.Navy)
        kind == ButtonKind.DangerOutline -> Triple(GpColor.White, GpColor.Danger, GpColor.Danger)
        else -> Triple(GpColor.Danger, GpColor.White, null)
    }
    Box(
        modifier
            .fillMaxWidth()
            .height(GpSize.buttonHeight)
            .then(if (focused) Modifier.border(2.dp, GpColor.Navy, shape).padding(3.dp) else Modifier)
            .background(fill, shape)
            .then(if (border != null) Modifier.border(1.dp, border, shape) else Modifier)
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = GpSpace.s16),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(GpSpace.s8)) {
            if (icon != null) GpIcon(icon, fg)
            GpText(text, LocalGpType.current.body.copy(fontWeight = FontWeight.SemiBold), color = fg, maxLines = 2)
        }
    }
}

/** A text link with a 44 dp touch target. */
@Composable
fun GpLink(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = GpColor.Navy,
    icon: ImageVector? = null,
    description: String? = null,
) {
    Row(
        modifier
            .heightIn(min = GpSize.touchTarget)
            .clickable(role = Role.Button, onClick = onClick)
            .then(if (description != null) Modifier.semantics { contentDescription = description } else Modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(GpSpace.s4),
    ) {
        GpText(text, LocalGpType.current.body.copy(textDecoration = TextDecoration.Underline), color = color)
        if (icon != null) GpIcon(icon, color, 16.dp)
    }
}

/** Radius 10, height 52, border Navy 50 % (3.5:1), 2 px Navy when focused. */
@Composable
fun GpTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    mono: Boolean = false,
    big: Boolean = false,
    singleLine: Boolean = true,
    keyboard: KeyboardOptions = KeyboardOptions.Default,
    error: String? = null,
    placeholder: String? = null,
) {
    val type = LocalGpType.current
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val shape = RoundedCornerShape(GpSize.inputRadius)
    val style = when {
        big -> type.amount
        mono -> type.mono.copy(fontSize = type.body.fontSize, lineHeight = type.body.lineHeight)
        else -> type.body
    }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(GpSpace.s4)) {
        GpText(label, type.label)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = style,
            singleLine = singleLine,
            keyboardOptions = keyboard,
            interactionSource = interaction,
            cursorBrush = SolidColor(GpColor.Navy),
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = label },
            decorationBox = { inner ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = GpSize.inputHeight)
                        .background(GpColor.White, shape)
                        .border(if (focused) 2.dp else 1.dp, if (focused) GpColor.Navy else GpColor.Navy50, shape)
                        .padding(horizontal = GpSpace.s12, vertical = GpSpace.s12),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (value.isEmpty() && placeholder != null) GpText(placeholder, style, color = GpColor.Navy64)
                    inner()
                }
            },
        )
        if (error != null) GpText(error, type.label, color = GpColor.Danger)
    }
}

@Composable
fun VSpace(h: Dp) = Spacer(Modifier.height(h))

@Composable
fun HSpace(w: Dp) = Spacer(Modifier.width(w))
