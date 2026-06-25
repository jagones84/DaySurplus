package com.example.startapp.data

import com.example.startapp.domain.model.ExpenseCategory
import org.junit.Assert.assertEquals
import org.junit.Test

class TransactionMigrationTest {

    @Test
    fun legacyExpenseWithoutCategoryGetsInferredCategory() {
        val migrated = normalizeTransactionCategory(
            amount = -42.0,
            description = "Lidl groceries",
            category = ""
        )

        assertEquals(ExpenseCategory.FOOD.label, migrated)
    }

    @Test
    fun legacyIncomeWithoutCategoryBecomesIncome() {
        val migrated = normalizeTransactionCategory(
            amount = 1000.0,
            description = "salary",
            category = ""
        )

        assertEquals("Income", migrated)
    }

    @Test
    fun legacyNullCategoryGetsInferredCategory() {
        val migrated = normalizeTransactionCategory(
            amount = -15.0,
            description = "train ticket",
            category = null
        )

        assertEquals(ExpenseCategory.TRANSPORT.label, migrated)
    }

    @Test
    fun previousOtherCategoryGetsReclassifiedWhenDescriptionIsMoreSpecific() {
        val migrated = normalizeTransactionCategory(
            amount = -12.0,
            description = "farmacia",
            category = ExpenseCategory.OTHER.label
        )

        assertEquals(ExpenseCategory.HEALTH.label, migrated)
    }

    @Test
    fun legacyOtherEntriesGetPromotedUsingRealWorldOverrides() {
        assertEquals(
            ExpenseCategory.HEALTH.label,
            normalizeTransactionCategory(
                amount = -360.0,
                description = "isber",
                category = ExpenseCategory.OTHER.label
            )
        )
        assertEquals(
            ExpenseCategory.SHOPPING.label,
            normalizeTransactionCategory(
                amount = -30.0,
                description = "temu",
                category = ExpenseCategory.OTHER.label
            )
        )
        assertEquals(
            ExpenseCategory.LEISURE.label,
            normalizeTransactionCategory(
                amount = -100.0,
                description = "1 maggio",
                category = ExpenseCategory.OTHER.label
            )
        )
    }

    @Test
    fun legacyNamedCategoriesCollapseIntoNewTaxonomy() {
        assertEquals(
            ExpenseCategory.SHOPPING.label,
            normalizeTransactionCategory(-25.0, "lampada", "Home")
        )
        assertEquals(
            ExpenseCategory.DIGITAL.label,
            normalizeTransactionCategory(-10.0, "trae", "Work")
        )
        assertEquals(
            ExpenseCategory.DIGITAL.label,
            normalizeTransactionCategory(-10.0, "patreon", "Subscriptions")
        )
    }
}
