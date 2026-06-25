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
        assertEquals(ExpenseCategory.DIGITAL.label, ExpenseCategory.inferFromDescription("internet bill"))
        assertEquals(ExpenseCategory.HEALTH.label, ExpenseCategory.inferFromDescription("farmacia antibiotico"))
        assertEquals(ExpenseCategory.GAMING.label, ExpenseCategory.inferFromDescription("steam game"))
        assertEquals(ExpenseCategory.LEISURE.label, ExpenseCategory.inferFromDescription("uscita con amici"))
    }

    @Test
    fun categoryCatalogContainsCategoriesEvenBeforeTransactionsExist() {
        val labels = ExpenseCategory.expenseCategories.map { it.label }
        assertTrue(labels.contains("Food"))
        assertTrue(labels.contains("Travel"))
        assertTrue(labels.contains("Pets"))
        assertTrue(labels.contains("Gaming"))
    }

    @Test
    fun activeExpenseCatalogMatchesFinalTaxonomy() {
        assertEquals(
            listOf(
                "Food",
                "Transport",
                "Health",
                "Shopping",
                "Gaming",
                "Leisure",
                "Travel",
                "Digital",
                "Education",
                "Taxes",
                "Gifts",
                "Pets",
                "Other"
            ),
            ExpenseCategory.expenseCategories.map { it.label }
        )
    }

    @Test
    fun realWorldDescriptionsCollapseIntoExpectedMacroCategories() {
        assertEquals(ExpenseCategory.HEALTH.label, ExpenseCategory.inferFromDescription("isber"))
        assertEquals(ExpenseCategory.HEALTH.label, ExpenseCategory.inferFromDescription("ciaita medica"))
        assertEquals(ExpenseCategory.SHOPPING.label, ExpenseCategory.inferFromDescription("temu"))
        assertEquals(ExpenseCategory.SHOPPING.label, ExpenseCategory.inferFromDescription("qmazon lampada puccola"))
        assertEquals(ExpenseCategory.SHOPPING.label, ExpenseCategory.inferFromDescription("antenne wireless"))
        assertEquals(ExpenseCategory.LEISURE.label, ExpenseCategory.inferFromDescription("serata"))
        assertEquals(ExpenseCategory.LEISURE.label, ExpenseCategory.inferFromDescription("uscita 27 maggio"))
        assertEquals(ExpenseCategory.LEISURE.label, ExpenseCategory.inferFromDescription("1 maggio"))
        assertEquals(ExpenseCategory.LEISURE.label, ExpenseCategory.inferFromDescription("21 marzo"))
        assertEquals(ExpenseCategory.LEISURE.label, ExpenseCategory.inferFromDescription("sabato 14 marzo"))
        assertEquals(ExpenseCategory.FOOD.label, ExpenseCategory.inferFromDescription("deliveroo"))
        assertEquals(ExpenseCategory.FOOD.label, ExpenseCategory.inferFromDescription("cena"))
        assertEquals(ExpenseCategory.FOOD.label, ExpenseCategory.inferFromDescription("prosecco"))
        assertEquals(ExpenseCategory.LEISURE.label, ExpenseCategory.inferFromDescription("noleggio snow"))
        assertEquals(ExpenseCategory.DIGITAL.label, ExpenseCategory.inferFromDescription("patreon"))
        assertEquals(ExpenseCategory.DIGITAL.label, ExpenseCategory.inferFromDescription("apple tv"))
        assertEquals(ExpenseCategory.DIGITAL.label, ExpenseCategory.inferFromDescription("trae"))
        assertEquals(ExpenseCategory.LEISURE.label, ExpenseCategory.inferFromDescription("attrezzatura snowboard, giacca, gianti, maschera"))
        assertEquals(ExpenseCategory.LEISURE.label, ExpenseCategory.inferFromDescription("bretelle sci"))
        assertEquals(ExpenseCategory.SHOPPING.label, ExpenseCategory.inferFromDescription("cartuccia stampante"))
        assertEquals(ExpenseCategory.SHOPPING.label, ExpenseCategory.inferFromDescription("no power banknin macchina"))
    }
}
