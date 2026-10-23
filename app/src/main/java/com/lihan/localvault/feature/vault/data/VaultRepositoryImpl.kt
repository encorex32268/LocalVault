package com.lihan.localvault.feature.vault.data

import com.lihan.localvault.core.domain.Vault
import com.lihan.localvault.core.domain.VaultContent
import com.lihan.localvault.core.domain.VaultLocalDataSource
import com.lihan.localvault.core.domain.util.DataError
import com.lihan.localvault.core.domain.util.EmptyResult
import com.lihan.localvault.core.domain.util.Result
import com.lihan.localvault.feature.vault.domain.VaultRepository
import kotlinx.coroutines.flow.Flow

class VaultRepositoryImpl(
    private val vaultLocalDataSource: VaultLocalDataSource
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
        return vaultLocalDataSource.deleteVault(id)
    }

    override suspend fun upsertVault(
        vault: Vault,
        content: VaultContent
    ): EmptyResult<DataError.Local> {
        return vaultLocalDataSource.upsertVault(vault, content)
    }
}
