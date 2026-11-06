package com.aurum.scanner

import java.io.File
import java.io.RandomAccessFile

object DesktopArtworkExtractor {

    private val cacheDir: File by lazy {
        val dir = File(File(System.getProperty("user.home"), ".aurum"), "cache/artwork")
        if (!dir.exists()) dir.mkdirs()
        dir
    }

    private fun toCoilFileUri(file: File): String {
        return file.absolutePath
    }

    private fun isValidImageFile(file: File): Boolean {
        if (!file.exists() || file.length() < 100) return false
        return try {
            val img = javax.imageio.ImageIO.read(file)
            img != null && img.width > 0 && img.height > 0
        } catch (_: Exception) {
            false
        }
    }

    private fun isValidImageBytes(bytes: ByteArray): Boolean {
        if (bytes.size < 100) return false
        return try {
            val img = javax.imageio.ImageIO.read(java.io.ByteArrayInputStream(bytes))
            img != null && img.width > 0 && img.height > 0
        } catch (_: Exception) {
            false
        }
    }

    fun extractArtworkUri(file: File): String? {
        if (!file.exists() || !file.isFile) return null

        val fileHash = kotlin.math.abs((file.absolutePath + "_" + file.lastModified()).hashCode()).toString()
        val cachedArtFile = File(cacheDir, "$fileHash.jpg")
        if (cachedArtFile.exists()) {
            if (isValidImageFile(cachedArtFile)) {
                return toCoilFileUri(cachedArtFile)
            } else {
                try { cachedArtFile.delete() } catch (_: Exception) {}
            }
        }

        // Tier 1: Try extracting embedded audio tags (MP3 ID3, MP4/M4A, FLAC)
        val embeddedArtBytes = extractEmbeddedPicture(file)
        if (embeddedArtBytes != null && isValidImageBytes(embeddedArtBytes)) {
            try {
                cachedArtFile.writeBytes(embeddedArtBytes)
                return toCoilFileUri(cachedArtFile)
            } catch (e: Exception) {
                println("DesktopArtworkExtractor: Failed writing cached artwork for ${file.name}: ${e.message}")
            }
        }

        // Tier 2: Search parent directory for cover art files
        val parentCoverUri = findFolderCoverImage(file)
        if (parentCoverUri != null) {
            return parentCoverUri
        }

        return null
    }

    private fun findFolderCoverImage(file: File): String? {
        val parent = file.parentFile ?: return null
        if (!parent.exists() || !parent.isDirectory) return null

        val candidateNames = listOf("cover", "folder", "album", "front", "art", "artwork")
        val candidateExtensions = listOf("jpg", "jpeg", "png", "webp")

        for (name in candidateNames) {
            for (ext in candidateExtensions) {
                val candidate = File(parent, "$name.$ext")
                if (candidate.exists() && candidate.isFile && candidate.length() > 0) {
                    return toCoilFileUri(candidate)
                }
            }
        }

        // Fallback: Check any single image file in the directory
        try {
            val imageFiles = parent.listFiles { f ->
                f.isFile && f.extension.lowercase() in candidateExtensions
            }
            if (!imageFiles.isNullOrEmpty()) {
                return toCoilFileUri(imageFiles.first())
            }
        } catch (_: Exception) {}

        return null
    }

    private fun extractEmbeddedPicture(file: File): ByteArray? {
        val ext = file.extension.lowercase()
        return when (ext) {
            "mp3" -> extractMp3Id3v2Picture(file)
            "m4a", "mp4", "aac" -> extractMp4CovrPicture(file)
            "flac" -> extractFlacPicture(file)
            else -> extractMp3Id3v2Picture(file)
        }
    }

    private fun extractMp3Id3v2Picture(file: File): ByteArray? {
        try {
            RandomAccessFile(file, "r").use { raf ->
                val header = ByteArray(10)
                if (raf.read(header) < 10) return null
                if (header[0] != 'I'.code.toByte() || header[1] != 'D'.code.toByte() || header[2] != '3'.code.toByte()) {
                    return null
                }

                val tagSize = ((header[6].toInt() and 0x7F) shl 21) or
                        ((header[7].toInt() and 0x7F) shl 14) or
                        ((header[8].toInt() and 0x7F) shl 7) or
                        (header[9].toInt() and 0x7F)

                if (tagSize <= 0) return null

                val tagData = ByteArray(tagSize.coerceAtMost(5 * 1024 * 1024))
                raf.read(tagData)

                // Search for APIC frame or JPEG/PNG header inside ID3 block
                return parseApicFromBuffer(tagData)
            }
        } catch (_: Exception) {
            return null
        }
    }

