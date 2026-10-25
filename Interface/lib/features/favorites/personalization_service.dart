import 'dart:convert';

import 'package:http/http.dart' as http;

import '../auth/auth_service.dart';

/// Một hiện vật trong danh sách yêu thích (GET /api/users/{userId}/favorites).
class FavoriteItem {
  final int artifactId;
  final String artifactName;
  final String? thumbnailUrl;
  final String? siteName;
  final DateTime? savedAt;

  const FavoriteItem({
    required this.artifactId,
    required this.artifactName,
    this.thumbnailUrl,
    this.siteName,
    this.savedAt,
  });

  factory FavoriteItem.fromJson(Map<String, dynamic> json) => FavoriteItem(
        artifactId: (json['artifactId'] as num).toInt(),
        artifactName: json['artifactName']?.toString() ?? '',
        thumbnailUrl: json['thumbnailUrl']?.toString(),
        siteName: json['siteName']?.toString(),
        savedAt: DateTime.tryParse(json['savedAt']?.toString() ?? ''),
      );
}

/// Một lượt tham quan (GET /api/users/{userId}/visits).
class VisitItem {
  final int id;
  final int artifactId;
  final String artifactName;
  final String? siteName;
  final DateTime? detectedAt;
  final int durationSeconds;

  const VisitItem({
    required this.id,
    required this.artifactId,
    required this.artifactName,
    this.siteName,
    this.detectedAt,
    this.durationSeconds = 0,
  });

  factory VisitItem.fromJson(Map<String, dynamic> json) => VisitItem(
        id: (json['id'] as num).toInt(),
        artifactId: (json['artifactId'] as num).toInt(),
        artifactName: json['artifactName']?.toString() ?? '',
        siteName: json['siteName']?.toString(),
        detectedAt: DateTime.tryParse(json['detectedAt']?.toString() ?? ''),
        durationSeconds: (json['durationSeconds'] as num?)?.toInt() ?? 0,
      );
}

/// Gọi API Cá nhân hoá (PersonalizationController – Phú).
/// Tuần 2 truyền userId của người đang đăng nhập qua URL; Tuần 3 đổi sang JWT.
class PersonalizationService {
  PersonalizationService._();

  static const Duration _timeout = Duration(seconds: 10);

  static int _requireUserId() {
    final user = AuthService.currentUser.value;
    if (user == null) {
      throw Exception('Bạn cần đăng nhập để dùng chức năng này');
    }
    return user.userId;
  }

  static Uri _uri(String path) => Uri.parse('${AuthService.baseUrl}$path');

  /// Đọc body dạng ApiResponse {success, message, data}; lỗi thì ném Exception.
  static dynamic _data(http.Response response) {
    Map<String, dynamic>? body;
    try {
      body = jsonDecode(utf8.decode(response.bodyBytes)) as Map<String, dynamic>;
    } catch (_) {}
    if (response.statusCode >= 400 || body == null || body['success'] != true) {
      throw Exception(body?['message'] ?? 'Lỗi máy chủ (HTTP ${response.statusCode})');
    }
    return body['data'];
  }

  static Future<List<FavoriteItem>> getFavorites() async {
    final userId = _requireUserId();
    final res = await http.get(_uri('/api/users/$userId/favorites')).timeout(_timeout);
    final list = _data(res) as List<dynamic>? ?? [];
    return list.map((e) => FavoriteItem.fromJson(e as Map<String, dynamic>)).toList();
  }

  static Future<bool> isFavorite(int artifactId) async {
    final userId = _requireUserId();
    final res = await http
        .get(_uri('/api/users/$userId/favorites/$artifactId/exists'))
        .timeout(_timeout);
    return _data(res) == true;
  }

  static Future<void> addFavorite(int artifactId) async {
    final userId = _requireUserId();
    final res = await http.put(_uri('/api/users/$userId/favorites/$artifactId')).timeout(_timeout);
    _data(res);
  }

  static Future<void> removeFavorite(int artifactId) async {
    final userId = _requireUserId();
    final res = await http.delete(_uri('/api/users/$userId/favorites/$artifactId')).timeout(_timeout);
    _data(res);
  }

  static Future<List<VisitItem>> getVisits() async {
    final userId = _requireUserId();
    final res = await http.get(_uri('/api/users/$userId/visits')).timeout(_timeout);
    final list = _data(res) as List<dynamic>? ?? [];
    return list.map((e) => VisitItem.fromJson(e as Map<String, dynamic>)).toList();
  }

  /// Ghi một lượt tham quan (Khoa gọi khi quét được iBeacon ở Tuần 3).
  static Future<void> recordVisit(int artifactId, {int durationSeconds = 0}) async {
    final userId = _requireUserId();
    final res = await http
        .post(
          _uri('/api/users/$userId/visits'),
          headers: {'Content-Type': 'application/json'},
          body: jsonEncode({'artifactId': artifactId, 'durationSeconds': durationSeconds}),
        )
        .timeout(_timeout);
    _data(res);
  }
}
