package com.tubetone.library.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [RingtoneEntity::class], version = 1, exportSchema = true)
abstract class TubeToneDatabase : RoomDatabase() {
    abstract fun ringtoneDao(): RingtoneDao

    companion object {
        @Volatile private var instance: TubeToneDatabase? = null
        fun get(context: Context): TubeToneDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext, TubeToneDatabase::class.java, "tubetone.db"
                ).build().also { instance = it }
            }
    }
}
