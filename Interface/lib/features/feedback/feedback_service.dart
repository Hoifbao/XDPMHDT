import 'dart:async';
import 'dart:convert';

import 'package:http/http.dart' as http;

import '../auth/auth_service.dart';

/// Một đánh giá đã duyệt (GET /api/artifacts/{id}/feedbacks).
class FeedbackItem {
  final int id;
  final int rating;
  final String? comment;
  final String reviewerName;
  final String status;
  final DateTime? createdAt;

  const FeedbackItem({
    required this.id,
    required this.rating,
    this.comment,
    required this.reviewerName,
    required this.status,
    this.createdAt,
  });

  factory FeedbackItem.fromJson(Map<String, dynamic> json) {
    final comment = json['comment']?.toString().trim();
    return FeedbackItem(
      id: (json['id'] as num?)?.toInt() ?? 0,
      rating: (json['rating'] as num?)?.toInt() ?? 0,
      comment: comment == null || comment.isEmpty ? null : comment,
      reviewerName: json['reviewerName']?.toString() ?? 'Khách',
      status: json['status']?.toString() ?? 'PENDING',
      createdAt: DateTime.tryParse(json['createdAt']?.toString() ?? ''),
    );
  }
}

/// Điểm trung bình và số lượt theo mức sao, chỉ tính đánh giá APPROVED.
class RatingSummary {
  final double? averageRating;
  final int totalRatings;

  /// Số sao (5..1) -> số lượt
  final Map<int, int> distribution;

  const RatingSummary({
    this.averageRating,
    required this.totalRatings,
    required this.distribution,
  });

  factory RatingSummary.fromJson(Map<String, dynamic> json) {
    final distribution = <int, int>{for (var star = 5; star >= 1; star--) star: 0};
    final raw = json['distribution'];
    if (raw is Map) {
      raw.forEach((key, value) {
        final star = int.tryParse(key.toString());
        if (star != null && distribution.containsKey(star) && value is num) {
          distribution[star] = value.toInt();
        }
      });
    }
    return RatingSummary(
      averageRating: (json['averageRating'] as num?)?.toDouble(),
      totalRatings: (json['totalRatings'] as num?)?.toInt() ?? 0,
      distribution: distribution,
    );
  }

  /// Tính từ danh sách đánh giá đã duyệt (dự phòng khi API rating-summary chưa gọi được).
  factory RatingSummary.fromItems(List<FeedbackItem> items) {
    final distribution = <int, int>{for (var star = 5; star >= 1; star--) star: 0};
    var sum = 0;
    for (final item in items) {
      if (distribution.containsKey(item.rating)) {
        distribution[item.rating] = distribution[item.rating]! + 1;
        sum += item.rating;
      }
    }
    final total = distribution.values.fold<int>(0, (a, b) => a + b);
    return RatingSummary(
      averageRating: total == 0 ? null : (sum / total * 10).roundToDouble() / 10,
      totalRatings: total,
      distribution: distribution,
    );
  }
}

/// Kết quả tải dữ liệu công khai của khu vực đánh giá.
class FeedbackOverview {
  final RatingSummary summary;
  final List<FeedbackItem> approved;
  final String? warning;

  const FeedbackOverview({required this.summary, required this.approved, this.warning});
}

/// Kết quả gửi đánh giá.
class FeedbackSubmitResult {
  final bool success;

  /// true: đã có đánh giá trước đó, server ghi đè (HTTP 200) và chờ duyệt lại
  final bool updated;

  /// true: chưa đăng nhập hoặc phiên hết hạn (HTTP 401)
  final bool needLogin;
  final String message;
  final FeedbackItem? feedback;

  const FeedbackSubmitResult({
    required this.success,
    this.updated = false,
    this.needLogin = false,
    required this.message,
    this.feedback,
  });
}

/// Gọi feedback-service qua Gateway (Tường - Sprint 5).
///   GET  /api/artifacts/{id}/rating-summary  – công khai
///   GET  /api/artifacts/{id}/feedbacks       – công khai, chỉ đánh giá đã duyệt
///   POST /api/artifacts/{id}/feedbacks       – cần đăng nhập; 201 tạo mới, 200 ghi đè đánh giá cũ
/// Token được AuthHttpClient (main.dart) tự gắn cho mọi lệnh http.*; không gửi userId trong body.
class FeedbackService {
  FeedbackService._();

  static const int maxRating = 5;
  static const int maxCommentLength = 500;
  static const Duration _timeout = Duration(seconds: 8);

  static String get _baseUrl => AuthService.baseUrl;

  static Map<String, String> _headers() => const {
        'Accept': 'application/json',
        'Content-Type': 'application/json; charset=UTF-8',
      };

