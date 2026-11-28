import 'dart:convert';

import 'package:flutter/foundation.dart';
import 'package:http/http.dart' as http;
import 'package:shared_preferences/shared_preferences.dart';

/// Thông tin người dùng đang đăng nhập (trả về từ /api/auth/login).
class AuthUser {
  final int userId;
  final String username;
  final String email;
  final String fullName;
  final String role;

  /// Ngôn ngữ ưa thích (`vi` | `en`) — dùng cho GET /api/artifacts/{id}?lang=
  final String preferredLanguage;

  const AuthUser({
    required this.userId,
    required this.username,
    required this.email,
    required this.fullName,
    required this.role,
    this.preferredLanguage = 'vi',
  });

  factory AuthUser.fromJson(Map<String, dynamic> json) {
    final lang = (json['preferredLanguage']?.toString() ?? 'vi').toLowerCase();
    return AuthUser(
      userId: (json['userId'] as num).toInt(),
      username: json['username']?.toString() ?? '',
      email: json['email']?.toString() ?? '',
      fullName: json['fullName']?.toString() ?? '',
      role: json['role']?.toString() ?? 'VISITOR',
      preferredLanguage: RegExp(r'^[a-z]{2,3}(-[a-z0-9]{2,6})?$').hasMatch(lang) && lang.length <= 10 ? lang : 'vi',
    );
  }

  Map<String, dynamic> toJson() => {
    'userId': userId,
    'username': username,
    'email': email,
    'fullName': fullName,
    'role': role,
    'preferredLanguage': preferredLanguage,
  };

  bool get isAdmin => role == 'ADMIN';

  /// Tên hiển thị: ưu tiên họ tên, nếu trống thì dùng username.
  String get displayName => fullName.isNotEmpty ? fullName : username;
}

