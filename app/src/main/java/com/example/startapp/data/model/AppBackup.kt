package com.example.startapp.data.model

import com.example.startapp.domain.model.DailySnapshot
import com.example.startapp.domain.model.Transaction

data class AppBackup(
    val schemaVersion: Int,
    val createdAtEpochMs: Long,
    val totalAmount: Double,
    val dailyIncrease: Double,
    val daysToDisplay: Int,
    val maxHistoryDays: Int,
    val transactions: List<Transaction>,
    val dailySnapshots: List<DailySnapshot>,
    val expenseCustomCategories: List<String>,
    val incomeCustomCategories: List<String>
)

