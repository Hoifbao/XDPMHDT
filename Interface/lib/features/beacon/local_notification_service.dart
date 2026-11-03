import 'package:flutter/foundation.dart';
import 'package:flutter_local_notifications/flutter_local_notifications.dart';

/// Gửi local notification khi app đang chạy nền (Khoa - Sprint 5, Thứ 3 03/11).
///
/// Kịch bản: app còn chạy (màn quét đang mở nhưng bị thu nhỏ/App-switcher), người
/// dùng đi vào vùng beacon → không thể đẩy màn hình khi bị ẩn → hiện thông báo
/// "Gần đây có hiện vật..." và bấm vào mở đúng màn chi tiết hiện vật.
///
/// Bằng chứng payload: bấm thông báo → app trở lại foreground → mở
/// ArtifactDetailScreen(artifactId, lang) đúng hiện vật vừa báo.
///
/// Lưu ý thiết bị thật: Android 13+ cần chấp nhận quyền Thông báo (plugin tự xin).
class LocalNotificationService {
  LocalNotificationService._internal();
  static final LocalNotificationService I = LocalNotificationService._internal();

  final FlutterLocalNotificationsPlugin _plugin = FlutterLocalNotificationsPlugin();

  /// Finish-step nhỏ: init đúng 1 lần; lỗi thì tắt lặng để luồng quét vẫn chạy.
  bool _ready = false;

  /// Callback khi người dùng bấm thông báo: (artifactId, lang).
  void Function(int artifactId, String lang)? onNotificationTap;

  static const String _logTag = '[BEACON-SCAN]';
  static const String _channelId = 'beacon_channel';
  static const String _payloadSeparator = '|';

  /// Khởi động plugin + xin quyền Android 13+ (chỉ chạy 1 lần).
  Future<void> initialize() async {
    if (_ready) return;
    try {
      const androidInit = AndroidInitializationSettings('ic_launcher');
      const initSettings = InitializationSettings(android: androidInit);
      await _plugin.initialize(
        settings: initSettings,
        onDidReceiveNotificationResponse: _onTap,
      );
      try {
        await _plugin
            .resolvePlatformSpecificImplementation<
                AndroidFlutterLocalNotificationsPlugin>()
            ?.requestNotificationsPermission();
      } catch (e) {
        // Android dưới 13 hoặc iOS...: bỏ qua, thông báo vẫn gửi được trên vài máy
        debugPrint('$_logTag Xin quyền thông báo không khả dụng: $e');
      }
      _ready = true;
      debugPrint('$_logTag Local notification đã sẵn sàng (kênh $_channelId, app nền).');
    } catch (e) {
      debugPrint('$_logTag Không khởi động được local notification (tắt lặng): $e');
    }
  }

  /// Hiện thông báo về hiện vật gần beacon. payload "artifactId|lang" để bấm vào mở đúng màn.
  Future<void> showArtifact({
    required int artifactId,
    required String lang,
    required String title,
    required String body,
  }) async {
    await initialize();
    if (!_ready) return;
    try {
      const androidDetails = AndroidNotificationDetails(
        _channelId,
        'Hiện vật gần bạn',
        channelDescription: 'Nhắc khi đến gần beacon có gán hiện vật (Sprint 5)',
        importance: Importance.high,
        priority: Priority.high,
        ticker: 'Smart Heritage – hiện vật gần bạn',
      );
      const details = NotificationDetails(android: androidDetails);
      await _plugin.show(
        id: artifactId, // notification id = artifactId: cùng hiện vật chỉ 1 thông báo
        title: title,
        body: body,
        notificationDetails: details,
        payload: '$artifactId$_payloadSeparator$lang',
      );
      debugPrint('$_logTag Đã đẩy local notification: "$title" (artifactId=$artifactId).');
    } catch (e) {
      debugPrint('$_logTag Gửi local notification thất bại: $e');
    }
  }

  void _onTap(NotificationResponse response) {
    final payload = response.payload ?? '';
    final parts = payload.split(_payloadSeparator);
    final artifactId = int.tryParse(parts.isNotEmpty ? parts[0] : '');
    final lang = parts.length > 1 && parts[1].isNotEmpty ? parts[1] : 'vi';
    debugPrint('$_logTag Bấm thông báo → mở chi tiết hiện vật #$artifactId (lang=$lang).');
    if (artifactId != null) {
      onNotificationTap?.call(artifactId, lang);
    }
  }
}
