package com.example.startapp.domain

import java.util.Calendar

fun shouldApplyDailyIncrease(lastSnapshotEpochMs: Long?, now: Long): Boolean {
    if (lastSnapshotEpochMs == null) {
        return true
    }
    return !isSameCalendarDay(lastSnapshotEpochMs, now)
}

fun nextSurplusRunDelayMs(now: Long): Long {
    val due = Calendar.getInstance().apply {
        timeInMillis = now
        set(Calendar.HOUR_OF_DAY, 12)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        if (timeInMillis <= now) {
            add(Calendar.DAY_OF_MONTH, 1)
        }
    }
    return due.timeInMillis - now
}

fun calendarDayKey(epochMs: Long): Long {
    val calendar = Calendar.getInstance().apply { timeInMillis = epochMs }
    return calendar.get(Calendar.YEAR) * 10_000L +
        (calendar.get(Calendar.MONTH) + 1) * 100L +
        calendar.get(Calendar.DAY_OF_MONTH)
}

private fun isSameCalendarDay(a: Long, b: Long): Boolean =
    calendarDayKey(a) == calendarDayKey(b)