    private fun parseApicFromBuffer(buffer: ByteArray): ByteArray? {
        val apicMarker = "APIC".toByteArray(Charsets.US_ASCII)
        val picMarker = "PIC".toByteArray(Charsets.US_ASCII)

        var idx = findSequence(buffer, apicMarker)
        var isV22 = false
        if (idx == -1) {
            idx = findSequence(buffer, picMarker)
            isV22 = true
        }

        if (idx != -1) {
            val frameStart = idx + if (isV22) 3 else 4
            if (frameStart + 10 < buffer.size) {
                // Find start of image magic bytes (JPEG: FF D8 FF, PNG: 89 50 4E 47)
                val imgStart = findImageMagicBytes(buffer, frameStart)
                if (imgStart != -1) {
                    val imgEnd = findImageEnd(buffer, imgStart)
                    val length = if (imgEnd > imgStart) imgEnd - imgStart else (buffer.size - imgStart)
                    if (length > 100) {
                        return buffer.copyOfRange(imgStart, (imgStart + length).coerceAtMost(buffer.size))
                    }
                }
            }
        }

        // Direct binary search for JPEG/PNG header if APIC tag structure differed
        val jpegStart = findSequence(buffer, byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte()))
        if (jpegStart != -1) {
            val jpegEnd = findSequence(buffer, byteArrayOf(0xFF.toByte(), 0xD9.toByte()), jpegStart + 2)
            val len = if (jpegEnd > jpegStart) (jpegEnd - jpegStart + 2) else (buffer.size - jpegStart)
            if (len in 100..5_000_000) {
                return buffer.copyOfRange(jpegStart, (jpegStart + len).coerceAtMost(buffer.size))
            }
        }

        val pngStart = findSequence(buffer, byteArrayOf(0x89.toByte(), 'P'.toByte(), 'N'.toByte(), 'G'.toByte()))
        if (pngStart != -1) {
            val len = (buffer.size - pngStart).coerceAtMost(3_000_000)
            if (len > 100) {
                return buffer.copyOfRange(pngStart, pngStart + len)
            }
        }

        return null
    }

    private fun extractMp4CovrPicture(file: File): ByteArray? {
        try {
            RandomAccessFile(file, "r").use { raf ->
                val buf = ByteArray(1024 * 1024)
                val read = raf.read(buf)
                if (read <= 0) return null

                val covrMarker = "covr".toByteArray(Charsets.US_ASCII)
                val idx = findSequence(buf, covrMarker)
                if (idx != -1) {
                    val imgStart = findImageMagicBytes(buf, idx + 4)
                    if (imgStart != -1) {
                        val len = (read - imgStart).coerceAtMost(2_000_000)
                        if (len > 100) {
                            return buf.copyOfRange(imgStart, imgStart + len)
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        return null
    }

    private fun extractFlacPicture(file: File): ByteArray? {
        try {
            RandomAccessFile(file, "r").use { raf ->
                val buf = ByteArray(512 * 1024)
                val read = raf.read(buf)
                if (read <= 0) return null

                val imgStart = findImageMagicBytes(buf, 0)
                if (imgStart != -1) {
                    val len = (read - imgStart).coerceAtMost(2_000_000)
                    if (len > 100) {
                        return buf.copyOfRange(imgStart, imgStart + len)
                    }
                }
            }
        } catch (_: Exception) {}
        return null
    }

    private fun findSequence(data: ByteArray, target: ByteArray, startOffset: Int = 0): Int {
        if (target.isEmpty() || data.size < target.size) return -1
        for (i in startOffset..(data.size - target.size)) {
            var match = true
            for (j in target.indices) {
                if (data[i + j] != target[j]) {
                    match = false
                    break
                }
            }
            if (match) return i
        }
        return -1
    }

    private fun findImageMagicBytes(data: ByteArray, startOffset: Int): Int {
        val jpegHeader = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())
        val pngHeader = byteArrayOf(0x89.toByte(), 'P'.toByte(), 'N'.toByte(), 'G'.toByte())

        for (i in startOffset.coerceAtLeast(0)..(data.size - 4)) {
            if (data[i] == jpegHeader[0] && data[i + 1] == jpegHeader[1] && data[i + 2] == jpegHeader[2]) {
                return i
            }
            if (data[i] == pngHeader[0] && data[i + 1] == pngHeader[1] && data[i + 2] == pngHeader[2] && data[i + 3] == pngHeader[3]) {
                return i
            }
        }
        return -1
    }

    private fun findImageEnd(data: ByteArray, startOffset: Int): Int {
        val jpegEnd = byteArrayOf(0xFF.toByte(), 0xD9.toByte())
        val idx = findSequence(data, jpegEnd, startOffset + 2)
        return if (idx != -1) idx + 2 else -1
    }
}
