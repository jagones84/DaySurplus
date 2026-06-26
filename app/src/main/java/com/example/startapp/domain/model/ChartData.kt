package com.example.startapp.domain.model

data class ChartPoint(
    val date: Long,
    val surplus: Double,
    val expense: Double,
    val income: Double
)

data class CategorySlice(
    val category: String,
    val total: Double,
    val percentage: Double,
    val monthlyAverage: Double
)

data class RankedMetric(
    val label: String,
    val total: Double
)

data class ChartStats(
    val points: List<ChartPoint>,
    val totalExpenses: Double,
    val totalIncome: Double,
    val avgSurplus: Double,
    val surplusStdDev: Double,
    val savingsRatio: Double,
    val categoryExpenses: List<CategorySlice>,
    val categoryIncome: List<CategorySlice>,
    val topExpenseDescriptions: List<RankedMetric>,
    val topExpenseDays: List<RankedMetric>
)
