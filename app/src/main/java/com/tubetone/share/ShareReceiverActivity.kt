package com.tubetone.share

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.tubetone.core.MainActivity

class ShareReceiverActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sharedText = intent?.getStringExtra(Intent.EXTRA_TEXT)
        val videoId = YoutubeUrlParser.extractVideoId(sharedText)
        if (videoId == null) {
            Toast.makeText(this, "유튜브 URL을 찾지 못했습니다", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        startActivity(Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            putExtra("videoId", videoId)
        })
        finish()
    }
}
