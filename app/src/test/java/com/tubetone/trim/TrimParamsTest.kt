package com.tubetone.trim

import org.junit.Assert.assertEquals
import org.junit.Test

class TrimParamsTest {
    @Test fun `converts ms to us correctly`() {
        val p = TrimParams("/in", "/out", 5_000, 35_000, false)
        assertEquals(5_000_000L, p.startUs)
        assertEquals(35_000_000L, p.endUs)
        assertEquals(30_000L, p.durationMs)
    }
    @Test(expected = IllegalArgumentException::class)
    fun `rejects zero-length range`() { TrimParams("/in", "/out", 1000, 1000, false) }
    @Test(expected = IllegalArgumentException::class)
    fun `rejects reversed range`() { TrimParams("/in", "/out", 2000, 1000, false) }
    @Test(expected = IllegalArgumentException::class)
    fun `rejects negative start`() { TrimParams("/in", "/out", -1, 1000, false) }
}
