package com.example.startapp.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class SurplusScheduleTest {

    @Test
    fun shouldApplyDailyIncrease_returnsTrueWhenNoSnapshotExists() {
        assertTrue(shouldApplyDailyIncrease(lastSnapshotEpochMs = null, now = fixedNow()))
    }

    @Test
    fun shouldApplyDailyIncrease_returnsFalseWhenSnapshotAlreadyTakenToday() {
        val now = fixedNow()
        val earlierToday = now - 6 * 60 * 60 * 1000L
        assertFalse(shouldApplyDailyIncrease(lastSnapshotEpochMs = earlierToday, now = now))
    }

    @Test
    fun shouldApplyDailyIncrease_returnsTrueWhenSnapshotWasYesterday() {
        val now = fixedNow()
        val yesterday = now - 24 * 60 * 60 * 1000L
        assertTrue(shouldApplyDailyIncrease(lastSnapshotEpochMs = yesterday, now = now))
    }

    @Test
    fun nextSurplusRunDelayMs_targetsNoonTodayWhenBeforeNoon() {
        val now = calendarAt(2026, 8, 5, 10, 0)
        val delayMs = nextSurplusRunDelayMs(now)
        assertEquals(2 * 60 * 60 * 1000L, delayMs)
    }

    @Test
    fun nextSurplusRunDelayMs_targetsNoonTomorrowWhenAfterNoon() {
        val now = calendarAt(2026, 8, 5, 13, 0)
        val delayMs = nextSurplusRunDelayMs(now)
        assertEquals(23 * 60 * 60 * 1000L, delayMs)
    }

    private fun fixedNow(): Long = calendarAt(2026, 8, 5, 15, 0)

    private fun calendarAt(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long {
        return Calendar.getInstance().apply {
            set(year, month - 1, day, hour, minute, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}
