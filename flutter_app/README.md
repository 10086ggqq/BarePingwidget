<p align="center">
  <a href="../README.md"><img src="https://img.shields.io/badge/README-中文-blue" alt="中文 README"></a>
  <a href="../README_EN.md"><img src="https://img.shields.io/badge/README-English-blue" alt="English README"></a>
</p>

# 裸连监测 · Flutter 版

这是 [裸连监测 (BarePingwidget)](../README.md) 的 Flutter 实现，包名 `com.example.bare_ping_widget`。

完整功能说明、截图与三套实现的对比请见**仓库根目录的 [README.md](../README.md)**，本文件只覆盖 Flutter 版的构建要点。

---

## 构建

```bash
flutter pub get
flutter run                                    # 调试运行
flutter build apk --release --split-per-abi    # 分 ABI 出 Release 包（推荐）
```

产物位于 `build/app/outputs/flutter-apk/`，例如 `app-arm64-v8a-release.apk`。

> 用 `--split-per-abi` 而不是 fat 包：三个 ABI 叠加会到约 50MB，拆分后 arm64 约 17MB、armeabi-v7a 约 15MB。
> 正式包已开启 R8 代码压缩与资源压缩（`isMinifyEnabled` / `isShrinkResources`），keep 规则见 `android/app/proguard-rules.pro`。

---

## Release 签名

签名文件与口令**不入库**。在 `android/` 下新建 `key.properties`：

```properties
storeFile=bareping.jks
storePassword=你的口令
keyAlias=bareping
keyPassword=你的口令
```

- `storeFile` 相对 `android/app/` 解析，也可写绝对路径
- 缺少 `key.properties` 时自动降级为 debug 签名，保证 clone 后可直接构建（仅供本地验证）

---

## 与原生版的差异

| 项 | 原生 `app/` | Flutter 版 |
| --- | --- | --- |
| 语言 | Kotlin | Dart |
| 后台周期监测 | WorkManager（15 分钟一档） | 无（仅前台轮询） |
| 桌面小组件 | `PingWidgetProvider.kt` | 同左，由 `android/` 宿主工程提供 |
| 通知 | 原生 Notification | 原生 MethodChannel 转发 |
| 小组件数据源 | 原生 SharedPreferences | `FlutterSharedPreferences`（`flutter.` 前缀） |

状态阈值与持久化键名两版保持一致：3 次采样取均值，绿 < 500ms / 黄 < 3000ms / 红 = 不可直连，红 → 绿仅提醒一次，前台 30 秒轮询一轮。

---

## 目录

```text
flutter_app/
├── lib/
│   ├── main.dart                 # 入口
│   ├── app_state.dart            # 全局状态
│   ├── data/                     # Site 模型 + SharedPreferences 持久化
│   ├── net/network_checker.dart  # 裸连探测
│   ├── monitor/status_monitor.dart # 检测引擎
│   ├── notify/                   # 通知（走 MethodChannel）
│   ├── theme/app_palette.dart    # 浅色 / 深色色板
│   ├── ui/                       # 主页、设置页、通用控件
│   └── widget/widget_bridge.dart # 与原生小组件交互的桥
└── android/                      # 宿主工程 + 原生小组件 Provider
```
