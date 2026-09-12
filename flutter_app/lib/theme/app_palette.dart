import 'package:flutter/material.dart';

/// 与原 Android 版 `values/colors.xml` + `values-night/colors.xml` 一一对应的色板。
class AppPalette {
  const AppPalette({
    required this.pageBg,
    required this.cardBg,
    required this.cardBorder,
    required this.textPrimary,
    required this.textSecondary,
    required this.textMuted,
    required this.divider,
    required this.toggleBg,
    required this.statusGreen,
    required this.statusYellow,
    required this.statusRed,
    required this.statusUnknown,
  });

  final Color pageBg;
  final Color cardBg;
  final Color cardBorder;
  final Color textPrimary;
  final Color textSecondary;
  final Color textMuted;
  final Color divider;
  final Color toggleBg;
  final Color statusGreen;
  final Color statusYellow;
  final Color statusRed;
  final Color statusUnknown;

  /// 浅色主题色板
  static const AppPalette light = AppPalette(
    pageBg: Color(0xFFF0F2F5),
    cardBg: Color(0xF2FFFFFF),
    cardBorder: Color(0xCCFFFFFF),
    textPrimary: Color(0xFF1A1A2E),
    textSecondary: Color(0xFF6B7280),
    textMuted: Color(0xFF9CA3AF),
    divider: Color(0x0F000000),
    toggleBg: Color(0x14000000),
    statusGreen: Color(0xFF22C55E),
    statusYellow: Color(0xFFF59E0B),
    statusRed: Color(0xFFEF4444),
    statusUnknown: Color(0xFF9CA3AF),
  );

  /// 深色主题色板
  static const AppPalette dark = AppPalette(
    pageBg: Color(0xFF0F1117),
    cardBg: Color(0x80000000),
    cardBorder: Color(0x14FFFFFF),
    textPrimary: Color(0xFFE5E7EB),
    textSecondary: Color(0xFF9CA3AF),
    textMuted: Color(0xFF6B7280),
    divider: Color(0x0FFFFFFF),
    toggleBg: Color(0x1FFFFFFF),
    statusGreen: Color(0xFF4ADE80),
    statusYellow: Color(0xFFFBBF24),
    statusRed: Color(0xFFF87171),
    statusUnknown: Color(0xFF6B7280),
  );

  /// 主色（原 themes.xml 的 colorPrimary）
  static const Color primaryGreen = Color(0xFF2DA44E);

  /// 按当前实际明暗取色板
  static AppPalette of(BuildContext context) =>
      Theme.of(context).brightness == Brightness.dark ? dark : light;

  /// 圆角卡片背景（原 drawable/card_bg.xml：16dp 圆角 + 1dp 描边）
  static BoxDecoration cardDecoration(AppPalette p) => BoxDecoration(
        color: p.cardBg,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: p.cardBorder, width: 1),
        boxShadow: const <BoxShadow>[
          BoxShadow(
            color: Color(0x1A000000),
            blurRadius: 12,
            offset: Offset(0, 4),
          ),
        ],
      );
}
