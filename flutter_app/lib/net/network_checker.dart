import 'dart:io';

import '../data/site.dart';

/// 裸连检测器：绕过系统代理直连目标站点，测 3 次取有效平均。
///
/// 与原 Android 版 `net/NetworkChecker.kt` 判定逻辑完全一致：
///  - 平均延迟 < 500ms  → 绿（可裸连）
///  - 平均延迟 < 3000ms → 黄（可裸连但慢）
///  - 全部失败          → 红（不可裸连）
class NetworkChecker {
  static const int testCount = 3;
  static const int timeoutMs = 5000;
  static const int thresholdFastMs = 500;
  static const int thresholdSlowMs = 3000;

  static const Duration _timeout = Duration(milliseconds: timeoutMs);

  /// 测一次延迟，失败返回 -1。
  ///
  /// `findProxy` 固定返回 `DIRECT`，等价于 Android 端的 `Proxy.NO_PROXY`，
  /// 确保测到的是「裸连」而非走系统代理。
  static Future<int> measureOnce(String url) async {
    final sw = Stopwatch()..start();
    HttpClient? client;
    try {
      client = HttpClient();
      // 等价于 Android 端的 Proxy.NO_PROXY：确保测到的是「裸连」
      client.findProxy = (Uri _) => 'DIRECT';
      client.connectionTimeout = _timeout;
      client.idleTimeout = _timeout;
      client.userAgent = 'BarePingWidget/1.0';

      final request = await client.getUrl(Uri.parse(url)).timeout(_timeout);
      // 与原版一致：不跟随跳转，3xx 也算连通
      request.followRedirects = false;
      // 只取响应头即可判断连通性，避免下载 body 浪费流量
      request.headers.set(HttpHeaders.rangeHeader, 'bytes=0-0');
      request.headers.set(HttpHeaders.cacheControlHeader, 'no-cache');

      final response = await request.close().timeout(_timeout);
      final code = response.statusCode;
      await response.drain<void>().timeout(_timeout);
      sw.stop();

      final elapsed = sw.elapsedMilliseconds;
      if (code >= 200 && code < 400 && elapsed < timeoutMs * 2) {
        return elapsed;
      }
      return -1;
    } catch (_) {
      return -1;
    } finally {
      client?.close(force: true);
    }
  }

  /// 测 3 次取有效平均，返回 (平均延迟ms, 状态)；失败延迟为 -1
  static Future<(int, PingStatus)> measureSite(String url) async {
    final samples = <int>[];
    for (var i = 0; i < testCount; i++) {
      samples.add(await measureOnce(url));
    }
    final valid = samples.where((e) => e >= 0).toList();
    if (valid.isEmpty) return (-1, PingStatus.red);

    final avg = valid.reduce((a, b) => a + b) ~/ valid.length;
    final status = avg < thresholdFastMs
        ? PingStatus.green
        : (avg < thresholdSlowMs ? PingStatus.yellow : PingStatus.red);
    return (avg, status);
  }
}
