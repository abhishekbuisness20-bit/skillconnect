package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// SkillConnect Brand Palette (Exact Hex Values from Specification)
val SkillNavy = Color(0xFF14213D)
val SkillBlue = Color(0xFF2563EB)
val SkillPurple = Color(0xFF7C3AED)
val SkillCyan = Color(0xFF06B6D4)
val SkillSuccessGreen = Color(0xFF16A34A)
val SkillWarningAmber = Color(0xFFF59E0B)
val SkillErrorRed = Color(0xFFDC2626)

// Light Mode Surfaces & Text
val LightMainBackground = Color(0xFFF8FAFC)
val LightCardBackground = Color(0xFFFFFFFF)
val LightPrimaryText = Color(0xFF0F172A)
val LightSecondaryText = Color(0xFF64748B)
val LightBorderColor = Color(0xFFE2E8F0)
val LightMutedBackground = Color(0xFFF1F5F9)

// Dark Mode Surfaces & Text
val DarkMainBackground = Color(0xFF0B1220)
val DarkSurface = Color(0xFF111827)
val DarkElevatedSurface = Color(0xFF1F2937)
val DarkPrimaryText = Color(0xFFF8FAFC)
val DarkSecondaryText = Color(0xFFCBD5E1)
val DarkBorderColor = Color(0xFF334155)

// Brand Linear Gradient (#2563EB to #7C3AED)
val SkillBrandGradient = Brush.linearGradient(
    colors = listOf(SkillBlue, SkillPurple)
)

val SkillNavyGradient = Brush.linearGradient(
    colors = listOf(SkillNavy, Color(0xFF1E3A8A))
)
