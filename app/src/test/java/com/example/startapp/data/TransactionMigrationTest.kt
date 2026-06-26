package com.example.startapp.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.example.startapp.domain.model.CategoryType
import com.example.startapp.domain.model.ExpenseCategory
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

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
    fun legacyIncomeWithoutCategoryGetsInferredIncomeCategory() {
        assertEquals(
            "Salary",
            normalizeTransactionCategory(
                amount = 1000.0,
                description = "stipendio marzo",
                category = ""
            )
        )
        assertEquals(
            "Refund",
            normalizeTransactionCategory(
                amount = 80.0,
                description = "rimborso assicurazione",
                category = ""
            )
        )
        assertEquals(
            "Other Income",
            normalizeTransactionCategory(
                amount = 20.0,
                description = "entrata strana",
                category = ""
            )
        )
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

    @Test
    fun addCustomCategory_persistsSeparatedExpenseAndIncomeCatalogs() = runBlocking {
        val repository = buildRepository()

        repository.addCustomCategory(CategoryType.EXPENSE, "Casa Vacanze")
        repository.addCustomCategory(CategoryType.INCOME, "Dividends")

        assertEquals(listOf("Casa Vacanze"), repository.expenseCustomCategories.first())
        assertEquals(listOf("Dividends"), repository.incomeCustomCategories.first())
    }

    private fun buildRepository(): CounterDataRepository {
        val tempFile = File.createTempFile("counter-data-repository-test", ".preferences_pb").apply {
            deleteOnExit()
        }
        val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(
            produceFile = { tempFile }
        )
        return CounterDataRepository(dataStore)
    }
}
