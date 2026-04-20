package com.tubetone.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Design-token spacing scale. Screen gutter = 16dp, section rhythm = 24dp,
 * waveform internal padding = 12dp. Use these instead of raw `.dp` literals
 * inside screens so future token edits stay centralized.
 */
object Spacing {
    val s4 = 4.dp
    val s8 = 8.dp
    val s12 = 12.dp
    val s16 = 16.dp
    val s24 = 24.dp
    val s32 = 32.dp
    val s48 = 48.dp

    /** Minimum hit-target for interactive elements (Fitts' law). */
    val hitTarget = 48.dp

    /** Waveform canvas height. */
    val waveformHeight = 160.dp
}