  static dynamic _decode(http.Response response) {
    try {
      return jsonDecode(utf8.decode(response.bodyBytes));
    } catch (_) {
      return null;
    }
  }

  static String _message(http.Response response, String fallback) {
    final body = _decode(response);
    if (body is Map && body['message'] != null) return body['message'].toString();
    return '$fallback (mã ${response.statusCode}).';
  }

  static String _connectionError() =>
      'Không kết nối được máy chủ ($_baseUrl). Hãy kiểm tra mạng hoặc Backend đã chạy chưa.';

  /// Tải điểm trung bình + bình luận đã duyệt. Danh sách lỗi thì báo lỗi;
  /// riêng rating-summary lỗi thì tự tính từ danh sách (cùng quy tắc: chỉ APPROVED).
  static Future<FeedbackOverview> loadOverview(int artifactId) async {
    final List<FeedbackItem> approved;
    try {
      final response = await http
          .get(Uri.parse('$_baseUrl/api/artifacts/$artifactId/feedbacks'), headers: _headers())
          .timeout(_timeout);
      if (response.statusCode != 200) {
        throw FeedbackException(_message(response, 'Không tải được đánh giá'));
      }
      final body = _decode(response);
      final data = body is Map ? body['data'] : null;
      approved = data is List
          ? data.whereType<Map<String, dynamic>>().map(FeedbackItem.fromJson).toList()
          : <FeedbackItem>[];
    } on FeedbackException {
      rethrow;
    } catch (_) {
      throw FeedbackException(_connectionError());
    }

    try {
      final response = await http
          .get(Uri.parse('$_baseUrl/api/artifacts/$artifactId/rating-summary'), headers: _headers())
          .timeout(_timeout);
      final body = _decode(response);
      final data = body is Map ? body['data'] : null;
      if (response.statusCode == 200 && data is Map<String, dynamic>) {
        return FeedbackOverview(summary: RatingSummary.fromJson(data), approved: approved);
      }
    } catch (_) {
      // Dùng phương án tự tính bên dưới
    }
    return FeedbackOverview(summary: RatingSummary.fromItems(approved), approved: approved);
  }

  /// Gửi (hoặc sửa) đánh giá. Kiểm tra số sao 1-5 và bình luận ≤ 500 ký tự trước khi gửi.
  static Future<FeedbackSubmitResult> submit(int artifactId, int rating, String comment) async {
    final trimmed = comment.trim();
    if (rating < 1 || rating > maxRating) {
      return const FeedbackSubmitResult(success: false, message: 'Vui lòng chọn từ 1 đến 5 sao.');
    }
    if (trimmed.length > maxCommentLength) {
      return const FeedbackSubmitResult(
          success: false, message: 'Bình luận tối đa $maxCommentLength ký tự.');
    }
    if (!AuthService.isLoggedIn) {
      return const FeedbackSubmitResult(
          success: false, needLogin: true, message: 'Bạn cần đăng nhập để gửi đánh giá.');
    }

    final http.Response response;
    try {
      response = await http
          .post(
            Uri.parse('$_baseUrl/api/artifacts/$artifactId/feedbacks'),
            headers: _headers(),
            body: jsonEncode({'rating': rating, if (trimmed.isNotEmpty) 'comment': trimmed}),
          )
          .timeout(_timeout);
    } on TimeoutException {
      return FeedbackSubmitResult(success: false, message: _connectionError());
    } catch (_) {
      return FeedbackSubmitResult(success: false, message: _connectionError());
    }

    if (response.statusCode == 200 || response.statusCode == 201) {
      final body = _decode(response);
      final data = body is Map ? body['data'] : null;
      final updated = response.statusCode == 200;
      return FeedbackSubmitResult(
        success: true,
        updated: updated,
        message: updated
            ? 'Đã cập nhật đánh giá của bạn. Đánh giá sẽ hiển thị lại sau khi được duyệt.'
            : 'Đã gửi đánh giá. Đánh giá sẽ hiển thị sau khi được duyệt.',
        feedback: data is Map<String, dynamic> ? FeedbackItem.fromJson(data) : null,
      );
    }
    if (response.statusCode == 401) {
      return const FeedbackSubmitResult(
          success: false, needLogin: true, message: 'Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại.');
    }
    if (response.statusCode == 403) {
      return const FeedbackSubmitResult(
          success: false, message: 'Tài khoản của bạn không có quyền gửi đánh giá.');
    }
    return FeedbackSubmitResult(success: false, message: _message(response, 'Gửi đánh giá thất bại'));
  }
}

class FeedbackException implements Exception {
  final String message;
  const FeedbackException(this.message);

  @override
  String toString() => message;
}
