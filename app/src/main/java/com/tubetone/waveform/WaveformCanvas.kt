package com.tubetone.waveform

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

data class WaveformSelection(val startFrac: Float, val endFrac: Float) {
    init { require(startFrac in 0f..1f && endFrac in 0f..1f && startFrac < endFrac) }

    /**
     * Return a new selection with the start handle moved to [newFrac],
     * clamped so that the window stays within [0, 1], keeps a minimum gap of
     * [minGapFrac], and never exceeds [maxSpanFrac] in span. The end handle
     * may be pushed right if [newFrac] would otherwise violate the min-gap.
     */
    fun withStart(newFrac: Float, minGapFrac: Float, maxSpanFrac: Float): WaveformSelection {
        val clampedStart = newFrac.coerceIn(0f, 1f - minGapFrac)
        val minEnd = clampedStart + minGapFrac
        val maxEnd = (clampedStart + maxSpanFrac).coerceAtMost(1f)
        val newEnd = endFrac.coerceIn(minEnd, maxEnd)
        return WaveformSelection(clampedStart, newEnd)
    }

    /**
     * Return a new selection with the end handle moved to [newFrac],
     * clamped so that the window stays within [0, 1], keeps a minimum gap of
     * [minGapFrac], and never exceeds [maxSpanFrac] in span. The start handle
     * may be pushed left if [newFrac] would otherwise violate the span cap.
     */
    fun withEnd(newFrac: Float, minGapFrac: Float, maxSpanFrac: Float): WaveformSelection {
        val clampedEnd = newFrac.coerceIn(minGapFrac, 1f)
        val maxStart = clampedEnd - minGapFrac
        val minStart = (clampedEnd - maxSpanFrac).coerceAtLeast(0f)
        val newStart = startFrac.coerceIn(minStart, maxStart)
        return WaveformSelection(newStart, clampedEnd)
    }
}

@Composable
fun WaveformCanvas(
    samples: FloatArray,
    selection: WaveformSelection,
    onSelectionChange: (WaveformSelection) -> Unit,
    modifier: Modifier = Modifier,
    totalDurationMs: Long = 0L,
    minSegmentMs: Long = 1_000L,
    maxSegmentMs: Long = 30_000L,
    zeroCrossingSnap: Boolean = true,
    barColor: Color = Color(0xFF546E7A),
    selectedColor: Color = Color(0xFF1976D2),
    handleColor: Color = Color(0xFFFFA000),
    clampedColor: Color = Color(0xFFFFC107)
) {
    val density = LocalDensity.current
    val hitZonePx = remember(density) { with(density) { 24.dp.toPx() } }
    val selectionState = rememberUpdatedState(selection)
    val minGapFrac = if (totalDurationMs > 0) (minSegmentMs.toFloat() / totalDurationMs).coerceIn(0.001f, 0.5f) else 0.01f
    val maxSpanFrac = if (totalDurationMs > 0) (maxSegmentMs.toFloat() / totalDurationMs).coerceIn(minGapFrac, 1f) else 1f
    var draggingHandle by remember { mutableStateOf<Handle?>(null) }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { pos ->
                        val current = selectionState.value
                        val startPx = current.startFrac * size.width
                        val endPx = current.endFrac * size.width
                        val dStart = kotlin.math.abs(pos.x - startPx)
                        val dEnd = kotlin.math.abs(pos.x - endPx)
                        // Prefer the handle whose hit zone was struck. If both are in range,
                        // pick the nearer one. If neither, fall back to nearest.
                        draggingHandle = when {
                            dStart <= hitZonePx && dEnd <= hitZonePx -> if (dStart <= dEnd) Handle.Start else Handle.End
                            dStart <= hitZonePx -> Handle.Start
                            dEnd <= hitZonePx -> Handle.End
                            dStart <= dEnd -> Handle.Start
                            else -> Handle.End
                        }
                    },
                    onDrag = { change, _ ->
                        val rawFrac = (change.position.x / size.width).coerceIn(0f, 1f)
                        val frac = if (zeroCrossingSnap && samples.isNotEmpty()) {
                            ZeroCrossingSnap.snapFrac(samples, rawFrac, windowRadius = 4)
                        } else rawFrac
                        val current = selectionState.value
                        val next = when (draggingHandle) {
                            Handle.Start -> current.withStart(frac, minGapFrac, maxSpanFrac)
                            Handle.End -> current.withEnd(frac, minGapFrac, maxSpanFrac)
                            null -> current
                        }
                        if (next != current) onSelectionChange(next)
                    },
                    onDragEnd = { draggingHandle = null },
                    onDragCancel = { draggingHandle = null }
                )
            }
    ) {
        if (samples.isEmpty()) return@Canvas
        val barCount = samples.size
        val barWidth = size.width / barCount
        val midY = size.height / 2f
        val startX = selection.startFrac * size.width
        val endX = selection.endFrac * size.width

        // S3: clamp visual feedback. If the selection span matches min or max
        // it means the user is pressed up against a clamp — brighten the
        // handles amber so the constraint is visible. Also used by TrimScreen
        // to fire a haptic tick on entry.
        val spanFrac = selection.endFrac - selection.startFrac
        val atMinClamp = kotlin.math.abs(spanFrac - minGapFrac) < 1e-3f
        val atMaxClamp = kotlin.math.abs(spanFrac - maxSpanFrac) < 1e-3f
        val clamped = atMinClamp || atMaxClamp
        val activeHandleColor = if (clamped) clampedColor else handleColor

        for (i in 0 until barCount) {
            val x = i * barWidth
            val amp = samples[i].coerceIn(0f, 1f)
            val half = amp * midY
            val color = if (x in startX..endX) selectedColor else barColor
            drawLine(color, Offset(x, midY - half), Offset(x, midY + half), strokeWidth = 1.5f)
        }
        val handleStroke = if (clamped) 6f else 4f
        drawLine(activeHandleColor, Offset(startX, 0f), Offset(startX, size.height), strokeWidth = handleStroke)
        drawLine(activeHandleColor, Offset(endX, 0f), Offset(endX, size.height), strokeWidth = handleStroke)
    }
}

private enum class Handle { Start, End }
