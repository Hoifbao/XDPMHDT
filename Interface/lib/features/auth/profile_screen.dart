import 'package:flutter/material.dart';

class ProfileScreen extends StatelessWidget {
  final String fullName;
  const ProfileScreen({super.key, required this.fullName});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Hồ Sơ Cá Nhân')),
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            const Icon(Icons.account_circle, size: 100, color: Colors.blue),
            const SizedBox(height: 20),
            Text('Xin chào, $fullName!', style: const TextStyle(fontSize: 24, fontWeight: FontWeight.bold)),
            const SizedBox(height: 20),
            ElevatedButton(
              onPressed: () {
                Navigator.pop(context);
              },
              child: const Text('Đăng xuất'),
            ),
          ],
        ),
      ),
    );
  }
}