package com.example.startapp.domain

import com.example.startapp.domain.model.CategoryCatalog
import com.example.startapp.domain.model.CategoryType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryCatalogTest {

    @Test
    fun incomeCatalog_containsBuiltInIncomeCategoriesAndNewCategoryEntry() {
        val incomeOptions = CategoryCatalog.dropdownOptions(CategoryType.INCOME, emptyList())

        assertTrue(incomeOptions.contains("Daily Surplus"))
        assertTrue(incomeOptions.contains("Salary"))
        assertTrue(incomeOptions.contains("Other Income"))
        assertEquals("+ New Category", incomeOptions.last())
    }

    @Test
    fun normalizeCustomCategoryName_trimsAndTitleCasesText() {
        assertEquals("Side Hustle", CategoryCatalog.normalizeCustomCategoryName("  side   hustle  "))
    }

    @Test
    fun canAddCustomCategory_blocksDuplicatesAgainstBuiltInAndCustomLabels() {
        val existing = listOf("Family Support")

        assertFalse(CategoryCatalog.canAddCustomCategory(CategoryType.INCOME, "salary", existing))
        assertFalse(CategoryCatalog.canAddCustomCategory(CategoryType.INCOME, " family support ", existing))
        assertTrue(CategoryCatalog.canAddCustomCategory(CategoryType.INCOME, "Dividends", existing))
    }
}
