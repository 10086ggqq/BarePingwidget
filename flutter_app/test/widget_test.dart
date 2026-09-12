import 'package:flutter_test/flutter_test.dart';

import 'package:bare_ping_widget/data/site.dart';
import 'package:bare_ping_widget/data/site_repository.dart';

void main() {
  test('默认监测站点与原 Android 版保持一致', () {
    final sites = SiteRepository.defaultSites();
    expect(sites.length, 6);
    expect(sites.first.name, 'github.com');
    expect(sites.first.url, 'https://github.com/favicon.ico');
    expect(sites.last.name, 'camo.githubusercontent.com');
  });

  test('Site 可正确序列化 / 反序列化', () {
    const site = Site('gist.github.com', 'https://gist.github.com/favicon.ico');
    final restored = Site.fromJson(site.toJson());
    expect(restored.name, site.name);
    expect(restored.url, site.url);
  });

  test('PingStatus 枚举覆盖四态', () {
    expect(PingStatus.values.length, 4);
    expect(
      PingStatus.values.map((e) => e.name).toList(),
      <String>['green', 'yellow', 'red', 'unknown'],
    );
  });
}
