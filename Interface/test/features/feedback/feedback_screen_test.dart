import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:smartheritage/features/auth/auth_service.dart';
import 'package:smartheritage/features/auth/login_screen.dart';
import 'package:smartheritage/features/feedback/feedback_screen.dart';

const _visitor = AuthUser(
  userId: 4,
  username: 'visitor_john',
  email: 'john@example.com',
  fullName: 'John Smith',
  role: 'VISITOR',
);

http.Response _json(int status, Object body) => http.Response(
      jsonEncode(body),
      status,
      headers: {'content-type': 'application/json; charset=utf-8'},
    );

/// API giả lập cho hiện vật 2: 1 đánh giá đã duyệt (4★). [submitStatus]: mã trả về khi POST.
MockClient _api({int submitStatus = 201, int summaryStatus = 200, List<http.Request>? posts}) {
  return MockClient((request) async {
    if (request.method == 'POST') {
      posts?.add(request);
      return _json(submitStatus, {
        'success': true,
        'data': {'id': 9, 'rating': 4, 'reviewerName': 'John Smith', 'status': 'PENDING'},
      });
    }
    if (request.url.path.endsWith('/rating-summary')) {
      if (summaryStatus != 200) return _json(summaryStatus, {'success': false, 'message': 'Not found'});
      return _json(200, {
        'success': true,
        'data': {'artifactId': 2, 'averageRating': 4.0, 'totalRatings': 1, 'distribution': {'5': 0, '4': 1}},
      });
    }
    return _json(200, {
      'success': true,
      'data': [
        {'id': 2, 'rating': 4, 'comment': 'Thông tin về bia Tiến sĩ hay', 'reviewerName': 'Nguyễn Văn An',
         'status': 'APPROVED', 'createdAt': '2026-10-05T09:45:00'},
      ],
    });
  });
}

Future<void> _open(WidgetTester tester) async {
  tester.view.physicalSize = const Size(900, 2600);
  tester.view.devicePixelRatio = 1.0;
  addTearDown(tester.view.reset);
  await tester.pumpWidget(const MaterialApp(
    home: FeedbackScreen(artifactId: 2, artifactName: 'Bia Tiến sĩ'),
  ));
  await tester.pumpAndSettle();
}

void main() {
  tearDown(() => AuthService.currentUser.value = null);

  testWidgets('chưa đăng nhập: xem điểm TB + bình luận, form thay bằng nút đăng nhập, không gửi POST',
      (tester) async {
    final posts = <http.Request>[];
    await http.runWithClient(() async {
      await _open(tester);
      expect(find.text('4.0'), findsOneWidget);
      expect(find.text('1 đánh giá đã duyệt'), findsOneWidget);
      expect(find.text('Thông tin về bia Tiến sĩ hay'), findsOneWidget);
      expect(find.text('Đăng nhập để đánh giá'), findsOneWidget);
      expect(find.text('Gửi đánh giá'), findsNothing);

      await tester.tap(find.text('Đăng nhập để đánh giá'));
      await tester.pumpAndSettle();
      expect(find.byType(LoginScreen), findsOneWidget);
    }, () => _api(posts: posts));
    expect(posts, isEmpty);
  });

  testWidgets('đã đăng nhập: chọn 4 sao, nhập bình luận, gửi → 201 "Đã gửi đánh giá"', (tester) async {
    AuthService.currentUser.value = _visitor;
    final posts = <http.Request>[];
    await http.runWithClient(() async {
      await _open(tester);
      await tester.tap(find.byTooltip('4 sao'));
      await tester.enterText(find.byType(TextField), 'Thuyết minh rõ ràng');
      await tester.tap(find.text('Gửi đánh giá'));
      await tester.pumpAndSettle();
      expect(find.text('Đã gửi đánh giá'), findsOneWidget);
      expect(find.text('Đánh giá đang chờ duyệt'), findsOneWidget);
      expect(find.text('Xem đánh giá của tôi'), findsOneWidget);
    }, () => _api(posts: posts));
    expect(posts, hasLength(1));
    expect(posts.single.url.path, '/api/artifacts/2/feedbacks');
    expect(jsonDecode(posts.single.body), {'rating': 4, 'comment': 'Thuyết minh rõ ràng'});
  });

  testWidgets('gửi lại (server trả 200) → "Đã cập nhật đánh giá"', (tester) async {
    AuthService.currentUser.value = _visitor;
    await http.runWithClient(() async {
      await _open(tester);
      await tester.tap(find.byTooltip('2 sao'));
      await tester.tap(find.text('Gửi đánh giá'));
      await tester.pumpAndSettle();
      expect(find.text('Đã cập nhật đánh giá'), findsOneWidget);
    }, () => _api(submitStatus: 200));
  });

  testWidgets('chưa chọn sao → báo lỗi, không gửi', (tester) async {
    AuthService.currentUser.value = _visitor;
    final posts = <http.Request>[];
    await http.runWithClient(() async {
      await _open(tester);
      await tester.tap(find.text('Gửi đánh giá'));
      await tester.pumpAndSettle();
      expect(find.text('Vui lòng chọn số sao trước khi gửi.'), findsOneWidget);
    }, () => _api(posts: posts));
    expect(posts, isEmpty);
  });

  testWidgets('ô bình luận giới hạn 500 ký tự', (tester) async {
    AuthService.currentUser.value = _visitor;
    await http.runWithClient(() async {
      await _open(tester);
      await tester.enterText(find.byType(TextField), 'a' * 600);
      await tester.pump();
      final field = tester.widget<TextField>(find.byType(TextField));
      expect(field.controller!.text.length, 500);
    }, () => _api());
  });

  testWidgets('rating-summary lỗi (404) → vẫn hiện điểm TB tự tính từ bình luận đã duyệt', (tester) async {
    await http.runWithClient(() async {
      await _open(tester);
      expect(find.text('4.0'), findsOneWidget);
    }, () => _api(summaryStatus: 404));
  });
}
