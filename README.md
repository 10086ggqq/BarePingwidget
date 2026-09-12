<p align="center">
  <a href="README_EN.md"><img src="https://img.shields.io/badge/README-English-blue" alt="English README"></a>
  <img src="https://img.shields.io/badge/License-MIT-yellow.svg" alt="License: MIT">
  <img src="https://img.shields.io/github/stars/10086ggqq/BarePingwidget?style=flat-square" alt="GitHub stars">
</p>

# 裸连监测 (BarePingWidget)

> 一个 Android 桌面小组件与 App，实时监测 GitHub 等站点能否**裸连（绕过系统代理直连）**，用绿 / 黄 / 红三色状态与延迟直观呈现，并在恢复可连时弹出横幅提醒。
>
> 同一套功能提供 **Android 原生**、**Flutter**、**纯网页** 三种实现。

---

## 项目名称与简介

**裸连监测（BarePingwidget）** 是轻量级的连通性监测工具：它绕过系统代理对目标站点发起直连探测，将「可裸连 / 可裸连但慢 / 不可裸连」映射为绿、黄、红三态，并以桌面小组件和通知的形式常驻呈现，专为需要随时掌握 GitHub 等站点直连状况的用户设计。

- 📦 License：[MIT](#license)
- ⭐ 若觉得有用，欢迎 Star 支持：`https://github.com/10086ggqq/BarePingwidget`

---

## 三种实现

| 实现 | 目录 | 形态 | 说明 |
| --- | --- | --- | --- |
| **Android 原生** | [`app/`](app/) | APK | Kotlin 实现，功能最完整；含 WorkManager 后台周期监测与三个尺寸桌面小组件 |
| **Flutter 版** | [`flutter_app/`](flutter_app/) | APK | Dart 实现，UI 与原生版对齐；**不含** WorkManager 后台监测（桌面组件仍由原生 Provider 提供） |
| **纯网页版** | [`html/index.html`](html/index.html) | 单文件 HTML | 零依赖、零构建，双击即可用浏览器打开；可直接部署为静态页 |

三套实现共用同一套状态阈值与持久化键名，行为一致：

- 每次探测 3 次采样取均值，**绿 < 500ms**、**黄 < 3000ms**、**红 = 不可直连**、未知 = 尚未探测
- 前台每 30 秒自动重测一轮，离开界面即停止以省电
- 站点由「红 → 绿」时仅提醒一次（多站点合并为一条通知）

---

## 功能特性

- [x] **桌面小组件**：提供 2×2、4×3、1×2 三种尺寸，锁定到主屏即可一眼查看裸连状态
- [x] **裸连状态检测**：绿（< 500ms）/ 黄（< 3000ms）/ 红（不可直连）/ 未知 四态着色
- [x] **后台周期监测**：基于 WorkManager，每 15 分钟一档，受「已联网 + 电量充足」约束省电运行
- [x] **恢复横幅通知**：任意站点由「红 → 绿」时弹出一次 heads-up 提醒（多站点合并为一条）
- [x] **自定义监测站点**：设置页可自由增删域名，默认内置六个 GitHub 相关域名
- [x] **浅色 / 深色 / 跟随系统 主题**：主界面一键循环切换
- [x] **前台 30 秒自动重测**：底部倒计时进度条，离开界面即停止以省电
- [x] **状态缓存与低流量**：仅读取响应头（`Range: bytes=0-0`），单轮流量 < 10KB，冷启动先渲染上次结果

---

## 目录结构

```text
BarePingwidget/
├── app/                             # Android 原生实现（Kotlin）
│   └── src/main/java/com/example/barepingwidget/
│       ├── BarePingApp.kt           # Application：通知渠道、后台调度、主题应用
│       ├── data/                    # Site 数据模型 + SharedPreferences 持久化
│       ├── net/NetworkChecker.kt    # 裸连探测：Proxy.NO_PROXY、3 次均值、阈值判定
│       ├── monitor/                 # WorkManager 工作器 / 调度 / 检测引擎
│       ├── ui/                      # 主界面、设置页、状态列表适配器
│       ├── notify/                  # 通知渠道与「恢复裸连」横幅
│       └── widget/                  # 小组件基类，派生 2×2 / 4×3 / 1×2
├── flutter_app/                     # Flutter 实现（Dart）
│   ├── lib/                         # app_state / data / net / monitor / ui / theme
│   └── android/                     # Flutter 宿主工程 + 原生小组件 Provider
├── html/
│   ├── index.html                   # 纯网页版（单文件自包含）
│   ├── preview-light.png            # 浅色主题截图
│   ├── preview-dark.png             # 深色主题截图
│   └── preview-settings.png         # 设置页截图
├── asset/
│   └── BarePingwidget.png           # 应用图标源图
├── gradle/libs.versions.toml        # 版本目录：依赖与插件版本集中管理
├── README.md / README_EN.md         # 中 / 英双语文档（互链）
└── LICENSE                          # MIT
```

---

## 一、Android 原生版

### 1. 环境要求

- **JDK**：11（项目以 `jvmTarget = JVM_11` 编译，Gradle 9.x 自带工具链解析）
- **Android SDK**：`compileSdk = 37`，`minSdk = 24`（Android 7.0+ 即可运行）
- **开发环境**：推荐 Android Studio（Hedgehog 或更新版本）
- **构建工具**：Gradle 9.5.0（已由项目 Wrapper 锁定，无需手动安装）

### 2. 拉取代码与构建

```bash
git clone https://github.com/10086ggqq/BarePingwidget.git
cd BarePingwidget

./gradlew build          # Linux / macOS
gradlew.bat build        # Windows

./gradlew assembleDebug  # 产物：app/build/outputs/apk/debug/
./gradlew installDebug   # 连接设备 / 模拟器后直接安装
```

首次在本机打开请确认 `local.properties` 中的 SDK 路径（该文件已被 `.gitignore` 忽略，不会提交）：

```properties
# local.properties
sdk.dir=/path/to/Android/Sdk
```

安装后长按桌面 →「小组件」→ 选择「裸连监测 2×2 / 4×3 / 1×2」即可添加到主屏。

如需修改默认监测站点，编辑 `app/src/main/java/com/example/barepingwidget/data/SiteRepository.kt` 中的 `defaultSites()`，或在 App 内「设置 → 监测站点」动态增删。

---

## 二、Flutter 版

### 1. 环境要求

- Flutter SDK（Dart `^3.13.1`）
- JDK 17（`jvmTarget = JVM_17`）
- Android SDK

### 2. 构建

```bash
cd flutter_app
flutter pub get

flutter run                                        # 调试运行
flutter build apk --release --split-per-abi        # 分 ABI 出包（推荐）
```

> 推荐使用 `--split-per-abi`，arm64 与 armeabi-v7a 各自成包（约 17MB / 15MB），避免 fat 包体积叠加到 50MB。

### 3. Release 签名配置

签名口令**不入库**。在 `flutter_app/android/` 下自建 `key.properties`（已被 `.gitignore` 忽略）：

```properties
storeFile=bareping.jks
storePassword=你的口令
keyAlias=bareping
keyPassword=你的口令
```

- `storeFile` 相对 `android/app/` 解析，也可填绝对路径；
- 未提供 `key.properties` 时构建脚本自动降级为 debug 签名，保证 clone 后仍能直接出包（仅供本地验证，勿用于分发）。

### 4. 与原生版的差异

- **不含** WorkManager 后台周期监测（前端轮询逻辑，加上离屏即停的策略与原生版体感一致）
- 桌面小组件仍由 `flutter_app/android/app/src/main/kotlin/.../PingWidgetProvider.kt` 提供，读取 Flutter 侧 `SharedPreferences`（`flutter.` 前缀）中的站点与状态

---

## 三、纯网页版

直接用浏览器打开 `html/index.html` 即可，无需任何依赖与构建。

- 单文件自包含，可拖入 Wallpaper Engine / Lively Wallpaper，或直接部署到任意静态托管
- 状态与站点存于 `localStorage`，与 App 端行为一致
- 效果预览见 `html/preview-light.png`、`html/preview-dark.png`、`html/preview-settings.png`

---

## 技术栈

| 技术                  | 用途                             | 版本      |
| --------------------- | -------------------------------- | --------- |
| Kotlin                | Android 原生版主力语言           | 1.9.x     |
| Android Gradle Plugin | 项目构建与打包                   | 9.3.0     |
| Gradle (Wrapper)      | 构建工具（已锁定发行版）         | 9.5.0     |
| AndroidX AppCompat    | 兼容 Activity / 主题 / 控件      | 1.6.1     |
| AndroidX Core KTX     | 核心 Kotlin 扩展                 | 1.10.1    |
| Material Components   | Material 设计控件（Switch 等）   | 1.10.0    |
| WorkManager           | 后台周期任务调度（省电）         | 2.10.1    |
| Kotlinx Coroutines    | 异步并发检测                     | 1.9.0     |
| RecyclerView          | 站点状态列表展示                 | 1.3.2     |
| ViewBinding           | 类型安全的视图绑定               | 内置      |
| JUnit / Espresso      | 单元测试 / UI 测试               | 4.13.2 / 3.5.1 |
| Flutter / Dart        | Flutter 版跨端实现               | Dart ^3.13.1 |
| shared_preferences    | Flutter 版持久化                 | 2.5.5     |

---

## 已知行为

- 检测仅将 **HTTP 200–399** 视为「可连」，因此返回 404 的站点（如部分默认域名）会显示为红色——这是设计如此，用于区分「通」与「不通」。
- 部分机型对后台任务管控较严，WorkManager 周期任务的实际触发间隔可能长于 15 分钟。

---

## License

本项目基于 **MIT 协议** 开源，详见 [LICENSE](LICENSE)。

---

## 贡献指南

欢迎 Issue 与 Pull Request！

### 提交 Issue

请使用清晰标题并包含以下信息，便于快速定位：

- **环境**：Android 系统版本、App 版本（`versionName = 1.0`）、设备型号、使用的实现（原生 / Flutter / 网页）
- **预期 / 实际**：期望的行为与观察到的现象
- **复现步骤**：可稳定复现的操作序列
- **日志**：必要时附上 `logcat` 中 `com.example.barepingwidget` 相关输出

### Pull Request 流程

1. Fork 本仓库并克隆到本地
2. 基于 `main` 创建特性分支：`git checkout -b feature/your-feature`
3. 保持代码风格一致（Kotlin 官方风格，已配置 `kotlin.code.style=official`）
4. 确保 `./gradlew build` 通过，必要时补充测试
5. 提交信息使用中文或英文简述改动；推送分支并发起 PR
6. 在 PR 描述中说明改动目的、影响范围与截图（若涉及 UI）

> ⚠️ 签名文件（`*.jks` / `*.keystore`）、`key.properties`、`local.properties` 已在 `.gitignore` 中排除，请**不要**提交任何密钥或本机路径。
