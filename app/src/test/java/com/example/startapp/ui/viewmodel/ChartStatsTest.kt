package com.example.startapp.ui.viewmodel

import com.example.startapp.data.model.DailySnapshot
import com.example.startapp.data.model.Transaction
import com.example.startapp.domain.model.ExpenseCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChartStatsTest {

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
