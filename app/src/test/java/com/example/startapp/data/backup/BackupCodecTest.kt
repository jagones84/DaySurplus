package com.example.startapp.data.backup

import com.example.startapp.data.model.AppBackup
import com.example.startapp.data.model.DailySnapshot
import com.example.startapp.data.model.Transaction
import org.junit.Assert.assertEquals
import org.junit.Test

class BackupCodecTest {
    @Test
    fun codec_roundTrip_preservesAllFields() {
        val backup = AppBackup(
            schemaVersion = 1,
            createdAtEpochMs = 123L,
            totalAmount = 10.0,
            dailyIncrease = 20.0,
            daysToDisplay = 30,
            maxHistoryDays = 900,
            transactions = listOf(Transaction(amount = -1.0, date = 1L, description = "x", category = "Food")),
            dailySnapshots = listOf(DailySnapshot(date = 1L, amount = 0.0)),
            expenseCustomCategories = listOf("Casa Vacanze"),
            incomeCustomCategories = listOf("Dividends")
        )

        val json = BackupCodec.encode(backup)
        val decoded = BackupCodec.decode(json)

        assertEquals(backup, decoded)
    }
}

