/// 数据模型：与原 Android 版 `data/Site.kt` 一一对应。
library;

/// 连接状态：绿=可裸连且快，黄=可裸连但慢，红=不可裸连，未知=尚未检测
enum PingStatus { green, yellow, red, unknown }

/// 被监测的站点
class Site {
  const Site(this.name, this.url);

  final String name;
  final String url;

  Map<String, dynamic> toJson() => <String, dynamic>{'name': name, 'url': url};

  factory Site.fromJson(Map<String, dynamic> json) =>
      Site(json['name'] as String, json['url'] as String);
}

/// 一次检测的结果
class SiteResult {
  const SiteResult(this.site, this.status, this.latencyMs);

  final Site site;
  final PingStatus status;

  /// 平均延迟（毫秒），失败时为 -1
  final int latencyMs;
}
