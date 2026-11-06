package com.aurum.data.repository

expect object DeezerMasterSecretExtractor {
    val FALLBACK_SECRET_BYTES: ByteArray
    val FALLBACK_SECRET_STRING: String

    fun getMasterSecret(): ByteArray
    suspend fun fetchMasterSecret(): ByteArray
}
