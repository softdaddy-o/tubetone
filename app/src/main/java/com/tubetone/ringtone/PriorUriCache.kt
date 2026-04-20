package com.tubetone.ringtone

import android.net.Uri

/**
 * Remembers the system ringtone URI that was active per slot before TubeTone
 * overwrote it, so the user can press UNDO in the save Snackbar and restore
 * the previous setting. 30-second TTL matches the Material 3 action-carrying
 * Snackbar duration. Pure in-memory — no persistence by design (v0.2.0).
 */
class PriorUriCache(
    private val ttlMs: Long = 30_000L,
    private val clock: () -> Long = { System.currentTimeMillis() }
) {
    private data class Entry(val uri: Uri?, val storedAt: Long)

    private val map = mutableMapOf<RingtoneSlot, Entry>()

    fun remember(slot: RingtoneSlot, priorUri: Uri?) {
        map[slot] = Entry(priorUri, clock())
    }

    /** Returns the prior URI if stored within the TTL, else null. */
    fun take(slot: RingtoneSlot): Uri? {
        val e = map[slot] ?: return null
        if (clock() - e.storedAt > ttlMs) {
            map.remove(slot)
            return null
        }
        map.remove(slot)
        return e.uri
    }

    /** Peek without clearing — for tests / UI decisions. */
    fun peek(slot: RingtoneSlot): Uri? = map[slot]?.takeIf { clock() - it.storedAt <= ttlMs }?.uri
}
