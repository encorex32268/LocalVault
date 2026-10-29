package com.lihan.localvault.core.session

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionManagerTest {

    @Test
    fun isSessionUnlocked_initiallyFalse() {
        val sessionManager = SessionManager()
        assertFalse(sessionManager.isSessionUnlocked.value)
    }

    @Test
    fun unlock_setsIsSessionUnlockedToTrue() {
        val sessionManager = SessionManager()
        sessionManager.unlock()
        assertTrue(sessionManager.isSessionUnlocked.value)
    }

    @Test
    fun lock_setsIsSessionUnlockedToFalse() {
        val sessionManager = SessionManager()
        sessionManager.unlock()
        assertTrue(sessionManager.isSessionUnlocked.value)

        sessionManager.lock()
        assertFalse(sessionManager.isSessionUnlocked.value)
    }

    @Test
    fun onAppForeground_withinTimeout_remainsUnlocked() {
        var currentTime = 1_000_000L
        val timeout = 5 * 60 * 1000L // 5 minutes
        val sessionManager = SessionManager(
            timeoutMillis = timeout,
            timeProvider = { currentTime }
        )

        sessionManager.unlock()
        assertTrue(sessionManager.isSessionUnlocked.value)

        // Advance 4 minutes (240,000 ms < 300,000 ms)
        currentTime += 4 * 60 * 1000L
        sessionManager.onAppForeground()

        assertTrue(sessionManager.isSessionUnlocked.value)
        assertFalse(sessionManager.isSessionExpired())
    }

    @Test
    fun onAppForeground_afterTimeout_locksSession() {
        var currentTime = 1_000_000L
        val timeout = 5 * 60 * 1000L // 5 minutes
        val sessionManager = SessionManager(
            timeoutMillis = timeout,
            timeProvider = { currentTime }
        )

        sessionManager.unlock()
        assertTrue(sessionManager.isSessionUnlocked.value)

        // Advance 5 minutes and 1 millisecond
        currentTime += 5 * 60 * 1000L + 1L
        assertTrue(sessionManager.isSessionExpired())

        sessionManager.onAppForeground()
        assertFalse(sessionManager.isSessionUnlocked.value)
    }

    @Test
    fun unlock_afterTimeoutLock_resetsWindow() {
        var currentTime = 1_000_000L
        val timeout = 5 * 60 * 1000L
        val sessionManager = SessionManager(
            timeoutMillis = timeout,
            timeProvider = { currentTime }
        )

        sessionManager.unlock()
        currentTime += 6 * 60 * 1000L
        sessionManager.onAppForeground()
        assertFalse(sessionManager.isSessionUnlocked.value)

        // Re-unlock
        sessionManager.unlock()
        assertTrue(sessionManager.isSessionUnlocked.value)

        // Advance 2 minutes from new unlock
        currentTime += 2 * 60 * 1000L
        sessionManager.onAppForeground()
        assertTrue(sessionManager.isSessionUnlocked.value)
    }
}
