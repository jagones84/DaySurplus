package com.example.startapp

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.startapp.utils.AppLog
import com.example.startapp.worker.AutoBackupWorker
import java.util.concurrent.TimeUnit

class DaySurpApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        AppLog.init(this)
        scheduleDailyAutoBackup()
    }

    private fun scheduleDailyAutoBackup() {
        val request = PeriodicWorkRequestBuilder<AutoBackupWorker>(1, TimeUnit.DAYS).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            AutoBackupWorker.UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}
