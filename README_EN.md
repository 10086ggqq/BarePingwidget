<p align="center">
  <a href="README.md"><img src="https://img.shields.io/badge/README-中文-blue" alt="中文 README"></a>
  <img src="https://img.shields.io/badge/License-MIT-yellow.svg" alt="License: MIT">
  <img src="https://img.shields.io/github/stars/10086ggqq/BarePingwidget?style=flat-square" alt="GitHub stars">
</p>

# BarePingWidget

> An Android home-screen widget and app that monitors whether sites like GitHub are **reachable without a proxy (bare connection)**, showing green / yellow / red status with latency, and popping a banner alert when connectivity is restored.
>
> The same feature set ships in **three implementations**: native Android, Flutter, and a pure-web single file.

---

## Project Name & Introduction

**BarePingWidget** is a lightweight connectivity monitor. It probes target hosts through a direct, proxy-bypassing connection and maps reachability into three colored states — green (fast & reachable), yellow (reachable but slow), and red (unreachable) — then surfaces that state on a home-screen widget and via notifications. It is built for anyone who needs to keep an eye on whether GitHub and related domains are directly accessible at a glance.

- 📦 License: [MIT](#license)
- ⭐ If you find it useful, a Star helps: `https://github.com/10086ggqq/BarePingwidget`

---

## Three Implementations

| Implementation | Directory | Form | Notes |
| --- | --- | --- | --- |
| **Native Android** | [`app/`](app/) | APK | Kotlin, the most complete; includes WorkManager background monitoring and three widget sizes |
| **Flutter** | [`flutter_app/`](flutter_app/) | APK | Dart, UI matches the native build; **no** WorkManager background monitoring (widgets are still served by the native provider) |
| **Pure web** | [`html/index.html`](html/index.html) | Single HTML file | Zero deps, zero build — just open it in a browser, or host it as a static page |

All three share the same thresholds and storage keys, so behavior is consistent:

- Each probe takes 3 samples and averages them: **green < 500 ms**, **yellow < 3000 ms**, **red = unreachable**, unknown = not probed yet
- Foreground auto-refresh every 30 seconds; stops when you leave the screen to save power
- A site flipping **red → green** triggers exactly one alert (multiple recoveries merged into a single notification)

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

## Repository Layout

```text
BarePingwidget/
├── app/                             # Native Android implementation (Kotlin)
│   └── src/main/java/com/example/barepingwidget/
│       ├── BarePingApp.kt           # Application: notification channel, scheduling, theme
│       ├── data/                    # Site models + SharedPreferences persistence
│       ├── net/NetworkChecker.kt    # Bare probe: Proxy.NO_PROXY, 3-sample average, thresholds
│       ├── monitor/                 # WorkManager worker / scheduler / probe engine
│       ├── ui/                      # Main screen, settings, status list adapter
│       ├── notify/                  # Notification channel & "restored" banner
│       └── widget/                  # Widget base class → 2×2 / 4×3 / 1×2
├── flutter_app/                     # Flutter implementation (Dart)
│   ├── lib/                         # app_state / data / net / monitor / ui / theme
│   └── android/                     # Flutter host project + native widget provider
├── html/
│   ├── index.html                   # Pure-web version (single self-contained file)
│   ├── preview-light.png            # Light-theme screenshot
│   ├── preview-dark.png             # Dark-theme screenshot
│   └── preview-settings.png         # Settings screenshot
├── asset/
│   └── BarePingwidget.png           # App icon source image
├── gradle/libs.versions.toml        # Version catalog: central dependency & plugin versions
├── README.md / README_EN.md         # Chinese / English docs (cross-linked)
└── LICENSE                          # MIT
```

---

## 1. Native Android

### Requirements

- **JDK**: 11 (compiled with `jvmTarget = JVM_11`; Gradle 9.x resolves the toolchain automatically)
- **Android SDK**: `compileSdk = 37`, `minSdk = 24` (runs on Android 7.0+)
- **IDE**: Android Studio (Hedgehog or newer) recommended
- **Build tool**: Gradle 9.5.0 (pinned by the project's Wrapper — no manual install needed)

### Clone & build

```bash
git clone https://github.com/10086ggqq/BarePingwidget.git
cd BarePingwidget

./gradlew build          # Linux / macOS
gradlew.bat build        # Windows

./gradlew assembleDebug  # Output: app/build/outputs/apk/debug/
./gradlew installDebug   # Install & launch on a connected device / emulator
```

On first open, make sure `local.properties` points at your SDK (this file is git-ignored and never committed):

```properties
# local.properties
sdk.dir=/path/to/Android/Sdk
```

After installing, long-press your home screen → **Widgets** → choose **BarePingWidget 2×2 / 4×3 / 1×2**.

To change the default monitored sites, edit `defaultSites()` in `app/src/main/java/com/example/barepingwidget/data/SiteRepository.kt`, or simply add/remove them in-app under **Settings → Monitored sites**.

---

## 2. Flutter

### Requirements

- Flutter SDK (Dart `^3.13.1`)
- JDK 17 (`jvmTarget = JVM_17`)
- Android SDK

### Build

```bash
cd flutter_app
flutter pub get

flutter run                                        # Debug run
flutter build apk --release --split-per-abi        # Per-ABI release (recommended)
```

> Prefer `--split-per-abi`: arm64 and armeabi-v7a each get their own APK (~17 MB / ~15 MB) instead of a fat APK stacking up to ~50 MB.

### Release signing

Signing credentials are **not** committed. Create `flutter_app/android/key.properties` (git-ignored):

```properties
storeFile=bareping.jks
storePassword=your_password
keyAlias=bareping
keyPassword=your_password
```

- `storeFile` resolves relative to `android/app/`, or use an absolute path;
- Without `key.properties` the build script falls back to debug signing so a fresh clone still builds (local verification only — do not distribute).

### Differences from the native build

- **No** WorkManager background monitoring (foreground polling with stop-on-leave matches the native experience closely enough)
- Home-screen widgets are still provided by `flutter_app/android/app/src/main/kotlin/.../PingWidgetProvider.kt`, reading sites and status from the Flutter-side `SharedPreferences` (`flutter.` prefix)

---

## 3. Pure Web

Open `html/index.html` in any browser — no dependencies, no build step.

- Single self-contained file; drop it into Wallpaper Engine / Lively Wallpaper, or deploy to any static host
- Sites and state live in `localStorage`, matching the app's behavior
- See `html/preview-light.png`, `html/preview-dark.png`, and `html/preview-settings.png` for previews

---

## Tech Stack

| Technology                | Purpose                                    | Version   |
| ------------------------- | ------------------------------------------ | --------- |
| Kotlin                    | Primary language (native build)            | 1.9.x     |
| Android Gradle Plugin     | Build & packaging                          | 9.3.0     |
| Gradle (Wrapper)          | Build tool (pinned distribution)           | 9.5.0     |
| AndroidX AppCompat        | Compat Activity / theme / widgets          | 1.6.1     |
| AndroidX Core KTX         | Core Kotlin extensions                     | 1.10.1    |
| Material Components       | Material Design widgets (Switch, …)        | 1.10.0    |
| WorkManager               | Battery-friendly background scheduling     | 2.10.1    |
| Kotlinx Coroutines        | Async concurrent probing                   | 1.9.0     |
| RecyclerView              | Site status list                           | 1.3.2     |
| ViewBinding               | Type-safe view binding                     | Built-in  |
| JUnit / Espresso          | Unit tests / UI tests                      | 4.13.2 / 3.5.1 |
| Flutter / Dart            | Cross-platform implementation              | Dart ^3.13.1 |
| shared_preferences        | Flutter-side persistence                   | 2.5.5     |

---

## Known Behavior

- Only **HTTP 200–399** counts as reachable, so a site returning 404 (some default domains do) shows red — that is intentional, to separate "responds" from "blocked".
- Some OEM ROMs aggressively throttle background work, so the WorkManager interval may stretch beyond 15 minutes in practice.

---

## License

Open source under the **MIT License** — see [LICENSE](LICENSE).

---

## Contributing

Issues and Pull Requests are welcome!

### Filing an Issue

Please use a clear title and include the following so we can triage quickly:

- **Environment**: Android version, app version (`versionName = 1.0`), device model, and which implementation (native / Flutter / web)
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

> ⚠️ Signing files (`*.jks` / `*.keystore`), `key.properties`, and `local.properties` are excluded via `.gitignore` — please **never** commit secrets or machine-specific paths.
