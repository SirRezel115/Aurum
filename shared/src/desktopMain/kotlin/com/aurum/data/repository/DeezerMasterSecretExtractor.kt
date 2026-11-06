package com.aurum.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URI
import java.security.MessageDigest

actual object DeezerMasterSecretExtractor {
    actual val FALLBACK_SECRET_STRING: String = "g4el58wc0zvf9na1"
    actual val FALLBACK_SECRET_BYTES: ByteArray = FALLBACK_SECRET_STRING.toByteArray(Charsets.UTF_8)

    private const val WEB_PLAYER_URL = "https://www.deezer.com/en/channels/explore/"
    private const val EXPECTED_SECRET_MD5 = "7ebf40da848f4a0fb3cc56ddbe6c2d09"
    private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36"

    @Volatile
    private var cachedSecret: ByteArray = FALLBACK_SECRET_BYTES.copyOf()

    actual fun getMasterSecret(): ByteArray {
        return cachedSecret.copyOf()
    }

    actual suspend fun fetchMasterSecret(): ByteArray = withContext(Dispatchers.IO) {
        try {
            val secret = extractSecretFromWebBundle()
            if (secret != null && secret.size == 16) {
                val md5Hex = md5(secret)
                if (md5Hex.equals(EXPECTED_SECRET_MD5, ignoreCase = true)) {
                    cachedSecret = secret
                    println("DeezerSecretExtractor: Extracted master secret successfully.")
                    return@withContext secret.copyOf()
                }
            }
        } catch (e: Exception) {
            println("DeezerSecretExtractor: Failed to extract master secret: ${e.message}. Using fallback.")
        }
        cachedSecret = FALLBACK_SECRET_BYTES.copyOf()
        FALLBACK_SECRET_BYTES.copyOf()
    }

    private fun extractSecretFromWebBundle(): ByteArray? {
        val html = httpGet(WEB_PLAYER_URL) ?: return null
        val bundleUrl = findBundleUrl(html) ?: return null
        val jsSource = httpGet(bundleUrl) ?: return null

        val firstHalf = findHalf(jsSource, 0x61.toByte(), 0x67.toByte()) ?: return null
        val secondHalf = findHalf(jsSource, 0x31.toByte(), 0x34.toByte()) ?: return null

        val secret = ByteArray(16)
        for (i in 0 until 8) {
            secret[i * 2] = firstHalf[i]
            secret[i * 2 + 1] = secondHalf[i]
        }
        return secret
    }

    private fun findBundleUrl(html: String): String? {
        var rest = html
        while (true) {
            val at = rest.indexOf("app-web")
            if (at == -1) break
            val head = rest.substring(0, at)
            val from = head.lastIndexOf("https://")
            if (from != -1) {
                val tail = rest.substring(from)
                val end = tail.indexOf('"')
                if (end != -1) {
                    val url = tail.substring(0, end)
                    if (url.endsWith(".js")) {
                        return url
                    }
                }
            }
            rest = rest.substring(at + 7)
        }
        return null
    }

    private fun findHalf(source: String, first: Byte, last: Byte): ByteArray? {
        val needle = "0x%02x%%2C".format(first)
        val terminator = "0x%02x".format(last)
        var rest = source

        while (true) {
            val at = rest.indexOf(needle)
            if (at == -1) break
            val candidate = rest.substring(at)
            val bytes = parseHalf(candidate, terminator)
            if (bytes != null && bytes.size == 8) {
                return bytes
            }
            rest = rest.substring(at + needle.length)
        }
        return null
    }

    private fun parseHalf(candidate: String, terminator: String): ByteArray? {
        val termIndex = candidate.indexOf(terminator)
        if (termIndex == -1) return null
        val end = termIndex + terminator.length

        val window = candidate.substring(0, end)
        val parts = window.split("%2C")
        if (parts.size != 8) return null

        val bytes = ByteArray(8)
        for (i in parts.indices) {
            val hex = parts[i].removePrefix("0x").trim()
            val parsedByte = hex.toIntOrNull(16)?.toByte() ?: return null
            bytes[i] = parsedByte
        }
        bytes.reverse()
        return bytes
    }

    private fun httpGet(urlString: String): String? {
        return try {
            val url = URI(urlString).toURL()
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.setRequestProperty("User-Agent", USER_AGENT)
            if (conn.responseCode == 200) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else null
        } catch (_: Exception) {
            null
        }
    }

    private fun md5(bytes: ByteArray): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }
}
