package com.nautesh.challengeclock.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Typed hour/minute kept as text, so a half-typed field ("" or "1") doesn't snap while editing. */
class TimeDraft(private val is24: Boolean) {
    var hourText by mutableStateOf("")
        private set
    var minuteText by mutableStateOf("")
        private set
    var pm by mutableStateOf(false)

    fun set(hour: Int, minute: Int) {
        hourText = "%02d".format(if (is24) hour else (hour + 11) % 12 + 1)
        minuteText = "%02d".format(minute)
        pm = hour >= 12
    }

    /** Puts back fields saved as typed, half-typed ones included. */
    fun restore(hour: String, minute: String, pm: Boolean) {
        hourText = hour
        minuteText = minute
        this.pm = pm
    }

    fun setHour(text: String) {
        hourText = clampDigits(text, if (is24) 0 else 1, if (is24) 23 else 12)
    }

    fun setMinute(text: String) {
        minuteText = clampDigits(text, 0, 59)
    }

    fun tidyHour() {
        hourText = "%02d".format((hourText.toIntOrNull() ?: if (is24) 0 else 12).coerceIn(if (is24) 0 else 1, if (is24) 23 else 12))
    }

    fun tidyMinute() {
        minuteText = "%02d".format(minuteText.toIntOrNull() ?: 0)
    }

    val hour: Int get() = (hourText.toIntOrNull() ?: 0).let { if (is24) it else it % 12 + if (pm) 12 else 0 }
    val minute: Int get() = minuteText.toIntOrNull() ?: 0

    private fun clampDigits(text: String, min: Int, max: Int): String {
        val digits = text.filter(Char::isDigit).takeLast(2)
        val value = digits.toIntOrNull() ?: return ""
        return if (value > max) "%02d".format(max) else if (digits.length == 2 && value < min) "%02d".format(min) else digits
    }
}
