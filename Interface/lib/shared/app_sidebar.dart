import 'package:flutter/material.dart';

import '../core/app_strings.dart';
import '../features/auth/auth_service.dart';
import '../features/auth/login_screen.dart';
import '../features/auth/profile_screen.dart';
import '../features/auth/register_screen.dart';
import '../features/favorites/favorites_screen.dart';
import '../features/feedback/my_feedbacks_screen.dart';
import '../features/heritage_sites/map_screen.dart'; // Ân W5
import '../features/visit_history/visit_history_screen.dart';

/// Menu bên (Drawer) dùng chung cho các màn hình của app.
class AppSidebar extends StatelessWidget {
  const AppSidebar({super.key});

  void _open(BuildContext context, Widget screen) {
    Navigator.pop(context); // đóng Drawer
    Navigator.push(context, MaterialPageRoute(builder: (_) => screen));
  }

  @override
  Widget build(BuildContext context) {
    const sage = Color(0xFF718B78);
    return Drawer(
      // Đăng nhập/đăng xuất hoặc đổi ngôn ngữ (nút VI/EN trang chủ) đều vẽ lại menu
      child: ListenableBuilder(
        listenable: Listenable.merge([AuthService.currentUser, AppLang.current]),
        builder: (context, _) {
          final user = AuthService.currentUser.value;
          return ListView(
            padding: EdgeInsets.zero,
            children: [
              DrawerHeader(
                decoration: const BoxDecoration(color: sage),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  mainAxisAlignment: MainAxisAlignment.end,
                  children: [
                    const Text('Smart Heritage', style: TextStyle(color: Colors.white, fontSize: 24, fontWeight: FontWeight.bold)),
                    const SizedBox(height: 6),
                    Text(
                      user == null ? tr('guest') : tr('hello').replaceAll('{name}', user.displayName),
                      style: const TextStyle(color: Colors.white70),
                    ),
                  ],
                ),
              ),
              ListTile(
                leading: const Icon(Icons.home_outlined),
                title: Text(tr('home')),
                onTap: () => Navigator.pop(context),
              ),
              // Ân W5: Bản đồ di tích
              ListTile(
                leading: const Icon(Icons.map_outlined),
                title: Text(tr('heritageMap')),
                onTap: () => _open(context, const MapScreen()),
              ),
              if (user == null) ...[
                ListTile(
                  leading: const Icon(Icons.login),
                  title: Text(tr('login')),
                  onTap: () => _open(context, const LoginScreen()),
                ),
                ListTile(
                  leading: const Icon(Icons.app_registration),
                  title: Text(tr('register')),
                  onTap: () => _open(context, const RegisterScreen()),
                ),
              ] else ...[
                ListTile(
                  leading: const Icon(Icons.account_circle_outlined),
                  title: Text(tr('profile')),
                  onTap: () => _open(context, const ProfileScreen()),
                ),
                ListTile(
                  leading: const Icon(Icons.favorite_border),
                  title: Text(tr('favorites')),
                  onTap: () => _open(context, const FavoritesScreen()),
                ),
                ListTile(
                  leading: const Icon(Icons.history),
                  title: Text(tr('visitHistory')),
                  onTap: () => _open(context, const VisitHistoryScreen()),
                ),
                ListTile(
                  leading: const Icon(Icons.rate_review_outlined),
                  title: Text(tr('myFeedbacks')),
                  onTap: () => _open(context, const MyFeedbacksScreen()),
                ),
                ListTile(
                  leading: const Icon(Icons.logout),
                  title: Text(tr('logout')),
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
