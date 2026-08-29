package com.example.barepingwidget.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.barepingwidget.data.SiteResult

object NotificationHelper {

    // 高优先级渠道：可在屏幕顶部弹出横幅（heads-up）
    private const val CHANNEL_ID = "bare_ping_status_alert"
    // 旧版低优先级渠道（升级后删除，因为渠道重要性创建后不可修改）
    private const val OLD_CHANNEL_ID = "bare_ping_status"
    private const val NOTIFICATION_ID = 1001

    fun createChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.deleteNotificationChannel(OLD_CHANNEL_ID)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "恢复裸连提醒",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "监测站点由红（不可直连）恢复为绿（可直连）时，弹出横幅提醒"
        }
        manager.createNotificationChannel(channel)
    }

    fun hasPermission(context: Context): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(
                    context, Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * 发送"恢复裸连"横幅通知。
     * 仅由 StatusMonitor 在检测到 红→绿 状态跃迁时调用，天然不会重复发送。
     * 同一轮多个站点同时恢复时合并为一条通知。
     */
    fun notifySitesRestored(context: Context, recovered: List<SiteResult>) {
        if (recovered.isEmpty() || !hasPermission(context)) return
        createChannel(context)

        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pending = PendingIntent.getActivity(
            context, 0, launch,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (recovered.size == 1) {
            "${recovered[0].site.name} 已恢复裸连 ✅"
        } else {
            "${recovered.size} 个站点已恢复裸连 ✅"
        }
        val text = recovered.joinToString("、") { r ->
            if (r.latencyMs >= 0) "${r.site.name} ${r.latencyMs}ms" else r.site.name
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            // Android 8 以下靠 priority 弹横幅；8+ 由渠道 IMPORTANCE_HIGH 决定
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            // 用户回收了通知权限，静默忽略
        }
    }
}
