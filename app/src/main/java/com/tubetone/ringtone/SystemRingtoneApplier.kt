package com.tubetone.ringtone

import android.content.Context
import android.content.Intent
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

    fun setAsDefaultRingtone(uri: Uri) {
        RingtoneManager.setActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE, uri)
    }
}
