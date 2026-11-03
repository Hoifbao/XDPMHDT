import 'dart:convert';
import 'dart:io';

import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:smartheritage/core/artifact_service.dart';
import 'package:smartheritage/features/auth/auth_service.dart';

Map<String, dynamic> artifactJson(int id, String lang) => {
  'id': id,
  'name': 'Artifact $id',
  'status': 'ACTIVE',
  'requestedLanguage': lang,
  'fallback': false,
  'content': {
    'languageCode': lang,
    'title': 'Title $id $lang',
    'description': 'Description',
  },
  'contents': [],
  'media': [],
};

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();
  late http.Client original;
  setUp(() {
    original = AuthService.rawClient;
    SharedPreferences.setMockInitialValues({});
    AuthService.rawClient = MockClient((request) async {
      final id = int.parse(request.url.pathSegments.last);
      return http.Response(
        jsonEncode({
          'data': artifactJson(id, request.url.queryParameters['lang']!),
        }),
        200,
      );
    });
  });
  tearDown(() {
    AuthService.rawClient.close();
    AuthService.rawClient = original;
  });

  test(
    'keeps 20 distinct artifacts with both VI and EN; evicts all variants',
    () async {
      for (var id = 1; id <= 20; id++) {
        await ArtifactApiService.fetchArtifact(id, 'vi');
        await ArtifactApiService.fetchArtifact(id, 'en');
      }
      expect((await ArtifactApiService.recentArtifacts()).length, 20);
      expect(await ArtifactApiService.hasCached(1), isTrue);
      await ArtifactApiService.fetchArtifact(21, 'vi');
      expect(await ArtifactApiService.hasCached(1), isFalse);
      final prefs = await SharedPreferences.getInstance();
      expect(prefs.containsKey('sh_artifact_1_vi'), isFalse);
      expect(prefs.containsKey('sh_artifact_1_en'), isFalse);
      expect((await ArtifactApiService.recentArtifacts()).map((a) => a.id), [
        21,
        ...List.generate(19, (i) => 20 - i),
      ]);
    },
  );

  test(
    'offline read updates recency and preserves requested translation',
    () async {
      for (var id = 1; id <= 20; id++) {
        await ArtifactApiService.fetchArtifact(id, 'vi');
      }
      await ArtifactApiService.fetchArtifact(1, 'en');
      final online = AuthService.rawClient;
      AuthService.rawClient = MockClient(
        (_) async => throw const SocketException('offline'),
      );
      final cached = await ArtifactApiService.fetchArtifact(2, 'vi');
      expect(cached.fromCache, isTrue);
      expect(
        (await ArtifactApiService.fetchArtifact(
          1,
          'en',
        )).artifact.contentFor('en')!.languageCode,
        'en',
      );
      AuthService.rawClient.close();
      AuthService.rawClient = online;
      await ArtifactApiService.fetchArtifact(21, 'vi');
      expect(await ArtifactApiService.hasCached(2), isTrue);
      expect(await ArtifactApiService.hasCached(3), isFalse);
    },
  );

  test('offline different language marks fallback and missing artifact fails safely', () async {
    await ArtifactApiService.fetchArtifact(1, 'vi');
    AuthService.rawClient = MockClient(
      (_) async => throw const SocketException('offline'),
    );
    final result = await ArtifactApiService.fetchArtifact(1, 'en');
    expect(result.artifact.contentFor('en')!.fallbackUsed, isTrue);
    expect(result.artifact.contentFor('en')!.languageCode, 'vi');
    await expectLater(
      ArtifactApiService.fetchArtifact(2, 'vi'),
      throwsA(isA<SocketException>()),
    );
  });

  test('concurrent saves do not lose cache IDs', () async {
    await Future.wait(
      List.generate(25, (i) => ArtifactApiService.fetchArtifact(i + 1, 'vi')),
    );
    final items = await ArtifactApiService.recentArtifacts();
    expect(items.length, 20);
    expect(items.map((a) => a.id).toSet().length, 20);
  });

  test(
    'migrates old per-language recency list without losing cached content',
    () async {
      SharedPreferences.setMockInitialValues({
        'sh_artifact_keys': ['1:en', '1:vi', '2:vi'],
        'sh_artifact_1': jsonEncode({'data': artifactJson(1, 'en')}),
        'sh_artifact_2': jsonEncode({'data': artifactJson(2, 'vi')}),
      });
      expect((await ArtifactApiService.recentArtifacts()).map((a) => a.id), [
        1,
        2,
      ]);
    },
  );
}
