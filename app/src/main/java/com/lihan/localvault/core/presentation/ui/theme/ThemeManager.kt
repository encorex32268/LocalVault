package com.lihan.localvault.core.presentation.ui.theme

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class ThemeManager(context: Context) {
    private val prefs = context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
    private val _isDarkTheme = MutableStateFlow(prefs.getBoolean(KEY_IS_DARK_THEME, true))
    val isDarkTheme = _isDarkTheme.asStateFlow()

    fun toggleTheme() {
        val newTheme = !_isDarkTheme.value
        prefs.edit().putBoolean(KEY_IS_DARK_THEME, newTheme).apply()
        _isDarkTheme.value = newTheme
    }

    fun setDarkTheme(isDark: Boolean) {
        prefs.edit().putBoolean(KEY_IS_DARK_THEME, isDark).apply()
        _isDarkTheme.value = isDark
    }

    companion object {
        private const val KEY_IS_DARK_THEME = "is_dark_theme"
    }
}
