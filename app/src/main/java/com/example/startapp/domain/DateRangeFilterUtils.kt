package com.example.startapp.domain

import com.example.startapp.data.model.DailySnapshot
import com.example.startapp.data.model.Transaction
import com.example.startapp.domain.model.DateRangeFilter
import java.util.Calendar
import kotlin.math.max

fun createNormalizedDateRange(startEpochMs: Long, endEpochMs: Long): DateRangeFilter {
    val lower = minOf(startEpochMs, endEpochMs)
    val upper = maxOf(startEpochMs, endEpochMs)
    return DateRangeFilter(
        startEpochMs = startOfDay(lower),
        endEpochMs = endOfDay(upper)
    )
}

fun defaultDateRange(now: Long = System.currentTimeMillis()): DateRangeFilter {
    val end = endOfDay(now)
    val calendar = Calendar.getInstance().apply { timeInMillis = end }
    calendar.add(Calendar.DAY_OF_YEAR, -30)
    return DateRangeFilter(
        startEpochMs = startOfDay(calendar.timeInMillis),
        endEpochMs = end
    )
}

fun filterTransactionsByDateRange(
    transactions: List<Transaction>,
    range: DateRangeFilter
): List<Transaction> = transactions.filter { it.date in range.startEpochMs..range.endEpochMs }

fun filterSnapshotsByDateRange(
    snapshots: List<DailySnapshot>,
    range: DateRangeFilter
): List<DailySnapshot> = snapshots
    .filter { it.date in range.startEpochMs..range.endEpochMs }
    .sortedBy { it.date }

fun calculateCoveredDays(range: DateRangeFilter): Int {
    val millis = max(range.endEpochMs - range.startEpochMs, 0L)
    return max(((millis / MILLIS_PER_DAY) + 1L).toInt(), 1)
}

private fun startOfDay(epochMs: Long): Long =
    Calendar.getInstance().apply {
        timeInMillis = epochMs
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

private fun endOfDay(epochMs: Long): Long =
    Calendar.getInstance().apply {
        timeInMillis = epochMs
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
    }.timeInMillis

private const val MILLIS_PER_DAY = 24L * 60L * 60L * 1000L
