package com.example.startapp.data.backup

import com.example.startapp.data.CounterDataRepository

class BackupRepository(private val repository: CounterDataRepository) {
    suspend fun exportJson(): String {
        return BackupCodec.encode(repository.exportBackup())
    }

    suspend fun importJson(json: String): Boolean {
        val decoded = try {
            BackupCodec.decode(json)
        } catch (_: Throwable) {
            return false
        }

        if (!BackupValidator.validate(decoded)) {
            return false
        }

        repository.importBackup(decoded)
        return true
    }
}

