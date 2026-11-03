import 'dart:convert';
import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:smartheritage/features/auth/auth_service.dart';
import 'package:smartheritage/screens/artifact_detail_screen.dart';
import 'package:smartheritage/screens/recent_artifacts_screen.dart';
import 'package:smartheritage/features/feedback/feedback_screen.dart';

import 'core/artifact_cache_test.dart' show artifactJson;

void main() {
  testWidgets(
    'favorites use real adapter and feedback receives the artifact ID',
    (tester) async {
      SharedPreferences.setMockInitialValues({});
      AuthService.currentUser.value = const AuthUser(
        userId: 1,
        username: 'visitor',
        email: '',
        fullName: '',
        role: 'VISITOR',
      );
      AuthService.rawClient = MockClient(
        (_) async =>
            http.Response(jsonEncode({'data': artifactJson(7, 'vi')}), 200),
      );
      final writes = <String>[];
      await http.runWithClient(
        () async {
          await tester.pumpWidget(
            const MaterialApp(home: ArtifactDetailScreen(artifactId: 7)),
          );
          await tester.pumpAndSettle();
          await tester.tap(find.byTooltip('Yêu thích'));
          await tester.pumpAndSettle();
          expect(find.byIcon(Icons.favorite), findsOneWidget);
          await tester.tap(find.byTooltip('Yêu thích'));
          await tester.pumpAndSettle();
          expect(find.byIcon(Icons.favorite_border), findsOneWidget);
          expect(writes, [
            'POST /api/me/favorites/7',
            'DELETE /api/me/favorites/7',
          ]);
          await tester.tap(find.text('Đánh giá'));
          await tester.pumpAndSettle();
          expect(
            tester
                .widget<FeedbackScreen>(find.byType(FeedbackScreen))
                .artifactId,
            7,
          );
        },
        () => MockClient((request) async {
          if (request.method != 'GET') {
            writes.add('${request.method} ${request.url.path}');
          }
          final Object data = request.url.path.endsWith('/exists')
              ? false
              : request.url.path.endsWith('/rating-summary')
              ? {
                  'artifactId': 7,
                  'averageRating': 0,
                  'totalRatings': 0,
                  'distribution': {},
                }
              : <Object>[];
          return http.Response(
            jsonEncode({'success': true, 'data': data}),
            200,
          );
        }),
      );
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'invalid audio URL displays an error and disposing does not crash',
    (tester) async {
      SharedPreferences.setMockInitialValues({});
      AuthService.rawClient = MockClient((_) async {
        final data = artifactJson(1, 'vi');
        (data['content'] as Map<String, dynamic>)['audioUrl'] = 'invalid-url';
        return http.Response(jsonEncode({'data': data}), 200);
      });
      await tester.pumpWidget(
        const MaterialApp(home: ArtifactDetailScreen(artifactId: 1)),
      );
      await tester.pumpAndSettle();
      await tester.tap(find.text('Âm thanh'));
      await tester.pumpAndSettle();
      expect(
        find.textContaining('Không tải được file âm thanh.'),
        findsOneWidget,
      );
      await tester.pumpWidget(const SizedBox());
      await tester.pumpAndSettle();
      expect(tester.takeException(), isNull);
    },
  );
  late http.Client original;
  setUp(() {
    original = AuthService.rawClient;
    SharedPreferences.setMockInitialValues({});
  });
  tearDown(() {
    AuthService.rawClient.close();
    AuthService.rawClient = original;
    AuthService.currentUser.value = null;
  });

  testWidgets('uses preferredLanguage and reloads content for VI/EN', (
    tester,
  ) async {
    final requests = <String>[];
    AuthService.currentUser.value = const AuthUser(
      userId: 1,
      username: 'user',
      email: '',
      fullName: '',
      role: 'VISITOR',
      preferredLanguage: 'en',
    );
    AuthService.rawClient = MockClient((request) async {
      final lang = request.url.queryParameters['lang']!;
      requests.add(lang);
      return http.Response(jsonEncode({'data': artifactJson(1, lang)}), 200);
    });
    await tester.pumpWidget(
      const MaterialApp(home: ArtifactDetailScreen(artifactId: 1)),
    );
    await tester.pumpAndSettle();
    expect(requests, ['en']);
    await tester.tap(find.text('🇻🇳 Tiếng Việt'));
    await tester.pumpAndSettle();
    expect(requests, ['en', 'vi']);
    expect(find.text('Title 1 vi'), findsWidgets);
  });

  testWidgets(
    'shows fallback label and invalid video URL reports error without crashing on exit',
    (tester) async {
      AuthService.rawClient = MockClient((_) async {
        final data = artifactJson(1, 'vi');
        data['fallback'] = true;
        (data['content'] as Map<String, dynamic>)['videoUrl'] = 'invalid-url';
        return http.Response(jsonEncode({'data': data}), 200);
      });
      await tester.pumpWidget(
        const MaterialApp(
          home: ArtifactDetailScreen(artifactId: 1, initialLang: 'en'),
        ),
      );
      await tester.pumpAndSettle();
      expect(
        find.text('Chưa có bản dịch, đang hiển thị tiếng Việt'),
        findsOneWidget,
      );
      await tester.tap(find.text('Xem video'));
      await tester.pumpAndSettle();
      expect(find.textContaining('Không tải được video.'), findsOneWidget);
      await tester.pageBack();
      await tester.pumpAndSettle();
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'recent artifacts can be reopened offline after restarting screen',
    (tester) async {
      AuthService.rawClient = MockClient(
        (_) async =>
            http.Response(jsonEncode({'data': artifactJson(1, 'vi')}), 200),
      );
      await tester.pumpWidget(
        const MaterialApp(home: ArtifactDetailScreen(artifactId: 1)),
      );
      await tester.pumpAndSettle();
      AuthService.rawClient = MockClient(
        (_) async => throw const SocketException('offline'),
      );
      await tester.pumpWidget(const MaterialApp(home: RecentArtifactsScreen()));
      await tester.pumpAndSettle();
      await tester.tap(find.text('Artifact 1'));
      await tester.pumpAndSettle();
      expect(find.textContaining('Đang xem nội dung đã tải'), findsOneWidget);
      expect(find.text('Title 1 vi'), findsWidgets);
      expect(tester.takeException(), isNull);
    },
  );
}
