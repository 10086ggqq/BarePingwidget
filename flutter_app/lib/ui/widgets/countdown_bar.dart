import 'package:flutter/material.dart';

import '../../theme/app_palette.dart';

/// 底部倒计时渐变条，还原原 HTML / Android 的 `countdown_gradient`：
/// 一条「绿 → 黄 → 红」的整宽渐变，按进度从左往右裁剪显示。
class CountdownBar extends StatelessWidget {
  const CountdownBar({
    super.key,
    required this.progress,
    required this.palette,
  });

  /// 0.0 ~ 1.0
  final double progress;
  final AppPalette palette;

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      height: 2,
      width: double.infinity,
      child: CustomPaint(
        painter: _CountdownPainter(
          progress: progress.clamp(0.0, 1.0),
          start: palette.statusGreen,
          middle: palette.statusYellow,
          end: palette.statusRed,
        ),
      ),
    );
  }
}

class _CountdownPainter extends CustomPainter {
  _CountdownPainter({
    required this.progress,
    required this.start,
    required this.middle,
    required this.end,
  });

  final double progress;
  final Color start;
  final Color middle;
  final Color end;

  @override
  void paint(Canvas canvas, Size size) {
    final full = Rect.fromLTWH(0, 0, size.width, size.height);
    final paint = Paint()
      ..shader = LinearGradient(
        colors: <Color>[start, middle, end],
      ).createShader(full);

    canvas.save();
    // 与原 clip 包裹的 ProgressBar 一致：整宽渐变，按进度裁剪
    canvas.clipRect(
      Rect.fromLTWH(0, 0, size.width * progress, size.height),
    );
    canvas.drawRect(full, paint);
    canvas.restore();
  }

  @override
  bool shouldRepaint(covariant _CountdownPainter oldDelegate) =>
      oldDelegate.progress != progress ||
      oldDelegate.start != start ||
      oldDelegate.middle != middle ||
      oldDelegate.end != end;
}
