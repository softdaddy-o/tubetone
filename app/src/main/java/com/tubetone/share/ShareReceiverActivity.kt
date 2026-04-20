package com.tubetone.share

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.tubetone.core.MainActivity
import com.tubetone.extract.ExtractionForegroundService

/**
 * Handles SEND intents from external apps. Two shapes:
 *  1. text/plain containing a YouTube URL — feed to the existing extractor.
 *  2. audio or video stream (audio slash star / video slash star MIME) — feed
 *     the content URI to the local-file path. Added in v0.2.0 per L1.
 */
class ShareReceiverActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = intent ?: run { finish(); return }
        val type = intent.type?.lowercase().orEmpty()

        if (type.startsWith("audio/") || type.startsWith("video/")) {
            val uri: Uri? = intent.getParcelableExtra(Intent.EXTRA_STREAM)
            if (uri == null) {
                Toast.makeText(this, "파일을 찾지 못했습니다", Toast.LENGTH_LONG).show()
                finish(); return
            }
            runCatching {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            ExtractionForegroundService.startLocal(this, uri)
            startActivity(Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            })
            finish()
            return
        }

        val videoId = YoutubeUrlParser.extractVideoId(intent.getStringExtra(Intent.EXTRA_TEXT))
        if (videoId == null) {
            Toast.makeText(this, "유튜브 URL 또는 오디오/비디오 파일이 필요합니다", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        ExtractionForegroundService.start(this, videoId)
        startActivity(Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            putExtra("videoId", videoId)
        })
        finish()
    }
}
