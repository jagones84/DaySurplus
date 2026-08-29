package com.example.startapp.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.startapp.data.CounterDataRepository
import com.example.startapp.utils.AppLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Daily automatic backup: dumps the full AppBackup JSON into the app's
 * external files directory and keeps only the most recent [MAX_KEPT_BACKUPS] files.
 */
class AutoBackupWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val repository = CounterDataRepository(applicationContext)
            val json = repository.exportBackup().let { backup ->
                com.example.startapp.data.backup.BackupCodec.encode(backup)
            }

            val written = withContext(Dispatchers.IO) {
                val backupDir = File(applicationContext.getExternalFilesDir(null), BACKUP_DIR)
                    .apply { mkdirs() }
                val fileName = "daysurp-backup-" +
                    SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date()) + ".json"
                File(backupDir, fileName).writeText(json, Charsets.UTF_8)

                backupDir.listFiles { file -> file.name.startsWith("daysurp-backup-") }
                    ?.sortedByDescending { file -> file.name }
                    ?.drop(MAX_KEPT_BACKUPS)
                    ?.forEach { file -> file.delete() }
                fileName
            }

            AppLog.i("AutoBackupWorker", "Automatic backup written: $written")
            Result.success()
        } catch (t: Throwable) {
            AppLog.e("AutoBackupWorker", "Automatic backup failed", t)
            Result.retry()
        }
    }

    companion object {
        const val UNIQUE_WORK_NAME = "AutoBackupDaily"
        const val BACKUP_DIR = "backups"
        const val MAX_KEPT_BACKUPS = 7
    }
}
