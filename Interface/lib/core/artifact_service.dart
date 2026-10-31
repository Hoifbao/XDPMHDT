import 'dart:convert';
import 'dart:io';

import 'package:http/http.dart' as http;
import 'package:shared_preferences/shared_preferences.dart';

import '../features/auth/auth_service.dart';

// ─────────────────────────────────────────────────────────────────────────────
//  MODELS  (map từ GET /api/artifacts/{id}?lang=vi|en)
// ─────────────────────────────────────────────────────────────────────────────

class ArtifactContentItem {
  final String languageCode;
  final String? title;
  final String? description;
  final String? audioUrl;
  final String? videoUrl;
  final List<String> imageUrls;
  final String? transcript;

  /// true khi server trả bản tiếng Việt thay cho ngôn ngữ yêu cầu
  /// (API: trường `fallback` ở data; cũng chấp nhận `fallbackUsed`).
  final bool fallbackUsed;

  const ArtifactContentItem({
    required this.languageCode,
    this.title,
    this.description,
    this.audioUrl,
    this.videoUrl,
    this.imageUrls = const [],
    this.transcript,
    this.fallbackUsed = false,
  });

  factory ArtifactContentItem.fromJson(
    Map<String, dynamic> json, {
    bool fallbackUsed = false,
  }) {
    return ArtifactContentItem(
      languageCode: json['languageCode']?.toString() ?? 'vi',
      title: _str(json['title']),
      description: _str(json['description']),
      audioUrl: _str(json['audioUrl']),
      videoUrl: _str(json['videoUrl']),
      imageUrls: _parseImageUrls(json['imageUrls']),
      transcript: _str(json['transcript']),
      fallbackUsed: fallbackUsed || json['fallbackUsed'] == true,
    );
  }

  String get languageLabel =>
      languageCode == 'vi' ? '🇻🇳 Tiếng Việt' : '🇬🇧 English';
}

class ArtifactDetail {
  final int id;
  final String name;
  final String? artifactCode;
  final String? thumbnailUrl;
  final String? period;
  final String? material;
  final String? dimensions;
  final String? beaconUuid;
  final String? locationName;
  final String status;
  final String? requestedLanguage;
  final bool fallback;

  /// Bản content đã chọn theo ?lang= (ưu tiên data.content từ API).
  final ArtifactContentItem? selectedContent;

  /// Tất cả bản ngôn ngữ (nếu API trả contents[]).
  final List<ArtifactContentItem> contents;

  /// URL ảnh từ media[] (IMAGE) — bổ sung khi content.imageUrls trống.
  final List<String> mediaImageUrls;
  final String? mediaAudioUrl;
  final String? mediaVideoUrl;

  const ArtifactDetail({
    required this.id,
    required this.name,
    this.artifactCode,
    this.thumbnailUrl,
    this.period,
    this.material,
    this.dimensions,
    this.beaconUuid,
    this.locationName,
    required this.status,
    this.requestedLanguage,
    this.fallback = false,
    this.selectedContent,
    required this.contents,
    this.mediaImageUrls = const [],
    this.mediaAudioUrl,
    this.mediaVideoUrl,
  });

