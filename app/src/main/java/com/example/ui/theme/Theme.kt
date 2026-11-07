package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SkillConnectDarkColorScheme = darkColorScheme(
    primary = CyanBrand,
    onPrimary = Color(0xFF001F2B),
    primaryContainer = Color(0xFF004D65),
    onPrimaryContainer = Color(0xFFBCE9FF),
    secondary = GreenBrand,
    onSecondary = Color(0xFF00210E),
    secondaryContainer = Color(0xFF005327),
    onSecondaryContainer = Color(0xFF8CF8AC),
    tertiary = BlueBrand,
    onTertiary = Color.White,
    background = NavyBackground,
    onBackground = TextWhite,
    surface = NavySurface,
    onSurface = TextWhite,
    surfaceVariant = NavySurfaceVariant,
    onSurfaceVariant = TextLight,
    outline = NavyCardBorder,
    outlineVariant = Color(0x1A00D2FF),
    error = ErrorRed,
    onError = Color.White
)

private val SkillConnectLightColorScheme = lightColorScheme(
    primary = CyanBrandLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC7F3FF),
    onPrimaryContainer = Color(0xFF003746),
    secondary = GreenBrandLight,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFBEF6CD),
    onSecondaryContainer = Color(0xFF003914),
    tertiary = BlueBrand,
    onTertiary = Color.White,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    outline = LightCardBorder,
    outlineVariant = Color(0x1A0084FF),
    error = ErrorRed,
    onError = Color.White
)

@Composable
fun SkillConnectTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) SkillConnectDarkColorScheme else SkillConnectLightColorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    SkillConnectTheme(darkTheme = darkTheme, content = content)
}
