package com.aurum.aurum.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.widget.RemoteViews
import com.aurum.aurum.MainActivity
import com.aurum.aurum.R
import com.aurum.player.AurumMusicService
import com.aurum.player.MusicServiceHolder

@androidx.media3.common.util.UnstableApi
class AurumWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action ?: return

        when (action) {
            ACTION_WIDGET_PLAY_PAUSE,
            ACTION_WIDGET_NEXT,
            ACTION_WIDGET_PREVIOUS -> {
                Handler(Looper.getMainLooper()).post {
                    ensureMusicServiceRunning(context)
                    val player = MusicServiceHolder.sharedPlayer
                    if (player != null) {
                        when (action) {
                            ACTION_WIDGET_PLAY_PAUSE -> {
                                if (player.isPlaying) player.pause() else player.play()
                            }
                            ACTION_WIDGET_NEXT -> {
                                if (player.hasNextMediaItem()) player.seekToNextMediaItem()
                            }
                            ACTION_WIDGET_PREVIOUS -> {
                                if (player.hasPreviousMediaItem()) player.seekToPreviousMediaItem()
                            }
                        }
                    }
                    updateAllWidgets(context)
                }
            }
            ACTION_UPDATE_WIDGET -> {
                updateAllWidgets(context)
            }
        }
    }

    companion object {
        const val ACTION_WIDGET_PLAY_PAUSE = "com.aurum.aurum.WIDGET_PLAY_PAUSE"
        const val ACTION_WIDGET_NEXT = "com.aurum.aurum.WIDGET_NEXT"
        const val ACTION_WIDGET_PREVIOUS = "com.aurum.aurum.WIDGET_PREVIOUS"
        const val ACTION_UPDATE_WIDGET = "com.aurum.aurum.UPDATE_WIDGET"

        private fun ensureMusicServiceRunning(context: Context) {
            if (MusicServiceHolder.sharedPlayer == null) {
                try {
                    val serviceIntent = Intent(context, AurumMusicService::class.java)
                    androidx.core.content.ContextCompat.startForegroundService(context, serviceIntent)
                } catch (_: Exception) {}
            }
        }

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, AurumWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId)
            }
        }

        private fun applyDefaultOrDynamicColors(context: Context, views: RemoteViews, onAccent: (Int) -> Unit) {
            var accentColor = Color.parseColor("#C4CBD4")
            var bgColor = Color.parseColor("#0D0D0D")
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                try {
                    accentColor = context.getColor(android.R.color.system_accent1_300)
                    bgColor = context.getColor(android.R.color.system_neutral1_900)
                } catch (_: Exception) {}
            }
            views.setInt(R.id.widget_container, "setBackgroundColor", bgColor)
            views.setTextColor(R.id.widget_song_artist, accentColor)
            onAccent(accentColor)
        }

        private fun extractDominantColor(bmp: Bitmap): Int {
            var r = 0
            var g = 0
            var b = 0
            val width = bmp.width
            val height = bmp.height
            val pixelCount = width * height
            val pixels = IntArray(pixelCount)
            bmp.getPixels(pixels, 0, width, 0, 0, width, height)

            for (pixel in pixels) {
                r += (pixel shr 16) and 0xFF
                g += (pixel shr 8) and 0xFF
                b += pixel and 0xFF
            }
            r /= pixelCount
            g /= pixelCount
            b /= pixelCount

            r = (r * 0.35f).toInt()
            g = (g * 0.35f).toInt()
            b = (b * 0.35f).toInt()

            return Color.rgb(r.coerceIn(10, 45), g.coerceIn(10, 45), b.coerceIn(10, 45))
        }

        private fun extractAccentColor(bmp: Bitmap): Int {
            var r = 0
            var g = 0
            var b = 0
            val width = bmp.width
            val height = bmp.height
            val pixelCount = width * height
            val pixels = IntArray(pixelCount)
            bmp.getPixels(pixels, 0, width, 0, 0, width, height)

            for (pixel in pixels) {
                r += (pixel shr 16) and 0xFF
                g += (pixel shr 8) and 0xFF
                b += pixel and 0xFF
            }
            r /= pixelCount
            g /= pixelCount
            b /= pixelCount

            return Color.rgb(r.coerceIn(160, 255), g.coerceIn(140, 240), b.coerceIn(60, 220))
        }

        private fun generateWaveformProgressBitmap(progressRatio: Float, accentColor: Int): Bitmap {
            val width = 600
            val height = 72
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bitmap)

            val paint = android.graphics.Paint().apply {
                isAntiAlias = true
            }

            val centerY = height / 2f
            val maxAmp = height * 0.38f
            val barCount = 45
            val gap = 6f
            val barWidth = (width - (barCount - 1) * gap) / barCount
            val activeX = progressRatio * width

            // Track background
            paint.color = accentColor
            paint.alpha = 50
            canvas.drawRoundRect(0f, centerY - 3f, width.toFloat(), centerY + 3f, 3f, 3f, paint)

            // Waveform bars
            for (i in 0 until barCount) {
                val t = i.toFloat() / (barCount - 1).coerceAtLeast(1)
                val envelope = kotlin.math.sin(t * Math.PI.toFloat()).coerceIn(0.2f, 1.0f)
                val amp = envelope * maxAmp * (0.6f + 0.4f * kotlin.math.sin(t * 15f))
                val x = i * (barWidth + gap)
                val barH = (amp * 1.6f).coerceIn(6f, maxAmp * 1.6f)

                val isPlayed = x <= activeX
                paint.alpha = if (isPlayed) 255 else 90
                paint.color = accentColor

                canvas.drawRoundRect(
                    x,
                    centerY - barH / 2f,
                    x + barWidth,
                    centerY + barH / 2f,
                    3f,
                    3f,
                    paint
                )
            }

            // Playhead thumb dot orb
            paint.alpha = 255
            paint.color = Color.WHITE
            canvas.drawCircle(activeX, centerY, 10f, paint)
            paint.color = accentColor
            canvas.drawCircle(activeX, centerY, 7f, paint)

            return bitmap
        }

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_aurum_player)
            val player = MusicServiceHolder.sharedPlayer
            val prefs = context.getSharedPreferences("aurum_widget_cache", Context.MODE_PRIVATE)

            // Open app on song title click
            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openAppPendingIntent = PendingIntent.getActivity(
                context, 0, openAppIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_song_title, openAppPendingIntent)

            var accentColor = Color.parseColor("#C4CBD4")
            var title: String
            var artist: String
            var isPlaying: Boolean
            var posMs: Long
            var durMs: Long

            if (player?.currentMediaItem != null) {
                val mediaItem = player.currentMediaItem!!
                val meta = mediaItem.mediaMetadata
                title = meta.title?.toString() ?: "Aurum Track"
                artist = "${meta.artist ?: "Artist"} • ${meta.albumTitle ?: "Album"}"
                isPlaying = player.isPlaying
                posMs = player.currentPosition.coerceAtLeast(0L)
                durMs = player.duration.coerceAtLeast(1L)

                prefs.edit()
                    .putString("title", title)
                    .putString("artist", artist)
                    .putBoolean("is_playing", isPlaying)
                    .putLong("position", posMs)
                    .putLong("duration", durMs)
                    .apply()

                // Dynamic coloring from album art
                val artUri = meta.artworkUri
                if (artUri != null) {
                    try {
                        context.contentResolver.openInputStream(artUri)?.use { stream ->
                            val bmp = BitmapFactory.decodeStream(stream)
                            if (bmp != null) {
                                val bgColor = extractDominantColor(bmp)
                                accentColor = extractAccentColor(bmp)
                                views.setInt(R.id.widget_container, "setBackgroundColor", bgColor)
                                views.setTextColor(R.id.widget_song_artist, accentColor)
                            }
                        }
                    } catch (_: Exception) {
                        applyDefaultOrDynamicColors(context, views) { accentColor = it }
                    }
                } else {
                    applyDefaultOrDynamicColors(context, views) { accentColor = it }
                }
            } else {
                title = prefs.getString("title", "Aurum Music") ?: "Aurum Music"
                artist = prefs.getString("artist", "Tap to open player") ?: "Tap to open player"
                isPlaying = prefs.getBoolean("is_playing", false)
                posMs = prefs.getLong("position", 0L)
                durMs = prefs.getLong("duration", 1L)

                applyDefaultOrDynamicColors(context, views) { accentColor = it }
            }

            views.setTextViewText(R.id.widget_song_title, title)
            views.setTextViewText(R.id.widget_song_artist, artist)
            val playIcon = if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
            views.setImageViewResource(R.id.widget_btn_play_pause, playIcon)

            // Generate custom waveform progress bar bitmap matching app style
            val progressRatio = if (durMs > 0) (posMs.toFloat() / durMs.toFloat()).coerceIn(0f, 1f) else 0f
            val waveformBmp = generateWaveformProgressBitmap(progressRatio, accentColor)
            views.setImageViewBitmap(R.id.widget_waveform_image, waveformBmp)

            views.setTextViewText(R.id.widget_text_position, formatTimeMs(posMs))
            views.setTextViewText(R.id.widget_text_duration, formatTimeMs(durMs))

            // Play/Pause broadcast pending intent
            val playPauseIntent = Intent(context, AurumWidgetProvider::class.java).apply {
                action = ACTION_WIDGET_PLAY_PAUSE
            }
            views.setOnClickPendingIntent(
                R.id.widget_btn_play_pause,
                PendingIntent.getBroadcast(context, 10, playPauseIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            )

            // Next broadcast pending intent
            val nextIntent = Intent(context, AurumWidgetProvider::class.java).apply {
                action = ACTION_WIDGET_NEXT
            }
            views.setOnClickPendingIntent(
                R.id.widget_btn_next,
                PendingIntent.getBroadcast(context, 11, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            )

            // Previous broadcast pending intent
            val prevIntent = Intent(context, AurumWidgetProvider::class.java).apply {
                action = ACTION_WIDGET_PREVIOUS
            }
            views.setOnClickPendingIntent(
                R.id.widget_btn_prev,
                PendingIntent.getBroadcast(context, 12, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            )

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun formatTimeMs(ms: Long): String {
            val totalSeconds = (ms / 1000).coerceAtLeast(0)
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return "$minutes:${if (seconds < 10) "0" else ""}$seconds"
        }
    }
}
