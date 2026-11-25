package com.example.wisatajogja.util

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val idLocale = Locale("id", "ID")

fun formatNumber(value: Int): String = NumberFormat.getNumberInstance(idLocale).format(value)

fun formatRupiah(value: Int): String = if (value <= 0) "Gratis" else "Rp${formatNumber(value)}"

fun formatRating(value: Double): String = String.format(idLocale, "%.1f", value)

fun formatTemperature(value: Int): String = "$value°"

fun seniorGreeting(): String = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
    in 4..10 -> "Selamat pagi,"
    in 11..14 -> "Selamat siang,"
    in 15..17 -> "Selamat sore,"
    else -> "Selamat malam,"
}

fun formatVisitDate(value: String?): String {
    if (value.isNullOrBlank()) return "-"
    val input = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val date = runCatching { input.parse(value) }.getOrNull() ?: return value
    return SimpleDateFormat("d MMMM yyyy", idLocale).format(date)
}

fun formatDateShort(value: String?): String {
    if (value.isNullOrBlank()) return "-"
    val patterns = listOf("yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd")
    patterns.forEach { pattern ->
        val date = runCatching { SimpleDateFormat(pattern, Locale.US).parse(value) }.getOrNull()
        if (date != null) return SimpleDateFormat("d MMM yyyy", idLocale).format(date)
    }
    return value
}

fun monthYearNow(): String = SimpleDateFormat("MMMM yyyy", idLocale).format(Date())

fun todayLabel(): String = SimpleDateFormat("EEEE, d MMMM", idLocale).format(Date())
