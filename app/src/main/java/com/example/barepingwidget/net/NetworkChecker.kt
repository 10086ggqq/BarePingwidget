package com.example.barepingwidget.net

import com.example.barepingwidget.data.PingStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.Proxy
import java.net.URL

/**
 * 裸连检测器：绕过系统代理直连目标站点，测 3 次取有效平均。
 * 与原 HTML 页面逻辑一致：
 *  - 平均延迟 < 500ms  → 绿（可裸连）
 *  - 平均延迟 < 3000ms → 黄（可裸连但慢）
 *  - 全部失败          → 红（不可裸连）
 */
object NetworkChecker {

    private const val TEST_COUNT = 3
    private const val TIMEOUT_MS = 5000
    private const val THRESHOLD_FAST = 500L
    private const val THRESHOLD_SLOW = 3000L

    /** 测一次延迟，失败返回 -1 */
    suspend fun measureOnce(url: String): Long = withContext(Dispatchers.IO) {
        val start = System.nanoTime()
        var conn: HttpURLConnection? = null
        try {
            // Proxy.NO_PROXY：确保是"裸连"而非走系统代理
            conn = URL(url).openConnection(Proxy.NO_PROXY) as HttpURLConnection
            conn.connectTimeout = TIMEOUT_MS
            conn.readTimeout = TIMEOUT_MS
            conn.instanceFollowRedirects = false
            conn.useCaches = false
            // 只取响应头即可判断连通性，避免下载 body 浪费流量
            conn.requestMethod = "GET"
            conn.setRequestProperty("Range", "bytes=0-0")
            val code = conn.responseCode
            val elapsed = (System.nanoTime() - start) / 1_000_000
            if (code in 200..399 && elapsed < TIMEOUT_MS * 2L) elapsed else -1L
        } catch (e: Exception) {
            -1L
        } finally {
            conn?.disconnect()
        }
    }

    /** 测 3 次取有效平均，返回 (平均延迟ms, 状态)；失败延迟为 -1 */
    suspend fun measureSite(url: String): Pair<Long, PingStatus> = withContext(Dispatchers.IO) {
        val results = (1..TEST_COUNT).map { measureOnce(url) }
        val valid = results.filter { it >= 0 }
        if (valid.isEmpty()) return@withContext -1L to PingStatus.RED
        val avg = valid.sum() / valid.size
        val status = when {
            avg < THRESHOLD_FAST -> PingStatus.GREEN
            avg < THRESHOLD_SLOW -> PingStatus.YELLOW
            else -> PingStatus.RED
        }
        avg to status
    }
}
