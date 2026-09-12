import 'dart:async';

import 'package:flutter/material.dart';

import '../app_state.dart';
import '../data/site.dart';
import '../theme/app_palette.dart';
import 'settings_page.dart';
import 'widgets/circle_icon_button.dart';
import 'widgets/countdown_bar.dart';

/// 主界面：还原原 Android 版 `activity_main.xml` 的卡片体验——
/// 状态点列表、30 秒自动重测、底部倒计时进度条、浅色/深色主题切换。
class HomePage extends StatefulWidget {
  const HomePage({super.key});

  static const Duration refreshInterval = Duration(seconds: 30);

  @override
  State<HomePage> createState() => _HomePageState();
}

class _HomePageState extends State<HomePage>
    with WidgetsBindingObserver, SingleTickerProviderStateMixin {
  final AppState _state = AppState.instance;

  late final AnimationController _countdown;
  Timer? _timer;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _countdown = AnimationController(
      vsync: this,
      duration: HomePage.refreshInterval,
    );
    // 前台期间按原页面的 30 秒节奏轮询
    WidgetsBinding.instance.addPostFrameCallback((_) => _startLoop());
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    _timer?.cancel();
    _countdown.dispose();
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState lifecycleState) {
    super.didChangeAppLifecycleState(lifecycleState);
    // 离开界面即停止，省电；回到前台立即重测
    if (lifecycleState == AppLifecycleState.resumed) {
      _startLoop();
    } else {
      _timer?.cancel();
      _timer = null;
    }
  }

  void _startLoop() {
    _timer?.cancel();
    _timer = null;
    unawaited(_tick());
    _timer = Timer.periodic(HomePage.refreshInterval, (_) => _tick());
  }

  Future<void> _tick() async {
    await _state.runCheck();
    if (!mounted) return;
    _countdown.forward(from: 0);
  }

  static String _formatTime(int millis) {
    final d = DateTime.fromMillisecondsSinceEpoch(millis);
    String two(int v) => v.toString().padLeft(2, '0');
    return '${two(d.hour)}:${two(d.minute)}:${two(d.second)}';
  }

  @override
  Widget build(BuildContext context) {
    final palette = AppPalette.of(context);

    return Scaffold(
      backgroundColor: palette.pageBg,
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(20),
          child: Center(
            child: SizedBox(
              width: 340,
              child: AnimatedBuilder(
                animation: _state,
                builder: (context, _) {
                  return Stack(
                    children: <Widget>[
                      // 主卡片
                      Container(
                        decoration: AppPalette.cardDecoration(palette),
                        padding: const EdgeInsets.all(16),
                        child: Column(
                          mainAxisSize: MainAxisSize.min,
                          crossAxisAlignment: CrossAxisAlignment.stretch,
                          children: <Widget>[
                            _buildHeader(palette),
                            const SizedBox(height: 12),
                            ..._buildRows(palette),
                            const SizedBox(height: 10),
                            _buildFooter(palette),
                          ],
                        ),
                      ),
                      // 底部倒计时渐变条
                      Positioned(
                        left: 1,
                        right: 1,
                        bottom: 0,
                        child: AnimatedBuilder(
                          animation: _countdown,
                          builder: (context, _) => CountdownBar(
                            progress: _countdown.value,
                            palette: palette,
                          ),
                        ),
                      ),
                    ],
                  );
                },
              ),
            ),
          ),
        ),
      ),
    );
  }

  // ---------- 卡片头部：标题 + 主题切换 + 设置 ----------

  Widget _buildHeader(AppPalette palette) {
    final themeIcon = switch (_state.themeMode) {
      1 => Icons.wb_sunny_outlined,
      2 => Icons.dark_mode_outlined,
      _ => Icons.brightness_auto_outlined,
    };

    return Row(
      children: <Widget>[
        Expanded(
          child: Text(
            'GitHub 状态',
            style: TextStyle(
              fontSize: 13,
              color: palette.textSecondary,
              // 原布局 letterSpacing = 0.02em
              letterSpacing: 13 * 0.02,
            ),
          ),
        ),
        CircleIconButton(
          icon: themeIcon,
          palette: palette,
          tooltip: '切换主题',
          onTap: _state.cycleTheme,
          marginEnd: 8,
        ),
        CircleIconButton(
          icon: Icons.settings_outlined,
          palette: palette,
          tooltip: '设置',
          onTap: () => Navigator.of(context).push(
            MaterialPageRoute<void>(builder: (_) => const SettingsPage()),
          ),
        ),
      ],
    );
  }

  // ---------- 状态列表 ----------

  List<Widget> _buildRows(AppPalette palette) {
    if (_state.results.isEmpty) {
      return <Widget>[
        Padding(
          padding: const EdgeInsets.symmetric(vertical: 8),
          child: Text(
            '暂无监测站点',
            style: TextStyle(fontSize: 14, color: palette.textMuted),
          ),
        ),
      ];
    }
    return _state.results
        .map((r) => _SiteStatusRow(
              result: r,
              testing: _state.testing,
              palette: palette,
            ))
        .toList();
  }

  Widget _buildFooter(AppPalette palette) {
    final text = _state.testing
        ? '检测中…'
        : (_state.lastUpdate > 0
            ? '上次更新：${_formatTime(_state.lastUpdate)} · 30 秒后重测'
            : '检测中…');

    return Opacity(
      opacity: 0.7,
      child: Text(
        text,
        textAlign: TextAlign.center,
        style: TextStyle(fontSize: 11, color: palette.textMuted),
      ),
    );
  }
}

/// 「圆点 + 域名 + 延迟」行，还原 `item_site_status.xml`
class _SiteStatusRow extends StatelessWidget {
  const _SiteStatusRow({
    required this.result,
    required this.testing,
    required this.palette,
  });

  final SiteResult result;
  final bool testing;
  final AppPalette palette;

  @override
  Widget build(BuildContext context) {
    final dotColor = _dotColor();
    final (String latencyText, Color latencyColor) = _latency();

    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 8),
      child: Row(
        children: <Widget>[
          Padding(
            padding: const EdgeInsets.only(left: 3, right: 15),
            child: Container(
              width: 8,
              height: 8,
              decoration: BoxDecoration(
                color: dotColor,
                shape: BoxShape.circle,
              ),
            ),
          ),
          Expanded(
            child: Text(
              result.site.name,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              style: TextStyle(fontSize: 14, color: palette.textPrimary),
            ),
          ),
          ConstrainedBox(
            constraints: const BoxConstraints(minWidth: 60),
            child: Text(
              latencyText,
              textAlign: TextAlign.right,
              style: TextStyle(
                fontSize: 13,
                fontFamily: 'monospace',
                color: latencyColor,
              ),
            ),
          ),
        ],
      ),
    );
  }

  Color _dotColor() {
    if (testing || result.status == PingStatus.unknown) {
      return palette.statusUnknown;
    }
    return switch (result.status) {
      PingStatus.green => palette.statusGreen,
      PingStatus.yellow => palette.statusYellow,
      PingStatus.red => palette.statusRed,
      PingStatus.unknown => palette.statusUnknown,
    };
  }

  (String, Color) _latency() {
    if (testing || result.status == PingStatus.unknown) {
      return ('检测中…', palette.textMuted);
    }
    return switch (result.status) {
      PingStatus.red => ('—', palette.statusRed),
      PingStatus.green => ('${result.latencyMs} ms', palette.statusGreen),
      PingStatus.yellow => ('${result.latencyMs} ms', palette.statusYellow),
      PingStatus.unknown => ('检测中…', palette.textMuted),
    };
  }
}

