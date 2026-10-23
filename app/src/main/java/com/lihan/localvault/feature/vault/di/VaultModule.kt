package com.lihan.localvault.feature.vault.di

import com.lihan.localvault.feature.vault.presentation.vault.VaultViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val vaultModule = module {
    viewModelOf(::VaultViewModel)
}