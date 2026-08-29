package com.example.barepingwidget.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.barepingwidget.R
import com.example.barepingwidget.data.PingStatus
import com.example.barepingwidget.data.SiteRepository
import com.example.barepingwidget.monitor.MonitorWorker
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 桌面小组件基类，派生出三种固定尺寸：
 *  - [PingWidgetProvider]    2x2：显示前两个站点
 *  - [PingWidgetProvider4x3] 4x3：显示全部站点
 *  - [PingWidgetProvider1x2] 1x2（2 格宽 × 1 格高横条）：只显示第一个站点
 *
 * 刷新策略：
 *  - widget 自身只读缓存渲染（零耗电）；
 *  - 由 WorkManager 周期任务 / 主界面 / 刷新按钮触发检测后统一调用 [updateAllWidgets]。
 */
open class PingWidgetProvider : AppWidgetProvider() {

    /** 该尺寸显示的站点行数上限 */
    protected open val maxRows: Int get() = 2

    /** 是否为横向单行紧凑样式（1x2） */
    protected open val compact: Boolean get() = false

    companion object {
        const val ACTION_REFRESH = "com.example.barepingwidget.action.WIDGET_REFRESH"
        private const val WIDGET_REFRESH_WORK = "bare_ping_widget_refresh"

        private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        private val providerClasses = listOf(
            PingWidgetProvider::class.java,     // 2x2：两个站点
            PingWidgetProvider4x3::class.java,  // 4x3：全部站点
            PingWidgetProvider1x2::class.java   // 1x2：单站点横条
        )

        /** 三种尺寸的小组件统一刷新 */
        fun updateAllWidgets(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            providerClasses.forEach { cls ->
                val provider = cls.getDeclaredConstructor().newInstance()
                manager.getAppWidgetIds(ComponentName(context, cls)).forEach { id ->
                    manager.updateAppWidget(id, provider.buildRemoteViews(context, repo(context)))
                }
            }
        }

        private fun repo(context: Context) = SiteRepository.getInstance(context)
    }

    protected open fun buildRemoteViews(context: Context, repo: SiteRepository): RemoteViews {
        return if (compact) buildCompactViews(context, repo) else buildListViews(context, repo)
    }

    // ---------- 列表样式（2x2 / 4x3） ----------

    private fun buildListViews(context: Context, repo: SiteRepository): RemoteViews {
        val sites = repo.getSites()
        val statuses = repo.getStatusMap()
        val latencies = repo.getLatencyMap()

        val views = RemoteViews(context.packageName, R.layout.widget_ping)
        attachCommonActions(context, views)

        val last = repo.lastUpdateTime
        views.setTextViewText(
            R.id.widget_time,
            if (last > 0) timeFormat.format(Date(last)) else "--:--"
        )

        // 动态添加站点行（行高弹性分布，任何高度都不会溢出）
        views.removeAllViews(R.id.widget_list)
        val rowsToShow = sites.take(maxRows)
        rowsToShow.forEach { site ->
            val row = RemoteViews(context.packageName, R.layout.widget_item_site)
            bindSiteRow(row, statuses[site.name] ?: PingStatus.UNKNOWN, site.name, latencies[site.name] ?: -1L)
            views.addView(R.id.widget_list, row)
        }

        // 列表被尺寸裁剪时提示还有更多站点
        val hiddenCount = sites.size - rowsToShow.size
        views.setViewVisibility(
            R.id.widget_more,
            if (hiddenCount > 0) View.VISIBLE else View.GONE
        )
        if (hiddenCount > 0) {
            views.setTextViewText(R.id.widget_more, "还有 $hiddenCount 个站点…")
        }

        return views
    }

    // ---------- 紧凑横条样式（1x2） ----------

    private fun buildCompactViews(context: Context, repo: SiteRepository): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_ping_compact)
        attachCommonActions(context, views)

        val site = repo.getSites().firstOrNull() ?: return views
        val status = repo.getStatusMap()[site.name] ?: PingStatus.UNKNOWN
        val latency = repo.getLatencyMap()[site.name] ?: -1L
        bindSiteRow(views, status, site.name, latency)
        return views
    }

    // ---------- 公共逻辑 ----------

    /** 点击卡片 → 打开主界面；刷新按钮 → 发广播回本 Provider 触发真实检测 */
    private fun attachCommonActions(context: Context, views: RemoteViews) {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        views.setOnClickPendingIntent(
            R.id.widget_container,
            PendingIntent.getActivity(
                context, 0, launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )

        val refreshIntent = Intent(context, this.javaClass).setAction(ACTION_REFRESH)
        views.setOnClickPendingIntent(
            R.id.widget_refresh,
            PendingIntent.getBroadcast(
                context, 1, refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
    }

    /** 把某个站点的状态/延迟绑定到行视图（列表行与紧凑横条共用一套 ID） */
    private fun bindSiteRow(row: RemoteViews, status: PingStatus, name: String, latency: Long) {
        val dotRes = when (status) {
            PingStatus.GREEN -> R.drawable.dot_green
            PingStatus.YELLOW -> R.drawable.dot_yellow
            PingStatus.RED -> R.drawable.dot_red
            PingStatus.UNKNOWN -> R.drawable.dot_unknown
        }
        row.setImageViewResource(R.id.widget_item_dot, dotRes)
        row.setTextViewText(R.id.widget_item_name, name)

        when (status) {
            PingStatus.UNKNOWN -> {
                row.setTextViewText(R.id.widget_item_latency, "…")
                row.setTextColor(R.id.widget_item_latency, 0xFF6B7280.toInt())
            }
            PingStatus.RED -> {
                row.setTextViewText(R.id.widget_item_latency, "—")
                row.setTextColor(R.id.widget_item_latency, 0xFFEF4444.toInt())
            }
            else -> {
                row.setTextViewText(R.id.widget_item_latency, "${latency}ms")
                row.setTextColor(
                    R.id.widget_item_latency,
                    if (status == PingStatus.GREEN) 0xFF16A34A.toInt()
                    else 0xFFD97706.toInt()
                )
            }
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val repo = SiteRepository.getInstance(context)
        appWidgetIds.forEach { id ->
            appWidgetManager.updateAppWidget(id, buildRemoteViews(context, repo))
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: android.os.Bundle?
    ) {
        appWidgetManager.updateAppWidget(
            appWidgetId,
            buildRemoteViews(context, SiteRepository.getInstance(context))
        )
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) {
            // 刷新按钮：把真实检测交给 WorkManager 立即执行。
            // 不在广播内做网络请求——广播（含 goAsync）只有约 10 秒时限，
            // 首次检测或站点全部超时的最坏情况会超时，导致系统 ANR。
            val request = OneTimeWorkRequestBuilder<MonitorWorker>().build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                WIDGET_REFRESH_WORK,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}

/** 4x3 尺寸：显示全部站点 */
class PingWidgetProvider4x3 : PingWidgetProvider() {
    override val maxRows: Int get() = Int.MAX_VALUE
}

/** 1x2 尺寸（2 格宽 × 1 格高横条）：只显示第一个站点 */
class PingWidgetProvider1x2 : PingWidgetProvider() {
    override val compact: Boolean get() = true
}
