package com.example.startapp.domain

import com.example.startapp.data.model.Transaction
import com.example.startapp.domain.model.buildGroupedTransactionState
import org.junit.Assert.assertEquals
import org.junit.Test

class GroupedTransactionsTest {

    @Test
    fun transactionsAreGroupedByCategoryForBothIncomeAndExpenses() {
        val now = 1_700_000_000_000L
        val grouped = buildGroupedTransactionState(
            listOf(
                Transaction(amount = -20.0, date = now, description = "Lidl", category = "Food"),
                Transaction(amount = -10.0, date = now - 1, description = "Train", category = "Transport"),
                Transaction(amount = 100.0, date = now - 2, description = "Salary", category = "Salary"),
                Transaction(amount = 50.0, date = now - 3, description = "Gift", category = "Bonus")
            )
        )

        assertEquals(2, grouped.incomeGroups.size)
        assertEquals(2, grouped.expenseGroups.size)
        assertEquals("Salary", grouped.incomeGroups.first().category)
        assertEquals(100.0, grouped.incomeGroups.first().total, 0.001)
        assertEquals("Food", grouped.expenseGroups.first().category)
    }

    @Test
    fun cleanedTaxonomyGroupsShoppingAndDigitalWithoutLegacyBuckets() {
        val now = 1_700_000_000_000L
        val grouped = buildGroupedTransactionState(
            listOf(
                Transaction(amount = -25.0, date = now, description = "lampada", category = "Shopping"),
                Transaction(amount = -10.0, date = now - 1, description = "trae", category = "Digital"),
                Transaction(amount = 100.0, date = now - 2, description = "Salary", category = "Salary")
            )
        )

        assertEquals(listOf("Shopping", "Digital"), grouped.expenseGroups.map { it.category })
    }

    @Test
    fun blankIncomeCategoryFallsBackToOtherIncome() {
        val now = 1_700_000_000_000L
        val grouped = buildGroupedTransactionState(
            listOf(
                Transaction(amount = 42.0, date = now, description = "mistero", category = "")
            )
        )

        assertEquals(listOf("Other Income"), grouped.incomeGroups.map { it.category })
        assertEquals(42.0, grouped.incomeGroups.single().total, 0.001)
    }
}