  factory ArtifactDetail.fromJson(Map<String, dynamic> json) {
    final fallback = json['fallback'] == true || json['fallbackUsed'] == true;
    final contentsRaw = json['contents'] as List<dynamic>? ?? [];
    final contents = contentsRaw
        .whereType<Map<String, dynamic>>()
        .map((c) => ArtifactContentItem.fromJson(c))
        .toList();

    ArtifactContentItem? selected;
    final contentRaw = json['content'];
    if (contentRaw is Map<String, dynamic>) {
      selected = ArtifactContentItem.fromJson(
        contentRaw,
        fallbackUsed: fallback,
      );
    } else if (contents.isNotEmpty) {
      final requested = json['requestedLanguage']?.toString() ??
          json['languageCode']?.toString();
      ArtifactContentItem match = contents.first;
      for (final c in contents) {
        if (requested != null && c.languageCode == requested) {
          match = c;
          break;
        }
      }
      selected = ArtifactContentItem(
        languageCode: match.languageCode,
        title: match.title,
        description: match.description,
        audioUrl: match.audioUrl,
        videoUrl: match.videoUrl,
        imageUrls: match.imageUrls,
        transcript: match.transcript,
        fallbackUsed: fallback,
      );
    }

    final media = json['media'] as List<dynamic>? ?? [];
    final imageUrls = <String>[];
    String? audioUrl;
    String? videoUrl;
    for (final item in media.whereType<Map<String, dynamic>>()) {
      final type = item['mediaType']?.toString().toUpperCase();
      final url = _str(item['fileUrl']);
      if (url == null) continue;
      switch (type) {
        case 'IMAGE':
          imageUrls.add(url);
        case 'AUDIO':
          audioUrl ??= url;
        case 'VIDEO':
          videoUrl ??= url;
      }
    }

    return ArtifactDetail(
      id: (json['id'] as num).toInt(),
      name: _str(json['name']) ?? '',
      artifactCode: _str(json['artifactCode']),
      thumbnailUrl: _str(json['thumbnailUrl']),
      period: _str(json['period']),
      material: _str(json['material']),
      dimensions: _str(json['dimensions']),
      beaconUuid: _str(json['beaconUuid']),
      locationName: _str(json['locationName']),
      status: _str(json['status']) ?? 'ACTIVE',
      requestedLanguage: _str(json['requestedLanguage']),
      fallback: fallback,
      selectedContent: selected,
      contents: contents,
      mediaImageUrls: imageUrls,
      mediaAudioUrl: audioUrl,
      mediaVideoUrl: videoUrl,
    );
  }

  /// Nội dung theo lang: ưu tiên selectedContent (đã gắn fallback), rồi contents[].
  ArtifactContentItem? contentFor(String lang) {
    if (selectedContent != null) {
      // Khi API đã chọn bản theo ?lang=, dùng luôn (kể cả fallback).
      if (selectedContent!.languageCode == lang ||
          selectedContent!.fallbackUsed ||
          contents.length <= 1) {
        return _withMediaFallback(selectedContent!);
      }
    }
    for (final c in contents) {
      if (c.languageCode == lang) return _withMediaFallback(c);
    }
    if (selectedContent != null) return _withMediaFallback(selectedContent!);
    return contents.isNotEmpty ? _withMediaFallback(contents.first) : null;
  }

  ArtifactContentItem _withMediaFallback(ArtifactContentItem c) {
    final images = c.imageUrls.isNotEmpty ? c.imageUrls : mediaImageUrls;
    return ArtifactContentItem(
      languageCode: c.languageCode,
      title: c.title,
      description: c.description,
      audioUrl: c.audioUrl ?? mediaAudioUrl,
      videoUrl: c.videoUrl ?? mediaVideoUrl,
      imageUrls: images,
      transcript: c.transcript,
      fallbackUsed: c.fallbackUsed || fallback,
    );
  }
}

/// Kết quả tải hiện vật — phân biệt online / cache offline.
class ArtifactFetchResult {
  final ArtifactDetail artifact;
  final bool fromCache;

  const ArtifactFetchResult({
    required this.artifact,
    required this.fromCache,
  });
}

// ─────────────────────────────────────────────────────────────────────────────
//  SERVICE
// ─────────────────────────────────────────────────────────────────────────────

class ArtifactApiService {
  ArtifactApiService._();

  static const Duration _timeout = Duration(seconds: 10);
  static const int _maxCache = 20;
  static const String _cachePrefix = 'sh_artifact_';
  static const String _cacheKeysKey = 'sh_artifact_keys';

