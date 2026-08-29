package com.example.barepingwidget.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

/**
 * 站点列表 / 状态缓存 / 用户设置 的持久化仓库。
 * 全部基于 SharedPreferences，轻量且跨进程（Widget Provider 与 Worker）可读。
 */
class SiteRepository private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ---------- 站点列表 ----------

    fun getSites(): List<Site> {
        val json = prefs.getString(KEY_SITES, null) ?: return defaultSites().also { saveSites(it) }
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                Site(o.getString("name"), o.getString("url"))
            }.ifEmpty { defaultSites() }
        } catch (e: Exception) {
            defaultSites()
        }
    }

    fun saveSites(sites: List<Site>) {
        val arr = JSONArray()
        sites.forEach { s ->
            arr.put(JSONObject().put("name", s.name).put("url", s.url))
        }
        prefs.edit().putString(KEY_SITES, arr.toString()).apply()
    }

    fun addSite(site: Site) {
        val sites = getSites().toMutableList()
        if (sites.none { it.name == site.name }) {
            sites.add(site)
            saveSites(sites)
        }
    }

    fun removeSite(name: String) {
        saveSites(getSites().filterNot { it.name == name })
        // 同时清掉该站点的状态与延迟缓存
        val statuses = getStatusMap().toMutableMap().also { it.remove(name) }
        val latencies = getLatencyMap().toMutableMap().also { it.remove(name) }
        saveStatusMap(statuses)
        saveLatencyMap(latencies)
    }

    // ---------- 检测结果缓存（供 Widget / 通知跃迁判断使用） ----------

    fun getStatusMap(): Map<String, PingStatus> {
        val json = prefs.getString(KEY_STATUSES, null) ?: return emptyMap()
        return try {
            val o = JSONObject(json)
            o.keys().asSequence().associateWith { k ->
                runCatching { PingStatus.valueOf(o.getString(k)) }.getOrDefault(PingStatus.UNKNOWN)
            }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    fun saveStatusMap(map: Map<String, PingStatus>) {
        val o = JSONObject()
        map.forEach { (k, v) -> o.put(k, v.name) }
        prefs.edit().putString(KEY_STATUSES, o.toString()).apply()
    }

    fun getLatencyMap(): Map<String, Long> {
        val json = prefs.getString(KEY_LATENCIES, null) ?: return emptyMap()
        return try {
            val o = JSONObject(json)
            o.keys().asSequence().associateWith { k -> o.getLong(k) }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    fun saveLatencyMap(map: Map<String, Long>) {
        val o = JSONObject()
        map.forEach { (k, v) -> o.put(k, v) }
        prefs.edit().putString(KEY_LATENCIES, o.toString()).apply()
    }

    var lastUpdateTime: Long
        get() = prefs.getLong(KEY_LAST_UPDATE, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_UPDATE, value).apply()

    // ---------- 设置 ----------

    var notificationsEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIFY_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIFY_ENABLED, value).apply()

    /** 主题模式：0=跟随系统 1=浅色 2=深色 */
    var themeMode: Int
        get() = prefs.getInt(KEY_THEME_MODE, 0)
        set(value) = prefs.edit().putInt(KEY_THEME_MODE, value).apply()

    companion object {
        private const val PREFS_NAME = "bare_ping_prefs"
        private const val KEY_SITES = "sites"
        private const val KEY_STATUSES = "statuses"
        private const val KEY_LATENCIES = "latencies"
        private const val KEY_LAST_UPDATE = "last_update"
        private const val KEY_NOTIFY_ENABLED = "notify_enabled"
        private const val KEY_THEME_MODE = "theme_mode"

        @Volatile
        private var instance: SiteRepository? = null

        fun getInstance(context: Context): SiteRepository =
            instance ?: synchronized(this) {
                instance ?: SiteRepository(context.applicationContext).also { instance = it }
            }

        /** 与原 HTML 页面一致的默认 GitHub 监测域名 */
        fun defaultSites(): List<Site> = listOf(
            Site("github.com", "https://github.com/favicon.ico"),
            Site("api.github.com", "https://api.github.com/favicon.ico"),
            Site("raw.githubusercontent.com", "https://raw.githubusercontent.com/favicon.ico"),
            Site("gist.github.com", "https://gist.github.com/favicon.ico"),
            Site("github.githubassets.com", "https://github.githubassets.com/favicon.ico"),
            Site("camo.githubusercontent.com", "https://camo.githubusercontent.com/favicon.ico")
        )
    }
}
