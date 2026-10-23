package com.lihan.localvault

import android.app.Application
import com.lihan.localvault.core.di.coreModule
import com.lihan.localvault.feature.vault.di.vaultModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class LocalVaultApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@LocalVaultApp)
            modules(
                listOf(
                    coreModule,
                    vaultModule
                ))
        }
    }
}
