package com.lihan.localvault.core.database

import android.database.sqlite.SQLiteFullException
import com.lihan.localvault.core.crypto.CryptoManager
import com.lihan.localvault.core.database.dao.VaultDao
import com.lihan.localvault.core.database.mapper.toJsonString
import com.lihan.localvault.core.database.mapper.toVault
import com.lihan.localvault.core.database.mapper.toVaultContent
import com.lihan.localvault.core.database.mapper.toVaultEntity
import com.lihan.localvault.core.domain.Vault
import com.lihan.localvault.core.domain.VaultCategory
import com.lihan.localvault.core.domain.VaultContent
import com.lihan.localvault.core.domain.VaultLocalDataSource
import com.lihan.localvault.core.domain.util.DataError
import com.lihan.localvault.core.domain.util.EmptyResult
import com.lihan.localvault.core.domain.util.Result
import java.security.GeneralSecurityException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

class RoomVaultDataSource(
    private val vaultDao: VaultDao,
    private val cryptoManager: CryptoManager,
    private val json: Json
) : VaultLocalDataSource {

    private var hasMigratedLegacyAttachmentsFlag = false

    override fun observeVaults(): Flow<List<Vault>> {
        return vaultDao.observeVaultSummaries().map { list ->
            if (!hasMigratedLegacyAttachmentsFlag) {
                hasMigratedLegacyAttachmentsFlag = true
                try {
                    val entities = vaultDao.getAllVaultEntities()
                    for (entity in entities) {
                        if (!entity.hasAttachments) {
                            val decryptedBytes = cryptoManager.decrypt(entity.ciphertext, entity.iv)
                            val content = decryptedBytes.decodeToString().toVaultContent(json = json)
                            if (content.attachments.isNotEmpty()) {
                                vaultDao.upsertVault(entity.copy(hasAttachments = true))
                            }
                        }
                    }
                } catch (_: Exception) {
                }
            }
            list.map { it.toVault() }
        }
    }

    override suspend fun getVaultById(id: Long): Result<Vault, DataError.Local> {
        return try {
            val summary = vaultDao.getVaultSummaryById(id)
                ?: return Result.Error(DataError.Local.NOT_FOUND)
            Result.Success(summary.toVault())
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.Error(DataError.Local.UNKNOWN)
        }
    }

    override suspend fun getVaultContent(id: Long): Result<VaultContent, DataError.Local> {
        return try {
            val encryptedTuple = vaultDao.getEncryptedContentById(id)
                ?: return Result.Error(DataError.Local.NOT_FOUND)

            val decryptedBytes = cryptoManager.decrypt(
                ciphertext = encryptedTuple.ciphertext,
                iv = encryptedTuple.iv
            )
            val jsonString = decryptedBytes.decodeToString()
            val category = try {
                VaultCategory.valueOf(encryptedTuple.category)
            } catch (e: Exception) {
                VaultCategory.CUSTOM
            }
            val content = jsonString.toVaultContent(category = category, json = json)

            Result.Success(content)
        } catch (e: GeneralSecurityException) {
            Result.Error(DataError.Local.CRYPTO_ERROR)
        } catch (e: SerializationException) {
            Result.Error(DataError.Local.SERIALIZATION)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.Error(DataError.Local.UNKNOWN)
        }
    }

    override suspend fun getAllVaultsWithContent(): Result<List<Pair<Vault, VaultContent>>, DataError.Local> {
        return try {
            val entities = vaultDao.getAllVaultEntities()
            val list = entities.map { entity ->
                val vault = entity.toVault()
                val decryptedBytes = cryptoManager.decrypt(
                    ciphertext = entity.ciphertext,
                    iv = entity.iv
                )
                val category = try {
                    VaultCategory.valueOf(entity.category)
                } catch (e: Exception) {
                    VaultCategory.CUSTOM
                }
                val content = decryptedBytes.decodeToString().toVaultContent(category = category, json = json)
                vault to content
            }
            Result.Success(list)
        } catch (e: GeneralSecurityException) {
            Result.Error(DataError.Local.CRYPTO_ERROR)
        } catch (e: SerializationException) {
            Result.Error(DataError.Local.SERIALIZATION)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.Error(DataError.Local.UNKNOWN)
        }
    }

    override suspend fun upsertVault(
        vault: Vault,
        content: VaultContent
    ): EmptyResult<DataError.Local> {
        return try {
            val jsonString = content.toJsonString(json)
            val encryptedData = cryptoManager.encrypt(jsonString.encodeToByteArray())
            val entity = vault.copy(hasAttachments = content.attachments.isNotEmpty()).toVaultEntity(
                ciphertext = encryptedData.ciphertext,
                iv = encryptedData.iv
            )
            vaultDao.upsertVault(entity)
            Result.Success(Unit)
        } catch (e: SQLiteFullException) {
            e.printStackTrace()
            Result.Error(DataError.Local.DISK_FULL)
        } catch (e: GeneralSecurityException) {
            e.printStackTrace()
            Result.Error(DataError.Local.CRYPTO_ERROR)
        } catch (e: SerializationException) {
            e.printStackTrace()
            Result.Error(DataError.Local.SERIALIZATION)
        } catch (e: Exception) {
            e.printStackTrace()
            if (e is CancellationException) throw e
            Result.Error(DataError.Local.UNKNOWN)
        }
    }

    override suspend fun deleteVault(id: Long): EmptyResult<DataError.Local> {
        return try {
            val deletedRows = vaultDao.deleteVaultById(id)
            if (deletedRows == 0) {
                Result.Error(DataError.Local.NOT_FOUND)
            } else {
                Result.Success(Unit)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            if (e is CancellationException) throw e
            Result.Error(DataError.Local.UNKNOWN)
        }
    }
}
