package com.lihan.localvault.core.di

import com.lihan.localvault.core.database.AppDatabase
import com.lihan.localvault.core.database.DatabaseFactory
import com.lihan.localvault.core.database.RoomVaultDataSource
import com.lihan.localvault.core.crypto.AndroidKeyStoreCryptoManager
import com.lihan.localvault.core.crypto.CryptoManager
import com.lihan.localvault.core.domain.VaultLocalDataSource
import com.lihan.localvault.core.presentation.ui.theme.ThemeManager
import kotlinx.serialization.json.Json
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val coreModule = module {
    single { ThemeManager(androidContext()) }
    single {
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    }
    single { DatabaseFactory.create(androidContext()) }
    single { get<AppDatabase>().vaultDao }
    singleOf(::AndroidKeyStoreCryptoManager) { bind<CryptoManager>() }
    singleOf(::RoomVaultDataSource) { bind<VaultLocalDataSource>() }
}
