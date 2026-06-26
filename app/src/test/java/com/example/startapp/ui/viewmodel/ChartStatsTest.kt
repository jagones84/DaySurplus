package com.example.startapp.ui.viewmodel

import com.example.startapp.data.model.DailySnapshot
import com.example.startapp.data.model.Transaction
import com.example.startapp.domain.model.ExpenseCategory
import com.example.startapp.domain.model.TimeFrame
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChartStatsTest {

    @Test
    fun calculateChartStatsForPeriod_filtersAllAnalyticsToSelectedPeriod() {
        val now = 1_700_000_000_000L
        val oldDate = now - (40L * 24 * 60 * 60 * 1000)
        val inRangeDate = now - (5L * 24 * 60 * 60 * 1000)
        val laterInRangeDate = now - (2L * 24 * 60 * 60 * 1000)

        val stats = calculateChartStatsForPeriod(
            snapshots = listOf(
                DailySnapshot(date = oldDate, amount = 20.0),
                DailySnapshot(date = inRangeDate, amount = 100.0),
                DailySnapshot(date = laterInRangeDate, amount = 130.0)
            ),
            transactions = listOf(
                Transaction(
                    amount = -50.0,
                    date = oldDate,
                    description = "old rent",
                    category = ExpenseCategory.OTHER.label
                ),
                Transaction(
                    amount = -10.0,
                    date = now - (3L * 24 * 60 * 60 * 1000),
                    description = "farmacia",
                    category = ExpenseCategory.HEALTH.label
                )
            ),
            daysToDisplay = 30,
            timeFrame = TimeFrame.Day,
            dailyIncrease = 0.0,
            now = now
        )

        assertEquals(10.0, stats.totalExpenses, 0.0001)
        assertEquals(40.0, stats.totalIncome, 0.0001)
        assertEquals(30.0 / 40.0, stats.savingsRatio, 0.0001)
        assertEquals(1, stats.categoryExpenses.size)
        assertEquals("Health", stats.categoryExpenses.single().category)
        assertEquals(10.0, stats.categoryExpenses.single().total, 0.0001)
        assertEquals(1, stats.topExpenseDescriptions.size)
        assertEquals("farmacia", stats.topExpenseDescriptions.single().label)
    }

    @Test
    fun calculateChartStatsForPeriod_returnsEmptyAnalyticsWhenPeriodHasNoSnapshots() {
        val now = 1_700_000_000_000L

        val stats = calculateChartStatsForPeriod(
            snapshots = listOf(
                DailySnapshot(date = now - (50L * 24 * 60 * 60 * 1000), amount = 20.0)
            ),
            transactions = listOf(
                Transaction(
                    amount = -25.0,
                    date = now - (40L * 24 * 60 * 60 * 1000),
                    description = "spesa",
                    category = ExpenseCategory.FOOD.label
                )
            ),
            daysToDisplay = 7,
            timeFrame = TimeFrame.Day,
            dailyIncrease = 0.0,
            now = now
        )

        assertTrue(stats.points.isEmpty())
        assertEquals(0.0, stats.totalExpenses, 0.0)
        assertEquals(0.0, stats.totalIncome, 0.0)
        assertEquals(0.0, stats.savingsRatio, 0.0)
        assertTrue(stats.categoryExpenses.isEmpty())
        assertTrue(stats.topExpenseDescriptions.isEmpty())
    }

    @Test
    fun savingRatioToDate_isNetSurplusDividedByTotalIncomeToDate() {
        val now = 1_700_000_000_000L
        val snapshots = listOf(
            DailySnapshot(date = now - 3_000L, amount = 0.0),
            DailySnapshot(date = now - 2_000L, amount = 120.0),
            DailySnapshot(date = now - 1_000L, amount = 140.0)
        )
        val transactions = listOf(
            Transaction(amount = -10.0, date = now - 2_500L, description = "farmacia", category = ExpenseCategory.HEALTH.label),
            Transaction(amount = -5.0, date = now - 1_500L, description = "steam", category = ExpenseCategory.GAMING.label)
        )

        val savingRatio = calculateSavingRatioToDate(
            snapshots = snapshots,
            transactions = transactions
        )

        assertEquals(140.0 / 155.0, savingRatio, 0.0001)
    }

    @Test
    fun incomeCategorySlicesIncludeSyntheticDailySurplus() {
        val now = 1_700_000_000_000L
        val slices = buildIncomeCategorySlices(
            transactions = listOf(
                Transaction(amount = 1000.0, date = now, description = "stipendio", category = "Salary"),
                Transaction(amount = 100.0, date = now - 1_000L, description = "rimborso", category = "Refund"),
            ),
            coveredDays = 30,
            dailyIncrease = 20.0
        )

        assertTrue(slices.any { it.category == "Daily Surplus" && it.total == 600.0 })
        assertTrue(slices.any { it.category == "Salary" && it.total == 1000.0 })
    }

    @Test
    fun monthlyAverageIsNormalizedByCoveredDays() {
        assertEquals(304.375, calculateMonthlyAverage(total = 300.0, coveredDays = 30), 0.0001)
    }
}
