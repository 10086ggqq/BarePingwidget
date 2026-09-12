import 'package:flutter/services.dart';

import '../data/site.dart';

/// 通知渠道与「恢复裸连」横幅提醒。
///
/// 通过 MethodChannel 调用原生 Android 端实现（不引入任何第三方插件），
/// 行为与原 Android 版 `notify/NotificationHelper.kt` 保持一致：
/// 高优先级渠道 → 屏幕顶部弹出横幅（heads-up）。
class NotificationHelper {
  static const MethodChannel _channel =
      MethodChannel('bare_ping_widget/native');

  static Future<void> createChannel() async {
    await _invoke('createChannel');
  }

  static Future<bool> hasPermission() async {
    try {
      return await _channel.invokeMethod<bool>('hasPermission') ?? true;
    } catch (_) {
      return true;
    }
  }

  static Future<void> requestPermission() async {
    await _invoke('requestPermission');
  }

  /// 发送「恢复裸连」横幅通知。
  ///
  /// 仅由 StatusMonitor 在检测到 红→绿 状态跃迁时调用，天然不会重复发送。
  /// 同一轮多个站点同时恢复时合并为一条通知。
  static Future<void> notifySitesRestored(List<SiteResult> recovered) async {
    if (recovered.isEmpty) return;

    final title = recovered.length == 1
        ? '${recovered[0].site.name} 已恢复裸连 ✅'
        : '${recovered.length} 个站点已恢复裸连 ✅';
    final text = recovered
        .map((r) => r.latencyMs >= 0 ? '${r.site.name} ${r.latencyMs}ms' : r.site.name)
        .join('、');

    try {
      await _channel.invokeMethod<void>('notifyRestored', <String, String>{
        'title': title,
        'text': text,
      });
    } catch (_) {
      // 非 Android 平台或权限被回收时静默忽略
    }
  }

  static Future<void> _invoke(String method) async {
    try {
      await _channel.invokeMethod<void>(method);
    } catch (_) {
      // 非 Android 平台静默忽略
    }
  }
}
