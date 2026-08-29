# 裸连监测 (BarePingWidget) — 项目备忘录

- **性质**：Android（Kotlin）桌面小组件 + App，监测 GitHub 等站点能否「裸连」（Proxy.NO_PROXY 直连）。
- **包名 / 项目名**：`com.example.barepingwidget` / `BarePingwidget`；应用中文名「裸连监测」。
- **状态映射**：绿(<500ms) / 黄(<3000ms) / 红(不可连) / 未知。
- **后台**：WorkManager 周期任务，15 分钟一档，约束「已联网 + 电量不低」。
- **通知**：仅站点「红→绿」时弹一次 heads-up 横幅（多站点合并）。
- **三尺寸 Widget**：2×2 / 4×3 / 1×2（PingWidgetProvider 派生）。
- **技术栈**：AGP 9.3.0 / Gradle 9.5.0 / compileSdk 37 / minSdk 24 / JVM 11；WorkManager 2.10.1、Coroutines 1.9.0、Material 1.10.0、AppCompat 1.6.1、RecyclerView 1.3.2。
- **持久化**：SharedPreferences（SiteRepository），站点/状态缓存/主题/通知开关。
- **注意**：用户曾一度误按「微信读书自动阅读器」的功能清单要求写 README；实际代码是裸连监测，已按真实代码撰写 README。
