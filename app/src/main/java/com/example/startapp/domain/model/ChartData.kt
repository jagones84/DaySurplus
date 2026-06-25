package com.example.startapp.domain.model

data class ChartPoint(
    val date: Long,
    val surplus: Double,
    val expense: Double,
    val income: Double
)

data class ChartStats(
    val points: List<ChartPoint>,
    val totalExpenses: Double,
    val totalIncome: Double,
    val avgSurplus: Double,
    val surplusStdDev: Double
)
