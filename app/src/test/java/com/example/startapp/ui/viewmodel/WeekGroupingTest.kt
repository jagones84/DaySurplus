package com.example.startapp.ui.viewmodel

import com.example.startapp.domain.model.TimeFrame
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.util.Calendar
import java.util.Locale

class WeekGroupingTest {

    @Test
    fun weekKey_groupsYearBoundaryDaysInTheSameIsoWeek() {
        val dec29 = dateAt(2025, 12, 29)
        val jan2 = dateAt(2026, 1, 2)

        for (locale in listOf(Locale.US, Locale.ITALY, Locale.JAPAN)) {
            assertEquals(
                "locale=$locale",
                getPeriodKey(dec29, TimeFrame.Week, locale),
                getPeriodKey(jan2, TimeFrame.Week, locale)
            )
        }
    }

    @Test
    fun weekKey_separatesConsecutiveWeeks() {
        val jan2 = dateAt(2026, 1, 2)
        val jan8 = dateAt(2026, 1, 8)

        val firstWeek = getPeriodKey(jan2, TimeFrame.Week, Locale.US)
        val secondWeek = getPeriodKey(jan8, TimeFrame.Week, Locale.US)

        assertNotEquals(firstWeek, secondWeek)
    }

    @Test
    fun monthKey_keepsCalendarMonths() {
        val dec29 = dateAt(2025, 12, 29)

        assertEquals("202512", getPeriodKey(dec29, TimeFrame.Month, Locale.US))
    }

    private fun dateAt(year: Int, month: Int, day: Int): Long {
        return Calendar.getInstance().apply {
            set(year, month - 1, day, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}
