package com.tomk322.rootscope.core

import java.util.Locale
import java.util.concurrent.TimeUnit

/** Shared value formatting, so every section renders sizes and durations the same way. */
object Format {

    private const val UNIT_STEP = 1024.0
    private const val KHZ_PER_MHZ = 1000.0
    private const val HOURS_PER_DAY = 24
    private const val MINUTES_PER_HOUR = 60
    private const val SECONDS_PER_MINUTE = 60
    private val SIZE_UNITS = listOf("KB", "MB", "GB", "TB")

    fun bytes(value: Long): String {
        if (value < UNIT_STEP) return "$value B"
        var scaled = value.toDouble() / UNIT_STEP
        var unit = 0
        while (scaled >= UNIT_STEP && unit < SIZE_UNITS.lastIndex) {
            scaled /= UNIT_STEP
            unit++
        }
        return String.format(Locale.US, "%.2f %s", scaled, SIZE_UNITS[unit])
    }

    fun kilobytes(value: Long): String = bytes(value * UNIT_STEP.toLong())

    fun duration(millis: Long): String {
        val days = TimeUnit.MILLISECONDS.toDays(millis)
        val hours = TimeUnit.MILLISECONDS.toHours(millis) % HOURS_PER_DAY
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % MINUTES_PER_HOUR
        val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % SECONDS_PER_MINUTE
        return buildString {
            if (days > 0) append("${days}d ")
            if (days > 0 || hours > 0) append("${hours}h ")
            append("${minutes}m ${seconds}s")
        }
    }

    /** cpufreq nodes report kHz. */
    fun khzAsMhz(raw: String): String? = raw.trim().toLongOrNull()
        ?.let { String.format(Locale.US, "%.0f MHz", it / KHZ_PER_MHZ) }

    fun String?.orUnknown(): String = this?.takeIf { it.isNotBlank() } ?: "unknown"
}
