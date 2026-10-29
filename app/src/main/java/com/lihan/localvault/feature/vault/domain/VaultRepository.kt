package com.lihan.localvault.feature.vault.domain

import com.lihan.localvault.core.domain.Vault
import com.lihan.localvault.core.domain.VaultContent
import com.lihan.localvault.core.domain.util.DataError
import com.lihan.localvault.core.domain.util.EmptyResult
import com.lihan.localvault.core.domain.util.Result
import kotlinx.coroutines.flow.Flow

interface VaultRepository {
    val vaultItems: Flow<List<Vault>>
    suspend fun getVaultById(id: Long): Result<Vault, DataError.Local>
    suspend fun getVaultContent(id: Long): Result<VaultContent, DataError.Local>
    suspend fun deleteVault(id: Long): EmptyResult<DataError.Local>
    suspend fun upsertVault(vault: Vault, content: VaultContent): EmptyResult<DataError.Local>
    suspend fun exportBackup(passphrase: String): Result<String, DataError.Local>
    suspend fun importBackup(passphrase: String, backupJson: String): Result<Int, DataError.Local>
}
