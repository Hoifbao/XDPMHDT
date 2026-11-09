import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:smartheritage/features/auth/auth_service.dart';
import 'package:smartheritage/features/feedback/feedback_screen.dart';
import 'package:smartheritage/features/feedback/my_feedbacks_screen.dart';

const _visitor = AuthUser(
  userId: 3,
  username: 'visitor_mai',
  email: 'mai@example.com',
  fullName: 'Trần Thị Mai',
  role: 'VISITOR',
);

http.Response _json(int status, Object body) => http.Response(
      jsonEncode(body),
      status,
      headers: {'content-type': 'application/json; charset=utf-8'},
    );

MockClient _api(List<Map<String, Object?>> mine, {int status = 200}) {
  return MockClient((request) async {
    if (request.url.path == '/api/artifacts/batch') {
      return _json(200, {
        'success': true,
        'data': [
          {'id': 1, 'name': 'Khuê Văn Các'},
          {'id': 2, 'name': 'Bia Tiến sĩ'},
          {'id': 3, 'name': 'Đoan Môn'},
        ],
      });
    }
    if (request.url.path == '/api/feedbacks/me') {
      if (status != 200) return _json(status, {'success': false, 'message': 'Lỗi máy chủ'});
      return _json(200, {'success': true, 'data': mine});
    }
    return _json(200, {'success': true, 'data': []});
  });
}

Future<void> _open(WidgetTester tester) async {
  await tester.pumpWidget(const MaterialApp(home: MyFeedbacksScreen()));
  await tester.pumpAndSettle();
}

void main() {
  tearDown(() => AuthService.currentUser.value = null);

  testWidgets('hiện tên hiện vật, bình luận và nhãn của từng trạng thái', (tester) async {
    AuthService.currentUser.value = _visitor;
    await http.runWithClient(() async {
      await _open(tester);
      expect(find.text('Khuê Văn Các'), findsOneWidget);
      expect(find.text('Tín hiệu hơi chậm'), findsOneWidget);
      expect(find.text('Chờ duyệt'), findsOneWidget);
      expect(find.text('Đã duyệt'), findsOneWidget);
      expect(find.text('Không được duyệt'), findsOneWidget);
    }, () => _api([
          {'id': 3, 'artifactId': 1, 'rating': 3, 'comment': 'Tín hiệu hơi chậm', 'status': 'PENDING'},
          {'id': 5, 'artifactId': 2, 'rating': 2, 'status': 'APPROVED'},
          {'id': 6, 'artifactId': 3, 'rating': 1, 'status': 'REJECTED'},
        ]));
  });

  testWidgets('bấm một dòng thì mở màn đánh giá của hiện vật đó', (tester) async {
    AuthService.currentUser.value = _visitor;
    await http.runWithClient(() async {
      await _open(tester);
      await tester.tap(find.text('Bia Tiến sĩ'));
      await tester.pumpAndSettle();
      final screen = tester.widget<FeedbackScreen>(find.byType(FeedbackScreen));
      expect(screen.artifactId, 2);
      expect(screen.artifactName, 'Bia Tiến sĩ');
    }, () => _api([
          {'id': 5, 'artifactId': 2, 'rating': 2, 'status': 'PENDING'},
        ]));
  });

  testWidgets('chưa có đánh giá thì hiện hướng dẫn', (tester) async {
    AuthService.currentUser.value = _visitor;
    await http.runWithClient(() async {
      await _open(tester);
      expect(find.textContaining('Bạn chưa có đánh giá nào'), findsOneWidget);
    }, () => _api([]));
  });

  testWidgets('máy chủ lỗi thì hiện thông báo và nút Thử lại', (tester) async {
    AuthService.currentUser.value = _visitor;
    await http.runWithClient(() async {
      await _open(tester);
      expect(find.text('Lỗi máy chủ'), findsOneWidget);
      expect(find.text('Thử lại'), findsOneWidget);
    }, () => _api([], status: 500));
  });

  testWidgets('chưa đăng nhập thì yêu cầu đăng nhập', (tester) async {
    await http.runWithClient(() async {
      await _open(tester);
      expect(find.text('Bạn cần đăng nhập để xem đánh giá của mình.'), findsOneWidget);
    }, () => _api([]));
  });
}
