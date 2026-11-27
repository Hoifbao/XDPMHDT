import 'dart:async';
import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:video_player_platform_interface/video_player_platform_interface.dart';
import 'package:smartheritage/core/media_lifecycle.dart';
import 'package:smartheritage/features/auth/auth_service.dart';
import 'package:smartheritage/screens/artifact_detail_screen.dart';

import 'core/artifact_cache_test.dart' show artifactJson;

class FakeVideoPlatform extends VideoPlayerPlatform {
  final events = <int, StreamController<VideoEvent>>{};
  final disposed = <int>[];
  int plays = 0, pauses = 0;
  @override
  Future<void> init() async {}
  @override
  Future<int> createWithOptions(VideoCreationOptions options) async {
    final id = events.length + 1;
    late StreamController<VideoEvent> controller;
    controller = StreamController.broadcast(
      onListen: () {
        controller.add(
          VideoEvent(
            eventType: VideoEventType.initialized,
            duration: const Duration(minutes: 1),
            size: const Size(640, 360),
          ),
        );
      },
    );
    events[id] = controller;
    return id;
  }

  @override
  Stream<VideoEvent> videoEventsFor(int playerId) => events[playerId]!.stream;
  @override
  Future<void> dispose(int playerId) async {
    disposed.add(playerId);
    await events[playerId]!.close();
  }

  @override
  Future<void> play(int playerId) async {
    plays++;
  }

  @override
  Future<void> pause(int playerId) async {
    pauses++;
  }

  @override
  Future<void> setLooping(int playerId, bool looping) async {}
  @override
  Future<void> setVolume(int playerId, double volume) async {}
  @override
  Future<void> setPlaybackSpeed(int playerId, double speed) async {}
  @override
  Future<void> seekTo(int playerId, Duration position) async {}
  @override
  Future<Duration> getPosition(int playerId) async => Duration.zero;
  @override
  Widget buildViewWithOptions(VideoViewOptions options) => const SizedBox();
}

void main() {
  late http.Client original;
  late VideoPlayerPlatform originalVideo;
  late FakeVideoPlatform video;
  setUp(() {
    SharedPreferences.setMockInitialValues({});
    original = AuthService.rawClient;
    originalVideo = VideoPlayerPlatform.instance;
    video = FakeVideoPlatform();
    VideoPlayerPlatform.instance = video;
    AuthService.rawClient = MockClient((_) async {
      final data = artifactJson(1, 'vi');
      (data['content'] as Map<String, dynamic>)['videoUrl'] =
          'https://museum.example/video.mp4';
      return http.Response(jsonEncode({'data': data}), 200);
    });
  });
  tearDown(() {
    AuthService.rawClient.close();
    AuthService.rawClient = original;
    VideoPlayerPlatform.instance = originalVideo;
  });

  Future<void> openVideo(WidgetTester tester) async {
    await tester.pumpWidget(
      MaterialApp(
        navigatorObservers: [mediaRouteObserver],
        home: const ArtifactDetailScreen(artifactId: 1),
      ),
    );
    await tester.pumpAndSettle();
    await tester.tap(find.text('Xem video'));
    await tester.pumpAndSettle();
  }

  testWidgets(
    'video plays, pauses when covered, supports fullscreen and disposes on exit',
    (tester) async {
      await openVideo(tester);
      expect(video.plays, greaterThan(0));
      await tester.tap(find.byTooltip('Toàn màn hình'));
      await tester.pumpAndSettle();
      expect(find.byTooltip('Thoát toàn màn hình'), findsOneWidget);
      await tester.tap(find.byTooltip('Thoát toàn màn hình'));
      await tester.pumpAndSettle();
      final navigator = Navigator.of(
        tester.element(find.text('Video thuyết minh')),
      );
      final pauses = video.pauses;
      unawaited(
        navigator.push(
          MaterialPageRoute<void>(
            builder: (_) => const Scaffold(body: Text('Other screen')),
          ),
        ),
      );
      await tester.pumpAndSettle();
      expect(video.pauses, greaterThan(pauses));
      navigator.pop();
      await tester.pumpAndSettle();
      await tester.pageBack();
      await tester.pumpAndSettle();
      await tester.runAsync(() async {
        await Future<void>.delayed(const Duration(milliseconds: 100));
      });
      await tester.pump();
      expect(video.disposed, [1]);
      expect(tester.takeException(), isNull);
    },
  );

  testWidgets(
    'network failure during playback shows message and retry releases old controller',
    (tester) async {
      await openVideo(tester);
      video.events[1]!.addError(
        PlatformException(code: 'network', message: 'connection lost'),
      );
      await tester.pumpAndSettle();
      expect(find.textContaining('Không tải được video.'), findsOneWidget);
      await tester.tap(find.text('Thử lại'));
      await tester.runAsync(() async {
        await Future<void>.delayed(const Duration(milliseconds: 100));
      });
      await tester.pump(const Duration(milliseconds: 500));
      expect(video.disposed, [1]);
      expect(video.events.length, 2);
      await tester.pumpAndSettle();
      expect(video.disposed, [1]);
      expect(video.events.length, 2);
      await tester.pageBack();
      await tester.pumpAndSettle();
      await tester.runAsync(() async {
        await Future<void>.delayed(const Duration(milliseconds: 100));
      });
      await tester.pump();
      expect(video.disposed, [1, 2]);
      expect(tester.takeException(), isNull);
    },
  );
}
