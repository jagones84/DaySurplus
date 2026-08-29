package com.example.startapp.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.startapp.data.CounterDataRepository
import com.example.startapp.domain.model.DailySnapshot
import com.example.startapp.domain.nextSurplusRunDelayMs
import com.example.startapp.domain.shouldApplyDailyIncrease
import com.example.startapp.utils.AppLog
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class SurplusWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            runDailySurplus()
            scheduleNextRun()
            Result.success()
        } catch (t: Throwable) {
            AppLog.e("SurplusWorker", "Daily surplus run failed (attempt $runAttemptCount)", t)
            Result.retry()
        }
    }

    private suspend fun runDailySurplus() {
        val repository = CounterDataRepository(applicationContext)
        val now = System.currentTimeMillis()

        // Idempotency guard: if a snapshot already exists for today the chain
        // was interrupted and restarted; applying the increase again would
        // double-count today's surplus.
        val lastSnapshotDate = repository.dailySnapshots.first().maxOfOrNull { it.date }
        if (!shouldApplyDailyIncrease(lastSnapshotDate, now)) {
            AppLog.i("SurplusWorker", "Snapshot already present for today, skipping increase")
            return
        }

        val dailyIncrease = repository.dailyIncrease.first()
        val currentTotal = repository.totalAmount.first()

        val newTotal = if (dailyIncrease > 0) {
            val updated = currentTotal + dailyIncrease
            repository.updateTotalAmount(updated)
            updated
        } else {
            currentTotal
        }

        repository.addDailySnapshot(DailySnapshot(date = now, amount = newTotal))
        AppLog.i("SurplusWorker", "Daily surplus applied: +$dailyIncrease, total=$newTotal")
    }

    private fun scheduleNextRun() {
        val delayMs = nextSurplusRunDelayMs(System.currentTimeMillis())
        val nextWorkRequest = OneTimeWorkRequestBuilder<SurplusWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(applicationContext).enqueueUniqueWork(
            "SurplusWorkerChain",
            ExistingWorkPolicy.REPLACE,
            nextWorkRequest
        )
        AppLog.i("SurplusWorker", "Next run scheduled in ${delayMs / 60000} minutes")
    }
}
