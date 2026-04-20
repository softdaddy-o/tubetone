package com.tubetone.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Inter (UI) + JetBrains Mono (timecodes / durations). We use the platform
// SansSerif + Monospace families as fallbacks for v0.2.0 — shipping the
// licensed font files is a follow-up that doesn't block the wedge.
private val UiFont: FontFamily = FontFamily.SansSerif
private val MonoFont: FontFamily = FontFamily.Monospace

val TubeToneTypography = Typography(
    displaySmall = TextStyle(fontFamily = UiFont, fontWeight = FontWeight.SemiBold, fontSize = 36.sp, lineHeight = 44.sp),
    headlineMedium = TextStyle(fontFamily = UiFont, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 36.sp),
    titleLarge = TextStyle(fontFamily = UiFont, fontWeight = FontWeight.Medium, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = UiFont, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 24.sp),
    bodyLarge = TextStyle(fontFamily = UiFont, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = UiFont, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontFamily = UiFont, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    // labelSmall renders timecodes and durations — monospace for instrument-grade feel.
    labelSmall = TextStyle(fontFamily = MonoFont, fontWeight = FontWeight.Normal, fontSize = 11.sp, lineHeight = 16.sp)
)

// Convenience style explicitly used for `00:12.34` / duration chip readouts.
val MonoTimecode: TextStyle = TextStyle(
    fontFamily = MonoFont,
    fontWeight = FontWeight.Normal,
    fontSize = 13.sp,
    lineHeight = 18.sp
)
