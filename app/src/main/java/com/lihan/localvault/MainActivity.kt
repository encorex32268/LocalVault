package com.lihan.localvault

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lihan.localvault.core.domain.datastore.AppDataStore
import com.lihan.localvault.core.presentation.ui.theme.LocalVaultTheme
import com.lihan.localvault.core.session.SessionManager
import com.lihan.localvault.navigation.NavigationRoot
import org.koin.android.ext.android.inject

class MainActivity : FragmentActivity() {
    private val appDataStore: AppDataStore by inject()
    private val sessionManager: SessionManager by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val isDarkTheme by appDataStore.isDarkTheme.collectAsStateWithLifecycle(initialValue = true)
            LocalVaultTheme(darkTheme = isDarkTheme) {
                NavigationRoot(modifier = Modifier.fillMaxSize())
            }
        }
    }

    override fun onStart() {
        super.onStart()
        sessionManager.onAppForeground()
    }
}
