package com.tubetone.ringtone

import android.content.Context
import android.content.Intent
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.provider.Settings

class SystemRingtoneApplier(private val context: Context) {

    fun canWriteSettings(): Boolean = Settings.System.canWrite(context)

    fun openWriteSettingsScreen() {
        context.startActivity(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    /** Kept for backwards compatibility with LibraryScreen. */
    fun setAsDefaultRingtone(uri: Uri) {
        setAsDefault(uri, RingtoneSlot.Ringtone)
    }

    fun setAsDefault(uri: Uri, slot: RingtoneSlot) {
        RingtoneManager.setActualDefaultRingtoneUri(context, slot.ringtoneManagerType, uri)
    }

    /**
     * Read the currently-applied system default for [slot], or null if none.
     * Used to populate [PriorUriCache] before overwriting so UNDO can restore.
     */
    fun currentDefault(slot: RingtoneSlot): Uri? =
        RingtoneManager.getActualDefaultRingtoneUri(context, slot.ringtoneManagerType)

    /**
     * Play the ringtone at [uri] (used by Snackbar PREVIEW action). Caller is
     * responsible for calling [stop] on the returned handle; return null if
     * [RingtoneManager] couldn't resolve the URI.
     */
    fun preview(uri: Uri): Ringtone? = runCatching {
        RingtoneManager.getRingtone(context, uri)?.apply { play() }
    }.getOrNull()
}
