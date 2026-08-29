package com.example.barepingwidget.monitor

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * 后台监测调度器。
 *
 * 电量与流量优化策略：
 *  - 使用 WorkManager 周期任务（系统最低 15 分钟一档，由系统合并唤醒，远比自起 Service 省电）；
 *  - 约束：仅在有网络连接且电量不低时执行；
 *  - 每次检测只读取响应头（Range: bytes=0-0），单轮流量 < 10KB；
 *  - KEEP 策略避免重复入队。
 */
object MonitorScheduler {

    private const val WORK_NAME = "bare_ping_periodic_monitor"
    private const val INTERVAL_MINUTES = 15L

    fun schedule(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val request = PeriodicWorkRequestBuilder<MonitorWorker>(
            INTERVAL_MINUTES, TimeUnit.MINUTES
        ).setConstraints(constraints).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}
