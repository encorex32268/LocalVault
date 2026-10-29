package com.lihan.localvault.core.session

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SessionManager(
    private val timeoutMillis: Long = DEFAULT_TIMEOUT_MILLIS,
    private val timeProvider: () -> Long = { System.currentTimeMillis() }
) {
    companion object {
        const val DEFAULT_TIMEOUT_MILLIS = 5 * 60 * 1000L // 5 minutes
    }

    private val _isSessionUnlocked = MutableStateFlow(false)
    val isSessionUnlocked: StateFlow<Boolean> = _isSessionUnlocked.asStateFlow()

    private var lastUnlockTimestamp: Long = 0L

    fun unlock() {
        lastUnlockTimestamp = timeProvider()
        _isSessionUnlocked.value = true
    }

    fun lock() {
        lastUnlockTimestamp = 0L
        _isSessionUnlocked.value = false
    }

    fun isSessionExpired(): Boolean {
        if (!_isSessionUnlocked.value) return true
        val elapsed = timeProvider() - lastUnlockTimestamp
        return elapsed >= timeoutMillis
    }

    fun onAppForeground() {
        if (_isSessionUnlocked.value && isSessionExpired()) {
            lock()
        }
    }
}
