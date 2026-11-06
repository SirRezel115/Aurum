package com.aurum.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

@androidx.media3.common.util.UnstableApi
object MusicServiceHolder {
    var serviceInstance: AurumMusicService? = null
    var sharedPlayer: ExoPlayer? = null
    var sharedMediaSession: MediaSession? = null
}

/**
 * Android MediaSessionService backed by ExoPlayer and Media3 MediaSession
 * ensuring fully integrated background playback and rich MediaStyle notification / lock screen controls.
 */
@androidx.media3.common.util.UnstableApi
class AurumMusicService : MediaSessionService() {

    var player: ExoPlayer? = null
        private set
    var mediaSession: MediaSession? = null
        private set

    private val widgetUpdateHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val widgetUpdateRunnable = object : Runnable {
        override fun run() {
            if (player?.isPlaying == true) {
                try {
                    val intent = Intent("com.aurum.aurum.UPDATE_WIDGET").apply {
                        setComponent(ComponentName(packageName, "com.aurum.aurum.widget.AurumWidgetProvider"))
                    }
                    sendBroadcast(intent)
                } catch (_: Exception) {}
            }
            widgetUpdateHandler.postDelayed(this, 1000L)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        val httpDataSourceFactory = androidx.media3.datasource.DefaultHttpDataSource.Factory()
            .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36")
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(20000)

        val dataSourceFactory = androidx.media3.datasource.DefaultDataSource.Factory(this, httpDataSourceFactory)
        val mediaSourceFactory = androidx.media3.exoplayer.source.DefaultMediaSourceFactory(this)
            .setDataSourceFactory(dataSourceFactory)

        val exoPlayer = ExoPlayer.Builder(this)
            .setMediaSourceFactory(mediaSourceFactory)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build()
        player = exoPlayer
        widgetUpdateHandler.post(widgetUpdateRunnable)

        val intent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = intent?.let {
            PendingIntent.getActivity(
                this,
                0,
                it,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val sessionBuilder = MediaSession.Builder(this, exoPlayer)
        pendingIntent?.let { sessionBuilder.setSessionActivity(it) }
        val session = sessionBuilder.build()
        mediaSession = session

        setMediaNotificationProvider(DefaultMediaNotificationProvider(this))
        addSession(session)

        MusicServiceHolder.sharedPlayer = exoPlayer
        MusicServiceHolder.sharedMediaSession = session
        MusicServiceHolder.serviceInstance = this

        // Immediately promote to foreground to satisfy Android startForegroundService() requirement
        val notification = Notification.Builder(this, "aurum_music_channel")
            .setContentTitle("Aurum Music")
            .setContentText("Background playback active")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(1, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
            } else {
                startForeground(1, notification)
            }
        } catch (e: Exception) {
            android.util.Log.e("AurumMusicService", "Failed startForeground with type, falling back", e)
            try {
                startForeground(1, notification)
            } catch (_: Exception) {}
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            "aurum_music_channel",
            "Aurum Music Playback",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(channel)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action != null) {
            val p = player ?: MusicServiceHolder.sharedPlayer
            if (p != null) {
                when (action) {
                    ACTION_WIDGET_PLAY_PAUSE -> {
                        if (p.isPlaying) p.pause() else p.play()
                    }
                    ACTION_WIDGET_NEXT -> {
                        if (p.hasNextMediaItem()) p.seekToNextMediaItem()
                    }
                    ACTION_WIDGET_PREVIOUS -> {
                        if (p.hasPreviousMediaItem()) p.seekToPreviousMediaItem()
                    }
                    ACTION_WIDGET_SHUFFLE -> {
                        p.shuffleModeEnabled = !p.shuffleModeEnabled
                    }
                    ACTION_WIDGET_REPEAT -> {
                        p.repeatMode = (p.repeatMode + 1) % 3
                    }
                }
                try {
                    val updateIntent = Intent("com.aurum.aurum.UPDATE_WIDGET")
                    sendBroadcast(updateIntent)
                } catch (_: Exception) {}
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        val prefs = getSharedPreferences("aurum_prefs", MODE_PRIVATE)
        val stopOnClose = prefs.getBoolean("stop_on_app_close", false)
        if (stopOnClose) {
            try {
                player?.stop()
                player?.release()
            } catch (_: Exception) {}
            player = null
            MusicServiceHolder.sharedPlayer = null
            MusicServiceHolder.sharedMediaSession = null
            MusicServiceHolder.serviceInstance = null
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                } else {
                    @Suppress("DEPRECATION")
                    stopForeground(true)
                }
            } catch (_: Exception) {}
            stopSelf()
        }
    }

    override fun onDestroy() {
        widgetUpdateHandler.removeCallbacks(widgetUpdateRunnable)
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        player = null
        MusicServiceHolder.sharedPlayer = null
        MusicServiceHolder.sharedMediaSession = null
        MusicServiceHolder.serviceInstance = null
        super.onDestroy()
    }

    companion object {
        const val ACTION_WIDGET_PLAY_PAUSE = "com.aurum.aurum.WIDGET_PLAY_PAUSE"
        const val ACTION_WIDGET_NEXT = "com.aurum.aurum.WIDGET_NEXT"
        const val ACTION_WIDGET_PREVIOUS = "com.aurum.aurum.WIDGET_PREVIOUS"
        const val ACTION_WIDGET_SHUFFLE = "com.aurum.aurum.WIDGET_SHUFFLE"
        const val ACTION_WIDGET_REPEAT = "com.aurum.aurum.WIDGET_REPEAT"
    }
}
