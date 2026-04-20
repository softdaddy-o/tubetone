package com.tubetone.ringtone

import android.content.Context

/**
 * S5 — Remember last-used slot across app launches.
 *
 * Backed by SharedPreferences rather than DataStore to avoid adding a
 * dependency for a single int. Read is synchronous and cheap (no I/O the
 * first time; ~50 µs subsequent calls). Default = [RingtoneSlot.Ringtone].
 */
class SlotPreferences(context: Context) {
    private val prefs = context
        .applicationContext
        .getSharedPreferences("tubetone_slot_prefs", Context.MODE_PRIVATE)

    fun lastUsed(): RingtoneSlot {
        val code = prefs.getInt(KEY_LAST_SLOT_TYPE, RingtoneSlot.Ringtone.ringtoneManagerType)
        return RingtoneSlot.fromTypeCode(code)
    }

    fun setLastUsed(slot: RingtoneSlot) {
        prefs.edit().putInt(KEY_LAST_SLOT_TYPE, slot.ringtoneManagerType).apply()
    }

    companion object {
        private const val KEY_LAST_SLOT_TYPE = "last_used_slot_type"
    }
}
