package com.example.startapp.domain.model

enum class ExpenseCategory(
    val label: String,
    val keywords: List<String>
) {
    FOOD("Food", listOf("grocery", "groceries", "supermarket", "coop", "lidl", "conad", "restaurant", "bar")),
    TRANSPORT("Transport", listOf("train", "bus", "metro", "taxi", "fuel", "parking", "toll")),
    HOME("Home", listOf("rent", "furniture", "repair", "ikea", "home")),
    BILLS("Bills", listOf("electricity", "gas", "water", "internet", "phone", "bill", "utility")),
    HEALTH("Health", listOf("pharmacy", "doctor", "dentist", "medicine", "hospital")),
    SHOPPING("Shopping", listOf("amazon", "clothes", "shoes", "electronics", "shopping")),
    LEISURE("Leisure", listOf("cinema", "movie", "game", "netflix", "spotify", "museum")),
    TRAVEL("Travel", listOf("flight", "hotel", "booking", "trip", "travel")),
    WORK("Work", listOf("office", "coworking", "software", "tool")),
    EDUCATION("Education", listOf("course", "book", "exam", "tuition")),
    TAXES("Taxes", listOf("tax", "fine", "fee")),
    SUBSCRIPTIONS("Subscriptions", listOf("subscription", "prime", "icloud")),
    GIFTS("Gifts", listOf("gift", "present")),
    PETS("Pets", listOf("pet", "dog", "cat", "vet")),
    OTHER("Other", emptyList()),
    INCOME("Income", emptyList());

    companion object {
        val expenseCategories: List<ExpenseCategory> = listOf(
            FOOD,
            TRANSPORT,
            HOME,
            BILLS,
            HEALTH,
            SHOPPING,
            LEISURE,
            TRAVEL,
            WORK,
            EDUCATION,
            TAXES,
            SUBSCRIPTIONS,
            GIFTS,
            PETS,
            OTHER
        )

        fun inferFromDescription(description: String): String {
            val normalized = description.trim().lowercase()
            return expenseCategories.firstOrNull { category ->
                category.keywords.any { keyword -> normalized.contains(keyword) }
            }?.label ?: OTHER.label
        }
    }
}
