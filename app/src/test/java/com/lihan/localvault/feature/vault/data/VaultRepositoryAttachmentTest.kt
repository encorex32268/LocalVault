package com.lihan.localvault.feature.vault.data

import com.lihan.localvault.core.domain.security.backup.BackupCryptoManager
import com.lihan.localvault.core.domain.Vault
import com.lihan.localvault.core.domain.VaultAttachment
import com.lihan.localvault.core.domain.VaultCategory
import com.lihan.localvault.core.domain.VaultContent
import com.lihan.localvault.core.domain.VaultLocalDataSource
import com.lihan.localvault.core.domain.util.DataError
import com.lihan.localvault.core.domain.util.EmptyResult
import com.lihan.localvault.core.domain.util.Result
import com.lihan.localvault.core.data.security.storage.FakeEncryptedFileManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VaultRepositoryAttachmentTest {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    private val backupCryptoManager = BackupCryptoManager()
    private val fakeEncryptedFileManager = FakeEncryptedFileManager()

    private class FakeLocalDataSource : VaultLocalDataSource {
        val vaultStore = mutableMapOf<Long, Pair<Vault, VaultContent>>()

        override fun observeVaults(): Flow<List<Vault>> = flowOf(vaultStore.values.map { it.first })

        override suspend fun getVaultById(id: Long): Result<Vault, DataError.Local> {
            val item = vaultStore[id]
            return if (item != null) Result.Success(item.first) else Result.Error(DataError.Local.NOT_FOUND)
        }

        override suspend fun getVaultContent(id: Long): Result<VaultContent, DataError.Local> {
            val item = vaultStore[id]
            return if (item != null) Result.Success(item.second) else Result.Error(DataError.Local.NOT_FOUND)
        }

        override suspend fun upsertVault(vault: Vault, content: VaultContent): EmptyResult<DataError.Local> {
            val id = if (vault.id == 0L) (vaultStore.size + 1).toLong() else vault.id
            val updatedVault = vault.copy(id = id, hasAttachments = content.attachments.isNotEmpty())
            vaultStore[id] = Pair(updatedVault, content)
            return Result.Success(Unit)
        }

        override suspend fun deleteVault(id: Long): EmptyResult<DataError.Local> {
            vaultStore.remove(id)
            return Result.Success(Unit)
        }

        override suspend fun getAllVaultsWithContent(): Result<List<Pair<Vault, VaultContent>>, DataError.Local> {
            return Result.Success(vaultStore.values.toList())
        }
    }

    @Test
    fun deleteVault_cleansUpAssociatedEncryptedFiles() = runBlocking {
        val dataSource = FakeLocalDataSource()
        val attachment1 = VaultAttachment(
            fileName = "id_card.jpg",
            mimeType = "image/jpeg",
            fileSize = 1024L,
            encryptedFileName = "att1.enc"
        )
        val attachment2 = VaultAttachment(
            fileName = "contract.pdf",
            mimeType = "application/pdf",
            fileSize = 2048L,
            encryptedFileName = "att2.enc"
        )
        fakeEncryptedFileManager.savedFiles["att1.enc"] = byteArrayOf(1, 2)
        fakeEncryptedFileManager.savedFiles["att2.enc"] = byteArrayOf(3, 4)

        dataSource.vaultStore[1L] = Pair(
            Vault(id = 1L, title = "Personal", iconIndex = 0, color = 0L, category = VaultCategory.CUSTOM, updatedAt = 100L),
            VaultContent(attachments = listOf(attachment1, attachment2))
        )

        val repository = VaultRepositoryImpl(dataSource, backupCryptoManager, fakeEncryptedFileManager, json)
        val deleteResult = repository.deleteVault(1L)

        assertTrue(deleteResult is Result.Success)
        assertTrue(fakeEncryptedFileManager.deletedFiles.contains("att1.enc"))
        assertTrue(fakeEncryptedFileManager.deletedFiles.contains("att2.enc"))
        assertEquals(0, fakeEncryptedFileManager.savedFiles.size)
    }

    @Test
    fun updateVault_removesOrphanedAttachments() = runBlocking {
        val dataSource = FakeLocalDataSource()
        val keepAttachment = VaultAttachment(
            id = "att_keep",
            fileName = "keep.jpg",
            mimeType = "image/jpeg",
            fileSize = 1024L,
            encryptedFileName = "keep.enc"
        )
        val removeAttachment = VaultAttachment(
            id = "att_remove",
            fileName = "remove.jpg",
            mimeType = "image/jpeg",
            fileSize = 1024L,
            encryptedFileName = "remove.enc"
        )
        fakeEncryptedFileManager.savedFiles["keep.enc"] = byteArrayOf(1)
        fakeEncryptedFileManager.savedFiles["remove.enc"] = byteArrayOf(2)

        val vault = Vault(id = 10L, title = "Work", iconIndex = 0, color = 0L, category = VaultCategory.CUSTOM, updatedAt = 100L)
        dataSource.vaultStore[10L] = Pair(
            vault,
            VaultContent(attachments = listOf(keepAttachment, removeAttachment))
        )

        val repository = VaultRepositoryImpl(dataSource, backupCryptoManager, fakeEncryptedFileManager, json)

        // Update vault with only keepAttachment
        val updateResult = repository.upsertVault(
            vault = vault,
            content = VaultContent(attachments = listOf(keepAttachment))
        )

        assertTrue(updateResult is Result.Success)
        assertTrue(fakeEncryptedFileManager.deletedFiles.contains("remove.enc"))
        assertTrue(!fakeEncryptedFileManager.deletedFiles.contains("keep.enc"))
        assertTrue(fakeEncryptedFileManager.savedFiles.containsKey("keep.enc"))
    }

    @Test
    fun upsertVault_withAttachments_setsHasAttachmentsTrue() = runBlocking {
        val dataSource = FakeLocalDataSource()
        val repository = VaultRepositoryImpl(dataSource, backupCryptoManager, fakeEncryptedFileManager, json)

        val attachment = VaultAttachment(
            fileName = "photo.jpg",
            mimeType = "image/jpeg",
            fileSize = 100L,
            encryptedFileName = "photo.enc"
        )
        val vault = Vault(title = "PhotoVault", iconIndex = 1, color = 0L, category = VaultCategory.CUSTOM, updatedAt = 100L)
        repository.upsertVault(vault, VaultContent(attachments = listOf(attachment)))

        val saved = dataSource.vaultStore[1L]?.first
        assertTrue(saved?.hasAttachments == true)
    }

    @Test
    fun upsertVault_withoutAttachments_setsHasAttachmentsFalse() = runBlocking {
        val dataSource = FakeLocalDataSource()
        val repository = VaultRepositoryImpl(dataSource, backupCryptoManager, fakeEncryptedFileManager, json)

        val vault = Vault(title = "PlainVault", iconIndex = 1, color = 0L, category = VaultCategory.CUSTOM, updatedAt = 100L)
        repository.upsertVault(vault, VaultContent(attachments = emptyList()))

        val saved = dataSource.vaultStore[1L]?.first
        assertTrue(saved?.hasAttachments == false)
    }
}
