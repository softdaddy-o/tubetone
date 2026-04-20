package com.tubetone.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

// TubeTone brand palette — dark-first, workshop-tool feel.
// Coral primary (YouTube-adjacent without plagiarizing), amber secondary (waveform
// highlight), cyan tertiary (info / source badge). See
// docs/superpowers/research/2026-04-19-tubetone-design-language.md for rationale.

val CoralPrimary = Color(0xFFFF5B5B)
val CoralOnPrimary = Color(0xFF390000)
val CoralContainer = Color(0xFF5C1010)
val AmberSecondary = Color(0xFFFFB86B)
val CyanTertiary = Color(0xFF7DD3FC)

val SurfaceNearBlack = Color(0xFF0E0E10)
val SurfaceVariantDark = Color(0xFF1C1C1F)
val SurfaceContainerDark = Color(0xFF141417)
val SurfaceContainerHighDark = Color(0xFF1A1A1E)
val OutlineDark = Color(0xFF2E2E33)
val BackgroundDark = Color(0xFF0A0A0C)

val TubeToneDark = darkColorScheme(
    primary = CoralPrimary,
    onPrimary = CoralOnPrimary,
    primaryContainer = CoralContainer,
    secondary = AmberSecondary,
    tertiary = CyanTertiary,
    surface = SurfaceNearBlack,
    surfaceVariant = SurfaceVariantDark,
    outline = OutlineDark,
    background = BackgroundDark
)
