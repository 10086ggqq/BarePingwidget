import 'package:flutter/material.dart';

import '../../theme/app_palette.dart';

/// 28dp 圆形图标按钮，还原原布局里 `toggle_bg`（圆形半透明底 + 6dp 内边距）的样式。
class CircleIconButton extends StatelessWidget {
  const CircleIconButton({
    super.key,
    required this.icon,
    required this.palette,
    required this.onTap,
    this.tooltip,
    this.marginEnd = 0,
    this.size = 28,
    this.iconSize = 16,
    this.iconColor,
  });

  final IconData icon;
  final AppPalette palette;
  final VoidCallback onTap;
  final String? tooltip;
  final double marginEnd;
  final double size;
  final double iconSize;
  final Color? iconColor;

  @override
  Widget build(BuildContext context) {
    Widget button = Material(
      color: palette.toggleBg,
      shape: const CircleBorder(),
      clipBehavior: Clip.antiAlias,
      child: InkWell(
        onTap: onTap,
        child: SizedBox(
          width: size,
          height: size,
          child: Padding(
            padding: const EdgeInsets.all(6),
            child: Icon(
              icon,
              size: iconSize,
              color: iconColor ?? palette.textSecondary,
            ),
          ),
        ),
      ),
    );

    if (tooltip != null) {
      button = Tooltip(message: tooltip!, child: button);
    }
    if (marginEnd > 0) {
      button = Padding(padding: EdgeInsets.only(right: marginEnd), child: button);
    }
    return button;
  }
}
