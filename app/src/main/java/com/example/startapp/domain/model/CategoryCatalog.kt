package com.example.startapp.domain.model

enum class CategoryType {
    EXPENSE,
    INCOME
}

object CategoryCatalog {
    const val NEW_CATEGORY_OPTION = "+ New Category"

    private val builtInExpenseCategories = listOf(
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
    )

    private val builtInIncomeCategories = listOf(
        "Daily Surplus",
        "Salary",
        "Family",
        "Refund",
        "Sales",
        "Bonus",
        "Other Income"
    )

    fun builtInCategories(type: CategoryType): List<String> {
        return when (type) {
            CategoryType.EXPENSE -> builtInExpenseCategories
            CategoryType.INCOME -> builtInIncomeCategories
        }
    }

    fun dropdownOptions(type: CategoryType, custom: List<String>): List<String> {
        return builtInCategories(type) + custom.sorted() + NEW_CATEGORY_OPTION
    }

    fun normalizeCustomCategoryName(raw: String): String {
        return raw
            .trim()
            .replace("\\s+".toRegex(), " ")
            .split(" ")
            .filter { it.isNotBlank() }
            .joinToString(" ") { part ->
                part.lowercase().replaceFirstChar { it.titlecase() }
            }
    }

    fun canAddCustomCategory(type: CategoryType, raw: String, existing: List<String>): Boolean {
        val normalized = normalizeCustomCategoryName(raw)
        if (normalized.isBlank()) {
            return false
        }

        val occupied = (builtInCategories(type) + existing)
            .map(::normalizeCustomCategoryName)
            .toSet()

        return normalized !in occupied
    }
}
