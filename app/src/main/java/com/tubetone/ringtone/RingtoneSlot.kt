package com.tubetone.ringtone

import android.content.ContentValues
import android.media.RingtoneManager
import android.os.Environment
import android.provider.MediaStore

/**
 * Which "slot" the ringtone file should be written to and applied against.
 * Mapped to both MediaStore flags/relative-path and RingtoneManager TYPE_*.
 */
enum class RingtoneSlot {
    Ringtone, Notification, Alarm;

    val ringtoneManagerType: Int
        get() = when (this) {
            Ringtone -> RingtoneManager.TYPE_RINGTONE
            Notification -> RingtoneManager.TYPE_NOTIFICATION
            Alarm -> RingtoneManager.TYPE_ALARM
        }

    val relativePath: String
        get() = when (this) {
            Ringtone -> Environment.DIRECTORY_RINGTONES
            Notification -> Environment.DIRECTORY_NOTIFICATIONS
            Alarm -> Environment.DIRECTORY_ALARMS
        }

    /** Korean label used in the save sheet and snackbar messages. */
    val koreanLabel: String
        get() = when (this) {
            Ringtone -> "기본 벨소리"
            Notification -> "알림음"
            Alarm -> "알람음"
        }

    /**
     * Apply the IS_RINGTONE/IS_NOTIFICATION/IS_ALARM flags on a ContentValues
     * object — exactly one is 1, the other two are 0. Also clears IS_MUSIC.
     */
    fun applyMediaStoreFlags(values: ContentValues) {
        values.put(MediaStore.Audio.Media.IS_RINGTONE, if (this == Ringtone) 1 else 0)
        values.put(MediaStore.Audio.Media.IS_NOTIFICATION, if (this == Notification) 1 else 0)
        values.put(MediaStore.Audio.Media.IS_ALARM, if (this == Alarm) 1 else 0)
        values.put(MediaStore.Audio.Media.IS_MUSIC, 0)
    }
}
