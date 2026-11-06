package com.aurum.data.repository

actual object DeezerSessionManager {
    actual fun getSessionInfo(): DeezerSessionInfo? = null
    actual suspend fun ensureActiveSession(arlToken: String?): DeezerSessionInfo? = null
    actual suspend fun connect(arlToken: String): DeezerSessionInfo? = null
    actual suspend fun refreshSession(): DeezerSessionInfo? = null
    actual suspend fun fetchJwtToken(): String? = null
}
