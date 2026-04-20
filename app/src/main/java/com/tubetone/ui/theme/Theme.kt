package com.tubetone.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

/**
 * Root Material 3 theme for TubeTone. Dark-only in v0.2.0;
 * dynamic color toggle deferred to v1.3 per v1.2 spec.
 */
@Composable
fun TubeToneTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TubeToneDark,
        typography = TubeToneTypography,
        shapes = TubeToneShapes,
        content = content
    )
}
