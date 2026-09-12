import 'package:flutter/services.dart';

/// 桌面小组件桥接。
///
/// Flutter 侧每轮检测结束后调用，通知原生侧刷新 2x2 / 4x3 / 1x2 三种尺寸的小组件；
/// 小组件读取的是同一份 SharedPreferences（`FlutterSharedPreferences`），
/// 因此无需额外传参。
class WidgetBridge {
  static const MethodChannel _channel =
      MethodChannel('bare_ping_widget/native');

  static Future<void> updateAllWidgets() async {
    try {
      await _channel.invokeMethod<void>('refreshWidgets');
    } catch (_) {
      // 非 Android 平台或未注册小组件时静默忽略
    }
  }
}
