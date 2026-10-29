package com.lihan.localvault.core.backup

import com.lihan.localvault.core.domain.util.DataError
import com.lihan.localvault.core.domain.util.Result
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupCryptoManagerTest {

    private val cryptoManager = BackupCryptoManager()

    @Test
    fun encryptAndDecrypt_withCorrectPassphrase_returnsOriginalData() {
        val passphrase = "1234"
        val payload = """{"version":1,"items":[{"title":"Google","fields":[]}]}"""

        val envelope = cryptoManager.encrypt(passphrase, payload)

        assertNotEquals(payload, envelope.ciphertext)
        assertTrue(envelope.salt.isNotBlank())
        assertTrue(envelope.iv.isNotBlank())

        val decryptResult = cryptoManager.decrypt(passphrase, envelope)

        assertTrue(decryptResult is Result.Success)
        assertEquals(payload, (decryptResult as Result.Success).data)
    }

    @Test
    fun decrypt_withIncorrectPassphrase_returnsInvalidPassphraseError() {
        val passphrase = "1234"
        val wrongPassphrase = "5678"
        val payload = """{"secret":"my_password"}"""

        val envelope = cryptoManager.encrypt(passphrase, payload)
        val decryptResult = cryptoManager.decrypt(wrongPassphrase, envelope)

        assertTrue(decryptResult is Result.Error)
        assertEquals(DataError.Local.INVALID_PASSPHRASE, (decryptResult as Result.Error).error)
    }

    @Test
    fun decrypt_withTamperedCiphertext_returnsError() {
        val passphrase = "654321"
        val payload = """{"secret":"sensitive"}"""

        val envelope = cryptoManager.encrypt(passphrase, payload)
        val tamperedEnvelope = envelope.copy(
            ciphertext = envelope.ciphertext.dropLast(4) + "AAAA"
        )

        val decryptResult = cryptoManager.decrypt(passphrase, tamperedEnvelope)
        assertTrue(decryptResult is Result.Error)
    }
}
