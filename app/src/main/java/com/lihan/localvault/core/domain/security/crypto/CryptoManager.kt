package com.lihan.localvault.core.domain.security.crypto

interface CryptoManager {
    fun encrypt(bytes: ByteArray): EncryptedData
    fun decrypt(ciphertext: ByteArray, iv: ByteArray): ByteArray
}


