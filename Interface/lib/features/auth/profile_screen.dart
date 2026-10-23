import 'package:flutter/material.dart';

import 'auth_service.dart';
import 'login_screen.dart';

/// Màn hình Hồ sơ cá nhân: hiển thị thông tin người dùng đang đăng nhập.
/// (Cập nhật hồ sơ và RBAC nâng cao làm ở Sprint 3.)
class ProfileScreen extends StatefulWidget {
  const ProfileScreen({super.key});

  @override
  State<ProfileScreen> createState() => _ProfileScreenState();
}

class _ProfileScreenState extends State<ProfileScreen> {
  bool _refreshing = false;

  Future<void> _refresh() async {
    setState(() => _refreshing = true);
    final result = await AuthService.refreshProfile();
    if (!mounted) return;
    setState(() => _refreshing = false);
    if (result.containsKey('error')) {
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(result['error'].toString())));
    }
  }

  void _logout() {
    AuthService.logout();
    Navigator.pushAndRemoveUntil(
      context,
      MaterialPageRoute(builder: (_) => const LoginScreen()),
      (route) => route.isFirst, // quay về Trang chủ, mở Login lên trên
    );
  }

  @override
  Widget build(BuildContext context) {
    const sage = Color(0xFF718B78);
    return Scaffold(
      backgroundColor: const Color(0xFFF7F5EF),
      appBar: AppBar(
        backgroundColor: Colors.transparent,
        elevation: 0,
        title: const Text('Hồ Sơ Cá Nhân'),
        actions: [
          IconButton(
            tooltip: 'Tải lại',
            onPressed: _refreshing ? null : _refresh,
            icon: const Icon(Icons.refresh),
          ),
        ],
      ),
      body: ValueListenableBuilder<AuthUser?>(
        valueListenable: AuthService.currentUser,
        builder: (context, user, _) {
          if (user == null) {
            return Center(
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  const Text('Bạn chưa đăng nhập.'),
                  const SizedBox(height: 12),
                  ElevatedButton(
                    onPressed: () => Navigator.pushReplacement(
                      context,
                      MaterialPageRoute(builder: (_) => const LoginScreen()),
                    ),
                    child: const Text('Đăng nhập'),
                  ),
                ],
              ),
            );
          }

          return ListView(
            padding: const EdgeInsets.all(24),
            children: [
              const Icon(Icons.account_circle, size: 100, color: sage),
              const SizedBox(height: 12),
              Text(
                'Xin chào, ${user.displayName}!',
                textAlign: TextAlign.center,
                style: const TextStyle(fontSize: 24, fontWeight: FontWeight.bold),
              ),
              const SizedBox(height: 24),
              Card(
                elevation: 0,
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
                child: Column(
                  children: [
                    _infoTile(Icons.person_outline, 'Tên đăng nhập', user.username),
                    const Divider(height: 1),
                    _infoTile(Icons.email_outlined, 'Email', user.email.isEmpty ? '—' : user.email),
                    const Divider(height: 1),
                    _infoTile(Icons.badge_outlined, 'Họ và tên', user.fullName.isEmpty ? '—' : user.fullName),
                    const Divider(height: 1),
                    _infoTile(Icons.verified_user_outlined, 'Vai trò', user.role),
                  ],
                ),
              ),
              const SizedBox(height: 24),
              SizedBox(
                height: 48,
                child: OutlinedButton.icon(
                  onPressed: _logout,
                  icon: const Icon(Icons.logout),
                  label: const Text('Đăng xuất'),
                ),
              ),
            ],
          );
        },
      ),
    );
  }

  Widget _infoTile(IconData icon, String label, String value) {
    return ListTile(
      leading: Icon(icon),
      title: Text(label, style: const TextStyle(fontSize: 13, color: Color(0xFF68746C))),
      subtitle: Text(value, style: const TextStyle(fontSize: 16, color: Color(0xFF17211B))),
    );
  }
}
