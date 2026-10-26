
import 'package:flutter/material.dart';

import '../features/auth/auth_service.dart';
import '../features/auth/login_screen.dart';
import '../features/auth/profile_screen.dart';
import '../features/auth/register_screen.dart';
import '../features/favorites/favorites_screen.dart';
import '../features/visit_history/visit_history_screen.dart';

/// Menu bên (Drawer) dùng chung cho các màn hình của app.
class AppSidebar extends StatelessWidget {
  const AppSidebar({super.key});

  void _open(BuildContext context, Widget screen) {
    Navigator.pop(context);
    Navigator.push(
      context,
      MaterialPageRoute(builder: (_) => screen),
    );
  }

  @override
  Widget build(BuildContext context) {
    const sage = Color(0xFF718B78);

    return Drawer(
      child: ValueListenableBuilder<AuthUser?>(
        valueListenable: AuthService.currentUser,
        builder: (context, user, _) {
          return ListView(
            padding: EdgeInsets.zero,
            children: [
              DrawerHeader(
                decoration: const BoxDecoration(color: sage),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  mainAxisAlignment: MainAxisAlignment.end,
                  children: [
                    const Text(
                      'Smart Heritage',
                      style: TextStyle(
                        color: Colors.white,
                        fontSize: 24,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                    const SizedBox(height: 6),
                    Text(
                      user == null
                          ? 'Khách tham quan'
                          : 'Xin chào, ${user.displayName}',
                      style: const TextStyle(color: Colors.white70),
                    ),
                  ],
                ),
              ),
              ListTile(
                leading: const Icon(Icons.home_outlined),
                title: const Text('Trang chủ'),
                onTap: () => Navigator.pop(context),
              ),
              if (user == null) ...[
                ListTile(
                  leading: const Icon(Icons.login),
                  title: const Text('Đăng nhập'),
                  onTap: () => _open(context, const LoginScreen()),
                ),
                ListTile(
                  leading: const Icon(Icons.app_registration),
                  title: const Text('Đăng ký'),
                  onTap: () => _open(context, const RegisterScreen()),
                ),
              ] else ...[
                ListTile(
                  leading: const Icon(Icons.account_circle_outlined),
                  title: const Text('Hồ sơ cá nhân'),
                  onTap: () => _open(context, const ProfileScreen()),
                ),
                ListTile(
                  leading: const Icon(Icons.favorite_border),
                  title: const Text('Hiện vật yêu thích'),
                  onTap: () => _open(context, const FavoritesScreen()),
                ),
                ListTile(
                  leading: const Icon(Icons.history),
                  title: const Text('Lịch sử tham quan'),
                  onTap: () => _open(context, const VisitHistoryScreen()),
                ),
                ListTile(
                  leading: const Icon(Icons.logout),
                  title: const Text('Đăng xuất'),
                  onTap: () {
                    AuthService.logout();
                    Navigator.pop(context);
                  },
                ),
              ],
            ],
          );
        },
      ),
    );
  }
}
