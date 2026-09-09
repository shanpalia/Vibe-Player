package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.R
import com.example.VibeApplication

@OptIn(UnstableApi::class)
class VibePlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null

    companion object {
        const val CHANNEL_ID = "vibe_playback_channel"
        const val NOTIFICATION_ID = 1001
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        val app = application as? VibeApplication
        val player = app?.playerManager?.exoPlayer
        if (player != null) {
            mediaSession = MediaSession.Builder(this, player).build()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Vibe Player Background Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Controls for background video and audio playback"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        mediaSession?.run {
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}
