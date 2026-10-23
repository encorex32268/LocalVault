package com.lihan.localvault.core.domain

import com.lihan.localvault.core.domain.util.DataError
import com.lihan.localvault.core.domain.util.EmptyResult
import com.lihan.localvault.core.domain.util.Result
import kotlinx.coroutines.flow.Flow

interface VaultLocalDataSource {
    fun observeVaults(): Flow<List<Vault>>
    suspend fun getVaultById(id: Long): Result<Vault, DataError.Local>
    suspend fun getVaultContent(id: Long): Result<VaultContent, DataError.Local>
    suspend fun upsertVault(vault: Vault, content: VaultContent): EmptyResult<DataError.Local>
    suspend fun deleteVault(id: Long): EmptyResult<DataError.Local>
}
