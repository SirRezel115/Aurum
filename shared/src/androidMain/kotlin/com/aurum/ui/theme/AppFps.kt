package com.aurum.ui.theme

import android.content.Context
import android.hardware.display.DisplayManager
import android.view.Display

actual fun getInitialAppFps(): AppFps {
    return try {
        val ctx = com.aurum.data.repository.DeezerMusicProvider.applicationContext
        if (ctx != null) {
            val dm = ctx.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
            val display = dm?.getDisplay(Display.DEFAULT_DISPLAY)
            val refreshRate = display?.refreshRate ?: 60f
            when {
                refreshRate >= 110f -> AppFps.FPS_120
                refreshRate >= 85f -> AppFps.FPS_90
                refreshRate <= 45f -> AppFps.FPS_30
                else -> AppFps.FPS_60
            }
        } else AppFps.FPS_60
    } catch (_: Exception) {
        AppFps.FPS_60
    }
}
