import 'dart:convert';

import 'package:shared_preferences/shared_preferences.dart';

import 'site.dart';

/// 站点列表 / 状态缓存 / 用户设置 的持久化仓库。
///
/// 与原 Android 版一致，全部基于键值存储（Android 端为 SharedPreferences），
/// 键名保持一致，便于与原生小组件读取同一份数据。
class SiteRepository {
  SiteRepository._(this._prefs);

  static const String prefsName = 'bare_ping_prefs';

  static const String keySites = 'sites';
  static const String keyStatuses = 'statuses';
  static const String keyLatencies = 'latencies';
  static const String keyLastUpdate = 'last_update';
  static const String keyNotifyEnabled = 'notify_enabled';
  static const String keyThemeMode = 'theme_mode';

  static SiteRepository? _instance;

  final SharedPreferences _prefs;

  static Future<SiteRepository> getInstance() async {
    if (_instance != null) return _instance!;
    final prefs = await SharedPreferences.getInstance();
    final repo = SiteRepository._(prefs);
    // 首次启动写入默认站点，与原版 getSites() 的懒初始化行为一致
    if (!prefs.containsKey(keySites)) {
      await repo.saveSites(defaultSites());
    }
    _instance = repo;
    return repo;
  }

  // ---------- 站点列表 ----------

  List<Site> getSites() {
    final raw = _prefs.getString(keySites);
    if (raw == null || raw.isEmpty) return defaultSites();
    try {
      final list = (jsonDecode(raw) as List<dynamic>)
          .map((e) => Site.fromJson(Map<String, dynamic>.from(e as Map)))
          .toList();
      return list.isEmpty ? defaultSites() : list;
    } catch (_) {
      return defaultSites();
    }
  }

  Future<void> saveSites(List<Site> sites) async {
    await _prefs.setString(
      keySites,
      jsonEncode(sites.map((s) => s.toJson()).toList()),
    );
  }

  Future<void> addSite(Site site) async {
    final sites = getSites().toList();
    if (sites.every((s) => s.name != site.name)) {
      sites.add(site);
      await saveSites(sites);
    }
  }

  Future<void> removeSite(String name) async {
    await saveSites(getSites().where((s) => s.name != name).toList());
    // 同时清掉该站点的状态与延迟缓存
    final statuses = getStatusMap()..remove(name);
    final latencies = getLatencyMap()..remove(name);
    await saveStatusMap(statuses);
    await saveLatencyMap(latencies);
  }

  // ---------- 检测结果缓存 ----------

  Map<String, PingStatus> getStatusMap() {
    final raw = _prefs.getString(keyStatuses);
    if (raw == null || raw.isEmpty) return <String, PingStatus>{};
    try {
      final map = jsonDecode(raw) as Map<String, dynamic>;
      return map.map(
        (k, v) => MapEntry(
          k,
          PingStatus.values.firstWhere(
            (e) => e.name == v,
            orElse: () => PingStatus.unknown,
          ),
        ),
      );
    } catch (_) {
      return <String, PingStatus>{};
    }
  }

  Future<void> saveStatusMap(Map<String, PingStatus> map) async {
    await _prefs.setString(
      keyStatuses,
      jsonEncode(map.map((k, v) => MapEntry(k, v.name))),
    );
  }

  Map<String, int> getLatencyMap() {
    final raw = _prefs.getString(keyLatencies);
    if (raw == null || raw.isEmpty) return <String, int>{};
    try {
      final map = jsonDecode(raw) as Map<String, dynamic>;
      return map.map((k, v) => MapEntry(k, (v as num).toInt()));
    } catch (_) {
      return <String, int>{};
    }
  }

  Future<void> saveLatencyMap(Map<String, int> map) async {
    await _prefs.setString(keyLatencies, jsonEncode(map));
  }

  int get lastUpdateTime => _prefs.getInt(keyLastUpdate) ?? 0;

  Future<void> setLastUpdateTime(int value) async {
    await _prefs.setInt(keyLastUpdate, value);
  }

  // ---------- 设置 ----------

  bool get notificationsEnabled => _prefs.getBool(keyNotifyEnabled) ?? true;

  Future<void> setNotificationsEnabled(bool value) async {
    await _prefs.setBool(keyNotifyEnabled, value);
  }

  /// 主题模式：0=跟随系统 1=浅色 2=深色
  int get themeMode => _prefs.getInt(keyThemeMode) ?? 0;

  Future<void> setThemeMode(int value) async {
    await _prefs.setInt(keyThemeMode, value);
  }

  /// 与原 HTML 页面一致的默认 GitHub 监测域名
  static List<Site> defaultSites() => const <Site>[
        Site('github.com', 'https://github.com/favicon.ico'),
        Site('api.github.com', 'https://api.github.com/favicon.ico'),
        Site('raw.githubusercontent.com',
            'https://raw.githubusercontent.com/favicon.ico'),
        Site('gist.github.com', 'https://gist.github.com/favicon.ico'),
        Site('github.githubassets.com',
            'https://github.githubassets.com/favicon.ico'),
        Site('camo.githubusercontent.com',
            'https://camo.githubusercontent.com/favicon.ico'),
      ];
}
