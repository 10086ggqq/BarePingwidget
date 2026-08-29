package com.example.barepingwidget.monitor

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/** 后台周期监测任务：由 WorkManager 调度，受网络/电量约束保护 */
class MonitorWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            StatusMonitor.checkAll(applicationContext)
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}
