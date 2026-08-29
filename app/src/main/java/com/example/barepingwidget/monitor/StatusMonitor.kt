package com.example.barepingwidget.monitor

import android.content.Context
import com.example.barepingwidget.data.PingStatus
import com.example.barepingwidget.data.SiteRepository
import com.example.barepingwidget.data.SiteResult
import com.example.barepingwidget.net.NetworkChecker
import com.example.barepingwidget.notify.NotificationHelper
import com.example.barepingwidget.widget.PingWidgetProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * 状态监测引擎：被主界面、WorkManager 后台任务、Widget 刷新按钮三方复用。
 *
 * 通知规则（与需求严格一致）：
 *  - 任意站点状态「由红变绿」时发一次横幅通知（同一轮多个站点恢复合并为一条）；
 *  - 绿→红、持续红、黄→绿等一切其它变化均不发通知；
 *  - 下一次由红变绿时才会再次通知，依次类推；
 *  - 设置页关闭通知开关后一律不发。
 */
object StatusMonitor {

    suspend fun checkAll(context: Context): List<SiteResult> {
        val repo = SiteRepository.getInstance(context)
        val sites = repo.getSites()

        val results: List<SiteResult> = coroutineScope {
            sites.map { site ->
                async {
                    val (latency, status) = NetworkChecker.measureSite(site.url)
                    SiteResult(site, status, latency)
                }
            }.map { it.await() }
        }

        // ---- 通知跃迁判断（必须在覆盖旧状态之前）----
        // 任意站点由红变绿时通知一次；同一轮多个站点恢复合并为一条通知
        val prevStatuses = repo.getStatusMap()
        if (repo.notificationsEnabled) {
            val recovered = results.filter { r ->
                (prevStatuses[r.site.name] ?: PingStatus.UNKNOWN) == PingStatus.RED &&
                        r.status == PingStatus.GREEN
            }
            if (recovered.isNotEmpty()) {
                NotificationHelper.notifySitesRestored(context, recovered)
            }
        }

        // ---- 持久化最新结果 ----
        repo.saveStatusMap(results.associate { it.site.name to it.status })
        repo.saveLatencyMap(results.associate { it.site.name to it.latencyMs })
        repo.lastUpdateTime = System.currentTimeMillis()

        // ---- 通知所有桌面小组件刷新 ----
        PingWidgetProvider.updateAllWidgets(context)

        return results
    }
}
