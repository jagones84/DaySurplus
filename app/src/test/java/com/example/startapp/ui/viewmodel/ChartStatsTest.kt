package com.example.startapp.ui.viewmodel

import com.example.startapp.data.model.DailySnapshot
import com.example.startapp.data.model.Transaction
import com.example.startapp.domain.model.ExpenseCategory
import org.junit.Assert.assertEquals
import org.junit.Test

class ChartStatsTest {

    @Test
    fun expenseRatioIsOverallExpensesDividedByOverallIncome() {
        assertEquals(0.25, calculateExpenseRatio(50.0, 200.0), 0.0001)
    }

    @Test
    fun savingRatioRepresentsSavedFractionOfTotalIncome() {
        assertEquals(0.75, calculateSavingsRatio(0.25), 0.0001)
    }

    @Test
    fun overallTotalsStayTheSameAcrossChartTimeframes() {
        val now = 1_700_000_000_000L
        val snapshots = listOf(
            DailySnapshot(date = now - 3_000L, amount = 100.0),
            DailySnapshot(date = now - 2_000L, amount = 120.0),
            DailySnapshot(date = now - 1_000L, amount = 140.0)
        )
        val transactions = listOf(
            Transaction(amount = -10.0, date = now - 2_500L, description = "farmacia", category = ExpenseCategory.HEALTH.label),
            Transaction(amount = -5.0, date = now - 1_500L, description = "steam", category = ExpenseCategory.GAMING.label)
        )

        val totals = calculateOverallTotals(
            snapshots = snapshots,
            transactions = transactions
        )

        assertEquals(15.0, totals.totalExpenses, 0.0001)
        assertEquals(55.0, totals.totalIncome, 0.0001)
        assertEquals(15.0 / 55.0, totals.expenseRatio, 0.0001)
        assertEquals(1 - (15.0 / 55.0), calculateSavingsRatio(totals.expenseRatio), 0.0001)
    }
}
