import '../data/site.dart';
import '../data/site_repository.dart';
import '../net/network_checker.dart';
import '../notify/notification_helper.dart';
import '../widget/widget_bridge.dart';

/// 状态监测引擎：与 Android 版 `monitor/StatusMonitor.kt` 逻辑一致。
///
/// 通知规则（与需求严格一致）：
///  - 任意站点状态「由红变绿」时发一次横幅通知（同一轮多个站点恢复合并为一条）；
///  - 绿→红、持续红、黄→绿等一切其它变化均不发通知；
///  - 下一次由红变绿时才会再次通知，依次类推；
///  - 设置页关闭通知开关后一律不发。
class StatusMonitor {
  static Future<List<SiteResult>> checkAll(SiteRepository repo) async {
    final sites = repo.getSites();

    // 并发检测（等价于原版的 coroutineScope + async）
    final results = await Future.wait(
      sites.map((site) async {
        final (latency, status) = await NetworkChecker.measureSite(site.url);
        return SiteResult(site, status, latency);
      }),
    );

    // ---- 通知跃迁判断（必须在覆盖旧状态之前）----
    final prevStatuses = repo.getStatusMap();
    if (repo.notificationsEnabled) {
      final recovered = results
          .where((r) =>
              (prevStatuses[r.site.name] ?? PingStatus.unknown) ==
                  PingStatus.red &&
              r.status == PingStatus.green)
          .toList();
      if (recovered.isNotEmpty) {
        await NotificationHelper.notifySitesRestored(recovered);
      }
    }

    // ---- 持久化最新结果 ----
    await repo.saveStatusMap({
      for (final r in results) r.site.name: r.status,
    });
    await repo.saveLatencyMap({
      for (final r in results) r.site.name: r.latencyMs,
    });
    await repo.setLastUpdateTime(DateTime.now().millisecondsSinceEpoch);

    // ---- 通知所有桌面小组件刷新 ----
    await WidgetBridge.updateAllWidgets();

    return results;
  }
}
