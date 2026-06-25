package com.example.startapp.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.startapp.data.CounterDataRepository
import com.example.startapp.data.model.DailySnapshot
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.concurrent.TimeUnit

class SurplusWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val repository = CounterDataRepository(applicationContext)
        val dailyIncrease = repository.dailyIncrease.first()
        val currentTotal = repository.totalAmount.first()

        // Apply daily increase
        var newTotal = currentTotal
        if (dailyIncrease > 0) {
            newTotal += dailyIncrease
            repository.updateTotalAmount(newTotal)
        }

        // Save daily snapshot
        repository.addDailySnapshot(DailySnapshot(System.currentTimeMillis(), newTotal))

        // Schedule the next worker for 12:00 PM tomorrow
        val currentDate = Calendar.getInstance()
        val dueDate = Calendar.getInstance()
        
        // Set execution time to 12:00 PM
        dueDate.set(Calendar.HOUR_OF_DAY, 12)
        dueDate.set(Calendar.MINUTE, 0)
        dueDate.set(Calendar.SECOND, 0)
        dueDate.set(Calendar.MILLISECOND, 0)

        // If today's 12:00 PM has already passed, schedule for tomorrow
        if (dueDate.before(currentDate)) {
            dueDate.add(Calendar.DAY_OF_MONTH, 1)
        }

        val timeDiff = dueDate.timeInMillis - currentDate.timeInMillis

        val nextWorkRequest = OneTimeWorkRequestBuilder<SurplusWorker>()
            .setInitialDelay(timeDiff, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(applicationContext).enqueueUniqueWork(
            "SurplusWorkerChain",
            ExistingWorkPolicy.REPLACE,
            nextWorkRequest
        )

        return Result.success()
    }
}
