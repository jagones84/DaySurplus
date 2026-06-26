package com.example.startapp.domain.model

import com.example.startapp.data.model.Transaction

data class TransactionGroup(
    val category: String,
    val total: Double,
    val transactions: List<Transaction>
)

data class GroupedTransactionState(
    val incomeGroups: List<TransactionGroup>,
    val expenseGroups: List<TransactionGroup>
)

fun buildGroupedTransactionState(transactions: List<Transaction>): GroupedTransactionState {
    val sorted = transactions.sortedByDescending { it.date }

    fun buildGroups(source: List<Transaction>, fallbackCategory: String, absoluteTotal: Boolean): List<TransactionGroup> {
        return source
            .groupBy { it.category.ifBlank { fallbackCategory } }
            .map { (category, items) ->
                TransactionGroup(
                    category = category,
                    total = if (absoluteTotal) items.sumOf { -it.amount } else items.sumOf { it.amount },
                    transactions = items
                )
            }
            .sortedByDescending { it.total }
    }

    return GroupedTransactionState(
        incomeGroups = buildGroups(
            source = sorted.filter { it.amount >= 0 },
            fallbackCategory = "Other Income",
            absoluteTotal = false
        ),
        expenseGroups = buildGroups(
            source = sorted.filter { it.amount < 0 },
            fallbackCategory = ExpenseCategory.OTHER.label,
            absoluteTotal = true
        )
    )
}
