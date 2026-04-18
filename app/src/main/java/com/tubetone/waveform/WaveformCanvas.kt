package com.tubetone.waveform

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

data class WaveformSelection(val startFrac: Float, val endFrac: Float) {
    init { require(startFrac in 0f..1f && endFrac in 0f..1f && startFrac < endFrac) }
}

@Composable
fun WaveformCanvas(
    samples: FloatArray,
    selection: WaveformSelection,
    onSelectionChange: (WaveformSelection) -> Unit,
    modifier: Modifier = Modifier,
    barColor: Color = Color(0xFF546E7A),
    selectedColor: Color = Color(0xFF1976D2),
    handleColor: Color = Color(0xFFFFA000)
) {
    var draggingHandle by remember { mutableStateOf<Handle?>(null) }
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp)
            .pointerInput(samples) {
                detectDragGestures(
                    onDragStart = { pos ->
                        val frac = (pos.x / size.width).coerceIn(0f, 1f)
                        draggingHandle = if (kotlin.math.abs(frac - selection.startFrac) <
                            kotlin.math.abs(frac - selection.endFrac)) Handle.Start else Handle.End
                    },
                    onDrag = { change, _ ->
                        val frac = (change.position.x / size.width).coerceIn(0f, 1f)
                        val next = when (draggingHandle) {
                            Handle.Start -> selection.copy(startFrac = frac.coerceAtMost(selection.endFrac - 0.01f))
                            Handle.End -> selection.copy(endFrac = frac.coerceAtLeast(selection.startFrac + 0.01f))
                            null -> selection
                        }
                        onSelectionChange(next)
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
        for (i in 0 until barCount) {
            val x = i * barWidth
            val amp = samples[i].coerceIn(0f, 1f)
            val half = amp * midY
            val color = if (x in startX..endX) selectedColor else barColor
            drawLine(color, Offset(x, midY - half), Offset(x, midY + half), strokeWidth = 1.5f)
        }
        drawLine(handleColor, Offset(startX, 0f), Offset(startX, size.height), strokeWidth = 4f)
        drawLine(handleColor, Offset(endX, 0f), Offset(endX, size.height), strokeWidth = 4f)
    }
}

private enum class Handle { Start, End }
