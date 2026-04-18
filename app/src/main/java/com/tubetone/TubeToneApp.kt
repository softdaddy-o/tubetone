package com.tubetone

import android.app.Application
import com.tubetone.core.cache.CachePolicy
import com.tubetone.library.RingtoneRepository
import com.tubetone.library.db.TubeToneDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File

class TubeToneApp : Application() {
    override fun onCreate() {
        super.onCreate()
        CoroutineScope(Dispatchers.IO).launch {
            // 1. clean unfinished downloads
            File(cacheDir, "extracts").listFiles()?.forEach { if (it.name.endsWith(".part")) it.delete() }
            // 2. enforce originals LRU
            val protectedPaths = RingtoneRepository(TubeToneDatabase.get(this@TubeToneApp).ringtoneDao())
                .all.first().mapNotNull { it.originalCachePath }.toSet()
            CachePolicy.enforce(File(filesDir, "originals"), 500L * 1024 * 1024, protectedPaths)
        }
    }
}
