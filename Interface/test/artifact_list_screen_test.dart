import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:smartheritage/features/auth/auth_service.dart';
import 'package:smartheritage/screens/artifact_list_screen.dart';

// Bảo W8: màn "Hiện Vật Di Sản" đọc GET /api/artifacts thay cho dữ liệu mẫu gõ cứng.

Map<String, dynamic> _item(int id, String name, String site,
        {String status = 'ACTIVE'}) =>
    {
      'id': id,
      'siteId': site == 'Văn Miếu - Quốc Tử Giám' ? 1 : 2,
      'locationName': site,
      'name': name,
      'artifactCode': 'A-$id',
      'thumbnailUrl': null,
      'period': 'Thế kỷ XV',
      'material': 'Đá',
      'status': status,
    };

http.Response _ok(List<Map<String, dynamic>> items) => http.Response.bytes(
      utf8.encode(jsonEncode({'success': true, 'message': 'OK', 'data': items})),
      200,
      headers: {'content-type': 'application/json; charset=utf-8'},
    );

final _sample = [
  _item(1, 'Khuê Văn Các', 'Văn Miếu - Quốc Tử Giám'),
  _item(2, 'Bia Tiến sĩ', 'Văn Miếu - Quốc Tử Giám'),
  _item(3, 'Đoan Môn', 'Hoàng thành Thăng Long'),
  _item(9, 'Hiện vật đã ẩn', 'Hoàng thành Thăng Long', status: 'INACTIVE'),
];

// Màn hình test mặc định 800x600 chỉ dựng vài ô đầu của GridView; mở rộng để thấy đủ hiện vật.
Future<void> _bigScreen(WidgetTester tester) async {
  await tester.binding.setSurfaceSize(const Size(800, 2400));
  addTearDown(() => tester.binding.setSurfaceSize(null));
}

void main() {
  testWidgets('hiện hiện vật thật từ API, bỏ hiện vật INACTIVE, không còn dữ liệu mẫu',
      (tester) async {
    final paths = <String>[];
    AuthService.rawClient = MockClient((request) async {
      paths.add(request.url.path);
      return _ok(_sample);
    });

    await _bigScreen(tester);
    await tester.pumpWidget(const MaterialApp(home: ArtifactListScreen()));
    await tester.pumpAndSettle();

    expect(paths, contains('/api/artifacts'));
    expect(find.text('3 hiện vật'), findsOneWidget);
    expect(find.text('Khuê Văn Các'), findsOneWidget);
    expect(find.text('Đoan Môn'), findsOneWidget);
    expect(find.text('Hiện vật đã ẩn'), findsNothing);
    expect(find.text('Trống đồng Đông Sơn'), findsNothing);
    expect(find.text('Thêm hiện vật'), findsNothing);
    expect(tester.takeException(), isNull);
  });

  testWidgets('lỗi mạng thì báo lỗi và Thử lại tải được', (tester) async {
    var fail = true;
    AuthService.rawClient = MockClient((_) async {
      if (fail) return http.Response('error', 503);
      return _ok(_sample);
    });

    await _bigScreen(tester);
    await tester.pumpWidget(const MaterialApp(home: ArtifactListScreen()));
    await tester.pumpAndSettle();

    expect(find.textContaining('Không tải được danh sách hiện vật'), findsOneWidget);
    expect(find.text('Khuê Văn Các'), findsNothing);

    fail = false;
    await tester.tap(find.text('Thử lại'));
    await tester.pumpAndSettle();

    expect(find.text('Khuê Văn Các'), findsOneWidget);
    expect(find.text('3 hiện vật'), findsOneWidget);
  });

  testWidgets('tìm theo tên và lọc theo di tích lấy từ dữ liệu thật', (tester) async {
    AuthService.rawClient = MockClient((_) async => _ok(_sample));

    await _bigScreen(tester);
    await tester.pumpWidget(const MaterialApp(home: ArtifactListScreen()));
    await tester.pumpAndSettle();

    await tester.enterText(find.byType(TextField), 'bia');
    await tester.pumpAndSettle();
    expect(find.text('1 hiện vật'), findsOneWidget);
    expect(find.text('Bia Tiến sĩ'), findsOneWidget);

    await tester.enterText(find.byType(TextField), '');
    await tester.pumpAndSettle();

    await tester.tap(find.byTooltip('Bộ lọc'));
    await tester.pumpAndSettle();
    // Chip di tích lấy từ locationName của API, không còn "Huế", "Hội An"...
    expect(find.text('Huế'), findsNothing);
    await tester.tap(find.widgetWithText(FilterChip, 'Hoàng thành Thăng Long'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Áp dụng'));
    await tester.pumpAndSettle();

    expect(find.text('1 hiện vật'), findsOneWidget);
    expect(find.text('Đoan Môn'), findsOneWidget);
    expect(find.text('Khuê Văn Các'), findsNothing);
  });
}