/// Gọi REST API xác thực của Backend (AuthController) và giữ phiên đăng nhập JWT.
///
/// Sprint 3 (Bảo): sau khi đăng nhập lưu accessToken + refreshToken vào
/// SharedPreferences (tắt app mở lại vẫn còn đăng nhập). Việc gắn token vào
/// request do AuthHttpClient (lib/core/network) làm tự động.
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
  static String get baseUrl => _serverOverride ?? _defaultBaseUrl;

  static String get _defaultBaseUrl {
    const fromEnv = String.fromEnvironment('API_BASE_URL');
    if (fromEnv.isNotEmpty) return fromEnv;
    if (!kIsWeb && defaultTargetPlatform == TargetPlatform.android) {
      return 'http://10.0.2.2:8080';
    }
    return 'http://localhost:8080';
  }

  static const Duration _timeout = Duration(seconds: 10);

  static const String _kUser = 'sh_auth_user';
  static const String _kAccess = 'sh_access_token';
  static const String _kRefresh = 'sh_refresh_token';
  static const String _kSessionBaseUrl = 'sh_auth_session_base_url';
  static const String _kServerOverride = 'sh_api_base_url';

  static String? _serverOverride;

  static String get serverStoragePrefix =>
      _serverOverride != null && _serverOverride != _defaultBaseUrl
      ? '${Uri.encodeComponent(baseUrl)}.'
      : '';

  static Uri? parseServerAddress(String value) {
    final address = Uri.tryParse(value.trim());
    if (address == null ||
        address.scheme != 'https' ||
        address.host.isEmpty ||
        address.authority.contains('@') ||
        address.hasQuery ||
        address.hasFragment ||
        (address.path.isNotEmpty && address.path != '/')) {
      return null;
    }
    return Uri.parse(address.origin);
  }

  /// Thử một API công khai mà không gửi thông tin đăng nhập.
  static Future<bool> canConnectToServer(Uri address) async {
    try {
      final request = http.Request(
        'GET',
        address.resolve('/api/heritage-sites'),
      )..followRedirects = false;
      final response = await rawClient
          .send(request)
          .then(http.Response.fromStream)
          .timeout(_timeout);
      if (response.statusCode != 200) return false;
      final body = jsonDecode(utf8.decode(response.bodyBytes));
      return body is Map<String, dynamic> && body['data'] is List;
    } catch (_) {
      return false;
    }
  }

  static Future<void> saveServerAddress(Uri address) async {
    final nextUrl = parseServerAddress(address.toString())?.origin;
    if (nextUrl == null) throw ArgumentError('Cần một địa chỉ HTTPS.');
    final prefs = await SharedPreferences.getInstance();
    final changed = nextUrl != baseUrl;
    final saved = await prefs.setString(_kServerOverride, nextUrl);
    if (!saved) throw StateError('Không lưu được địa chỉ máy chủ.');
    _serverOverride = nextUrl;
    if (changed) await _clearSession();
  }

  /// Người dùng hiện tại; các màn hình lắng nghe để tự cập nhật.
  static final ValueNotifier<AuthUser?> currentUser = ValueNotifier<AuthUser?>(
    null,
  );

  static String? _accessToken;
  static String? _refreshToken;
  static Future<bool>? _refreshing;

  /// Client gốc (không qua AuthHttpClient), main() gán khi khởi động.
  static http.Client rawClient = http.Client();

  static bool get isLoggedIn => currentUser.value != null;
  static String? get accessToken => _accessToken;
  static bool get hasRefreshToken => _refreshToken != null;

  /// Ngôn ngữ nội dung: preferredLanguage của user; chưa đăng nhập → `vi`.
  static String get preferredLanguage =>
      currentUser.value?.preferredLanguage ?? 'vi';

  /// Gọi 1 lần trong main(): đọc lại phiên đã lưu.
  static Future<void> init() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      _serverOverride = parseServerAddress(
        prefs.getString(_kServerOverride) ?? '',
      )?.origin;
      _accessToken = prefs.getString(_kAccess);
      _refreshToken = prefs.getString(_kRefresh);
      final rawUser = prefs.getString(_kUser);
      final storedServer = prefs.getString(_kSessionBaseUrl);
      if (rawUser != null && _refreshToken != null && storedServer == baseUrl) {
        currentUser.value = AuthUser.fromJson(
          jsonDecode(rawUser) as Map<String, dynamic>,
        );
      } else {
        await _clearSession();
      }
    } catch (_) {
      // Dữ liệu lưu bị hỏng: coi như chưa đăng nhập
      await _clearSession();
    }
  }

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

  static Future<Map<String, dynamic>> login(
    String username,
    String password,
  ) async {
    final server = baseUrl;
    final result = await _post('/api/auth/login', {
      'username': username.trim(),
      'password': password,
    });
    if (baseUrl != server) return {'error': 'Địa chỉ máy chủ đã thay đổi.'};
    if (!result.containsKey('error')) {
      if (!await _saveSession(result, server)) {
        return {'error': 'Địa chỉ máy chủ đã thay đổi.'};
      }
    }
    return result;
  }

  /// Tải lại hồ sơ từ Backend (GET /api/auth/me, cần token).
  static Future<Map<String, dynamic>> refreshProfile() async {
    if (currentUser.value == null) return {'error': 'Bạn chưa đăng nhập.'};
    final server = baseUrl;
    try {
      // http.get đi qua AuthHttpClient nên tự có Bearer token
      final response = await http
          .get(Uri.parse('$server/api/auth/me'))
          .timeout(_timeout);
      if (baseUrl != server || currentUser.value == null) {
        return {'error': 'Phiên đăng nhập đã thay đổi.'};
      }
      if (response.statusCode == 401 && !hasRefreshToken) {
        return {'error': 'Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại.'};
      }
      final result = _handle(response);
      if (!result.containsKey('error')) {
        final user = AuthUser.fromJson(result);
        currentUser.value = user;
        final prefs = await SharedPreferences.getInstance();
        await prefs.setString(_kUser, jsonEncode(user.toJson()));
      }
      return result;
    } catch (_) {
      return {'error': _connectionError};
    }
  }

  /// Đổi refreshToken lấy cặp token mới. Nhiều request cùng gặp 401 chỉ gọi 1 lần.
  /// Thất bại (token hết hạn/bị thu hồi) -> xóa phiên, người dùng phải đăng nhập lại.
  static Future<bool> refreshTokens() {
    return _refreshing ??= _doRefresh().whenComplete(() => _refreshing = null);
  }

  static Future<bool> _doRefresh() async {
    final token = _refreshToken;
    if (token == null) return false;
    final server = baseUrl;
    try {
      final response = await rawClient
          .post(
            Uri.parse('$server/api/auth/refresh'),
            headers: {'Content-Type': 'application/json; charset=UTF-8'},
            body: jsonEncode({'refreshToken': token}),
          )
          .timeout(_timeout);
      if (baseUrl != server || _refreshToken != token) return false;
      if (response.statusCode == 200) {
        return await _saveSession(
          jsonDecode(utf8.decode(response.bodyBytes)) as Map<String, dynamic>,
          server,
        );
      }
      if (response.statusCode == 401) {
        await _clearSession();
      }
      return false;
    } catch (_) {
      // Mất mạng: giữ phiên, lần sau thử lại
      return false;
    }
  }

  /// Đăng xuất: thu hồi refreshToken trên Backend rồi xóa phiên trên máy.
  static Future<void> logout() async {
    final token = _refreshToken;
    final server = baseUrl;
    await _clearSession();
    if (token == null) return;
    try {
      await rawClient
          .post(
            Uri.parse('$server/api/auth/logout'),
            headers: {'Content-Type': 'application/json; charset=UTF-8'},
            body: jsonEncode({'refreshToken': token}),
          )
          .timeout(_timeout);
    } catch (_) {
      // Mất mạng: phiên trên máy đã xóa, token trên server tự hết hạn
    }
  }

  static Future<bool> _saveSession(
    Map<String, dynamic> body,
    String server,
  ) async {
    final user = AuthUser.fromJson(body);
    final access = body['accessToken']?.toString();
    final refresh = body['refreshToken']?.toString();
    final prefs = await SharedPreferences.getInstance();
    if (baseUrl != server) return false;
    _accessToken = access;
    _refreshToken = refresh;
    currentUser.value = user;
    await Future.wait([
      prefs.setString(_kSessionBaseUrl, server),
      prefs.setString(_kUser, jsonEncode(user.toJson())),
      if (access != null) prefs.setString(_kAccess, access),
      if (refresh != null) prefs.setString(_kRefresh, refresh),
    ]);
    return baseUrl == server;
  }

  static Future<void> _clearSession() async {
    _accessToken = null;
    _refreshToken = null;
    currentUser.value = null;
    try {
      final prefs = await SharedPreferences.getInstance();
      await prefs.remove(_kUser);
      await prefs.remove(_kAccess);
      await prefs.remove(_kRefresh);
      await prefs.remove(_kSessionBaseUrl);
    } catch (_) {}
  }

  // ---------- Helpers ----------

  static String get _connectionError =>
      'Không kết nối được máy chủ ($baseUrl). Hãy kiểm tra Backend đã chạy chưa.';

  static Future<Map<String, dynamic>> _post(
    String path,
    Map<String, dynamic> body,
  ) async {
    try {
      final response = await rawClient
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
      'error':
          data['message']?.toString() ??
          'Có lỗi xảy ra (mã ${response.statusCode}).',
    };
  }
}
