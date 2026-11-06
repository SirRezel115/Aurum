package com.aurum.ui.theme

enum class AppFps(val targetFps: Int, val displayName: String) {
    FPS_30(30, "30 FPS (Battery Saver)"),
    FPS_60(60, "60 FPS (Balanced)"),
    FPS_90(90, "90 FPS (Smooth)"),
    FPS_120(120, "120 FPS (Ultra Smooth)")
}

expect fun getInitialAppFps(): AppFps
