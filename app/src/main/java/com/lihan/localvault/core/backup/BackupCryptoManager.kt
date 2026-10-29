package com.lihan.localvault.core.backup

import com.lihan.localvault.core.backup.model.BackupEnvelope
import com.lihan.localvault.core.domain.util.DataError
import com.lihan.localvault.core.domain.util.Result
import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
class BackupCryptoManager {

    companion object {
        private const val KDF_ALGORITHM = "PBKDF2WithHmacSHA256"
        private const val CIPHER_ALGORITHM = "AES/GCM/NoPadding"
        private const val ITERATIONS = 100_000
        private const val KEY_LENGTH_BITS = 256
        private const val SALT_SIZE_BYTES = 16
        private const val IV_SIZE_BYTES = 12
        private const val TAG_LENGTH_BITS = 128
    }

    private val secureRandom = SecureRandom()

    fun encrypt(passphrase: String, plaintextJson: String): BackupEnvelope {
        val salt = ByteArray(SALT_SIZE_BYTES).apply { secureRandom.nextBytes(this) }
        val iv = ByteArray(IV_SIZE_BYTES).apply { secureRandom.nextBytes(this) }

        val key = deriveKey(passphrase, salt)

        val cipher = Cipher.getInstance(CIPHER_ALGORITHM).apply {
            init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_LENGTH_BITS, iv))
        }

        val ciphertext = cipher.doFinal(plaintextJson.toByteArray(Charsets.UTF_8))

        return BackupEnvelope(
            version = 1,
            createdAt = System.currentTimeMillis(),
            kdf = KDF_ALGORITHM,
            iterations = ITERATIONS,
            salt = Base64.encode(salt),
            iv = Base64.encode(iv),
            ciphertext = Base64.encode(ciphertext)
        )
    }

    fun decrypt(passphrase: String, envelope: BackupEnvelope): Result<String, DataError.Local> {
        return try {
            val salt = Base64.decode(envelope.salt)
            val iv = Base64.decode(envelope.iv)
            val ciphertext = Base64.decode(envelope.ciphertext)

            val key = deriveKey(passphrase, salt, envelope.iterations)

            val cipher = Cipher.getInstance(CIPHER_ALGORITHM).apply {
                init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_LENGTH_BITS, iv))
            }

            val decryptedBytes = cipher.doFinal(ciphertext)
            Result.Success(String(decryptedBytes, Charsets.UTF_8))
        } catch (e: AEADBadTagException) {
            Result.Error(DataError.Local.INVALID_PASSPHRASE)
        } catch (e: IllegalArgumentException) {
            Result.Error(DataError.Local.CORRUPTED_DATA)
        } catch (e: Exception) {
            Result.Error(DataError.Local.CRYPTO_ERROR)
        }
    }

    private fun deriveKey(
        passphrase: String,
        salt: ByteArray,
        iterations: Int = ITERATIONS
    ): SecretKey {
        val spec = PBEKeySpec(passphrase.toCharArray(), salt, iterations, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance(KDF_ALGORITHM)
        val keyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }
}
