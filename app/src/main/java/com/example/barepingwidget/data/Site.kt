package com.example.barepingwidget.data

/** 连接状态：绿=可裸连且快，黄=可裸连但慢，红=不可裸连，未知=尚未检测 */
enum class PingStatus { GREEN, YELLOW, RED, UNKNOWN }

/** 被监测的站点 */
data class Site(
    val name: String,
    val url: String
)

/** 一次检测的结果 */
data class SiteResult(
    val site: Site,
    val status: PingStatus,
    /** 平均延迟（毫秒），失败时为 -1 */
    val latencyMs: Long
)
