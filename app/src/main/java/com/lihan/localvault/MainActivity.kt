package com.lihan.localvault

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lihan.localvault.core.presentation.ui.theme.LocalVaultTheme
import com.lihan.localvault.core.presentation.ui.theme.ThemeManager
import com.lihan.localvault.navigation.NavigationRoot
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    private val themeManager: ThemeManager by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val isDarkTheme by themeManager.isDarkTheme.collectAsStateWithLifecycle()
            LocalVaultTheme(darkTheme = isDarkTheme) {
                NavigationRoot(modifier = Modifier.fillMaxSize())
            }
        }
    }
}
