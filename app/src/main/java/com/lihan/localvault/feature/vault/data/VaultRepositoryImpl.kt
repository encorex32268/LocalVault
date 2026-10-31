package com.lihan.localvault.feature.vault.data

import com.lihan.localvault.core.backup.BackupCryptoManager
import com.lihan.localvault.core.backup.model.BackupEnvelope
import com.lihan.localvault.core.backup.model.VaultBackupAttachment
import com.lihan.localvault.core.backup.model.VaultBackupField
import com.lihan.localvault.core.backup.model.VaultBackupItem
import com.lihan.localvault.core.backup.model.VaultBackupPayload
import com.lihan.localvault.core.domain.FieldType
import com.lihan.localvault.core.domain.Vault
import com.lihan.localvault.core.domain.VaultCategory
import com.lihan.localvault.core.domain.VaultContent
import com.lihan.localvault.core.domain.VaultField
import com.lihan.localvault.core.domain.VaultLocalDataSource
import com.lihan.localvault.core.domain.util.DataError
import com.lihan.localvault.core.domain.util.EmptyResult
import com.lihan.localvault.core.domain.util.Result
import com.lihan.localvault.core.storage.EncryptedFileManager
import com.lihan.localvault.feature.vault.domain.VaultRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class VaultRepositoryImpl(
    private val vaultLocalDataSource: VaultLocalDataSource,
    private val backupCryptoManager: BackupCryptoManager,
    private val encryptedFileManager: EncryptedFileManager,
    private val json: Json
): VaultRepository {
    override val vaultItems: Flow<List<Vault>>
        get() = vaultLocalDataSource.observeVaults()

    override suspend fun getVaultById(id: Long): Result<Vault, DataError.Local> {
        return vaultLocalDataSource.getVaultById(id)
    }

    override suspend fun getVaultContent(id: Long): Result<VaultContent, DataError.Local> {
        return vaultLocalDataSource.getVaultContent(id)
    }

    override suspend fun deleteVault(id: Long): EmptyResult<DataError.Local> {
        val contentResult = vaultLocalDataSource.getVaultContent(id)
        if (contentResult is Result.Success) {
            contentResult.data.attachments.forEach { attachment ->
                encryptedFileManager.deleteEncryptedFile(attachment.encryptedFileName)
            }
        }
        return vaultLocalDataSource.deleteVault(id)
    }

    override suspend fun upsertVault(
        vault: Vault,
        content: VaultContent
    ): EmptyResult<DataError.Local> {
        if (vault.id != 0L) {
            val oldContentResult = vaultLocalDataSource.getVaultContent(vault.id)
            if (oldContentResult is Result.Success) {
                val newAttachmentIds = content.attachments.map { it.id }.toSet()
                oldContentResult.data.attachments
                    .filter { it.id !in newAttachmentIds }
                    .forEach { encryptedFileManager.deleteEncryptedFile(it.encryptedFileName) }
            }
        }
        return vaultLocalDataSource.upsertVault(vault, content)
    }

    override suspend fun exportBackup(
        passphrase: String,
        includeAttachments: Boolean
    ): Result<String, DataError.Local> {
        return when (val result = vaultLocalDataSource.getAllVaultsWithContent()) {
            is Result.Error -> Result.Error(result.error)
            is Result.Success -> {
                try {
                    val backupItems = result.data.map { (vault, content) ->
                        val fields = content.fields.map { field ->
                            VaultBackupField(
                                id = field.id,
                                label = field.label,
                                value = field.value,
                                type = field.type.name
                            )
                        }
                        val backupAttachments = if (includeAttachments) {
                            content.attachments.mapNotNull { att ->
                                when (val bytesResult = encryptedFileManager.readDecryptedBytes(att.encryptedFileName)) {
                                    is Result.Success -> {
                                        @OptIn(ExperimentalEncodingApi::class)
                                        val base64 = Base64.encode(bytesResult.data)
                                        VaultBackupAttachment(
                                            fileName = att.fileName,
                                            mimeType = att.mimeType,
                                            dataBase64 = base64
                                        )
                                    }
                                    is Result.Error -> null
                                }
                            }
                        } else {
                            emptyList()
                        }
                        VaultBackupItem(
                            title = vault.title,
                            color = vault.color,
                            iconIndex = vault.iconIndex,
                            category = vault.category.name,
                            updatedAt = vault.updatedAt,
                            fields = fields,
                            attachments = backupAttachments
                        )
                    }
                    val payload = VaultBackupPayload(
                        version = 1,
                        exportedAt = System.currentTimeMillis(),
                        items = backupItems
                    )
                    val payloadJson = json.encodeToString(VaultBackupPayload.serializer(), payload)
                    val envelope = backupCryptoManager.encrypt(passphrase, payloadJson)
                    val envelopeJson = json.encodeToString(BackupEnvelope.serializer(), envelope)
                    Result.Success(envelopeJson)
                } catch (e: Exception) {
                    Result.Error(DataError.Local.SERIALIZATION)
                }
            }
        }
    }

    override suspend fun importBackup(passphrase: String, backupJson: String): Result<Int, DataError.Local> {
        val envelope = try {
            json.decodeFromString(BackupEnvelope.serializer(), backupJson)
        } catch (e: Exception) {
            return Result.Error(DataError.Local.CORRUPTED_DATA)
        }

        val decryptedJsonResult = backupCryptoManager.decrypt(passphrase, envelope)
        if (decryptedJsonResult is Result.Error) {
            return Result.Error(decryptedJsonResult.error)
        }

        val decryptedJson = (decryptedJsonResult as Result.Success).data
        val payload = try {
            json.decodeFromString(VaultBackupPayload.serializer(), decryptedJson)
        } catch (e: Exception) {
            return Result.Error(DataError.Local.CORRUPTED_DATA)
        }

        var importedCount = 0
        for (item in payload.items) {
            val category = try {
                VaultCategory.valueOf(item.category)
            } catch (e: Exception) {
                VaultCategory.CUSTOM
            }
            val fields = item.fields.map { f ->
                val fieldType = try {
                    FieldType.valueOf(f.type)
                } catch (e: Exception) {
                    when (f.type) {
                        "TEXT" -> FieldType.SINGLE_LINE
                        "PASSWORD" -> FieldType.PASSWORD
                        "NOTE", "MULTI_LINE" -> FieldType.MULTI_LINE
                        else -> FieldType.SINGLE_LINE
                    }
                }
                VaultField(
                    id = f.id,
                    label = f.label,
                    value = f.value,
                    type = fieldType
                )
            }

            val restoredAttachments = item.attachments.mapNotNull { att ->
                try {
                    @OptIn(ExperimentalEncodingApi::class)
                    val bytes = Base64.decode(att.dataBase64)
                    when (val saveResult = encryptedFileManager.saveEncryptedBytes(att.fileName, att.mimeType, bytes)) {
                        is Result.Success -> saveResult.data
                        is Result.Error -> null
                    }
                } catch (e: Exception) {
                    null
                }
            }

            val vault = Vault(
                id = 0L,
                iconIndex = item.iconIndex,
                color = item.color,
                title = item.title,
                category = category,
                updatedAt = item.updatedAt,
                hasAttachments = restoredAttachments.isNotEmpty()
            )
            val content = VaultContent(fields = fields, attachments = restoredAttachments)
            when (vaultLocalDataSource.upsertVault(vault, content)) {
                is Result.Success -> importedCount++
                is Result.Error -> Unit
            }
        }

        return Result.Success(importedCount)
    }
}
