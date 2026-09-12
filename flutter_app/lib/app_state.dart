import 'package:flutter/foundation.dart';

import 'data/site.dart';
import 'data/site_repository.dart';
import 'monitor/status_monitor.dart';
import 'notify/notification_helper.dart';

/// 全局应用状态，等价于原 Android 版中 Activity + Repository 的协作关系。
class AppState extends ChangeNotifier {
  AppState._();

  static final AppState instance = AppState._();

  late SiteRepository repo;

  /// 主题模式：0=跟随系统 1=浅色 2=深色
  int themeMode = 0;

  bool notificationsEnabled = true;

  List<SiteResult> results = <SiteResult>[];

  /// 是否处于「检测中」脉冲态
  bool testing = false;

  int lastUpdate = 0;

  bool _initialized = false;

  Future<void> init() async {
    if (_initialized) return;
    repo = await SiteRepository.getInstance();
    themeMode = repo.themeMode;
    notificationsEnabled = repo.notificationsEnabled;
    lastUpdate = repo.lastUpdateTime;
    results = _cachedResults();

    NotificationHelper.createChannel();
    final granted = await NotificationHelper.hasPermission();
    if (!granted) {
      await NotificationHelper.requestPermission();
    }
    _initialized = true;
  }

  /// 冷启动时先渲染上次缓存结果，避免空白
  List<SiteResult> _cachedResults() {
    final statuses = repo.getStatusMap();
    final latencies = repo.getLatencyMap();
    return repo
        .getSites()
        .map((site) => SiteResult(
              site,
              statuses[site.name] ?? PingStatus.unknown,
              latencies[site.name] ?? -1,
            ))
        .toList();
  }

  List<Site> get sites => repo.getSites();

  /// 跟随系统 → 浅色 → 深色 → 跟随系统
  Future<void> cycleTheme() async {
    themeMode = (themeMode + 1) % 3;
    await repo.setThemeMode(themeMode);
    notifyListeners();
  }

  Future<void> setNotificationsEnabled(bool value) async {
    notificationsEnabled = value;
    await repo.setNotificationsEnabled(value);
    notifyListeners();
  }

  Future<void> addSite(Site site) async {
    await repo.addSite(site);
    results = _cachedResults();
    notifyListeners();
  }

  Future<void> removeSite(String name) async {
    await repo.removeSite(name);
    results = _cachedResults();
    notifyListeners();
  }

  /// 执行一轮检测：先展示脉冲「检测中」状态，再填充结果
  Future<void> runCheck() async {
    final current = repo.getSites();

    testing = true;
    results = current
        .map((site) => SiteResult(site, PingStatus.unknown, -1))
        .toList();
    notifyListeners();

    final fresh = await StatusMonitor.checkAll(repo);

    testing = false;
    results = fresh;
    lastUpdate = repo.lastUpdateTime;
    notifyListeners();
  }
}
