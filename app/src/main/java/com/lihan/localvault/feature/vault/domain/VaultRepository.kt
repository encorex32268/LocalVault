package com.lihan.localvault.feature.vault.domain

import com.lihan.localvault.core.domain.Vault
import com.lihan.localvault.core.domain.VaultContent
import com.lihan.localvault.core.domain.util.DataError
import com.lihan.localvault.core.domain.util.EmptyResult
import com.lihan.localvault.core.domain.util.Result
import kotlinx.coroutines.flow.Flow

interface VaultRepository {
    val vaultItems: Flow<List<Vault>>
    suspend fun getVaultContent(id: Long): Result<VaultContent, DataError.Local>
    suspend fun deleteVault(id: Long): EmptyResult<DataError.Local>
}