  /// Lấy chi tiết hiện vật theo ngôn ngữ.
  ///
  /// 1. GET /api/artifacts/{id}?lang={lang}
  /// 2. Lỗi mạng / HTTP → đọc cache SharedPreferences (tối đa 20 bài)
  /// 3. Không có cache → ném lỗi
  static Future<ArtifactFetchResult> fetchArtifact(int id, String lang) async {
    try {
      final uri = Uri.parse(
        '${AuthService.baseUrl}/api/artifacts/$id?lang=$lang',
      );
      final response = await http.get(uri).timeout(_timeout);

      if (response.statusCode == 200) {
        final bodyStr = utf8.decode(response.bodyBytes);
        final body = jsonDecode(bodyStr) as Map<String, dynamic>;
        final data = (body['data'] as Map<String, dynamic>?) ?? body;
        final artifact = ArtifactDetail.fromJson(data);
        await _saveCache(id, lang, bodyStr);
        return ArtifactFetchResult(artifact: artifact, fromCache: false);
      }

      final cached = await _loadCache(id, lang);
      if (cached != null) {
        return ArtifactFetchResult(artifact: cached, fromCache: true);
      }
      throw HttpException(
        'Không tải được hiện vật (mã ${response.statusCode}).',
      );
    } on SocketException {
      final cached = await _loadCache(id, lang);
      if (cached != null) {
        return ArtifactFetchResult(artifact: cached, fromCache: true);
      }
      throw const SocketException('Không có kết nối mạng và chưa có cache.');
    } catch (e) {
      if (e is SocketException || e is HttpException) rethrow;
      final cached = await _loadCache(id, lang);
      if (cached != null) {
        return ArtifactFetchResult(artifact: cached, fromCache: true);
      }
      rethrow;
    }
  }

  static Future<bool> hasCached(int id, [String lang = 'vi']) async {
    try {
      final prefs = await SharedPreferences.getInstance();
      return prefs.containsKey(_cacheKey(id, lang)) ||
          prefs.containsKey('$_cachePrefix$id');
    } catch (_) {
      return false;
    }
  }

  static String _cacheKey(int id, String lang) => '$_cachePrefix${id}_$lang';

  static Future<void> _saveCache(int id, String lang, String jsonStr) async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final key = _cacheKey(id, lang);
      await prefs.setString(key, jsonStr);
      // Giữ key cũ không có lang để tương thích
      await prefs.setString('$_cachePrefix$id', jsonStr);

      final keys = prefs.getStringList(_cacheKeysKey) ?? [];
      final idStr = '$id:$lang';
      keys.remove(idStr);
      keys.insert(0, idStr);
      while (keys.length > _maxCache) {
        final evicted = keys.removeLast();
        final parts = evicted.split(':');
        if (parts.length == 2) {
          await prefs.remove(_cacheKey(int.parse(parts[0]), parts[1]));
        }
        await prefs.remove('$_cachePrefix${parts.first}');
      }
      await prefs.setStringList(_cacheKeysKey, keys);
    } catch (_) {}
  }

  static Future<ArtifactDetail?> _loadCache(int id, String lang) async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final raw = prefs.getString(_cacheKey(id, lang)) ??
          prefs.getString('$_cachePrefix$id');
      if (raw == null) return null;
      final body = jsonDecode(raw) as Map<String, dynamic>;
      final data = (body['data'] as Map<String, dynamic>?) ?? body;
      return ArtifactDetail.fromJson(data);
    } catch (_) {
      return null;
    }
  }
}

// ─────────────────────────────────────────────────────────────────────────────
//  HELPERS
// ─────────────────────────────────────────────────────────────────────────────

List<String> _parseImageUrls(dynamic raw) {
  if (raw is String && raw.isNotEmpty) {
    try {
      final decoded = jsonDecode(raw);
      if (decoded is List) {
        return decoded.whereType<String>().where((s) => s.isNotEmpty).toList();
      }
    } catch (_) {}
  } else if (raw is List) {
    return raw.whereType<String>().where((s) => s.isNotEmpty).toList();
  }
  return const [];
}

String? _str(dynamic v) {
  if (v == null) return null;
  final s = v.toString().trim();
  return s.isEmpty ? null : s;
}
