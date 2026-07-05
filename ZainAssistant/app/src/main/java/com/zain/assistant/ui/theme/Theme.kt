package com.zain.assistant.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ZainDarkColors = darkColorScheme(
    primary = ElectricBlue,
    secondary = CyanGlow,
    background = DeepSpace,
    surface = SurfaceDark,
    onPrimary = TextPrimary,
    onSecondary = DeepSpace,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    error = DangerRed
)

// The assistant is designed around a dark, futuristic aesthetic. The "light" scheme
// is a softer variant used only when the user explicitly disables dark mode in Settings.
private val ZainLightColors = lightColorScheme(
    primary = ElectricBlue,
    secondary = CyanGlow,
    background = Color(0xFFF4F6FB),
    surface = Color(0xFFFFFFFF),
    onPrimary = Color(0xFFFFFFFF),
    onBackground = Color(0xFF10162A),
    onSurface = Color(0xFF10162A),
    error = DangerRed
)

@Composable
fun ZainAssistantTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) ZainDarkColors else ZainLightColors
    MaterialTheme(
        colorScheme = colorScheme,
        typography = ZainTypography,
        content = content
    )
}
