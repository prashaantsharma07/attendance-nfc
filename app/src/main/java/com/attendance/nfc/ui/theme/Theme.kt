package com.attendance.nfc.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val GlassDarkScheme = darkColorScheme(
    primary = AccentBlue,
    onPrimary = DeepBlack,
    secondary = AccentBlueBright,
    background = DeepBlack,
    onBackground = TextPrimary,
    surface = PanelBlack,
    onSurface = TextPrimary,
    error = ErrorRed
)

private val GlassTypography = Typography(
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 26.sp, color = TextPrimary),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 20.sp, color = TextPrimary),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = TextPrimary),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp, color = TextPrimary),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp, color = TextSecondary),
    labelLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp, color = TextPrimary)
)

@Composable
fun AttendanceNfcTheme(content: @Composable () -> Unit) {
    // Deliberately ignoring isSystemInDarkTheme(): this UI is dark/glass only.
    MaterialTheme(
        colorScheme = GlassDarkScheme,
        typography = GlassTypography,
        content = content
    )
}
