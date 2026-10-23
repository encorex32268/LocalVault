package com.lihan.localvault.core.crypto

interface CryptoManager {
    fun encrypt(bytes: ByteArray): EncryptedData
    fun decrypt(ciphertext: ByteArray, iv: ByteArray): ByteArray
}


