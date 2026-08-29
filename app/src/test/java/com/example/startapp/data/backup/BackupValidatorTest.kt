package com.example.startapp.data.backup

import com.example.startapp.data.model.AppBackup
import com.example.startapp.domain.model.DailySnapshot
import com.example.startapp.domain.model.Transaction
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupValidatorTest {

    @Test
    fun validate_rejectsUnsupportedSchemaVersion() {
        val backup = validBackup().copy(schemaVersion = 999)
        assertFalse(BackupValidator.validate(backup))
    }

    @Test
    fun validate_rejectsNonFiniteTotalAmount() {
        val backup = validBackup().copy(totalAmount = Double.NaN)
        assertFalse(BackupValidator.validate(backup))
    }

    @Test
    fun validate_rejectsInfiniteDailyIncrease() {
        val backup = validBackup().copy(dailyIncrease = Double.POSITIVE_INFINITY)
        assertFalse(BackupValidator.validate(backup))
    }

    @Test
    fun validate_rejectsNonFiniteTransactionAmount() {
        val backup = validBackup().copy(
            transactions = listOf(
                Transaction(amount = Double.NaN, date = 1_000L, description = "broken")
            )
        )
        assertFalse(BackupValidator.validate(backup))
    }

    @Test
    fun validate_rejectsNonFiniteSnapshotAmount() {
        val backup = validBackup().copy(
            dailySnapshots = listOf(
                DailySnapshot(date = 1_000L, amount = Double.NEGATIVE_INFINITY)
            )
        )
        assertFalse(BackupValidator.validate(backup))
    }

    @Test
    fun validate_rejectsTransactionWithNegativeDate() {
        val backup = validBackup().copy(
            transactions = listOf(
                Transaction(amount = -1.0, date = -1L, description = "bad-date")
            )
        )
        assertFalse(BackupValidator.validate(backup))
    }

    @Test
    fun validate_rejectsNonPositiveDaysToDisplay() {
        val backup = validBackup().copy(daysToDisplay = 0)
        assertFalse(BackupValidator.validate(backup))
    }

    @Test
    fun validate_acceptsValidBackup() {
        assertTrue(BackupValidator.validate(validBackup()))
    }

    private fun validBackup(): AppBackup {
        return AppBackup(
            schemaVersion = BackupValidator.SUPPORTED_SCHEMA_VERSION,
            createdAtEpochMs = 123L,
            totalAmount = 10.0,
            dailyIncrease = 20.0,
            daysToDisplay = 30,
            maxHistoryDays = 900,
            transactions = emptyList(),
            dailySnapshots = emptyList(),
            expenseCustomCategories = emptyList(),
            incomeCustomCategories = emptyList()
        )
    }
}

