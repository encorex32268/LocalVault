package com.lihan.localvault.core.presentation.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class DateFormatterTest {

    @Test
    fun toFormattedDateTime_zeroOrNegative_returnsEmpty() {
        assertEquals("", 0L.toFormattedDateTime())
        assertEquals("", (-100L).toFormattedDateTime())
    }

    @Test
    fun toFormattedDateTime_formatsWithLocale() {
        val timestamp = System.currentTimeMillis()

        val formattedTw = timestamp.toFormattedDateTime(locale = Locale.TRADITIONAL_CHINESE)
        assertTrue(formattedTw.isNotBlank())

        val formattedUs = timestamp.toFormattedDateTime(locale = Locale.US)
        assertTrue(formattedUs.isNotBlank())
    }
}
