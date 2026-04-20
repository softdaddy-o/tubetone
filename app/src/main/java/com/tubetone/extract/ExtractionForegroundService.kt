package com.tubetone.extract

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.tubetone.waveform.WaveformGenerator
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.atomic.AtomicReference

class ExtractionForegroundService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val localUri = intent?.getStringExtra(EXTRA_LOCAL_URI)
        val videoId = intent?.getStringExtra(EXTRA_VIDEO_ID)
        if (localUri != null) {
            startForeground(NOTI_ID, buildNotification("파일 불러오는 중...", null))
            scope.launch { runLocalIngest(android.net.Uri.parse(localUri)) }
            return START_STICKY
        }
        if (videoId == null) { stopSelf(); return START_NOT_STICKY }
        startForeground(NOTI_ID, buildNotification("정보 가져오는 중...", null))
        scope.launch { runExtraction(videoId) }
        return START_STICKY
    }

    private suspend fun runLocalIngest(uri: android.net.Uri) {
        try {
            updateState(ExtractionState.FetchingMetadata("local"))
            val ingested = LocalFileIngestor(this).ingest(uri)
            updateState(ExtractionState.AnalyzingWaveform(ingested.metadata))
            val wave = WaveformGenerator().generate(ingested.file, targetBuckets = 512)
            updateState(ExtractionState.Ready(ingested.metadata, ingested.file, wave, source = ExtractionSource.LOCAL))
        } catch (c: CancellationException) {
            updateState(ExtractionState.Failed(FailureReason.CANCELLED, c))
        } catch (t: LocalFileIngestor.UnsupportedTypeException) {
            updateState(ExtractionState.Failed(FailureReason.EXTRACTOR_BROKEN, t))
        } catch (t: Throwable) {
            updateState(ExtractionState.Failed(FailureReason.UNKNOWN, t))
        } finally {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private suspend fun runExtraction(videoId: String) {
        try {
            updateState(ExtractionState.FetchingMetadata(videoId))
            val meta = NewPipeExtractorService(OkHttpClient()).fetch(videoId)
            if (meta.durationMs > 30 * 60 * 1000) {
                updateState(ExtractionState.Failed(FailureReason.TOO_LONG, null))
                return
            }
            val audioFile = File(cacheDir, "extracts/$videoId.m4a").apply { parentFile?.mkdirs() }
            AudioDownloader(OkHttpClient()).download(meta.audioStreamUrl, audioFile).collect { progress ->
                updateState(ExtractionState.Downloading(meta, progress))
                notificationManager().notify(NOTI_ID, buildNotification(meta.title, progress))
            }
            updateState(ExtractionState.AnalyzingWaveform(meta))
            val wave = WaveformGenerator().generate(audioFile, targetBuckets = 512)
            updateState(ExtractionState.Ready(meta, audioFile, wave))
        } catch (c: CancellationException) {
            updateState(ExtractionState.Failed(FailureReason.CANCELLED, c))
        } catch (t: Throwable) {
            val reason = when {
                t is java.io.IOException -> FailureReason.NETWORK
                t.message?.contains("audio") == true -> FailureReason.EXTRACTOR_BROKEN
                else -> FailureReason.UNKNOWN
            }
            updateState(ExtractionState.Failed(reason, t))
        } finally {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun updateState(s: ExtractionState) { stateRef.set(s); _stateFlow.tryEmit(s) }

    private fun buildNotification(title: String, progress: Float?): Notification {
        ensureChannel()
        val b = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("TubeTone")
            .setContentText(title)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
        if (progress != null) b.setProgress(100, (progress * 100).toInt(), false)
        else b.setProgress(0, 0, true)
        return b.build()
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(CHANNEL_ID, "Extraction", NotificationManager.IMPORTANCE_LOW)
            notificationManager().createNotificationChannel(ch)
        }
    }

    private fun notificationManager() = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    override fun onDestroy() { scope.cancel(); super.onDestroy() }

    companion object {
        const val CHANNEL_ID = "tubetone.extraction"
        const val NOTI_ID = 1001
        const val EXTRA_VIDEO_ID = "videoId"
        const val EXTRA_LOCAL_URI = "localUri"
        private val stateRef = AtomicReference<ExtractionState>(ExtractionState.Idle)
        private val _stateFlow = MutableSharedFlow<ExtractionState>(replay = 1, extraBufferCapacity = 64)
        val state: StateFlow<ExtractionState> = _stateFlow.stateIn(
            CoroutineScope(Dispatchers.Default),
            SharingStarted.Eagerly,
            ExtractionState.Idle
        )

        fun start(context: Context, videoId: String) {
            val i = Intent(context, ExtractionForegroundService::class.java).apply { putExtra(EXTRA_VIDEO_ID, videoId) }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(i)
            else context.startService(i)
        }

        fun startLocal(context: Context, uri: android.net.Uri) {
            val i = Intent(context, ExtractionForegroundService::class.java).apply {
                putExtra(EXTRA_LOCAL_URI, uri.toString())
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(i)
            else context.startService(i)
        }

        fun reset() {
            stateRef.set(ExtractionState.Idle)
            _stateFlow.tryEmit(ExtractionState.Idle)
        }
    }
}
