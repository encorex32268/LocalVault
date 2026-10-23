package com.lihan.localvault.feature.vault.di

import com.lihan.localvault.feature.vault.data.VaultRepositoryImpl
import com.lihan.localvault.feature.vault.domain.VaultRepository
import com.lihan.localvault.feature.vault.presentation.create.CreateVaultViewModel
import com.lihan.localvault.feature.vault.presentation.vault.VaultViewModel
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val vaultModule = module {
    singleOf(::VaultRepositoryImpl) { bind<VaultRepository>() }
    viewModelOf(::VaultViewModel)
    viewModelOf(::CreateVaultViewModel)
}
