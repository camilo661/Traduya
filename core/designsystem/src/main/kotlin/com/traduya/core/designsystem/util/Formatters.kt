package com.traduya.core.designsystem.util

import android.telephony.PhoneNumberUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private val spanish = Locale("es", "ES")

/** 75 → "01:15"; 3700 → "1:01:40" */
fun formatElapsed(totalSeconds: Long): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(spanish, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(spanish, "%02d:%02d", minutes, seconds)
    }
}

/** 75 → "1 min 15 s" */
fun formatDuration(totalSeconds: Long): String {
    if (totalSeconds <= 0) return "Sin conectar"
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return when {
        minutes == 0L -> "$seconds s"
        seconds == 0L -> "$minutes min"
        else -> "$minutes min $seconds s"
    }
}

/** "Hoy, 14:32", "Ayer, 09:10" o "12 oct, 18:45" */
fun formatCallDate(millis: Long): String {
    val time = SimpleDateFormat("HH:mm", spanish).format(millis)
    val day = Calendar.getInstance().apply { timeInMillis = millis }
    val today = Calendar.getInstance()
    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    return when {
        day.isSameDay(today) -> "Hoy, $time"
        day.isSameDay(yesterday) -> "Ayer, $time"
        else -> SimpleDateFormat("d MMM, HH:mm", spanish).format(millis)
    }
}

fun formatPhoneNumber(number: String): String =
    PhoneNumberUtils.formatNumber(number, Locale.getDefault().country) ?: number

private fun Calendar.isSameDay(other: Calendar) =
    get(Calendar.YEAR) == other.get(Calendar.YEAR) &&
        get(Calendar.DAY_OF_YEAR) == other.get(Calendar.DAY_OF_YEAR)
