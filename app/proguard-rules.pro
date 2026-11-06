# ── Koin ─────────────────────────────────────────
-keep class ** extends io.insert-koin.core.instance.InstanceInjector
-keep class * extends io.insert-koin.core.module.Module

# ── SQLDelight ───────────────────────────────────
-keep class ** extends com.aurum.database.AurumDatabase
-keepclassmembers class ** extends com.aurum.database.AurumDatabase {
    <methods>;
}

# ── Compose ──────────────────────────────────────
-keep class androidx.compose.material3.** { *; }
-keep class androidx.compose.foundation.** { *; }

# ── Media3 / ExoPlayer ───────────────────────────
-keep class androidx.media3.** { *; }

# ── Default rules ────────────────────────────────
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable

-verbose
