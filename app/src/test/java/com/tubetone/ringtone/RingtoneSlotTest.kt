package com.tubetone.ringtone

import android.content.ContentValues
import android.media.RingtoneManager
import android.os.Environment
import android.provider.MediaStore
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RingtoneSlotTest {
    @Test fun `ringtoneManagerType maps to TYPE_RINGTONE`() {
        assertEquals(RingtoneManager.TYPE_RINGTONE, RingtoneSlot.Ringtone.ringtoneManagerType)
    }
    @Test fun `ringtoneManagerType maps to TYPE_NOTIFICATION`() {
        assertEquals(RingtoneManager.TYPE_NOTIFICATION, RingtoneSlot.Notification.ringtoneManagerType)
    }
    @Test fun `ringtoneManagerType maps to TYPE_ALARM`() {
        assertEquals(RingtoneManager.TYPE_ALARM, RingtoneSlot.Alarm.ringtoneManagerType)
    }

    @Test fun `relativePath maps to correct Environment dir`() {
        assertEquals(Environment.DIRECTORY_RINGTONES, RingtoneSlot.Ringtone.relativePath)
        assertEquals(Environment.DIRECTORY_NOTIFICATIONS, RingtoneSlot.Notification.relativePath)
        assertEquals(Environment.DIRECTORY_ALARMS, RingtoneSlot.Alarm.relativePath)
    }

    @Test fun `applyMediaStoreFlags sets only the ringtone flag for Ringtone slot`() {
        val v = ContentValues()
        RingtoneSlot.Ringtone.applyMediaStoreFlags(v)
        assertEquals(1, v.getAsInteger(MediaStore.Audio.Media.IS_RINGTONE))
        assertEquals(0, v.getAsInteger(MediaStore.Audio.Media.IS_NOTIFICATION))
        assertEquals(0, v.getAsInteger(MediaStore.Audio.Media.IS_ALARM))
        assertEquals(0, v.getAsInteger(MediaStore.Audio.Media.IS_MUSIC))
    }

    @Test fun `applyMediaStoreFlags sets only the notification flag for Notification slot`() {
        val v = ContentValues()
        RingtoneSlot.Notification.applyMediaStoreFlags(v)
        assertEquals(0, v.getAsInteger(MediaStore.Audio.Media.IS_RINGTONE))
        assertEquals(1, v.getAsInteger(MediaStore.Audio.Media.IS_NOTIFICATION))
        assertEquals(0, v.getAsInteger(MediaStore.Audio.Media.IS_ALARM))
    }

    @Test fun `applyMediaStoreFlags sets only the alarm flag for Alarm slot`() {
        val v = ContentValues()
        RingtoneSlot.Alarm.applyMediaStoreFlags(v)
        assertEquals(0, v.getAsInteger(MediaStore.Audio.Media.IS_RINGTONE))
        assertEquals(0, v.getAsInteger(MediaStore.Audio.Media.IS_NOTIFICATION))
        assertEquals(1, v.getAsInteger(MediaStore.Audio.Media.IS_ALARM))
    }

    @Test fun `korean labels are non-empty and distinct`() {
        val labels = RingtoneSlot.values().map { it.koreanLabel }
        assertEquals(labels.size, labels.toSet().size)
        labels.forEach { assertEquals(false, it.isBlank()) }
    }
}
