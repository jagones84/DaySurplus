package com.example.startapp.data.backup

import com.example.startapp.data.model.AppBackup
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

