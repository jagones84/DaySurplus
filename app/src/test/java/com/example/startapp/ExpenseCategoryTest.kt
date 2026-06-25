package com.example.startapp

import com.example.startapp.domain.model.ExpenseCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpenseCategoryTest {

    @Test
    fun knownKeywordsMapToStableCategories() {
        assertEquals(ExpenseCategory.FOOD.label, ExpenseCategory.inferFromDescription("Lidl groceries"))
        assertEquals(ExpenseCategory.TRANSPORT.label, ExpenseCategory.inferFromDescription("train ticket"))
        assertEquals(ExpenseCategory.BILLS.label, ExpenseCategory.inferFromDescription("internet bill"))
    }

    @Test
    fun categoryCatalogContainsCategoriesEvenBeforeTransactionsExist() {
        val labels = ExpenseCategory.expenseCategories.map { it.label }
        assertTrue(labels.contains("Food"))
        assertTrue(labels.contains("Travel"))
        assertTrue(labels.contains("Pets"))
    }
}
