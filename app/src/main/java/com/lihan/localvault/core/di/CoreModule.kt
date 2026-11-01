package com.lihan.localvault.core.di


import com.lihan.localvault.core.domain.security.backup.BackupCryptoManager
import com.lihan.localvault.core.presentation.util.biometric.BiometricPromptManager
import com.lihan.localvault.core.data.datastore.DefaultAppDataStore
import com.lihan.localvault.core.data.datastore.appDataStore
import com.lihan.localvault.core.domain.datastore.AppDataStore
import org.koin.android.ext.koin.androidContext
import com.lihan.localvault.core.database.AppDatabase
import com.lihan.localvault.core.database.DatabaseFactory
import com.lihan.localvault.core.database.RoomVaultDataSource
import com.lihan.localvault.core.data.security.crypto.AndroidKeyStoreCryptoManager
import com.lihan.localvault.core.domain.security.crypto.CryptoManager
import com.lihan.localvault.core.domain.VaultLocalDataSource
import com.lihan.localvault.core.domain.session.SessionManager
import com.lihan.localvault.core.data.security.storage.AndroidEncryptedFileManager
import com.lihan.localvault.core.domain.security.storage.EncryptedFileManager
import kotlinx.serialization.json.Json
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val coreModule = module {
    single { androidContext().appDataStore }
    singleOf(::DefaultAppDataStore) { bind<AppDataStore>() }
    singleOf(::BiometricPromptManager)
    singleOf(::BackupCryptoManager)
    singleOf(::AndroidEncryptedFileManager) { bind<EncryptedFileManager>() }
    single { SessionManager() }

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
