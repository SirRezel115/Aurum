package com.aurum.data.repository

actual object DeezerMasterSecretExtractor {
    actual val FALLBACK_SECRET_STRING: String = "g4el58wc0zvf9na1"
    actual val FALLBACK_SECRET_BYTES: ByteArray = FALLBACK_SECRET_STRING.encodeToByteArray()

    actual fun getMasterSecret(): ByteArray = FALLBACK_SECRET_BYTES.copyOf()
    actual suspend fun fetchMasterSecret(): ByteArray = FALLBACK_SECRET_BYTES.copyOf()
}
