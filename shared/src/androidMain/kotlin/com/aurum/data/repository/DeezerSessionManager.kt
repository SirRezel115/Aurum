package com.aurum.data.repository

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URI

actual object DeezerSessionManager {
    private const val GATEWAY_URL = "https://www.deezer.com/ajax/gw-light.php"
    private const val ARL_LOGIN_URL = "https://auth.deezer.com/login/arl?jo=p&rto=c&i=c"
    private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36"

    @Volatile
    private var activeSession: DeezerSessionInfo? = null

    actual fun getSessionInfo(): DeezerSessionInfo? = activeSession

    actual suspend fun ensureActiveSession(arlToken: String?): DeezerSessionInfo? = withContext(Dispatchers.IO) {
        val cleanToken = arlToken?.trim()?.removeSurrounding("\"")?.removeSurrounding("'") ?: ""
        val session = activeSession
        if (session != null && session.checkForm.isNotBlank() && session.sid.isNotBlank()) {
            if (cleanToken.isNotBlank() && session.arl != cleanToken) {
                return@withContext connect(cleanToken)
            }
            return@withContext session
        }
        connect(cleanToken)
    }

    actual suspend fun connect(arlToken: String): DeezerSessionInfo? = withContext(Dispatchers.IO) {
        val cleanArl = arlToken.trim().removeSurrounding("\"").removeSurrounding("'")
        val isValidArlProvided = cleanArl.length >= 32

        // If no token is provided but an active authenticated session already exists, preserve it!
        val currentSession = activeSession
        if (!isValidArlProvided && currentSession != null && currentSession.userId != "0" && currentSession.arl.isNotBlank()) {
            Log.d("DeezerSessionManager", "Preserving active authenticated user session (${currentSession.userName})")
            return@withContext currentSession
        }

        try {
            val arlForHeader = if (isValidArlProvided) cleanArl else ""
            val sid = pingSession(arlForHeader) ?: ""
            val userDataJson = fetchUserData(arlForHeader, sid)

            if (userDataJson != null) {
                val results = userDataJson.optJSONObject("results")
                val checkForm = results?.optString("checkForm")
                    ?.takeIf { it.isNotBlank() && it != "null" }

                val userObj = results?.optJSONObject("USER")
                val userIdStr = userObj?.optString("USER_ID") ?: "0"
                val rawUserId = userIdStr.takeIf { it.isNotBlank() && it != "null" } ?: "0"

                val isUserAuthenticated = isValidArlProvided && rawUserId != "0"

                val userName = if (isUserAuthenticated) {
                    userObj?.optString("BLOG_NAME")
                        ?.takeIf { it.isNotBlank() && it != "null" }
                        ?: userObj?.optString("FIRSTNAME")
                        ?.takeIf { it.isNotBlank() && it != "null" }
                        ?: "Deezer User"
                } else {
                    "Deezer Guest"
                }

                val optionsObj = userObj?.optJSONObject("OPTIONS")
                val licenseToken = optionsObj?.optString("license_token")
                    ?.takeIf { it.isNotBlank() && it != "null" }

                val maxQuality = if (isUserAuthenticated) determineQuality(optionsObj) else DeezerAudioQuality.MP3_320

                if (!checkForm.isNullOrBlank()) {
                    val session = DeezerSessionInfo(
                        arl = arlForHeader,
                        sid = sid,
                        checkForm = checkForm,
                        licenseToken = licenseToken,
                        userId = rawUserId,
                        userName = userName,
                        maxQuality = maxQuality
                    )
                    activeSession = session
                    Log.d("DeezerSessionManager", "Active session initialized ($userName, ID: $rawUserId, Quality: ${maxQuality.name})")
                    return@withContext session
                }
            }
        } catch (e: Exception) {
            Log.e("DeezerSessionManager", "Error connecting Deezer session: ${e.message}")
        }

        activeSession = null
        null
    }

    actual suspend fun refreshSession(): DeezerSessionInfo? {
        val current = activeSession ?: return connect("")
        return connect(current.arl)
    }

    actual suspend fun fetchJwtToken(): String? = withContext(Dispatchers.IO) {
        val session = activeSession ?: return@withContext null
        if (session.arl.isBlank()) return@withContext null

        try {
            val url = URI(ARL_LOGIN_URL).toURL()
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.requestMethod = "POST"
            conn.setRequestProperty("User-Agent", USER_AGENT)
            conn.setRequestProperty("Cookie", "arl=${session.arl}")
            conn.setRequestProperty("Accept", "application/json")
            conn.setRequestProperty("Content-Length", "0")

            if (conn.responseCode == 200) {
                val jsonText = conn.inputStream.bufferedReader().use { it.readText() }
                val obj = runCatching { JSONObject(jsonText) }.getOrNull()
                val jwt = obj?.optString("jwt")
                if (!jwt.isNullOrBlank() && jwt != "null") {
                    return@withContext jwt
                }
            }
        } catch (e: Exception) {
            Log.e("DeezerSessionManager", "Error fetching JWT token: ${e.message}")
        }
        null
    }

    private fun pingSession(arl: String): String? {
        try {
            val urlString = "$GATEWAY_URL?method=deezer.ping&input=3&api_version=1.0&api_token="
            val conn = URI(urlString).toURL().openConnection() as HttpURLConnection
            conn.connectTimeout = 6000
            conn.readTimeout = 6000
            conn.setRequestProperty("User-Agent", USER_AGENT)
            if (arl.isNotBlank()) {
                conn.setRequestProperty("Cookie", "arl=$arl")
            }

            if (conn.responseCode == 200) {
                val jsonText = conn.inputStream.bufferedReader().use { it.readText() }
                val obj = runCatching { JSONObject(jsonText) }.getOrNull()
                val sessionVal = obj?.optJSONObject("results")?.optString("SESSION")
                if (!sessionVal.isNullOrBlank() && sessionVal != "null") {
                    return sessionVal
                }
            }
        } catch (_: Exception) {}
        return null
    }

    private fun fetchUserData(arl: String, sid: String): JSONObject? {
        try {
            val urlString = "$GATEWAY_URL?method=deezer.getUserData&input=3&api_version=1.0&api_token="
            val conn = URI(urlString).toURL().openConnection() as HttpURLConnection
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.requestMethod = "POST"
            conn.setRequestProperty("User-Agent", USER_AGENT)

            val cookieHeader = when {
                arl.isNotBlank() && sid.isNotBlank() -> "arl=$arl; sid=$sid"
                arl.isNotBlank() -> "arl=$arl"
                sid.isNotBlank() -> "sid=$sid"
                else -> ""
            }
            if (cookieHeader.isNotBlank()) {
                conn.setRequestProperty("Cookie", cookieHeader)
            }
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            conn.outputStream.use { it.write("{}".toByteArray(Charsets.UTF_8)) }

            if (conn.responseCode == 200) {
                val jsonText = conn.inputStream.bufferedReader().use { it.readText() }
                return runCatching { JSONObject(jsonText) }.getOrNull()
            }
        } catch (_: Exception) {}
        return null
    }

    private fun determineQuality(options: JSONObject?): DeezerAudioQuality {
        if (options == null) return DeezerAudioQuality.MP3_320

        val webLossless = options.optBoolean("web_lossless", false) || options.optInt("web_lossless", 0) != 0
        val mobileLossless = options.optBoolean("mobile_lossless", false) || options.optInt("mobile_lossless", 0) != 0

        if (webLossless || mobileLossless) {
            return DeezerAudioQuality.FLAC
        }

        val webHq = options.optBoolean("web_hq", false) || options.optInt("web_hq", 0) != 0
        val mobileHq = options.optBoolean("mobile_hq", false) || options.optInt("mobile_hq", 0) != 0

        if (webHq || mobileHq) {
            return DeezerAudioQuality.MP3_320
        }

        return DeezerAudioQuality.MP3_128
    }
}
