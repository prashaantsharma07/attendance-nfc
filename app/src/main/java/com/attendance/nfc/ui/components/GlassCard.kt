package com.attendance.nfc.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.attendance.nfc.ui.theme.AccentBlue
import com.attendance.nfc.ui.theme.GlassBorder
import com.attendance.nfc.ui.theme.GlassFillBottom
import com.attendance.nfc.ui.theme.GlassFillTop

/**
 * The core "glass panel" building block used everywhere: a translucent
 * gradient fill over a soft blue border, on rounded corners.
 *
 * This fakes glassmorphism with layered translucency + border rather than
 * true backdrop blur. For real blur-behind-the-card on API 31+, wrap the
 * background content in Modifier.blur() or pull in a library like "Haze" —
 * left out here to keep the module dependency-free.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Int = 20,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val clickModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = interactionSource,
            indication = rememberRipple(color = AccentBlue),
            onClick = onClick
        )
    } else Modifier

    Column(
        modifier = modifier
            .clip(shape)
            .background(Brush.verticalGradient(listOf(GlassFillTop, GlassFillBottom)))
            .border(1.dp, GlassBorder, shape)
            .then(clickModifier)
            .fillMaxWidth()
            .padding(18.dp),
        content = content
    )
}
