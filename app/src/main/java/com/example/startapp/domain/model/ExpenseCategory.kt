package com.example.startapp.domain.model

import java.text.Normalizer

enum class ExpenseCategory(
    val label: String,
    val keywords: List<String>
) {
    FOOD("Food", listOf("grocery", "groceries", "supermarket", "coop", "lidl", "conad", "deliveroo", "hamburger", "kebab", "poke", "piadina", "giappo", "mc donald", "pranzo", "cena", "prosecco", "spumante", "pub")),
    TRANSPORT("Transport", listOf("train", "bus", "metro", "taxi", "fuel", "parking", "toll", "area c", "parcheggio")),
    HEALTH("Health", listOf("pharmacy", "farmacia", "doctor", "dentist", "medicine", "medicina", "hospital", "salute", "health", "medica", "medico", "clinica", "ospedale", "isber", "integratori")),
    SHOPPING("Shopping", listOf("amazon", "temu", "amz", "qmazon", "clothes", "shoes", "electronics", "shopping", "mobile", "lampada", "tavolo", "doccia", "robot pavimento", "strip led", "antenne wireless", "caricacell", "cavo usb hdmi", "cavo eth")),
    GAMING("Gaming", listOf("steam", "playstation", "psn", "xbox", "nintendo", "game", "gaming", "videogioco", "switch", "gdr")),
    LEISURE("Leisure", listOf("cinema", "movie", "netflix", "spotify", "museum", "restaurant", "bar", "uscita", "uscite", "aperitivo", "serata", "capodanno", "bowling", "noleggio snow", "hard rock", "weekend")),
    TRAVEL("Travel", listOf("flight", "hotel", "booking", "trip", "travel", "viaggio", "gita", "cervinia", "bergamo")),
    DIGITAL("Digital", listOf("internet", "phone", "bill", "utility", "software", "tool", "gemini", "trae", "openrouter", "google cloud", "google ai", "telegram", "comfyui", "voxta", "patreon", "apple tv", "subscription", "prime", "icloud")),
    EDUCATION("Education", listOf("course", "book", "exam", "tuition")),
    TAXES("Taxes", listOf("tax", "fine", "fee")),
    GIFTS("Gifts", listOf("gift", "present")),
    PETS("Pets", listOf("pet", "dog", "cat", "vet")),
    OTHER("Other", emptyList()),
    INCOME("Income", emptyList());

    companion object {
        private data class CategoryRule(
            val category: ExpenseCategory,
            val fragments: List<String>
        )

        private val prioritizedRules: List<CategoryRule> = listOf(
            CategoryRule(
                category = HEALTH,
                fragments = listOf(
                    "isber",
                    "medica",
                    "medico",
                    "visita",
                    "clinica",
                    "ospedale",
                    "ps",
                    "ist",
                    "farmacia",
                    "farmaco",
                    "siringhe",
                    "integratori",
                    "ginocchiera",
                    "dentifric",
                    "omega3",
                    "vitamina",
                    "strisce denti",
                    "ceretta",
                    "salviette"
                )
            ),
            CategoryRule(
                category = SHOPPING,
                fragments = listOf(
                    "temu",
                    "amazon",
                    "qmazon",
                    "amz",
                    "antenne wireless",
                    "caricacell",
                    "cavo usb hdmi",
                    "cavo eth",
                    "lampada",
                    "mobile",
                    "robot pavimento",
                    "tavolo",
                    "doccia",
                    "strip led",
                    "ram pc",
                    "ram 2",
                    "cartuccia stampante",
                    "power bank"
                )
            ),
            CategoryRule(
                category = GAMING,
                fragments = listOf("gaming", "game", "god of war", "switch", "gdr", "steam")
            ),
            CategoryRule(
                category = GIFTS,
                fragments = listOf("regalo")
            ),
            CategoryRule(
                category = DIGITAL,
                fragments = listOf(
                    "patreon",
                    "apple tv",
                    "subscription",
                    "prime",
                    "icloud",
                    "gemini",
                    "trae",
                    "openrouter",
                    "google ai",
                    "google cloud",
                    "google play console",
                    "telegram",
                    "social app",
                    "comfyui",
                    "voxta",
                    "vam model",
                    "gitub",
                    "cool dgx",
                    "claud"
                )
            ),
            CategoryRule(
                category = TRAVEL,
                fragments = listOf("viaggio", "cervinia", "gita", "bergamo", "lainate")
            ),
            CategoryRule(
                category = TRANSPORT,
                fragments = listOf("taxi", "area c", "parcheggio")
            ),
            CategoryRule(
                category = LEISURE,
                fragments = listOf("serata", "uscita", "capodanno", "cinema", "hard rock", "bowling", "roknroll", "weekend", "noleggio snow", "snowboard", "bretelle sci")
            ),
            CategoryRule(
                category = FOOD,
                fragments = listOf("cena", "prosecco", "spumante", "deliveroo", "hamburger", "kebab", "poke", "piadina", "mc donald", "giappo", "pranzo", "fortimel", "cola")
            )
        )

        private val monthMarkers = listOf(
            "gennaio",
            "febbraio",
            "marzo",
            "aprile",
            "maggio",
            "giugno",
            "luglio",
            "agosto",
            "settembre",
            "ottobre",
            "novembre",
            "dicembre"
        )

        private val weekdayMarkers = listOf(
            "lun",
            "mar",
            "mer",
            "gio",
            "ven",
            "sab",
            "dom",
            "venerdi"
        )

        private val incomeKeywordMap = linkedMapOf(
            "Salary" to listOf("salary", "stipendio", "ral", "payroll", "busta paga"),
            "Family" to listOf("family", "famiglia", "genitori", "mamma", "papa", "papà", "padre", "madre"),
            "Refund" to listOf("refund", "rimborso", "reimbursement", "reso", "cashback"),
            "Sales" to listOf("sale", "sales", "vendita", "venduto", "sold", "marketplace", "subito"),
            "Bonus" to listOf("bonus", "premio", "reward", "incentivo")
        )

        val expenseCategories: List<ExpenseCategory> = listOf(
            FOOD,
            TRANSPORT,
            HEALTH,
            SHOPPING,
            GAMING,
            LEISURE,
            TRAVEL,
            DIGITAL,
            EDUCATION,
            TAXES,
            GIFTS,
            PETS,
            OTHER
        )

        fun builtInExpenseLabels(): List<String> {
            return CategoryCatalog.builtInCategories(CategoryType.EXPENSE)
        }

        fun inferFromDescription(description: String): String {
            val normalized = normalizeDescription(description)
            prioritizedRules.firstOrNull { rule ->
                rule.fragments.any(normalized::contains)
            }?.let { return it.category.label }

            if (looksLikeLeisureEvent(normalized)) {
                return LEISURE.label
            }

            return expenseCategories.firstOrNull { category ->
                category.keywords.any { keyword -> normalized.contains(normalizeDescription(keyword)) }
            }?.label ?: OTHER.label
        }

        fun inferIncomeCategory(description: String): String {
            val normalized = normalizeDescription(description)
            return incomeKeywordMap.entries.firstOrNull { (_, keywords) ->
                keywords.any(normalized::contains)
            }?.key ?: "Other Income"
        }

        private fun normalizeDescription(value: String): String {
            val normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
            return normalized
                .replace("\\p{M}+".toRegex(), "")
                .lowercase()
                .replace("[^a-z0-9]+".toRegex(), " ")
                .trim()
        }

        private fun looksLikeLeisureEvent(normalized: String): Boolean {
            if (normalized.isBlank()) {
                return false
            }

            val hasMonth = monthMarkers.any(normalized::contains)
            val hasWeekday = weekdayMarkers.any(normalized::contains)
            val hasNumber = normalized.any(Char::isDigit)
            val hasEventWord = normalized.contains("sera") || normalized.contains("notte") || normalized.contains("liberazione")

            return hasEventWord || ((hasMonth || hasWeekday) && hasNumber)
        }
    }
}
