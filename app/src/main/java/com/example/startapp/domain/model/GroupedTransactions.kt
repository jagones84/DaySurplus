package com.example.startapp.domain.model

import com.example.startapp.data.model.Transaction

data class ExpenseGroup(
    val category: String,
    val total: Double,
    val transactions: List<Transaction>
)

data class GroupedTransactionState(
    val incomeTransactions: List<Transaction>,
    val expenseGroups: List<ExpenseGroup>
)

fun buildGroupedTransactionState(transactions: List<Transaction>): GroupedTransactionState {
    val sorted = transactions.sortedByDescending { it.date }
    val incomeTransactions = sorted.filter { it.amount >= 0 }
    val expenseGroups = sorted
        .filter { it.amount < 0 }
        .groupBy { it.category.ifBlank { ExpenseCategory.OTHER.label } }
        .map { (category, items) ->
            ExpenseGroup(
                category = category,
                total = items.sumOf { -it.amount },
                transactions = items
            )
        }
        .sortedByDescending { it.total }

    return GroupedTransactionState(
        incomeTransactions = incomeTransactions,
        expenseGroups = expenseGroups
    )
}
