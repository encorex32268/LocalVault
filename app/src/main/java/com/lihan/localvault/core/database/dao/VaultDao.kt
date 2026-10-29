package com.lihan.localvault.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.lihan.localvault.core.database.entity.VaultEncryptedTuple
import com.lihan.localvault.core.database.entity.VaultEntity
import com.lihan.localvault.core.database.entity.VaultSummaryTuple
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultDao {

    @Query("SELECT id, iconIndex, color, title, category, updatedAt FROM vault_items ORDER BY updatedAt DESC")
    fun observeVaultSummaries(): Flow<List<VaultSummaryTuple>>

    @Query("SELECT id, iconIndex, color, title, category, updatedAt FROM vault_items WHERE id = :id LIMIT 1")
    suspend fun getVaultSummaryById(id: Long): VaultSummaryTuple?

    @Query("SELECT category, ciphertext, iv FROM vault_items WHERE id = :id LIMIT 1")
    suspend fun getEncryptedContentById(id: Long): VaultEncryptedTuple?

    @Upsert
    suspend fun upsertVault(entity: VaultEntity)

    @Query("SELECT * FROM vault_items")
    suspend fun getAllVaultEntities(): List<VaultEntity>

    @Query("DELETE FROM vault_items WHERE id = :id")
    suspend fun deleteVaultById(id: Long): Int
}
