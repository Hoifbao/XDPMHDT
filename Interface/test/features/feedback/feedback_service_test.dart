import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:smartheritage/features/auth/auth_service.dart';
import 'package:smartheritage/features/feedback/feedback_service.dart';

http.Response _json(int status, Object body) => http.Response(
      jsonEncode(body),
      status,
      headers: {'content-type': 'application/json; charset=utf-8'},
    );

const _visitor = AuthUser(
  userId: 3,
  username: 'visitor_mai',
  email: 'mai@example.com',
  fullName: 'Trần Thị Mai',
  role: 'VISITOR',
);

void main() {
  tearDown(() => AuthService.currentUser.value = null);

  group('RatingSummary', () {
    test('fromJson đọc distribution có khóa dạng chuỗi', () {
      final s = RatingSummary.fromJson({
        'averageRating': 4.7,
        'totalRatings': 3,
        'distribution': {'5': 2, '4': 1, '3': 0, '2': 0, '1': 0},
      });
      expect(s.averageRating, 4.7);
      expect(s.totalRatings, 3);
      expect(s.distribution[5], 2);
      expect(s.distribution[1], 0);
    });

    test('fromItems tự tính, làm tròn 1 chữ số; rỗng thì average null', () {
      const items = [
        FeedbackItem(id: 1, rating: 5, reviewerName: 'A', status: 'APPROVED'),
        FeedbackItem(id: 2, rating: 5, reviewerName: 'B', status: 'APPROVED'),
        FeedbackItem(id: 3, rating: 4, reviewerName: 'C', status: 'APPROVED'),
      ];
      expect(RatingSummary.fromItems(items).averageRating, 4.7);
      expect(RatingSummary.fromItems(const []).averageRating, isNull);
    });
  });

  group('loadOverview', () {
    test('đọc danh sách đã duyệt + rating-summary', () async {
      final overview = await http.runWithClient(
        () => FeedbackService.loadOverview(1),
        () => MockClient((request) async {
          if (request.url.path.endsWith('/rating-summary')) {
            return _json(200, {
              'success': true,
              'data': {'artifactId': 1, 'averageRating': 5.0, 'totalRatings': 1, 'distribution': {'5': 1}},
            });
          }
          return _json(200, {
            'success': true,
            'data': [
              {'id': 1, 'artifactId': 1, 'rating': 5, 'comment': 'Rất đẹp', 'reviewerName': 'Nguyễn Văn An',
               'status': 'APPROVED', 'createdAt': '2026-10-05T09:30:00'},
            ],
          });
        }),
      );
      expect(overview.approved, hasLength(1));
      expect(overview.approved.first.reviewerName, 'Nguyễn Văn An');
      expect(overview.summary.averageRating, 5.0);
    });

    test('rating-summary lỗi (Gateway chưa có route) thì tự tính từ danh sách', () async {
      final overview = await http.runWithClient(
        () => FeedbackService.loadOverview(2),
        () => MockClient((request) async {
          if (request.url.path.endsWith('/rating-summary')) {
            return _json(404, {'success': false, 'message': 'Không tìm thấy'});
          }
          return _json(200, {
            'success': true,
            'data': [
              {'id': 2, 'rating': 4, 'reviewerName': 'Nguyễn Văn An', 'status': 'APPROVED'},
            ],
          });
        }),
      );
      expect(overview.summary.averageRating, 4.0);
      expect(overview.summary.totalRatings, 1);
    });

    test('danh sách lỗi thì ném FeedbackException kèm thông báo của server', () async {
      expect(
        () => http.runWithClient(
          () => FeedbackService.loadOverview(99),
          () => MockClient((_) async => _json(404, {'success': false, 'message': 'Không tìm thấy hiện vật'})),
        ),
        throwsA(isA<FeedbackException>()
            .having((e) => e.message, 'message', 'Không tìm thấy hiện vật')),
      );
    });
  });

  group('loadMine', () {
    test('đọc mọi trạng thái và ghép tên hiện vật từ /api/artifacts/batch', () async {
      final batchQueries = <String?>[];
      final mine = await http.runWithClient(
        FeedbackService.loadMine,
        () => MockClient((request) async {
          if (request.url.path == '/api/artifacts/batch') {
            batchQueries.add(request.url.queryParameters['ids']);
            return _json(200, {
              'success': true,
              'data': [
                {'id': 1, 'name': 'Khuê Văn Các'},
              ],
            });
          }
          expect(request.url.path, '/api/feedbacks/me');
          return _json(200, {
            'success': true,
            'data': [
              {'id': 3, 'artifactId': 1, 'rating': 3, 'reviewerName': 'Trần Thị Mai', 'status': 'PENDING'},
              {'id': 5, 'artifactId': 2, 'rating': 2, 'reviewerName': 'Trần Thị Mai', 'status': 'REJECTED'},
            ],
          });
        }),
      );
      expect(batchQueries, ['1,2']);
      expect(mine.map((m) => m.item.status), ['PENDING', 'REJECTED']);
      expect(mine.map((m) => m.artifactName), ['Khuê Văn Các', 'Hiện vật #2']);
    });

    test('không lấy được tên hiện vật thì vẫn trả danh sách', () async {
      final mine = await http.runWithClient(
        FeedbackService.loadMine,
        () => MockClient((request) async {
          if (request.url.path == '/api/artifacts/batch') return _json(503, {'success': false});
          return _json(200, {
            'success': true,
            'data': [
              {'id': 3, 'artifactId': 1, 'rating': 3, 'status': 'PENDING'},
            ],
          });
        }),
      );
      expect(mine.single.artifactName, 'Hiện vật #1');
    });

    test('danh sách rỗng thì không gọi batch', () async {
      final paths = <String>[];
      final mine = await http.runWithClient(
        FeedbackService.loadMine,
        () => MockClient((request) async {
          paths.add(request.url.path);
          return _json(200, {'success': true, 'data': []});
        }),
      );
      expect(mine, isEmpty);
      expect(paths, ['/api/feedbacks/me']);
    });

    test('401 thì báo phiên hết hạn', () async {
      expect(
        () => http.runWithClient(
          FeedbackService.loadMine,
          () => MockClient((_) async => _json(401, {'success': false})),
        ),
        throwsA(isA<FeedbackException>().having((e) => e.message, 'message', contains('hết hạn'))),
      );
    });
  });

  group('submit', () {
    test('chưa đăng nhập: không gọi API, yêu cầu đăng nhập', () async {
      var called = false;
      final result = await http.runWithClient(
        () => FeedbackService.submit(1, 5, 'Hay'),
        () => MockClient((_) async {
          called = true;
          return _json(201, {});
        }),
      );
      expect(result.success, isFalse);
      expect(result.needLogin, isTrue);
      expect(called, isFalse);
    });

    test('201: gửi mới; body chỉ có rating + comment đã trim', () async {
      AuthService.currentUser.value = _visitor;
      late Map<String, dynamic> sent;
      final result = await http.runWithClient(
        () => FeedbackService.submit(2, 4, '  Rất hay  '),
        () => MockClient((request) async {
          expect(request.method, 'POST');
          expect(request.url.path, '/api/artifacts/2/feedbacks');
          sent = jsonDecode(request.body) as Map<String, dynamic>;
          return _json(201, {
            'success': true,
            'data': {'id': 7, 'rating': 4, 'comment': 'Rất hay', 'reviewerName': 'Trần Thị Mai', 'status': 'PENDING'},
          });
        }),
      );
      expect(sent, {'rating': 4, 'comment': 'Rất hay'});
      expect(result.success, isTrue);
      expect(result.updated, isFalse);
      expect(result.feedback?.status, 'PENDING');
    });

    test('200: đã có đánh giá, server ghi đè -> updated = true', () async {
      AuthService.currentUser.value = _visitor;
      final result = await http.runWithClient(
        () => FeedbackService.submit(2, 2, ''),
        () => MockClient((_) async => _json(200, {
              'success': true,
              'data': {'id': 5, 'rating': 2, 'reviewerName': 'Trần Thị Mai', 'status': 'PENDING'},
            })),
      );
      expect(result.success, isTrue);
      expect(result.updated, isTrue);
    });

    test('401: phiên hết hạn -> yêu cầu đăng nhập lại', () async {
      AuthService.currentUser.value = _visitor;
      final result = await http.runWithClient(
        () => FeedbackService.submit(1, 5, ''),
        () => MockClient((_) async => _json(401, {'success': false, 'message': 'Unauthorized'})),
      );
      expect(result.needLogin, isTrue);
    });

    test('400: hiện thông báo của server', () async {
      AuthService.currentUser.value = _visitor;
      final result = await http.runWithClient(
        () => FeedbackService.submit(4, 3, ''),
        () => MockClient((_) async =>
            _json(400, {'success': false, 'message': 'Hiện vật ID 4 không hoạt động, không thể gửi đánh giá'})),
      );
      expect(result.success, isFalse);
      expect(result.message, contains('không hoạt động'));
    });

    test('kiểm tra trước khi gửi: thiếu sao hoặc bình luận > 500 ký tự', () async {
      AuthService.currentUser.value = _visitor;
      expect((await FeedbackService.submit(1, 0, '')).success, isFalse);
      expect((await FeedbackService.submit(1, 5, 'a' * 501)).message, contains('500'));
    });
  });
}
