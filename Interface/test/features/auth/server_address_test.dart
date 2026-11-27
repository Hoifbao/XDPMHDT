import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:smartheritage/core/network/auth_http_client.dart';
import 'package:smartheritage/features/auth/auth_service.dart';
import 'package:smartheritage/features/auth/login_screen.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();
  const server = 'https://demo.example.org';
  final originalClient = AuthService.rawClient;

  setUp(() async {
    SharedPreferences.setMockInitialValues({});
    await AuthService.init();
  });

  tearDown(() => AuthService.rawClient = originalClient);

  testWidgets('login checks and saves an HTTPS server for the next launch', (
    tester,
  ) async {
    var requests = 0;
    AuthService.rawClient = MockClient((request) async {
      requests++;
      expect(request.url.toString(), '$server/api/heritage-sites');
      expect(request.headers.containsKey('Authorization'), isFalse);
      expect(request.followRedirects, isFalse);
      return requests == 1
          ? http.Response('<html>Not Smart Heritage</html>', 200)
          : http.Response('{"data":[]}', 200);
    });
    await tester.pumpWidget(const MaterialApp(home: LoginScreen()));
    final settings = find.text('Địa chỉ máy chủ');
    await tester.ensureVisible(settings);
    await tester.tap(settings);
    await tester.pumpAndSettle();
    final address = find.widgetWithText(TextField, 'Link HTTPS');
    final check = find.text('Kiểm tra kết nối');
    final save = find.widgetWithText(FilledButton, 'Lưu');

    await tester.enterText(address, 'http://demo.example.org');
    await tester.tap(check);
    await tester.pumpAndSettle();
    expect(requests, 0);
    expect(tester.widget<FilledButton>(save).onPressed, isNull);

    await tester.enterText(address, '$server/');
    await tester.tap(check);
    await tester.pumpAndSettle();
    expect(tester.widget<FilledButton>(save).onPressed, isNull);
    await tester.tap(check);
    await tester.pumpAndSettle();
    expect(find.text('Kết nối thành công.'), findsOneWidget);
    await tester.tap(save);
    await tester.pumpAndSettle();

    await AuthService.init();
    expect(AuthService.baseUrl, server);
    expect(requests, 2);
  });

  test('switching servers discards the previous login and scopes JWTs', () async {
    AuthService.rawClient = MockClient((request) async {
      expect(request.url.origin, AuthService.baseUrl);
      return http.Response(
        jsonEncode({
          'userId': 1,
          'username': 'visitor',
          'accessToken': 'access',
          'refreshToken': 'refresh',
        }),
        200,
      );
    });
    await AuthService.login('visitor', 'password');
    expect(AuthService.isLoggedIn, isTrue);
    await AuthService.saveServerAddress(Uri.parse(server));
    expect(AuthService.accessToken, isNull);
    await AuthService.init();
    expect(AuthService.isLoggedIn, isFalse);

    await AuthService.login('visitor', 'password');
    final client = AuthHttpClient(MockClient((request) async {
      expect(
        request.headers['authorization'],
        request.url.origin == server ? 'Bearer access' : isNull,
      );
      return http.Response('{}', 200);
    }));
    await client.get(Uri.parse('$server/api/auth/me'));
    await client.get(Uri.parse('https://demo.example.org.other.test/api/auth/me'));
  });
}
