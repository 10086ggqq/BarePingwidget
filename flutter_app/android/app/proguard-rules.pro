# ---------------------------------------------------------------------------
# R8 压缩/混淆规则
#
# Flutter Gradle 插件在 isMinifyEnabled = true 时会自动附加官方
# flutter_proguard_rules.pro，因此这里只补「本工程特有、且会被系统按类名
# 反射实例化」的部分，其余交给 R8 正常裁剪。
# ---------------------------------------------------------------------------

# Flutter 引擎与嵌入层：部分类由引擎按名字反射加载，整体保留
-keep class io.flutter.** { *; }
-dontwarn io.flutter.**

# 本工程：MainActivity 与三个桌面组件 Provider 均由系统按类名实例化，
# 保留整包（本包只有几十个符号，对体积影响可忽略）
-keep class com.example.bare_ping_widget.** { *; }

# Kotlin / 协程：仅压制缺失引用告警，不做额外保留，让 R8 继续裁剪未用代码
-dontwarn kotlin.**
-dontwarn kotlinx.**
