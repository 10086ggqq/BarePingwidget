import 'package:flutter/material.dart';

import '../app_state.dart';
import '../data/site.dart';
import '../theme/app_palette.dart';
import 'widgets/circle_icon_button.dart';

/// 设置页：通知开关 + 自定义监测站点（添加 / 移除）
/// 还原原 Android 版 `activity_settings.xml` 与 `item_site_manage.xml`。
class SettingsPage extends StatefulWidget {
  const SettingsPage({super.key});

  @override
  State<SettingsPage> createState() => _SettingsPageState();
}

class _SettingsPageState extends State<SettingsPage> {
  final AppState _state = AppState.instance;
  final TextEditingController _nameController = TextEditingController();
  final TextEditingController _urlController = TextEditingController();

  @override
  void dispose() {
    _nameController.dispose();
    _urlController.dispose();
    super.dispose();
  }

  void _toast(String message) {
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(
        SnackBar(
          content: Text(message),
          duration: const Duration(seconds: 2),
          behavior: SnackBarBehavior.floating,
        ),
      );
  }

  Future<void> _addSite() async {
    final name = _nameController.text.trim();
    var url = _urlController.text.trim();

    if (name.isEmpty) {
      _toast('请输入域名');
      return;
    }
    if (url.isEmpty) {
      url = 'https://$name/favicon.ico';
    }
    if (!url.startsWith('http://') && !url.startsWith('https://')) {
      url = 'https://$url';
    }
    if (_state.sites.any((s) => s.name == name)) {
      _toast('该站点已存在');
      return;
    }

    await _state.addSite(Site(name, url));
    if (!mounted) return;
    _nameController.clear();
    _urlController.clear();
    _toast('已添加 $name');
  }

  @override
  Widget build(BuildContext context) {
    final palette = AppPalette.of(context);

    return Scaffold(
      backgroundColor: palette.pageBg,
      body: SafeArea(
        child: AnimatedBuilder(
          animation: _state,
          builder: (context, _) {
            return Padding(
              padding: const EdgeInsets.all(20),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: <Widget>[
                  _buildHeader(palette),
                  const SizedBox(height: 20),
                  _buildNotifyCard(palette),
                  const SizedBox(height: 16),
                  Expanded(child: _buildSitesCard(palette)),
                ],
              ),
            );
          },
        ),
      ),
    );
  }

  // ---------- 头部 ----------

  Widget _buildHeader(AppPalette palette) {
    return Row(
      children: <Widget>[
        CircleIconButton(
          icon: Icons.arrow_back,
          palette: palette,
          tooltip: '返回',
          onTap: () => Navigator.of(context).maybePop(),
        ),
        const SizedBox(width: 12),
        Text(
          '设置',
          style: TextStyle(
            fontSize: 18,
            fontWeight: FontWeight.bold,
            color: palette.textPrimary,
          ),
        ),
      ],
    );
  }

  // ---------- 通知开关卡片 ----------

  Widget _buildNotifyCard(AppPalette palette) {
    return Container(
      decoration: AppPalette.cardDecoration(palette),
      padding: const EdgeInsets.all(16),
      child: Row(
        children: <Widget>[
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: <Widget>[
                Text(
                  '状态通知',
                  style: TextStyle(fontSize: 15, color: palette.textPrimary),
                ),
                const SizedBox(height: 2),
                Text(
                  '任意站点由「不可裸连」恢复为「可裸连」时横幅提醒一次',
                  style: TextStyle(fontSize: 12, color: palette.textMuted),
                ),
              ],
            ),
          ),
          const SizedBox(width: 12),
          Switch(
            value: _state.notificationsEnabled,
            onChanged: (v) => _state.setNotificationsEnabled(v),
          ),
        ],
      ),
    );
  }

  // ---------- 站点管理卡片 ----------

  Widget _buildSitesCard(AppPalette palette) {
    return Container(
      decoration: AppPalette.cardDecoration(palette),
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: <Widget>[
          Text(
            '监测站点',
            style: TextStyle(fontSize: 13, color: palette.textSecondary),
          ),
          const SizedBox(height: 8),
          Expanded(
            child: ListView.builder(
              padding: EdgeInsets.zero,
              itemCount: _state.sites.length,
              itemBuilder: (context, index) {
                final site = _state.sites[index];
                return _SiteManageRow(
                  site: site,
                  palette: palette,
                  onRemove: () => _state.removeSite(site.name),
                );
              },
            ),
          ),
          Container(height: 1, color: palette.divider),
          const SizedBox(height: 12),
          _buildField(
            controller: _nameController,
            hint: '域名，如 github.com',
            palette: palette,
          ),
          const SizedBox(height: 4),
          _buildField(
            controller: _urlController,
            hint: '检测地址（可选，默认 https://域名/favicon.ico）',
            palette: palette,
          ),
          const SizedBox(height: 8),
          SizedBox(
            height: 40,
            child: ElevatedButton(
              onPressed: _addSite,
              style: ElevatedButton.styleFrom(
                backgroundColor: AppPalette.primaryGreen,
                foregroundColor: Colors.white,
                elevation: 0,
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(4),
                ),
              ),
              child: const Text('添加站点'),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildField({
    required TextEditingController controller,
    required String hint,
    required AppPalette palette,
  }) {
    return TextField(
      controller: controller,
      style: TextStyle(fontSize: 14, color: palette.textPrimary),
      decoration: InputDecoration(
        hintText: hint,
        hintStyle: TextStyle(fontSize: 14, color: palette.textMuted),
        isDense: true,
        contentPadding: const EdgeInsets.symmetric(vertical: 8),
        enabledBorder: UnderlineInputBorder(
          borderSide: BorderSide(color: palette.textMuted),
        ),
        focusedBorder: UnderlineInputBorder(
          borderSide: BorderSide(color: AppPalette.primaryGreen),
        ),
      ),
    );
  }
}

/// 站点管理行，还原 `item_site_manage.xml`
class _SiteManageRow extends StatelessWidget {
  const _SiteManageRow({
    required this.site,
    required this.palette,
    required this.onRemove,
  });

  final Site site;
  final AppPalette palette;
  final VoidCallback onRemove;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 8),
      child: Row(
        children: <Widget>[
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: <Widget>[
                Text(
                  site.name,
                  style: TextStyle(fontSize: 14, color: palette.textPrimary),
                ),
                Text(
                  site.url,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: TextStyle(fontSize: 11, color: palette.textMuted),
                ),
              ],
            ),
          ),
          Material(
            color: Colors.transparent,
            shape: const CircleBorder(),
            clipBehavior: Clip.antiAlias,
            child: InkWell(
              onTap: onRemove,
              child: SizedBox(
                width: 32,
                height: 32,
                child: Padding(
                  padding: const EdgeInsets.all(6),
                  child: Icon(
                    Icons.delete_outline,
                    size: 18,
                    color: palette.statusRed,
                  ),
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}
