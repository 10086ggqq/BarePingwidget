import 'package:flutter/material.dart';

import 'app_state.dart';
import 'theme/app_palette.dart';
import 'ui/home_page.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await AppState.instance.init();
  runApp(const BarePingApp());
}

/// 应用根：与原 Android 版 `BarePingApp.applyTheme()` 一致，
/// 支持「跟随系统 / 浅色 / 深色」三态循环。
class BarePingApp extends StatelessWidget {
  const BarePingApp({super.key});

  @override
  Widget build(BuildContext context) {
    final state = AppState.instance;

    return AnimatedBuilder(
      animation: state,
      builder: (context, _) {
        return MaterialApp(
          title: '裸连监测',
          debugShowCheckedModeBanner: false,
          themeMode: switch (state.themeMode) {
            1 => ThemeMode.light,
            2 => ThemeMode.dark,
            _ => ThemeMode.system,
          },
          theme: _buildTheme(AppPalette.light, Brightness.light),
          darkTheme: _buildTheme(AppPalette.dark, Brightness.dark),
          home: const HomePage(),
        );
      },
    );
  }

  ThemeData _buildTheme(AppPalette palette, Brightness brightness) {
    return ThemeData(
      brightness: brightness,
      colorScheme: ColorScheme.fromSeed(
        seedColor: AppPalette.primaryGreen,
        brightness: brightness,
      ).copyWith(
        primary: AppPalette.primaryGreen,
        onPrimary: Colors.white,
      ),
      scaffoldBackgroundColor: palette.pageBg,
      snackBarTheme: const SnackBarThemeData(
        behavior: SnackBarBehavior.floating,
      ),
    );
  }
}
