package com.aurum.data.repository

enum class DeezerAudioQuality(val formatName: String, val displayName: String) {
    FLAC("FLAC", "FLAC Lossless (16-bit 44.1kHz)"),
    MP3_320("MP3_320", "High Quality (320 kbps)"),
    MP3_128("MP3_128", "Standard (128 kbps)")
}

data class DeezerSessionInfo(
    val arl: String,
    val sid: String,
    val checkForm: String,
    val licenseToken: String?,
    val userId: String,
    val userName: String,
    val maxQuality: DeezerAudioQuality
)

expect object DeezerSessionManager {
    fun getSessionInfo(): DeezerSessionInfo?
    suspend fun ensureActiveSession(arlToken: String? = null): DeezerSessionInfo?
    suspend fun connect(arlToken: String): DeezerSessionInfo?
    suspend fun refreshSession(): DeezerSessionInfo?
    suspend fun fetchJwtToken(): String?
}
