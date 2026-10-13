import 'package:flutter/material.dart';
import 'auth_service.dart';

class RegisterScreen extends StatefulWidget {
  const RegisterScreen({super.key});

  @override
  State<RegisterScreen> createState() => _RegisterScreenState();
}

class _RegisterScreenState extends State<RegisterScreen> {
  final TextEditingController _usernameController = TextEditingController();
  final TextEditingController _passwordController = TextEditingController();
  final TextEditingController _emailController = TextEditingController();
  final TextEditingController _fullNameController = TextEditingController();
  String _message = '';

  void _handleRegister() async {
    final result = await AuthService.register(
      _usernameController.text,
      _emailController.text,
      _passwordController.text,
      _fullNameController.text,
    );

    setState(() {
      if (result.containsKey('error')) {
        _message = result['error'];
      } else {
        _message = 'Đăng ký thành công! Hãy quay lại trang đăng nhập.';
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Đăng Ký SmartHeritage')),
      body: Padding(
        padding: const EdgeInsets.all(16.0),
        child: ListView(
          children: [
            TextField(controller: _fullNameController, decoration: const InputDecoration(labelText: 'Họ và tên')),
            const SizedBox(height: 16),
            TextField(controller: _emailController, decoration: const InputDecoration(labelText: 'Email')),
            const SizedBox(height: 16),
            TextField(controller: _usernameController, decoration: const InputDecoration(labelText: 'Tên đăng nhập')),
            const SizedBox(height: 16),
            TextField(controller: _passwordController, decoration: const InputDecoration(labelText: 'Mật khẩu'), obscureText: true),
            const SizedBox(height: 24),
            ElevatedButton(onPressed: _handleRegister, child: const Text('Đăng ký')),
            const SizedBox(height: 16),
            Text(_message, style: const TextStyle(color: Colors.red, fontWeight: FontWeight.bold)),
          ],
        ),
      ),
    );
  }
}