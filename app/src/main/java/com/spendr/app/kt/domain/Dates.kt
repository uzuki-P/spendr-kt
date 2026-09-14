package com.spendr.app.kt.domain

import com.spendr.app.kt.domain.model.DateRange
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val zone: ZoneId = ZoneId.systemDefault()

fun localDate(epochMs: Long): LocalDate =
    Instant.ofEpochMilli(epochMs).atZone(zone).toLocalDate()

/**
 * Month range with an inclusive end bound (last day, 23:59:59.999.999.999 local),
 * matching the RN app's `monthRange`.
 */
fun monthRange(epochMs: Long): DateRange {
    val yearMonth = localDate(epochMs).let { java.time.YearMonth.from(it) }
    val start = yearMonth.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
    val end = yearMonth.atEndOfMonth().atTime(23, 59, 59, 999_999_999).atZone(zone).toInstant().toEpochMilli()
    return DateRange(start, end)
}

/** Noon-of-the-1st epoch ms; the identity token for "a month". */
fun monthCursor(epochMs: Long): Long {
    val yearMonth = java.time.YearMonth.from(localDate(epochMs))
    return yearMonth.atDay(1).atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
}

fun shiftMonth(cursorEpochMs: Long, offset: Int): Long {
    val yearMonth = java.time.YearMonth.from(
        Instant.ofEpochMilli(cursorEpochMs).atZone(zone).toLocalDate(),
    ).plusMonths(offset.toLong())
    return yearMonth.atDay(1).atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
}

fun currentMonthRange(): DateRange = monthRange(System.currentTimeMillis())

fun daysInMonth(range: DateRange): Int = localDate(range.end).dayOfMonth

private fun pattern(pattern: String) = DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH)

private val mediumFormat = pattern("d MMM yyyy")
private val fullDateFormat = pattern("EEEE, d MMMM yyyy")
private val dateTimeFormat = pattern("d MMM yyyy, HH.mm")
private val monthYearFormat = pattern("MMMM yyyy")
private val dayShortFormat = pattern("EEE, d MMM")

fun formatDate(epochMs: Long): String = mediumFormat.format(localDate(epochMs))

fun formatFullDate(epochMs: Long): String = fullDateFormat.format(localDate(epochMs))

fun formatDateTime(epochMs: Long): String =
    dateTimeFormat.format(Instant.ofEpochMilli(epochMs).atZone(zone))

fun formatMonthYear(epochMs: Long): String = monthYearFormat.format(localDate(epochMs))

fun formatDayShort(epochMs: Long): String = dayShortFormat.format(localDate(epochMs))
