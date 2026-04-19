package com.tubetone.waveform

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WaveformSelectionTest {
    // Model: 120s total, 1s minGap, 30s max span  ->  minGapFrac ~= 0.00833, maxSpanFrac = 0.25
    private val minGap = 1_000f / 120_000f
    private val maxSpan = 30_000f / 120_000f

    @Test
    fun `withStart moves start without exceeding end`() {
        val s = WaveformSelection(0f, 0.25f)
        val next = s.withStart(0.1f, minGap, maxSpan)
        assertEquals(0.1f, next.startFrac, 1e-4f)
        assertEquals(0.25f, next.endFrac, 1e-4f)
    }

    @Test
    fun `withStart clamps to zero when going negative`() {
        val s = WaveformSelection(0.05f, 0.25f)
        val next = s.withStart(-0.2f, minGap, maxSpan)
        assertEquals(0f, next.startFrac, 1e-4f)
    }

    @Test
    fun `withStart pushes end right when min gap would be violated`() {
        val s = WaveformSelection(0f, 0.01f)
        // drag start to 0.2; end must follow so that gap stays >= minGapFrac
        val next = s.withStart(0.2f, minGap, maxSpan)
        assertEquals(0.2f, next.startFrac, 1e-4f)
        assertTrue("end should be pushed to keep min gap", next.endFrac >= 0.2f + minGap - 1e-4f)
    }

    @Test
    fun `withStart enforces max span by pulling end in`() {
        // start at 0, end at 0.5 (which is 60s on a 120s track — already over 30s cap),
        // moving start forward should trim end down to start + maxSpan.
        val s = WaveformSelection(0f, 0.5f)
        val next = s.withStart(0.1f, minGap, maxSpan)
        assertEquals(0.1f, next.startFrac, 1e-4f)
        assertTrue("end must not exceed start + maxSpan", next.endFrac <= 0.1f + maxSpan + 1e-4f)
    }

    @Test
    fun `withEnd moves end without crossing start`() {
        val s = WaveformSelection(0.1f, 0.2f)
        val next = s.withEnd(0.3f, minGap, maxSpan)
        assertEquals(0.1f, next.startFrac, 1e-4f)
        assertEquals(0.3f, next.endFrac, 1e-4f)
    }

    @Test
    fun `withEnd clamps to one`() {
        val s = WaveformSelection(0.1f, 0.2f)
        val next = s.withEnd(1.5f, minGap, maxSpan)
        assertEquals(1f, next.endFrac, 1e-4f)
    }

    @Test
    fun `withEnd enforces max span by pulling start forward`() {
        // start at 0, end dragged to 0.9 on 120s (=108s); must cap to 30s window.
        val s = WaveformSelection(0f, 0.1f)
        val next = s.withEnd(0.9f, minGap, maxSpan)
        assertEquals(0.9f, next.endFrac, 1e-4f)
        assertTrue("start must be pulled up to preserve maxSpan", next.startFrac >= 0.9f - maxSpan - 1e-4f)
    }

    @Test
    fun `withEnd keeps min gap when dragging end toward start`() {
        val s = WaveformSelection(0.2f, 0.5f)
        // drag end to 0.201 (too close to start)
        val next = s.withEnd(0.201f, minGap, maxSpan)
        assertTrue("gap must remain >= minGap", next.endFrac - next.startFrac >= minGap - 1e-4f)
    }
}
