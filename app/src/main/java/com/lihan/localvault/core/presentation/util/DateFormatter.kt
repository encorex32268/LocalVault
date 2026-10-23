package com.lihan.localvault.core.presentation.util

import java.text.DateFormat
import java.util.Date
import java.util.Locale

fun Long.toFormattedDateTime(locale: Locale = Locale.getDefault()): String {
    if (this <= 0L) return ""
    val date = Date(this)
    val dateStr = DateFormat.getDateInstance(DateFormat.MEDIUM, locale).format(date)
    val timeStr = DateFormat.getTimeInstance(DateFormat.SHORT, locale).format(date)
    return "$dateStr $timeStr"
}
