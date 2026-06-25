package com.example.startapp.domain

import com.example.startapp.data.model.Transaction
import com.example.startapp.domain.model.buildGroupedTransactionState
import org.junit.Assert.assertEquals
import org.junit.Test

class GroupedTransactionsTest {

    @Test
    fun expensesAreGroupedByCategoryWhileIncomeStaysSeparate() {
        val now = 1_700_000_000_000L
        val grouped = buildGroupedTransactionState(
            listOf(
                Transaction(amount = -20.0, date = now, description = "Lidl", category = "Food"),
                Transaction(amount = -10.0, date = now - 1, description = "Train", category = "Transport"),
                Transaction(amount = 100.0, date = now - 2, description = "Salary", category = "Income")
            )
        )

        assertEquals(1, grouped.incomeTransactions.size)
        assertEquals(2, grouped.expenseGroups.size)
        assertEquals("Food", grouped.expenseGroups.first().category)
    }

    @Test
    fun cleanedTaxonomyGroupsShoppingAndDigitalWithoutLegacyBuckets() {
        val now = 1_700_000_000_000L
        val grouped = buildGroupedTransactionState(
            listOf(
                Transaction(amount = -25.0, date = now, description = "lampada", category = "Shopping"),
                Transaction(amount = -10.0, date = now - 1, description = "trae", category = "Digital"),
                Transaction(amount = 100.0, date = now - 2, description = "Salary", category = "Income")
            )
        )

        assertEquals(listOf("Shopping", "Digital"), grouped.expenseGroups.map { it.category })
    }
}
