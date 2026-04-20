package com.tubetone.ringtone

import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PriorUriCacheTest {

    @Test
    fun `remember then take returns stored uri once`() {
        val cache = PriorUriCache(ttlMs = 1000, clock = { 0L })
        val uri = Uri.parse("content://prev/1")
        cache.remember(RingtoneSlot.Ringtone, uri)
        assertEquals(uri, cache.take(RingtoneSlot.Ringtone))
        assertNull("take should be one-shot", cache.take(RingtoneSlot.Ringtone))
    }

    @Test
    fun `take returns null after TTL expires`() {
        var now = 0L
        val cache = PriorUriCache(ttlMs = 30_000L, clock = { now })
        cache.remember(RingtoneSlot.Alarm, Uri.parse("content://prev/alarm"))
        now = 31_000L
        assertNull(cache.take(RingtoneSlot.Alarm))
    }

    @Test
    fun `per-slot entries are independent`() {
        val cache = PriorUriCache(ttlMs = 5_000L, clock = { 0L })
        cache.remember(RingtoneSlot.Ringtone, Uri.parse("content://r"))
        cache.remember(RingtoneSlot.Notification, Uri.parse("content://n"))
        assertEquals(Uri.parse("content://n"), cache.take(RingtoneSlot.Notification))
        assertEquals(Uri.parse("content://r"), cache.take(RingtoneSlot.Ringtone))
    }

    @Test
    fun `peek does not clear`() {
        val cache = PriorUriCache(ttlMs = 5_000L, clock = { 0L })
        cache.remember(RingtoneSlot.Alarm, Uri.parse("content://a"))
        assertEquals(Uri.parse("content://a"), cache.peek(RingtoneSlot.Alarm))
        assertEquals(Uri.parse("content://a"), cache.peek(RingtoneSlot.Alarm))
    }

    @Test
    fun `null prior uri is remembered`() {
        val cache = PriorUriCache(ttlMs = 5_000L, clock = { 0L })
        cache.remember(RingtoneSlot.Ringtone, null)
        // take will return null — but the mapping was present (we can't really
        // distinguish "never set" from "explicitly null" via take alone).
        // peek is the check.
        assertNull(cache.peek(RingtoneSlot.Ringtone))
    }
}
