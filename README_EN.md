<p align="center">
  <a href="README.md"><img src="https://img.shields.io/badge/README-中文-blue" alt="中文 README"></a>
  <img src="https://img.shields.io/badge/License-MIT-yellow.svg" alt="License: MIT">
  <img src="https://img.shields.io/github/stars/USERNAME/BarePingWidget?style=flat-square" alt="GitHub stars">
</p>

# BarePingWidget

> An Android home-screen widget and app that monitors whether sites like GitHub are **reachable without a proxy (bare connection)**, showing green / yellow / red status with latency, and popping a banner alert when connectivity is restored.

---

## Project Name & Introduction

**BarePingWidget** is a lightweight Android connectivity monitor. It probes target hosts through a direct, proxy-bypassing connection and maps reachability into three colored states — green (fast & reachable), yellow (reachable but slow), and red (unreachable) — then surfaces that state on a home-screen widget and via notifications. It is built for anyone who needs to keep an eye on whether GitHub and related domains are directly accessible at a glance.

- 📦 License: [MIT](#license)
- ⭐ If you find it useful, a Star helps: `https://github.com/USERNAME/BarePingWidget` (replace `USERNAME`)

---

## Features

- [x] **Home-screen widgets**: three sizes — 2×2, 4×3, and 1×2 — pin to your launcher for an at-a-glance status
- [x] **Bare-connection detection**: green (< 500 ms) / yellow (< 3000 ms) / red (unreachable) / unknown, color-coded
- [x] **Background periodic monitoring**: WorkManager runs every 15 minutes, gated by "network connected + battery not low" for battery efficiency
- [x] **Recovery banner**: a one-time heads-up notification when any site flips from red → green (multiple recoveries merged into one)
- [x] **Custom monitored sites**: add or remove hosts from Settings; six GitHub-related domains ship by default
- [x] **Light / Dark / System theme**: cycle with one tap on the main screen
- [x] **30-second foreground auto-refresh**: a countdown progress bar at the bottom; stops when you leave the screen to save power
- [x] **Cached state & low traffic**: only reads response headers (`Range: bytes=0-0`), under 10 KB per round; renders last cached result on cold start

---

## Installation & Usage

### 1. Requirements

- **JDK**: 11 (compiled with `jvmTarget = JVM_11`; Gradle 9.x resolves the toolchain automatically)
- **Android SDK**: `compileSdk = 37`, `minSdk = 24` (runs on Android 7.0+)
- **IDE**: Android Studio (Hedgehog or newer) recommended
- **Build tool**: Gradle 9.5.0 (pinned by the project's Wrapper — no manual install needed)

```bash
# Check JDK version (needs 11+)
java -version

# Locate the Android SDK (Android Studio → Settings → SDK Manager)
echo $ANDROID_HOME
```

### 2. Get the code & install dependencies

```bash
# Clone the repo
git clone https://github.com/USERNAME/BarePingWidget.git
cd BarePingWidget

# Sync and download all dependencies with the Gradle Wrapper (no npm / pip needed)
./gradlew build        # Linux / macOS
gradlew.bat build      # Windows
```

> Dependencies (AppCompat, Material, WorkManager, Coroutines, RecyclerView, …) are declared centrally in `gradle/libs.versions.toml`; the Wrapper downloads them for you.

### 3. Configure

No extra configuration is required to build. On first open, make sure `local.properties` points at your SDK (this file is git-ignored and never committed):

```properties
# local.properties
sdk.dir=/path/to/Android/Sdk
```

To change the default monitored sites, edit `defaultSites()` in `app/src/main/java/com/example/barepingwidget/data/SiteRepository.kt`, or simply add/remove them in-app under **Settings → Monitored sites**.

### 4. Build & run

```bash
# Build the Debug APK (output: app/build/outputs/apk/debug/)
./gradlew assembleDebug

# Install and launch on a connected device / emulator
./gradlew installDebug
```

After installing, long-press your home screen → **Widgets** → choose **BarePingWidget 2×2 / 4×3 / 1×2** to add it to the launcher.

---

## Tech Stack

| Technology                | Purpose                                    | Version   |
| ------------------------- | ------------------------------------------ | --------- |
| Kotlin                    | Primary language                           | 1.9.x     |
| Android Gradle Plugin     | Build & packaging                          | 9.3.0     |
| Gradle (Wrapper)          | Build tool (pinned distribution)           | 9.5.0     |
| AndroidX AppCompat        | Compat Activity / theme / widgets          | 1.6.1     |
| AndroidX Core KTX         | Core Kotlin extensions                     | 1.10.1    |
| Material Components        | Material Design widgets (Switch, …)        | 1.10.0    |
| WorkManager               | Battery-friendly background scheduling    | 2.10.1    |
| Kotlinx Coroutines        | Async concurrent probing                   | 1.9.0     |
| RecyclerView               | Site status list                           | 1.3.2     |
| ViewBinding               | Type-safe view binding                     | Built-in  |
| JUnit / Espresso          | Unit tests / UI tests                      | 4.13.2 / 3.5.1 |

---

## Project Structure

```text
BarePingWidget/
├── settings.gradle.kts              # Project name & module declaration (rootProject.name = "BarePingwidget")
├── build.gradle.kts                 # Root build script (applies the Android application plugin)
├── gradle.properties                # Gradle global config (JVM args, config cache)
├── gradlew / gradlew.bat            # Gradle Wrapper launchers (cross-platform)
├── .gitignore                       # Ignores build output and local config
├── local.properties                 # Local SDK path (ignored, not committed)
├── gradle/
│   ├── libs.versions.toml           # Version catalog: central dependency & plugin versions
│   └── wrapper/                     # Gradle Wrapper distribution (gradle-9.5.0)
└── app/
    ├── build.gradle.kts             # App module config: SDK levels, signing, dependencies
    ├── .gitignore                   # Ignores app build output
    └── src/main/
        ├── AndroidManifest.xml      # Manifest: permissions, MainActivity, SettingsActivity, three Widget receivers
        ├── java/com/example/barepingwidget/
        │   ├── BarePingApp.kt       # Application: notification channel, background scheduling, theme
        │   ├── data/
        │   │   ├── Site.kt          # Models: Site / SiteResult / PingStatus enum
        │   │   └── SiteRepository.kt# SharedPreferences persistence: sites, status cache, settings
        │   ├── net/
        │   │   └── NetworkChecker.kt# Bare-connection probe: Proxy.NO_PROXY, 3-sample average, thresholds
        │   ├── monitor/
        │   │   ├── MonitorWorker.kt # WorkManager worker running one probe round
        │   │   ├── MonitorScheduler.kt # Periodic scheduling (15 min, network + battery constraints)
        │   │   └── StatusMonitor.kt # Probe engine: concurrent checks, recovery detection, widget refresh
        │   ├── ui/
        │   │   ├── MainActivity.kt      # Main UI: status list, 30s auto-refresh, countdown bar, theme toggle
        │   │   ├── SettingsActivity.kt  # Settings: notification switch + custom site add/remove
        │   │   └── SiteStatusAdapter.kt # Main-screen status list adapter
        │   ├── notify/
        │   │   └── NotificationHelper.kt# Notification channel & "restored" banner alert
        │   └── widget/
        │       └── PingWidgetProvider.kt# Widget base class, derived into 2×2 / 4×3 / 1×2 sizes
        └── res/
            ├── values/              # strings.xml / colors.xml / themes.xml
            ├── layout/              # Main, settings, list-item, and three widget layouts
            ├── drawable/            # Status dots (green/yellow/red/unknown/pulse), icons, backgrounds
            ├── mipmap/              # Launcher icons
            └── xml/                # Three widget configs + backup rules
```

---

## Screenshots

> Place screenshots under `doc/` named `1.jpg`, `2.jpg`, … so GitHub renders them automatically.

![Main screen and status list](doc/1.jpg)

![Home-screen widgets (three sizes)](doc/2.jpg)

![Settings: notification toggle and custom sites](doc/3.jpg)

---

## License

This project is open source under the **MIT License**.

```text
MIT License

Copyright (c) 2026 BarePingWidget

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

## Contributing

Issues and Pull Requests are welcome!

### Filing an Issue

Please use a clear title and include the following so we can triage quickly:

- **Environment**: Android version, app version (`versionName = 1.0`), device model
- **Expected / Actual**: what you expected vs. what you observed
- **Steps to reproduce**: a stable sequence of actions
- **Logs**: if relevant, attach `logcat` lines filtered by `com.example.barepingwidget`

### Pull Request workflow

1. Fork the repo and clone it locally
2. Branch off `main`: `git checkout -b feature/your-feature`
3. Keep the code style consistent (official Kotlin style; `kotlin.code.style=official` is set)
4. Make sure `./gradlew build` passes; add tests where it makes sense
5. Write concise commit messages (Chinese or English); push and open a PR
6. In the PR description, explain the intent, impact, and attach screenshots if UI is involved

Before merging, please assess permission and privacy risks; do not introduce third-party services that report data over the network.
