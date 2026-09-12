package com.example.bare_ping_widget

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

/**
 * Flutter 宿主 Activity。
 *
 * 通过 MethodChannel 向 Dart 侧暴露「恢复裸连」横幅通知能力，
 * 逻辑与原 Android 版 `notify/NotificationHelper.kt` 保持一致：
 *  - 高优先级渠道 → 屏幕顶部弹出 heads-up 横幅；
 *  - 仅由 StatusMonitor 在检测到 红→绿 状态跃迁时调用一次。
 *
 * 这里刻意只使用系统框架 API（不依赖 androidx.core 与任何第三方插件）。
 */
class MainActivity : FlutterActivity() {

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL_NAME)
            .setMethodCallHandler { call, result ->
                when (call.method) {
                    "createChannel" -> {
                        createChannel()
                        result.success(null)
                    }

                    "hasPermission" -> result.success(hasPermission())

                    "requestPermission" -> {
                        requestPermission()
                        result.success(null)
                    }

                    "notifyRestored" -> {
                        notifyRestored(
                            title = call.argument<String>("title").orEmpty(),
                            text = call.argument<String>("text").orEmpty()
                        )
                        result.success(null)
                    }

                    // 每轮检测结束后刷新三种尺寸的桌面小组件
                    "refreshWidgets" -> {
                        PingWidgetProvider.updateAllWidgets(this)
                        result.success(null)
                    }

                    else -> result.notImplemented()
                }
            }
    }

    // ---------- 通知渠道 ----------

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // 旧版低优先级渠道（渠道重要性创建后不可修改，只能删除重建）
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

    private fun hasPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (hasPermission()) return
        requestPermissions(
            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            REQUEST_CODE_NOTIFICATIONS
        )
    }

    // ---------- 横幅通知 ----------

    private fun notifyRestored(title: String, text: String) {
        if (title.isEmpty() || !hasPermission()) return
        createChannel()

        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            ?: return
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }

        val notification = builder
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(Notification.BigTextStyle().bigText(text))
            // Android 8 以下靠 priority 弹横幅；8+ 由渠道 IMPORTANCE_HIGH 决定
            .setPriority(Notification.PRIORITY_HIGH)
            .setCategory(Notification.CATEGORY_STATUS)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // 用户回收了通知权限，静默忽略
        }
    }

    private companion object {
        const val CHANNEL_NAME = "bare_ping_widget/native"
        const val CHANNEL_ID = "bare_ping_status_alert"
        const val OLD_CHANNEL_ID = "bare_ping_status"
        const val NOTIFICATION_ID = 1001
        const val REQUEST_CODE_NOTIFICATIONS = 1001
    }
}
