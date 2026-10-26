import 'package:http/http.dart' as http;

import '../../features/auth/auth_service.dart';

/// HTTP client tự gắn JWT cho mọi request gọi Backend (Bảo - Sprint 3).
///
/// main.dart bọc app trong `http.runWithClient(..., () => AuthHttpClient(inner))`,
/// nên mọi lệnh `http.get/post/put/delete` ở bất kỳ màn hình nào (Phú, Tường,
/// Xuân Anh...) đều tự đi qua client này, KHÔNG cần sửa code gọi API:
///   1. Gắn header `Authorization: Bearer <accessToken>` nếu đã đăng nhập.
///   2. Gặp 401 (access token hết hạn) -> gọi /api/auth/refresh 1 lần rồi gửi lại.
///   3. Refresh thất bại -> AuthService xóa phiên (người dùng phải đăng nhập lại).
class AuthHttpClient extends http.BaseClient {
  AuthHttpClient(this._inner);

  /// Client thật, tạo 1 lần ở main() (ngoài runWithClient để tránh gọi đệ quy).
  final http.Client _inner;

  static const _authPaths = {
    '/api/auth/login',
    '/api/auth/register',
    '/api/auth/refresh',
    '/api/auth/logout',
  };

  bool _isBackend(Uri url) => url.toString().startsWith(AuthService.baseUrl);

  @override
  Future<http.StreamedResponse> send(http.BaseRequest request) async {
    if (!_isBackend(request.url) || _authPaths.contains(request.url.path)) {
      return _inner.send(request);
    }

    // Giữ bản sao trước khi gửi: request đã gửi thì không gửi lại được
    final retry = _copy(request);
    _attachToken(request);
    final response = await _inner.send(request);

    if (response.statusCode != 401 || retry == null || !AuthService.hasRefreshToken) {
      return response;
    }

    await response.stream.drain<void>();
    final refreshed = await AuthService.refreshTokens();
    if (!refreshed) {
      // Trả về 401 rỗng để màn hình gọi API hiển thị lỗi như bình thường
      return http.StreamedResponse(const Stream.empty(), 401, request: request);
    }
    _attachToken(retry);
    return _inner.send(retry);
  }

  void _attachToken(http.BaseRequest request) {
    final token = AuthService.accessToken;
    if (token != null && !request.headers.containsKey('Authorization')) {
      request.headers['Authorization'] = 'Bearer $token';
    }
  }

  /// Chỉ sao chép được http.Request (http.get/post/... đều dùng loại này).
  /// Multipart/Streamed không gửi lại được -> trả null, giữ nguyên lỗi 401.
  static http.BaseRequest? _copy(http.BaseRequest request) {
    if (request is! http.Request) return null;
    final copy = http.Request(request.method, request.url)
      ..headers.addAll(request.headers)
      ..followRedirects = request.followRedirects
      ..maxRedirects = request.maxRedirects
      ..persistentConnection = request.persistentConnection
      ..bodyBytes = request.bodyBytes;
    copy.headers.remove('Authorization');
    return copy;
  }

  /// Hàm http.get/post cấp cao gọi close() sau MỖI request; không được đóng client thật.
  @override
  void close() {}
}
