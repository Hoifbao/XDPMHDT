import 'package:firebase_core/firebase_core.dart';
import 'package:firebase_messaging/firebase_messaging.dart';
import 'package:flutter/foundation.dart';
import 'package:http/http.dart' as http;

import '../auth/auth_service.dart';

/// Kết nối FCM cho thiết bị (Sprint 5):
///  1. Khởi tạo Firebase; nếu app Android chưa có cấu hình (google-services.json
///     hoặc cấu hình Dart) thì BỎ QUA lặng lẽ — không làm chặn app, chỉ ghi log.
///  2. Xin quyền nhận thông báo + lấy FCM token của máy.
///  3. Gửi token lên `POST /api/me/devices` (beacon-service qua Gateway);
///     gọi lại mỗi khi đổi tài khoản đăng nhập — backend upsert theo token.
///  4. Nhận sự kiện `onTokenRefresh` để luôn đăng ký token mới nhất.
///     (HTTP qua AuthHttpClient toàn cục — tự gắn Bearer + tự refresh JWT.)
class DeviceRegistrationService {
  DeviceRegistrationService._internal();
  static final DeviceRegistrationService I = DeviceRegistrationService._internal();

  static const String _logTag = '[DEVICE-REG]';
  static const Duration _timeout = Duration(seconds: 8);
  static const String _devicesPath = '/api/me/devices';

  static String get _baseUrl => AuthService.baseUrl;

  int? _lastRegisteredUserId;
  bool _listeningTokenRefresh = false;

  /// Đăng ký nếu hợp lệ; không ném lỗi (chỉ log) để không làm chặn bootstrap.
  Future<void> ensureRegistered() async {
    final userId = AuthService.currentUser.value?.userId;
    if (userId == null) return;
    if (userId == _lastRegisteredUserId) return;

    // 1) Firebase.initializeApp — Android cần google-services.json;
    //    chưa có thì ghi log và thoát, app vẫn chạy bình thường.
    try {
      await Firebase.initializeApp();
    } catch (e) {
      debugPrint('$_logTag Chưa cấu hình Firebase (google-services.json?) '
          '— bỏ qua đăng ký FCM: $e');
      return;
    }

    try {
      final messaging = FirebaseMessaging.instance;
      await messaging.requestPermission(
        alert: true,
        badge: true,
        sound: true,
      );

      // 2) FCM token của máy
      // Firebase Messaging 17.x đổi API (register/onRegistered trả FID),
      // nhưng backend Admin SDK 9.9.0 gửi theo classic registration token
      // → giữ getToken() để hai nửa tương thích nhau.
      // ignore: deprecated_member_use
      final token = await messaging.getToken();
      if (token == null || token.isEmpty) {
        debugPrint('$_logTag Chưa lấy được FCM token (thử lại lần sau).');
        return;
      }

      final ok = await _postToken(token, userId);
      if (ok) {
        debugPrint('$_logTag Đã đăng ký thiết bị lên beacon-service '
            '(user $userId, token ${token.substring(0, 8)}…).');
      } else {
        debugPrint('$_logTag POST $_devicesPath trả mã lỗi (thử lại lần sau).');
      }

      // 3) Firebase có thể đổi token — đăng ký lại token mới.
      if (!_listeningTokenRefresh) {
        _listeningTokenRefresh = true;
        // ignore: deprecated_member_use
        messaging.onTokenRefresh.listen(_onTokenRefresh);
      }
    } catch (e) {
      debugPrint('$_logTag Thiết lập FCM lỗi (thử lại lần sau): $e');
    }
  }

  Future<void> _onTokenRefresh(String token) async {
    final userId = AuthService.currentUser.value?.userId;
    if (userId == null) return;
    final ok = await _postToken(token, userId);
    debugPrint(
        '$_logTag Token refresh → ${ok ? "đã đăng ký lại" : "đăng ký lại thất bại"}.');
  }

  Future<bool> _postToken(String token, int userId) async {
    try {
      final response = await http
          .post(
            Uri.parse('$_baseUrl$_devicesPath'),
            headers: const {
              'Accept': 'application/json',
              'Content-Type': 'application/json; charset=UTF-8',
            },
            body: '{"token": "$token", "platform": "android"}',
          )
          .timeout(_timeout);
      return response.statusCode == 200 || response.statusCode == 201;
    } catch (e) {
      debugPrint('$_logTag Gọi POST $_devicesPath thất bại: $e');
      return false;
    }
  }
}
