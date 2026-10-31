package com.lihan.localvault.feature.vault.data

import com.lihan.localvault.core.backup.BackupCryptoManager
import com.lihan.localvault.core.domain.FieldType
import com.lihan.localvault.core.domain.Vault
import com.lihan.localvault.core.domain.VaultCategory
import com.lihan.localvault.core.domain.VaultContent
import com.lihan.localvault.core.domain.VaultField
import com.lihan.localvault.core.domain.VaultLocalDataSource
import com.lihan.localvault.core.domain.util.DataError
import com.lihan.localvault.core.domain.util.EmptyResult
import com.lihan.localvault.core.domain.util.Result
import com.lihan.localvault.core.storage.FakeEncryptedFileManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VaultRepositoryBackupTest {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    private val backupCryptoManager = BackupCryptoManager()
    private val encryptedFileManager = FakeEncryptedFileManager()

    private class FakeLocalDataSource : VaultLocalDataSource {
        val vaultStore = mutableListOf<Pair<Vault, VaultContent>>()

        override fun observeVaults(): Flow<List<Vault>> = flowOf(vaultStore.map { it.first })

        override suspend fun getVaultById(id: Long): Result<Vault, DataError.Local> {
            val found = vaultStore.find { it.first.id == id }
            return if (found != null) Result.Success(found.first) else Result.Error(DataError.Local.NOT_FOUND)
        }

        override suspend fun getVaultContent(id: Long): Result<VaultContent, DataError.Local> {
            val found = vaultStore.find { it.first.id == id }
            return if (found != null) Result.Success(found.second) else Result.Error(DataError.Local.NOT_FOUND)
        }

        override suspend fun getAllVaultsWithContent(): Result<List<Pair<Vault, VaultContent>>, DataError.Local> {
            return Result.Success(vaultStore.toList())
        }

        override suspend fun upsertVault(vault: Vault, content: VaultContent): EmptyResult<DataError.Local> {
            vaultStore.add(vault to content)
            return Result.Success(Unit)
        }

        override suspend fun deleteVault(id: Long): EmptyResult<DataError.Local> {
            vaultStore.removeIf { it.first.id == id }
            return Result.Success(Unit)
        }
    }

    @Test
    fun exportAndImport_withCorrectPassword_restoresDataCorrectly() = runBlocking {
        val fakeDataSource = FakeLocalDataSource().apply {
            vaultStore.add(
                Vault(
                    id = 1L,
                    title = "Google Account",
                    iconIndex = 2,
                    color = 0xFF4285F4,
                    category = VaultCategory.CUSTOM,
                    updatedAt = 1000L
                ) to VaultContent(
                    fields = listOf(
                        VaultField(id = "1", type = FieldType.SINGLE_LINE, label = "Email", value = "test@gmail.com"),
                        VaultField(id = "2", type = FieldType.PASSWORD, label = "Password", value = "secret123")
                    )
                )
            )
        }

        val repository = VaultRepositoryImpl(fakeDataSource, backupCryptoManager, encryptedFileManager, json)
        val password = "1234"

        // 1. Export
        val exportResult = repository.exportBackup(password)
        assertTrue(exportResult is Result.Success)
        val backupJson = (exportResult as Result.Success).data

        // Verify that sensitive data is encrypted in the backup file
        assertFalse(backupJson.contains("test@gmail.com"))
        assertFalse(backupJson.contains("secret123"))

        // 2. Import into a new empty database
        val newFakeDataSource = FakeLocalDataSource()
        val newRepository = VaultRepositoryImpl(newFakeDataSource, backupCryptoManager, encryptedFileManager, json)

        val importResult = newRepository.importBackup(password, backupJson)
        assertTrue(importResult is Result.Success)
        assertEquals(1, (importResult as Result.Success).data)

        assertEquals(1, newFakeDataSource.vaultStore.size)
        val restoredVault = newFakeDataSource.vaultStore.first().first
        val restoredContent = newFakeDataSource.vaultStore.first().second

        assertEquals("Google Account", restoredVault.title)
        assertEquals(2, restoredContent.fields.size)
        assertEquals("test@gmail.com", restoredContent.fields[0].value)
        assertEquals("secret123", restoredContent.fields[1].value)
    }

    @Test
    fun import_withWrongPassword_returnsInvalidPassphraseError() = runBlocking {
        val fakeDataSource = FakeLocalDataSource().apply {
            vaultStore.add(
                Vault(id = 1L, title = "Bank", iconIndex = 1, color = 0L, category = VaultCategory.CUSTOM, updatedAt = 1L) to VaultContent()
            )
        }
        val repository = VaultRepositoryImpl(fakeDataSource, backupCryptoManager, encryptedFileManager, json)

        val exportResult = repository.exportBackup("654321")
        val backupJson = (exportResult as Result.Success).data

        val importResult = repository.importBackup("111111", backupJson)
        assertTrue(importResult is Result.Error)
        assertEquals(DataError.Local.INVALID_PASSPHRASE, (importResult as Result.Error).error)
    }

    @Test
    fun import_withCorruptedData_returnsCorruptedDataError() = runBlocking {
        val fakeDataSource = FakeLocalDataSource()
        val repository = VaultRepositoryImpl(fakeDataSource, backupCryptoManager, encryptedFileManager, json)

        val importResult = repository.importBackup("1234", "not_a_json_string")
        assertTrue(importResult is Result.Error)
        assertEquals(DataError.Local.CORRUPTED_DATA, (importResult as Result.Error).error)
    }
}
