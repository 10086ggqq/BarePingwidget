package com.example.barepingwidget.ui

import android.Manifest
import android.animation.ObjectAnimator
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.barepingwidget.BarePingApp
import com.example.barepingwidget.R
import com.example.barepingwidget.data.PingStatus
import com.example.barepingwidget.data.SiteRepository
import com.example.barepingwidget.data.SiteResult
import com.example.barepingwidget.monitor.MonitorScheduler
import com.example.barepingwidget.monitor.StatusMonitor
import com.example.barepingwidget.notify.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 主界面：还原原 HTML 卡片的核心体验——
 * 状态点列表、30 秒自动重测、底部倒计时进度条、浅色/深色主题切换。
 */
class MainActivity : AppCompatActivity() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val handler = Handler(Looper.getMainLooper())
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    private lateinit var repo: SiteRepository
    private lateinit var adapter: SiteStatusAdapter
    private lateinit var footer: TextView
    private lateinit var countdownBar: ProgressBar
    private lateinit var themeToggle: ImageButton

    private val refreshRunnable = object : Runnable {
        override fun run() {
            runCheck()
            handler.postDelayed(this, REFRESH_INTERVAL_MS)
        }
    }

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        repo = SiteRepository.getInstance(this)
        BarePingApp.applyTheme(repo.themeMode)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        NotificationHelper.createChannel(this)
        MonitorScheduler.schedule(this)
        requestNotificationPermissionIfNeeded()

        adapter = SiteStatusAdapter()
        findViewById<RecyclerView>(R.id.status_list).apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = this@MainActivity.adapter
        }

        footer = findViewById(R.id.footer)
        countdownBar = findViewById(R.id.countdown_bar)
        themeToggle = findViewById(R.id.theme_toggle)

        themeToggle.setOnClickListener { cycleTheme() }
        findViewById<ImageButton>(R.id.settings_button).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        updateThemeIcon()
        showCachedResults()
    }

    override fun onResume() {
        super.onResume()
        // 前台期间按原页面的 30 秒节奏轮询；离开界面即停止，省电
        handler.removeCallbacks(refreshRunnable)
        handler.post(refreshRunnable)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(refreshRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    // ---------- 检测 ----------

    private fun runCheck() {
        val sites = repo.getSites()
        // 先展示脉冲"检测中"状态
        adapter.markTesting(sites.map { SiteResult(it, PingStatus.UNKNOWN, -1) })
        footer.text = getString(R.string.testing)

        scope.launch {
            val results = withContext(Dispatchers.IO) {
                StatusMonitor.checkAll(this@MainActivity)
            }
            adapter.submitList(results)
            footer.text = getString(
                R.string.last_update_format,
                timeFormat.format(Date())
            )
            startCountdown()
        }
    }

    /** 冷启动时先渲染上次缓存结果，避免空白 */
    private fun showCachedResults() {
        val statuses = repo.getStatusMap()
        val latencies = repo.getLatencyMap()
        if (statuses.isEmpty()) return
        val results = repo.getSites().map { site ->
            SiteResult(
                site,
                statuses[site.name] ?: PingStatus.UNKNOWN,
                latencies[site.name] ?: -1
            )
        }
        adapter.submitList(results)
        val last = repo.lastUpdateTime
        if (last > 0) {
            footer.text = getString(R.string.last_update_format, timeFormat.format(Date(last)))
        }
    }

    // ---------- 倒计时进度条（还原 HTML 底部渐变条） ----------

    private fun startCountdown() {
        countdownBar.clearAnimation()
        ObjectAnimator.ofInt(countdownBar, "progress", 0, 1000).apply {
            duration = REFRESH_INTERVAL_MS
            interpolator = LinearInterpolator()
            start()
        }
    }

    // ---------- 主题 ----------

    private fun cycleTheme() {
        // 跟随系统 → 浅色 → 深色 → 跟随系统
        repo.themeMode = (repo.themeMode + 1) % 3
        BarePingApp.applyTheme(repo.themeMode)
        updateThemeIcon()
    }

    private fun updateThemeIcon() {
        themeToggle.setImageResource(
            when (repo.themeMode) {
                1 -> R.drawable.ic_sun
                2 -> R.drawable.ic_moon
                else -> R.drawable.ic_theme_auto
            }
        )
    }

    // ---------- 通知权限（Android 13+） ----------

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    companion object {
        private const val REFRESH_INTERVAL_MS = 30_000L
    }
}
