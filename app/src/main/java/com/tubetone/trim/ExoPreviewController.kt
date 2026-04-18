package com.tubetone.trim

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ClippingMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import java.io.File

class ExoPreviewController(context: Context) {
    private val player = ExoPlayer.Builder(context).build().apply {
        repeatMode = Player.REPEAT_MODE_ONE
        playWhenReady = false
    }
    private val dataFactory = DefaultDataSource.Factory(context)

    fun load(audio: File, startMs: Long, endMs: Long) {
        val media = MediaItem.fromUri(android.net.Uri.fromFile(audio))
        val base = ProgressiveMediaSource.Factory(dataFactory).createMediaSource(media)
        val clip = ClippingMediaSource(base, startMs * 1000L, endMs * 1000L)
        player.setMediaSource(clip)
        player.prepare()
    }

    fun updateRange(startMs: Long, endMs: Long, audio: File) {
        val wasPlaying = player.isPlaying
        load(audio, startMs, endMs)
        if (wasPlaying) player.play()
    }

    fun togglePlay() { if (player.isPlaying) player.pause() else player.play() }
    fun isPlaying() = player.isPlaying
    fun release() = player.release()
}
