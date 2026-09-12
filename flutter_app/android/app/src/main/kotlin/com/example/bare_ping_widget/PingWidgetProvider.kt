package com.example.bare_ping_widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 桌面小组件基类，派生出三种固定尺寸：
 *  - [PingWidgetProvider]    2x2：显示前两个站点
 *  - [PingWidgetProvider4x3] 4x3：显示全部站点
 *  - [PingWidgetProvider1x2] 1x2（2 格宽 × 1 格高横条）：只显示第一个站点
 *
 * 数据来源：Flutter 侧 `shared_preferences` 写入的 SharedPreferences
 * （文件 `FlutterSharedPreferences`，键前缀 `flutter.`），
 * 因此小组件与 Flutter 界面共享同一份站点与状态缓存，无需额外通道。
 *
 * 刷新策略：
 *  - widget 自身只读缓存渲染（零耗电）；
 *  - Flutter 侧每轮检测结束后通过 MethodChannel 调用 [updateAllWidgets]；
 *  - 点按刷新按钮时打开 App，由 App 触发一次真实检测。
 */
open class PingWidgetProvider : AppWidgetProvider() {

    /** 该尺寸显示的站点行数上限 */
    protected open val maxRows: Int get() = 2

    /** 是否为横向单行紧凑样式（1x2） */
    protected open val compact: Boolean get() = false

    companion object {
        /** Flutter shared_preferences 的存储文件与键前缀 */
        const val FLUTTER_PREFS_NAME = "FlutterSharedPreferences"
        const val FLUTTER_KEY_PREFIX = "flutter."

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
                val views = provider.buildRemoteViews(context)
                manager.getAppWidgetIds(ComponentName(context, cls)).forEach { id ->
                    manager.updateAppWidget(id, views)
                }
            }
        }
    }

    // ---------- 缓存读取 ----------

    private class WidgetData(
        val siteNames: List<String>,
        val statuses: Map<String, String>,
        val latencies: Map<String, Long>,
        val lastUpdate: Long
    )

    private fun readData(context: Context): WidgetData {
        val prefs = context.getSharedPreferences(
            FLUTTER_PREFS_NAME, Context.MODE_PRIVATE
        )

        val siteNames = mutableListOf<String>()
        readString(prefs, "sites")?.let { raw ->
            runCatching {
                val arr = JSONArray(raw)
                for (i in 0 until arr.length()) {
                    val name = arr.optJSONObject(i)?.optString("name").orEmpty()
                    if (name.isNotEmpty()) siteNames.add(name)
                }
            }
        }

        val statuses = mutableMapOf<String, String>()
        readString(prefs, "statuses")?.let { raw ->
            runCatching {
                val obj = JSONObject(raw)
                val keys = obj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    statuses[k] = obj.optString(k)
                }
            }
        }

        val latencies = mutableMapOf<String, Long>()
        readString(prefs, "latencies")?.let { raw ->
            runCatching {
                val obj = JSONObject(raw)
                val keys = obj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    latencies[k] = obj.optLong(k, -1L)
                }
            }
        }

        val lastUpdate = runCatching {
            prefs.getLong(FLUTTER_KEY_PREFIX + "last_update", 0L)
        }.getOrDefault(0L)

        return WidgetData(siteNames, statuses, latencies, lastUpdate)
    }

    private fun readString(
        prefs: android.content.SharedPreferences,
        key: String
    ): String? = runCatching {
        prefs.getString(FLUTTER_KEY_PREFIX + key, null)
    }.getOrNull()

    // ---------- 视图构建 ----------

    protected open fun buildRemoteViews(context: Context): RemoteViews {
        val data = readData(context)
        return if (compact) buildCompactViews(context, data) else buildListViews(context, data)
    }

    /** 列表样式（2x2 / 4x3） */
    private fun buildListViews(context: Context, data: WidgetData): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_ping)
        attachCommonActions(context, views)

        views.setTextViewText(
            R.id.widget_time,
            if (data.lastUpdate > 0) timeFormat.format(Date(data.lastUpdate)) else "--:--"
        )

        // 动态添加站点行（行高弹性分布，任何高度都不会溢出）
        views.removeAllViews(R.id.widget_list)
        val rowsToShow = data.siteNames.take(maxRows)
        rowsToShow.forEach { name ->
            val row = RemoteViews(context.packageName, R.layout.widget_item_site)
            bindSiteRow(row, data.statuses[name], name, data.latencies[name] ?: -1L)
            views.addView(R.id.widget_list, row)
        }

        // 列表被尺寸裁剪时提示还有更多站点
        val hiddenCount = data.siteNames.size - rowsToShow.size
        views.setViewVisibility(
            R.id.widget_more,
            if (hiddenCount > 0) View.VISIBLE else View.GONE
        )
        if (hiddenCount > 0) {
            views.setTextViewText(R.id.widget_more, "还有 $hiddenCount 个站点…")
        }

        return views
    }

    /** 紧凑横条样式（1x2） */
    private fun buildCompactViews(context: Context, data: WidgetData): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_ping_compact)
        attachCommonActions(context, views)

        val name = data.siteNames.firstOrNull() ?: return views
        bindSiteRow(views, data.statuses[name], name, data.latencies[name] ?: -1L)
        return views
    }

    /** 点击卡片 → 打开主界面；刷新按钮 → 打开主界面并触发一次检测 */
    private fun attachCommonActions(context: Context, views: RemoteViews) {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?: return
        views.setOnClickPendingIntent(
            R.id.widget_container,
            PendingIntent.getActivity(
                context, 0, launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
        views.setOnClickPendingIntent(
            R.id.widget_refresh,
            PendingIntent.getActivity(
                context, 1, Intent(launchIntent),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
    }

    /** 把某个站点的状态/延迟绑定到行视图（列表行与紧凑横条共用一套 ID） */
    private fun bindSiteRow(row: RemoteViews, status: String?, name: String, latency: Long) {
        val dotRes = when (status) {
            "green" -> R.drawable.dot_green
            "yellow" -> R.drawable.dot_yellow
            "red" -> R.drawable.dot_red
            else -> R.drawable.dot_unknown
        }
        row.setImageViewResource(R.id.widget_item_dot, dotRes)
        row.setTextViewText(R.id.widget_item_name, name)

        when (status) {
            null, "unknown" -> {
                row.setTextViewText(R.id.widget_item_latency, "…")
                row.setTextColor(R.id.widget_item_latency, 0xFF6B7280.toInt())
            }

            "red" -> {
                row.setTextViewText(R.id.widget_item_latency, "—")
                row.setTextColor(R.id.widget_item_latency, 0xFFEF4444.toInt())
            }

            else -> {
                row.setTextViewText(R.id.widget_item_latency, "${latency}ms")
                row.setTextColor(
                    R.id.widget_item_latency,
                    if (status == "green") 0xFF16A34A.toInt() else 0xFFD97706.toInt()
                )
            }
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { id ->
            appWidgetManager.updateAppWidget(id, buildRemoteViews(context))
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: android.os.Bundle?
    ) {
        appWidgetManager.updateAppWidget(appWidgetId, buildRemoteViews(context))
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
