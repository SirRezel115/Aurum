package com.aurum.data.repository

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object DeezerDecrypter {

    private val INITIAL_IV = byteArrayOf(0, 1, 2, 3, 4, 5, 6, 7)
    private const val BLOCK_SIZE = 2048

    fun generateBlowfishKey(trackId: String, customSecret: ByteArray? = null): ByteArray {
        val cleanId = trackId.removePrefix("deezer_stream_").trim()
        val md5Hex = md5(cleanId)
        val md5Bytes = md5Hex.toByteArray(Charsets.UTF_8)

        val secret = customSecret ?: DeezerMasterSecretExtractor.getMasterSecret()
        val keyBytes = ByteArray(16)
        for (i in 0 until 16) {
            val h1 = md5Bytes[i].toInt()
            val h2 = md5Bytes[i + 16].toInt()
            val s = secret.getOrElse(i) { 0 }.toInt()
            keyBytes[i] = (h1 xor h2 xor s).toByte()
        }
        return keyBytes
    }

    private fun md5(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    /**
     * Decrypts Deezer's BF_CBC_STRIPE audio stream 1:1 matching Sonora's Striped decrypter.
     * Accumulates incoming bytes into a 2048-byte block buffer.
     * For every complete 2048-byte block, if blockIndex % 3 == 0, decrypts via Blowfish CBC with INITIAL_IV.
     * Partial tail blocks (at EOF) are written directly as plaintext.
     */
    fun decryptTrackStream(
        encryptedInputStream: InputStream,
        outputStream: OutputStream,
        trackId: String
    ) {
        val key = generateBlowfishKey(trackId)
        val keySpec = SecretKeySpec(key, "Blowfish")
        val ivSpec = IvParameterSpec(INITIAL_IV)

        val pending = ByteArrayOutputStream(BLOCK_SIZE * 4)
        val rawBuffer = ByteArray(8192)
        var blockCount = 0L

        val cipher = try {
            Cipher.getInstance("Blowfish/CBC/NoPadding")
        } catch (_: Exception) {
            null
        }

        while (true) {
            val bytesRead = encryptedInputStream.read(rawBuffer)
            if (bytesRead == -1) break
            pending.write(rawBuffer, 0, bytesRead)

            val pendingBytes = pending.toByteArray()
            val wholeBlocks = pendingBytes.size / BLOCK_SIZE

            if (wholeBlocks > 0) {
                for (b in 0 until wholeBlocks) {
                    val offset = b * BLOCK_SIZE
                    val blockBytes = pendingBytes.copyOfRange(offset, offset + BLOCK_SIZE)

                    if (blockCount % 3L == 0L && cipher != null) {
                        try {
                            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)
                            val decrypted = cipher.doFinal(blockBytes)
                            outputStream.write(decrypted)
                        } catch (_: Exception) {
                            outputStream.write(blockBytes)
                        }
                    } else {
                        outputStream.write(blockBytes)
                    }
                    blockCount++
                }

                val remainder = pendingBytes.size % BLOCK_SIZE
                pending.reset()
                if (remainder > 0) {
                    pending.write(pendingBytes, wholeBlocks * BLOCK_SIZE, remainder)
                }
            }
        }

        // Flush remaining tail (partial block < 2048 is unencrypted)
        if (pending.size() > 0) {
            outputStream.write(pending.toByteArray())
        }
        outputStream.flush()
    }
}
