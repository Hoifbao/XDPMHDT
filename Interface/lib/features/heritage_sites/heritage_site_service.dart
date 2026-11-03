import 'dart:convert';

import 'package:http/http.dart' as http;

import '../auth/auth_service.dart';

// ─────────────────────────────────────────────────────────────────────────────
//  MODEL
// ─────────────────────────────────────────────────────────────────────────────

/// Di tích trả về từ GET /api/heritage-sites/nearby hoặc GET /api/heritage-sites.
/// Khớp với NearbyHeritageSiteResponse.java (site-service).
class HeritageSiteItem {
  final int id;
  final String name;
  final String? location;
  final String? mapImageUrl;
  final double? latitude;
  final double? longitude;

  /// Khoảng cách Haversine (km); chỉ có khi gọi /nearby, null khi gọi /all.
  final double? distanceKm;

  const HeritageSiteItem({
    required this.id,
    required this.name,
    this.location,
    this.mapImageUrl,
    this.latitude,
    this.longitude,
    this.distanceKm,
  });

  factory HeritageSiteItem.fromJson(Map<String, dynamic> json) {
    return HeritageSiteItem(
      id: (json['id'] as num).toInt(),
      name: json['name']?.toString() ?? 'Di tích #${json['id']}',
      location: json['location']?.toString(),
      mapImageUrl: json['mapImageUrl']?.toString(),
      latitude: (json['latitude'] as num?)?.toDouble(),
      longitude: (json['longitude'] as num?)?.toDouble(),
      distanceKm: (json['distanceKm'] as num?)?.toDouble(),
    );
  }

  bool get hasCoords => latitude != null && longitude != null;
}

// ─────────────────────────────────────────────────────────────────────────────
//  SERVICE
// ─────────────────────────────────────────────────────────────────────────────

/// Gọi site-service qua Gateway (port 8080). Cả 2 endpoint đều công khai.
class HeritageSiteService {
  HeritageSiteService._();

  static const Duration _timeout = Duration(seconds: 10);

  /// GET /api/heritage-sites/nearby?lat=&lng=&radius=
  static Future<List<HeritageSiteItem>> getNearby({
    required double lat,
    required double lng,
    double radius = 50,
  }) async {
    final uri = Uri.parse('${AuthService.baseUrl}/api/heritage-sites/nearby')
        .replace(queryParameters: {
      'lat': lat.toString(),
      'lng': lng.toString(),
      'radius': radius.toString(),
    });
    final response = await http.get(uri).timeout(_timeout);
    final body =
        jsonDecode(utf8.decode(response.bodyBytes)) as Map<String, dynamic>;
    if (response.statusCode != 200) {
      throw Exception(
          body['message'] ?? 'Không tải được di tích (${response.statusCode})');
    }
    final data = body['data'] as List<dynamic>;
    return data
        .map((e) => HeritageSiteItem.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  /// GET /api/heritage-sites – toàn bộ danh sách (dùng khi chưa có GPS).
  static Future<List<HeritageSiteItem>> getAll() async {
    final uri = Uri.parse('${AuthService.baseUrl}/api/heritage-sites');
    final response = await http.get(uri).timeout(_timeout);
    final body = jsonDecode(utf8.decode(response.bodyBytes));
    final List<dynamic> data =
        body is List ? body : (body['data'] as List<dynamic>);
    return data
        .map((e) => HeritageSiteItem.fromJson(e as Map<String, dynamic>))
        .toList();
  }
}
