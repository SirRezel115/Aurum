package com.aurum.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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

        val currentSession = activeSession
        if (!isValidArlProvided && currentSession != null && currentSession.userId != "0" && currentSession.arl.isNotBlank()) {
            return@withContext currentSession
        }

        try {
            val sid = pingSession(cleanArl) ?: ""
            val userDataJson = fetchUserData(cleanArl, sid) ?: return@withContext null

            val checkForm = extractJsonField(userDataJson, "\"checkForm\":\"")
            if (checkForm.isBlank() || checkForm == "null") return@withContext null

            val userId = extractJsonField(userDataJson, "\"USER_ID\":\"").ifBlank {
                extractJsonField(userDataJson, "\"USER_ID\":")
            }
            if (userId.isBlank() || userId == "0" || userId == "null") return@withContext null

            val blogName = extractJsonField(userDataJson, "\"BLOG_NAME\":\"").ifBlank {
                extractJsonField(userDataJson, "\"FIRSTNAME\":\"")
            }
            val userName = blogName.ifBlank { "Deezer Listener" }

            val licenseToken = extractJsonField(userDataJson, "\"license_token\":\"")
                .takeIf { it.isNotBlank() && it != "null" }

            val maxQuality = determineQualityFromText(userDataJson)

            val session = DeezerSessionInfo(
                arl = cleanArl,
                sid = sid,
                checkForm = checkForm,
                licenseToken = licenseToken,
                userId = userId,
                userName = userName,
                maxQuality = maxQuality
            )
            activeSession = session
            println("DeezerSessionManager: Connected session for $userName ($userId), quality: ${maxQuality.name}")
            return@withContext session
        } catch (e: Exception) {
            println("DeezerSessionManager: Error connecting session: ${e.message}")
        }
        activeSession = null
        null
    }

    actual suspend fun refreshSession(): DeezerSessionInfo? {
        val current = activeSession ?: return null
        return connect(current.arl)
    }

    actual suspend fun fetchJwtToken(): String? = withContext(Dispatchers.IO) {
        val session = activeSession ?: return@withContext null
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
                val jwt = extractJsonField(jsonText, "\"jwt\":\"")
                if (jwt.isNotBlank() && jwt != "null") {
                    return@withContext jwt
                }
            }
        } catch (e: Exception) {
            println("DeezerSessionManager: Error fetching JWT token: ${e.message}")
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
            conn.setRequestProperty("Cookie", "arl=$arl")

            if (conn.responseCode == 200) {
                val jsonText = conn.inputStream.bufferedReader().use { it.readText() }
                val sessionVal = extractJsonField(jsonText, "\"SESSION\":\"")
                if (sessionVal.isNotBlank() && sessionVal != "null") {
                    return sessionVal
                }
            }
        } catch (_: Exception) {}
        return null
    }

    private fun fetchUserData(arl: String, sid: String): String? {
        try {
            val urlString = "$GATEWAY_URL?method=deezer.getUserData&input=3&api_version=1.0&api_token="
            val conn = URI(urlString).toURL().openConnection() as HttpURLConnection
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.requestMethod = "POST"
            conn.setRequestProperty("User-Agent", USER_AGENT)
            val cookieHeader = if (sid.isNotBlank()) "arl=$arl; sid=$sid" else "arl=$arl"
            conn.setRequestProperty("Cookie", cookieHeader)
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            conn.outputStream.use { it.write("{}".toByteArray()) }

            if (conn.responseCode == 200) {
                return conn.inputStream.bufferedReader().use { it.readText() }
            }
        } catch (_: Exception) {}
        return null
    }

    private fun determineQualityFromText(jsonText: String): DeezerAudioQuality {
        val webLossless = jsonText.contains("\"web_lossless\":true") || jsonText.contains("\"web_lossless\":1")
        val mobileLossless = jsonText.contains("\"mobile_lossless\":true") || jsonText.contains("\"mobile_lossless\":1")

        if (webLossless || mobileLossless) {
            return DeezerAudioQuality.FLAC
        }

        val webHq = jsonText.contains("\"web_hq\":true") || jsonText.contains("\"web_hq\":1")
        val mobileHq = jsonText.contains("\"mobile_hq\":true") || jsonText.contains("\"mobile_hq\":1")

        if (webHq || mobileHq) {
            return DeezerAudioQuality.MP3_320
        }

        return DeezerAudioQuality.MP3_128
    }

    private fun extractJsonField(json: String, keyPattern: String): String {
        val at = json.indexOf(keyPattern)
        if (at == -1) return ""
        val start = at + keyPattern.length
        val endQuote = json.indexOf("\"", start)
        if (endQuote == -1) {
            val comma = json.indexOf(",", start)
            val brace = json.indexOf("}", start)
            val end = listOf(comma, brace).filter { it != -1 }.minOrNull() ?: json.length
            return json.substring(start, end).trim()
        }
        return json.substring(start, endQuote).trim()
    }
}
