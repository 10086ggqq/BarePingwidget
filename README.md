<p align="center">
  <a href="README_EN.md"><img src="https://img.shields.io/badge/README-English-blue" alt="English README"></a>
  <img src="https://img.shields.io/badge/License-MIT-yellow.svg" alt="License: MIT">
  <img src="https://img.shields.io/github/stars/USERNAME/BarePingWidget?style=flat-square" alt="GitHub stars">
</p>

# 裸连监测 (BarePingWidget)

> 一个 Android 桌面小组件与 App，实时监测 GitHub 等站点能否**裸连（绕过系统代理直连）**，用绿 / 黄 / 红三色状态与延迟直观呈现，并在恢复可连时弹出横幅提醒。

---

## 项目名称与简介

**裸连监测（BarePingWidget）** 是轻量级的 Android 连通性监测工具：它绕过系统代理对目标站点发起直连探测，将「可裸连 / 可裸连但慢 / 不可裸连」映射为绿、黄、红三态，并以桌面小组件和通知的形式常驻呈现，专为需要随时掌握 GitHub 等站点直连状况的用户设计。

- 📦 License：[MIT](#license)
- ⭐ 若觉得有用，欢迎 Star 支持：`https://github.com/USERNAME/BarePingWidget`（请替换 `USERNAME`）

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

## 安装与使用说明

### 1. 环境要求

- **JDK**：11（项目以 `jvmTarget = JVM_11` 编译，Gradle 9.x 自带工具链解析）
- **Android SDK**：`compileSdk = 37`，`minSdk = 24`（Android 7.0+ 即可运行）
- **开发环境**：推荐 Android Studio（Hedgehog 或更新版本）
- **构建工具**：Gradle 9.5.0（已由项目 Wrapper 锁定，无需手动安装）

```bash
# 查看 JDK 版本（需 11+）
java -version

# 查看 Android SDK 路径（Android Studio 中 Settings → SDK Manager）
echo $ANDROID_HOME
```

### 2. 拉取代码与依赖安装

```bash
# 克隆仓库
git clone https://github.com/USERNAME/BarePingWidget.git
cd BarePingWidget

# 使用 Gradle Wrapper 同步并下载全部依赖（无需 npm / pip）
./gradlew build        # Linux / macOS
gradlew.bat build      # Windows
```

> 依赖（AppCompat、Material、WorkManager、Coroutines、RecyclerView 等）由 `gradle/libs.versions.toml` 统一声明，Wrapper 会自动下载。

### 3. 配置文件修改

Android 项目无需额外配置即可编译。若在本机首次打开，请确认 `local.properties` 中的 SDK 路径（该文件已被 `.gitignore` 忽略，不会提交）：

```properties
# local.properties
sdk.dir=/path/to/Android/Sdk
```

如需自定义默认监测站点，直接修改 `app/src/main/java/com/example/barepingwidget/data/SiteRepository.kt` 中的 `defaultSites()`，或在 App 内「设置 → 监测站点」动态增删。

### 4. 启动与运行

```bash
# 生成 Debug 版 APK（产物位于 app/build/outputs/apk/debug/）
./gradlew assembleDebug

# 连接设备 / 模拟器后直接安装并启动
./gradlew installDebug
```

安装后在桌面长按 →「小组件」→ 选择「裸连监测 2×2 / 4×3 / 1×2」即可添加到主屏。

---

## 技术栈

| 技术                       | 用途                               | 版本      |
| -------------------------- | ---------------------------------- | --------- |
| Kotlin                     | 主力开发语言                       | 1.9.x     |
| Android Gradle Plugin      | 项目构建与打包                     | 9.3.0     |
| Gradle (Wrapper)           | 构建工具（已锁定发行版）           | 9.5.0     |
| AndroidX AppCompat         | 兼容 Activity / 主题 / 控件       | 1.6.1     |
| AndroidX Core KTX          | 核心 Kotlin 扩展                   | 1.10.1    |
| Material Components        | Material 设计控件（Switch 等）     | 1.10.0    |
| WorkManager                | 后台周期任务调度（省电）           | 2.10.1    |
| Kotlinx Coroutines         | 异步并发检测                       | 1.9.0     |
| RecyclerView               | 站点状态列表展示                   | 1.3.2     |
| ViewBinding                | 类型安全的视图绑定                 | 内置      |
| JUnit / Espresso           | 单元测试 / UI 测试                 | 4.13.2 / 3.5.1 |

---

## 项目结构

```text
BarePingWidget/
├── settings.gradle.kts              # 项目名与模块声明（rootProject.name = "BarePingwidget"）
├── build.gradle.kts                 # 顶层构建脚本（引入 Android 应用插件）
├── gradle.properties                # Gradle 全局配置（JVM 参数、配置缓存）
├── gradlew / gradlew.bat            # Gradle Wrapper 启动脚本（跨平台）
├── .gitignore                       # 忽略构建产物与本地配置
├── local.properties                 # 本机 SDK 路径（已忽略，不提交）
├── gradle/
│   ├── libs.versions.toml           # 版本目录：依赖与插件版本集中管理
│   └── wrapper/                     # Gradle Wrapper 发行版（gradle-9.5.0）
└── app/
    ├── build.gradle.kts             # app 模块配置：SDK 版本、签名、依赖
    ├── .gitignore                   # 忽略 app 构建产物
    └── src/main/
        ├── AndroidManifest.xml      # 清单：权限、MainActivity、SettingsActivity、三个 Widget Receiver
        ├── java/com/example/barepingwidget/
        │   ├── BarePingApp.kt       # Application：初始化通知渠道、调度后台监测、应用主题
        │   ├── data/
        │   │   ├── Site.kt          # 数据模型：Site / SiteResult / PingStatus 枚举
        │   │   └── SiteRepository.kt# SharedPreferences 持久化：站点、状态缓存、用户设置
        │   ├── net/
        │   │   └── NetworkChecker.kt# 裸连检测器：Proxy.NO_PROXY 直连、3 次测均值、阈值判定
        │   ├── monitor/
        │   │   ├── MonitorWorker.kt # WorkManager 工作器，执行一轮检测
        │   │   ├── MonitorScheduler.kt # 周期任务调度（15 分钟，网络+电量约束）
        │   │   └── StatusMonitor.kt # 检测引擎：并发检测、通知跃迁判断、刷新 Widget
        │   ├── ui/
        │   │   ├── MainActivity.kt      # 主界面：状态列表、30s 自动重测、倒计时条、主题切换
        │   │   ├── SettingsActivity.kt  # 设置页：通知开关 + 自定义站点增删
        │   │   └── SiteStatusAdapter.kt # 主界面状态列表适配器
        │   ├── notify/
        │   │   └── NotificationHelper.kt# 通知渠道与「恢复裸连」横幅提醒
        │   └── widget/
        │       └── PingWidgetProvider.kt# 小组件基类，派生 2×2 / 4×3 / 1×2 三尺寸
        └── res/
            ├── values/              # strings.xml / colors.xml / themes.xml
            ├── layout/              # 主界面、设置页、列表项、三种 Widget 布局
            ├── drawable/            # 状态圆点（绿/黄/红/未知/脉冲）、图标、背景
            ├── mipmap/              # 启动图标
            └── xml/                # 三个 widget 配置 + 备份规则
```

---


## License

本项目基于 **MIT 协议** 开源。

```text
MIT License

Copyright (c) 2026 裸连监测 (BarePingWidget)

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

---

## 贡献指南

欢迎 Issue 与 Pull Request！

### 提交 Issue

请使用清晰标题并包含以下信息，便于快速定位：

- **环境**：Android 系统版本、App 版本（`versionName = 1.0`）、设备型号
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

合并前请确权限与隐私风险已评估，勿引入需联网上报的第三方服务。
