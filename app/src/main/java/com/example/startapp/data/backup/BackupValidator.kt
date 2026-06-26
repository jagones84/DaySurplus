package com.example.startapp.data.backup

import com.example.startapp.data.model.AppBackup

object BackupValidator {
    const val SUPPORTED_SCHEMA_VERSION = 1

    fun validate(backup: AppBackup): Boolean {
        if (backup.schemaVersion != SUPPORTED_SCHEMA_VERSION) {
            return false
        }
        if (backup.daysToDisplay <= 0) {
            return false
        }
        if (backup.maxHistoryDays <= 0) {
            return false
        }
        return true
    }
}

