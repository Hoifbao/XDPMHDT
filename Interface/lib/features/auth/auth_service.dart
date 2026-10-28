import 'dart:convert';

import 'package:flutter/foundation.dart';
import 'package:http/http.dart' as http;

/// Thông tin người dùng đang đăng nhập (trả về từ /api/auth/login).
class AuthUser {
  final int userId;
  final String username;
  final String email;
  final String fullName;
  final String role;

  const AuthUser({
    required this.userId,
    required this.username,
    required this.email,
    required this.fullName,
    required this.role,
  });

  factory AuthUser.fromJson(Map<String, dynamic> json) {
    return AuthUser(
      userId: (json['userId'] as num).toInt(),
      username: json['username']?.toString() ?? '',
      email: json['email']?.toString() ?? '',
      fullName: json['fullName']?.toString() ?? '',
      role: json['role']?.toString() ?? 'VISITOR',
    );
  }

  /// Tên hiển thị: ưu tiên họ tên, nếu trống thì dùng username.
  String get displayName => fullName.isNotEmpty ? fullName : username;
}

/// Gọi REST API xác thực của Backend (AuthController).
///
/// Quy ước kết quả: thành công trả về body JSON của Backend,
/// thất bại trả về `{'error': '<thông báo>'}`.
class AuthService {
  AuthService._();

  /// Địa chỉ Backend:
  ///  - Android emulator truy cập máy tính qua 10.0.2.2
  ///  - Web / Windows / iOS simulator dùng localhost
  /// Chạy trên điện thoại thật: truyền IP LAN của máy chạy Backend, ví dụ
  ///   flutter run --dart-define=API_BASE_URL=http://192.168.1.10:8080
  static String get baseUrl {
    const fromEnv = String.fromEnvironment('API_BASE_URL');
    if (fromEnv.isNotEmpty) return fromEnv;
    if (!kIsWeb && defaultTargetPlatform == TargetPlatform.android) {
      return 'http://10.0.2.2:8080';
    }
    return 'http://localhost:8080';
  }

  static const Duration _timeout = Duration(seconds: 10);

  /// Người dùng hiện tại (lưu trong bộ nhớ; lưu bền vững/JWT làm ở Sprint 3).
  static final ValueNotifier<AuthUser?> currentUser = ValueNotifier<AuthUser?>(null);

  static bool get isLoggedIn => currentUser.value != null;

  static Future<Map<String, dynamic>> register(
    String username,
    String email,
    String password,
    String fullName,
  ) {
    return _post('/api/auth/register', {
      'username': username.trim(),
      'email': email.trim(),
      'password': password,
      'fullName': fullName.trim(),
    });
  }

  static Future<Map<String, dynamic>> login(String username, String password) async {
    final result = await _post('/api/auth/login', {
      'username': username.trim(),
      'password': password,
    });
    if (!result.containsKey('error')) {
      currentUser.value = AuthUser.fromJson(result);
    }
    return result;
  }

  /// Tải lại hồ sơ từ Backend (GET /api/auth/users/{id}).
  static Future<Map<String, dynamic>> refreshProfile() async {
    final user = currentUser.value;
    if (user == null) return {'error': 'Bạn chưa đăng nhập.'};
    try {
      final response = await http
          .get(Uri.parse('$baseUrl/api/auth/users/${user.userId}'))
          .timeout(_timeout);
      final result = _handle(response);
      if (!result.containsKey('error')) {
        currentUser.value = AuthUser.fromJson(result);
      }
      return result;
    } catch (_) {
      return {'error': _connectionError};
    }
  }

  static void logout() {
    currentUser.value = null;
  }

  // ---------- Helpers ----------

  static String get _connectionError =>
      'Không kết nối được máy chủ ($baseUrl). Hãy kiểm tra Backend đã chạy chưa.';

  static Future<Map<String, dynamic>> _post(String path, Map<String, dynamic> body) async {
    try {
      final response = await http
          .post(
            Uri.parse('$baseUrl$path'),
            headers: {'Content-Type': 'application/json; charset=UTF-8'},
            body: jsonEncode(body),
          )
          .timeout(_timeout);
      return _handle(response);
    } catch (_) {
      return {'error': _connectionError};
    }
  }

  static Map<String, dynamic> _handle(http.Response response) {
    Map<String, dynamic> data = {};
    try {
      final decoded = jsonDecode(utf8.decode(response.bodyBytes));
      if (decoded is Map<String, dynamic>) data = decoded;
    } catch (_) {
      // Body không phải JSON
    }

    if (response.statusCode >= 200 && response.statusCode < 300) {
      return data;
    }
    return {
      'error': data['message']?.toString() ?? 'Có lỗi xảy ra (mã ${response.statusCode}).',
    };
  }
}
