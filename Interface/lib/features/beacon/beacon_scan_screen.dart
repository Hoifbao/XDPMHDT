import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:uuid/uuid.dart';

import '../../screens/artifact_detail_screen.dart';
import '../auth/auth_service.dart';
import '../personalization/personalization_service.dart';
import 'beacon_service.dart';
import 'local_notification_service.dart';

/// Màn hình quét iBeacon – luồng beacon chính của đề tài.
///
/// Luồng:
/// 1. Khởi động: tải registry (cache 30 phút + bộ nhớ máy), initialize BLE.
/// 2. Mỗi lần quét: so khớp (uuid, major, minor); bỏ qua beacon ngoài registry.
/// 3. Chọn gần nhất (avg RSSI 5 lần); vào vùng ≥ rssiEnter; hysteresis rssiExit.
/// 4. Chống trùng: không báo lại trong 60 giây kể từ lần báo trước.
/// 5. Vào vùng:
///    - app ở foreground → ArtifactDetailScreen (KHÔNG tự đóng khi rời vùng);
///    - app bị thu nhỏ (chạy nền) → local notification, bấm vào mở đúng hiện vật.
/// 6. Ra vùng (RSSI ≤ rssiExit hoặc không thấy > 10s) → POST visits (BEACON).
/// 7. Bản debug có nút "Giả lập phát hiện beacon" để demo không cần beacon thật.
class BeaconScanScreen extends StatefulWidget {
  /// Ngôn ngữ ưa thích; null → AuthService.preferredLanguage.
  final String? lang;

  const BeaconScanScreen({super.key, this.lang});

  @override
  State<BeaconScanScreen> createState() => _BeaconScanScreenState();
}

class _BeaconScanScreenState extends State<BeaconScanScreen>
    with WidgetsBindingObserver {
  final Map<String, RegistryBeacon> _registry = {};
  final Map<String, RssiBuffer> _rssiBuffers = {};
  final Map<String, bool> _activeBeacons = {};
  final Map<String, DateTime> _enterTimes = {};
  final Map<String, DateTime> _lastSeenTimes = {};

  /// Thời điểm báo lần cuối (enter) — cooldown 60s kể cả ra/vào liên tục.
  final Map<String, DateTime> _lastAlertTimes = {};

  /// Đang mở màn chi tiết (tránh push chồng khi nhiều beacon cùng vào).
  bool _navigatingToDetail = false;

  StreamSubscription<List<ScannedBeacon>>? _subscription;
  Timer? _tickTimer;
  Timer? _registryRefreshTimer;

  bool _loading = true;
  String? _error;

  static const _uuid = Uuid();

  /// Tag chung cho log của luồng quét — dễ lọc trong console (đầu ra Thứ 2 02/11).
  static const _logTag = '[BEACON-SCAN]';

  String get _lang => widget.lang ?? AuthService.preferredLanguage;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _start();

    _tickTimer = Timer.periodic(const Duration(seconds: 1), (_) {
      if (mounted) {
        _checkExitConditions();
        setState(() {});
      }
    });

    _registryRefreshTimer = Timer.periodic(
      BeaconConfig.registryCacheAge,
      (_) => _reloadRegistry(),
    );
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    _subscription?.cancel();
    _tickTimer?.cancel();
    _registryRefreshTimer?.cancel();
    BeaconService.stopScan();
    super.dispose();
  }

  /// Bấm thông báo (app nền) → trở lại app → mở đúng màn chi tiết của Xuân Anh.
  void _onNotificationTap(int artifactId, String lang) {
    if (!mounted || _navigatingToDetail) return;
    _navigatingToDetail = true;
    Navigator.push(
      context,
      MaterialPageRoute(
        builder: (_) => ArtifactDetailScreen(
          artifactId: artifactId,
          initialLang: lang,
        ),
      ),
    ).whenComplete(() {
      if (mounted) _navigatingToDetail = false;
    });
  }

  Future<void> _start() async {
    await _subscription?.cancel();
    setState(() {
      _loading = true;
      _error = null;
    });

    try {
      final items = await BeaconService.getRegistry();
      _registry
        ..clear()
        ..addEntries(items.map((b) => MapEntry(b.key, b)));
      // Bằng chứng tải registry khi mở app (nạp 1 lần, tự làm mới mỗi 30 phút)
      debugPrint('$_logTag Registry nạp được ${items.length} beacon khi mở app '
          '(tự làm mới mỗi ${BeaconConfig.registryCacheAge.inMinutes} phút). '
          'Có hiện vật gán: ${items.where((b) => b.artifactId != null).length}.');
      // Local notification cho kịch bản app đang chạy nền (Sprint 5)
      unawaited(LocalNotificationService.I.initialize());
      LocalNotificationService.I.onNotificationTap = _onNotificationTap;
      await BeaconService.initialize();
      await BeaconService.startScan();

      _subscription = BeaconService.ranging().listen(
        _onBeaconsSeen,
        onError: (Object e) {
          if (mounted) setState(() => _error = 'Lỗi quét BLE: $e');
        },
      );

      if (mounted) setState(() => _loading = false);
    } catch (e) {
      if (mounted) {
        setState(() {
          _loading = false;
          _error = e.toString();
        });
      }
    }
  }

  Future<void> _reloadRegistry() async {
    try {
      final items = await BeaconService.getRegistry(forceRefresh: true);
      if (mounted) {
        setState(() {
          _registry
            ..clear()
            ..addEntries(items.map((b) => MapEntry(b.key, b)));
        });
        debugPrint('$_logTag Làm mới registry theo chu kỳ '
            '${BeaconConfig.registryCacheAge.inMinutes} phút: ${items.length} beacon.');
      }
    } catch (_) {}
  }

  void _onBeaconsSeen(List<ScannedBeacon> beacons) {
    if (!mounted) return;

    final now = DateTime.now();
    String? nearestEnterKey;
    var nearestRssi = -999;

    for (final beacon in beacons) {
      // Bỏ qua beacon không có trong registry
      if (!_registry.containsKey(beacon.key)) continue;

      _rssiBuffers.putIfAbsent(beacon.key, RssiBuffer.new).add(beacon.rssi);
      _lastSeenTimes[beacon.key] = now;

      final avgRssi = _rssiBuffers[beacon.key]!.average;
      final isActive = _activeBeacons[beacon.key] ?? false;
      final registered = _registry[beacon.key]!;

      // Hysteresis: đang trong vùng mà RSSI ≤ rssiExit → đánh dấu ra (xử lý ở tick)
      if (isActive && avgRssi <= BeaconConfig.rssiExit) {
        continue;
      }

      // Ứng viên vào vùng
      if (!isActive &&
          avgRssi >= BeaconConfig.rssiEnter &&
          registered.artifactId != null) {
        final lastAlert = _lastAlertTimes[beacon.key];
        final inCooldown = lastAlert != null &&
            now.difference(lastAlert) < BeaconConfig.antispamCooldown;
        if (!inCooldown && avgRssi > nearestRssi) {
          nearestRssi = avgRssi;
          nearestEnterKey = beacon.key;
        }
      }
    }

    // Chỉ mở 1 hiện vật gần nhất mỗi lần quét
    if (nearestEnterKey != null) {
      final chosen = _registry[nearestEnterKey]!;
      debugPrint('$_logTag Chọn beacon gần nhất qua trung bình RSSI 5 lần đọc: '
          'Major ${chosen.major} · Minor ${chosen.minor} (${chosen.artifactName ?? nearestEnterKey}) '
          'avg ${_rssiBuffers[nearestEnterKey]!.average} dBm — đạt ngưỡng vào ${BeaconConfig.rssiEnter} dBm.');
      _onEnterRegion(nearestEnterKey, now);
    }
  }

  void _onEnterRegion(String key, DateTime now) {
    final registered = _registry[key];
    if (registered == null || registered.artifactId == null) return;
    if (_navigatingToDetail) return;

    setState(() {
      _activeBeacons[key] = true;
      _enterTimes[key] = now;
      _lastAlertTimes[key] = now;
    });

    final appResumed =
        WidgetsBinding.instance.lifecycleState == AppLifecycleState.resumed;
    if (!appResumed) {
      // App đang chạy nền: hiện thông báo, KHÔNG đẩy màn hình
      // (sau khi bấm thông báo, app về foreground → _onNotificationTap mở màn đúng hiện vật).
      debugPrint('$_logTag VÀO VÙNG $key ở CHẾ ĐỘ NỀN → gửi local notification '
          'cho hiện vật #${registered.artifactId} (bấm vào sẽ mở).');
      unawaited(LocalNotificationService.I.showArtifact(
        artifactId: registered.artifactId!,
        lang: _lang,
        title: 'Gần đây có hiện vật',
        body: '${registered.artifactName ?? 'Hiện vật #${registered.artifactId}'} '
            '(${registered.siteName ?? ''}). Bấm để xem chi tiết.',
      ));
      return;
    }

    _navigatingToDetail = true;
    debugPrint('$_logTag VÀO VÙNG $key (cooldown ghi lúc ${now.toIso8601String()}) '
        '→ mở chi tiết hiện vật #${registered.artifactId} cho Xuân Anh nhận, lang=$_lang');
    Navigator.push(
      context,
      MaterialPageRoute(
        builder: (_) => ArtifactDetailScreen(
          artifactId: registered.artifactId!,
          initialLang: _lang,
        ),
      ),
    ).whenComplete(() {
      if (mounted) _navigatingToDetail = false;
    });
  }

  /// Ra vùng: RSSI ≤ rssiExit hoặc không thấy quá exitTimeout.
  void _checkExitConditions() {
    final now = DateTime.now();
    final toExit = <String>[];

    for (final entry in _activeBeacons.entries) {
      if (!entry.value) continue;
      final key = entry.key;
      final lastSeen = _lastSeenTimes[key];
      final avgRssi = _rssiBuffers[key]?.average;
      final timedOut = lastSeen == null ||
          now.difference(lastSeen) >= BeaconConfig.exitTimeout;
      final weakSignal =
          avgRssi != null && avgRssi <= BeaconConfig.rssiExit;

      if (timedOut || weakSignal) {
        toExit.add(key);
      }
    }

    for (final key in toExit) {
      _onExitRegion(key, now);
    }
  }

  void _onExitRegion(String key, DateTime now) {
    final enterTime = _enterTimes[key];
    final durationSec =
        enterTime != null ? now.difference(enterTime).inSeconds : 0;

    setState(() {
      _activeBeacons[key] = false;
    });

    // Màn chi tiết KHÔNG tự đóng — chỉ ghi lượt tham quan
    final registered = _registry[key];
    debugPrint('$_logTag RA VÙNG $key sau ${durationSec}s '
        '(exit ≤ ${BeaconConfig.rssiExit} dBm hoặc mất sóng > ${BeaconConfig.exitTimeout.inSeconds}s) '
        '→ ${durationSec > 0 ? "ghi lượt tham quan source=BEACON, durationSeconds=$durationSec" : "chưa có thời gian vào, bỏ qua ghi"}');
    if (registered?.artifactId != null && durationSec > 0) {
      PersonalizationService.recordVisit(
        artifactId: registered!.artifactId!,
        artifactName: registered.artifactName ?? 'Hiện vật',
        siteName: registered.siteName ?? '',
        thumbnailUrl: '',
        source: 'BEACON',
        durationSeconds: durationSec,
        clientEventId: _uuid.v4(),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Quét iBeacon'),
        actions: [
          // Sprint 5 (Thứ 4): nút giả lập chỉ có ở bản debug — demo chạy được
          // toàn luồng như beacon thật khi không có beacon ngoài thực tế.
          if (kDebugMode)
            IconButton(
              tooltip: 'Giả lập phát hiện beacon',
              icon: const Icon(Icons.bug_report),
              onPressed: _loading ? null : _showSimulateDialog,
            ),
          IconButton(
            tooltip: 'Làm mới',
            icon: const Icon(Icons.refresh),
            onPressed: _loading ? null : _start,
          ),
        ],
      ),
      body: _buildBody(),
    );
  }

  /// Chọn 1 beacon trong registry để chạy toàn luồng như thật:
  /// "Vào vùng" → mở chi tiết/notification; "Ra vùng" → ghi lượt tham quan (Phú).
  void _showSimulateDialog() {
    final simulateAreaBeacons = _registry.values
        .where((b) => b.artifactId != null)
        .toList()
      ..sort((a, b) => (a.major * 1000 + a.minor).compareTo(b.major * 1000 + b.minor));

    if (simulateAreaBeacons.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(
        content: Text('Registry chưa có beacon nào được gán hiện vật.'),
      ));
      return;
    }

    showModalBottomSheet<void>(
      context: context,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(16)),
      ),
      builder: (sheetContext) {
        return SafeArea(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Padding(
                padding: EdgeInsets.fromLTRB(16, 16, 16, 4),
                child: Text(
                  'Giả lập phát hiện beacon',
                  style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16),
                ),
              ),
              const Padding(
                padding: EdgeInsets.symmetric(horizontal: 16),
                child: Text(
                  'Chạy đúng toàn luồng như beacon thật (trung bình RSSI 5 lần đọc, ngưỡng −75/−85, cooldown 60s).',
                  style: TextStyle(fontSize: 12, color: Colors.grey),
                ),
              ),
              const Divider(),
              Flexible(
                child: ListView.builder(
                  shrinkWrap: true,
                  itemCount: simulateAreaBeacons.length,
                  itemBuilder: (listContext, index) {
                    final beacon = simulateAreaBeacons[index];
                    final isActive = _activeBeacons[beacon.key] ?? false;
                    return ListTile(
                      dense: true,
                      leading: Icon(
                        Icons.beenhere,
                        color: isActive ? Colors.green : Colors.blueGrey,
                      ),
                      title: Text(
                        beacon.artifactName ?? 'Hiện vật #${beacon.artifactId}',
                        style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 14),
                      ),
                      subtitle: Text(
                        'Major ${beacon.major} · Minor ${beacon.minor}'
                        '${isActive ? ' · ĐANG TRONG VÙNG ${DateTime.now().difference(_enterTimes[beacon.key]!).inSeconds}s' : ''}',
                        style: const TextStyle(fontSize: 11),
                      ),
                      trailing: Row(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          TextButton(
                            onPressed: () => _simulateEnter(sheetContext, beacon.key),
                            child: const Text('Vào vùng'),
                          ),
                          TextButton(
                            onPressed: isActive
                                ? () => _simulateExit(sheetContext, beacon.key)
                                : null,
                            child: const Text('Ra vùng'),
                          ),
                        ],
                      ),
                    );
                  },
                ),
              ),
            ],
          ),
        );
      },
    );
  }

  /// Giả lập vào vùng: RSSI luôn ≥ ngưỡng vào, rồi đi đúng nhánh _onEnterRegion
  /// (vẫn giữ cooldown 60s để demo "chống báo trùng" như thật).
  void _simulateEnter(BuildContext sheetContext, String key) {
    final now = DateTime.now();
    final lastAlert = _lastAlertTimes[key];
    final waitSec = lastAlert == null
        ? 0
        : BeaconConfig.antispamCooldown.inSeconds - now.difference(lastAlert).inSeconds;

    if (waitSec > 0) {
      debugPrint('$_logTag GIẢ LẬP vào vùng $key bị cooldown chặn '
          '(thử lại sau ${waitSec}s) — đúng tiêu chí chống báo trùng.');
      Navigator.pop(sheetContext);
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(
        content: Text('Beacon này vừa báo rồi — thử lại sau $waitSec giây (cooldown 60s).'),
      ));
      return;
    }

    debugPrint('$_logTag GIẢ LẬP phát hiện beacon $key → chạy toàn luồng như thật.');
    final buffer = _rssiBuffers.putIfAbsent(key, RssiBuffer.new);
    buffer.clear();
    for (var i = 0; i < 5; i++) {
      buffer.add(BeaconConfig.rssiEnter + 3); // trung bình 5 lần đọc > ngưỡng vào
    }
    _lastSeenTimes[key] = now;
    Navigator.pop(sheetContext);
    _onEnterRegion(key, now);
  }

  /// Giả lập ra vùng: dùng đúng nhánh _onExitRegion → ghi lượt tham quan
  /// (durationSeconds đo từ lúc vào giả lập — đầu ra cho Phú).
  void _simulateExit(BuildContext sheetContext, String key) {
    if (!_activeBeacons[key]!) {
      Navigator.pop(sheetContext);
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(
        content: Text('Beacon này chưa ở trong vùng — bấm "Vào vùng" trước.'),
      ));
      return;
    }
    debugPrint('$_logTag GIẢ LẬP ra khỏi vùng $key sau '
        '${DateTime.now().difference(_enterTimes[key]!).inSeconds}s.');
    Navigator.pop(sheetContext);
    _onExitRegion(key, DateTime.now());
    ScaffoldMessenger.of(context).showSnackBar(const SnackBar(
      content: Text('Đã ra vùng (giả lập) — đã ghi lượt tham quan source=BEACON.'),
    ));
  }

  Widget _buildBody() {
    if (_loading) {
      return const Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            CircularProgressIndicator(),
            SizedBox(height: 16),
            Text('Đang tải registry và khởi động BLE...'),
          ],
        ),
      );
    }

    if (_error != null) {
      return Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Icon(Icons.bluetooth_disabled, size: 56, color: Colors.grey),
              const SizedBox(height: 16),
              Text(_error!, textAlign: TextAlign.center),
              const SizedBox(height: 16),
              ElevatedButton.icon(
                onPressed: _start,
                icon: const Icon(Icons.refresh),
                label: const Text('Thử lại'),
              ),
            ],
          ),
        ),
      );
    }

    final now = DateTime.now();
    final visible = _lastSeenTimes.entries
        .where((e) =>
            _registry.containsKey(e.key) &&
            now.difference(e.value) < BeaconConfig.exitTimeout)
        .map((e) => e.key)
        .toList();

    final activeKeys = _activeBeacons.entries
        .where((e) => e.value)
        .map((e) => e.key)
        .toList();

    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        _StatsCard(
          totalVisible: visible.length,
          registryCount: _registry.length,
          activeCount: activeKeys.length,
        ),
        const SizedBox(height: 8),
        Text(
          'Ngưỡng vào ${BeaconConfig.rssiEnter} dBm · ra ${BeaconConfig.rssiExit} dBm · '
          'cooldown ${BeaconConfig.antispamCooldown.inSeconds}s',
          style: TextStyle(fontSize: 11, color: Colors.grey.shade600),
        ),
        const SizedBox(height: 12),
        if (activeKeys.isNotEmpty) ...[
          const Text(
            'Đang trong vùng',
            style: TextStyle(fontWeight: FontWeight.bold, fontSize: 14),
          ),
          const SizedBox(height: 8),
          ...activeKeys.map((key) => _ActiveBeaconCard(
                registry: _registry[key]!,
                enterTime: _enterTimes[key],
              )),
          const SizedBox(height: 16),
        ],
        if (visible.isEmpty) ...[
          const SizedBox(height: 32),
          const Center(
            child: Column(
              children: [
                Icon(Icons.bluetooth_searching, size: 56, color: Colors.grey),
                SizedBox(height: 16),
                Text(
                  'Chưa thấy beacon nào trong registry.\nHãy bật Bluetooth và đến gần beacon.',
                  textAlign: TextAlign.center,
                  style: TextStyle(color: Colors.grey),
                ),
              ],
            ),
          ),
        ] else ...[
          const Text(
            'Beacon đang thấy',
            style: TextStyle(fontWeight: FontWeight.bold, fontSize: 14),
          ),
          const SizedBox(height: 8),
          ...visible.map((key) => _BeaconTile(
                registryBeacon: _registry[key]!,
                avgRssi: _rssiBuffers[key]?.average ?? -100,
                isActive: _activeBeacons[key] ?? false,
                lastSeen: _lastSeenTimes[key]!,
              )),
        ],
        const SizedBox(height: 24),
        _buildOutsideRegistrySection(now),
      ],
    );
  }

  Widget _buildOutsideRegistrySection(DateTime now) {
    final outsideKeys = _lastSeenTimes.entries
        .where((e) =>
            !_registry.containsKey(e.key) &&
            now.difference(e.value) < BeaconConfig.exitTimeout)
        .toList();

    if (outsideKeys.isEmpty) return const SizedBox.shrink();

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          'Beacon ngoài registry (${outsideKeys.length}) — bỏ qua',
          style: const TextStyle(
            fontWeight: FontWeight.bold,
            fontSize: 13,
            color: Colors.grey,
          ),
        ),
        const SizedBox(height: 4),
        ...outsideKeys.map(
          (e) => Card(
            child: ListTile(
              dense: true,
              leading: const Icon(Icons.help_outline, color: Colors.grey),
              title: Text(e.key,
                  style: const TextStyle(fontSize: 12, color: Colors.grey)),
              subtitle: Text(
                'RSSI: ${_rssiBuffers[e.key]?.average ?? '?'} dBm',
                style: const TextStyle(fontSize: 11),
              ),
            ),
          ),
        ),
      ],
    );
  }
}

class _StatsCard extends StatelessWidget {
  final int totalVisible;
  final int registryCount;
  final int activeCount;

  const _StatsCard({
    required this.totalVisible,
    required this.registryCount,
    required this.activeCount,
  });

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Row(
          mainAxisAlignment: MainAxisAlignment.spaceAround,
          children: [
            _Stat(label: 'Registry', value: '$registryCount'),
            _Stat(label: 'Đang thấy', value: '$totalVisible'),
            _Stat(
              label: 'Trong vùng',
              value: '$activeCount',
              color: activeCount > 0 ? Colors.green.shade700 : null,
            ),
          ],
        ),
      ),
    );
  }
}

class _Stat extends StatelessWidget {
  final String label;
  final String value;
  final Color? color;

  const _Stat({required this.label, required this.value, this.color});

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        Text(
          value,
          style: TextStyle(
            fontSize: 24,
            fontWeight: FontWeight.bold,
            color: color ?? Theme.of(context).colorScheme.primary,
          ),
        ),
        Text(label, style: const TextStyle(fontSize: 12, color: Colors.grey)),
      ],
    );
  }
}

class _ActiveBeaconCard extends StatelessWidget {
  final RegistryBeacon registry;
  final DateTime? enterTime;

  const _ActiveBeaconCard({required this.registry, this.enterTime});

  @override
  Widget build(BuildContext context) {
    final elapsed = enterTime != null
        ? DateTime.now().difference(enterTime!).inSeconds
        : 0;

    return Card(
      color: Colors.green.shade50,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(12),
        side: BorderSide(color: Colors.green.shade300),
      ),
      child: ListTile(
        leading: const Icon(Icons.location_on, color: Colors.green, size: 28),
        title: Text(
          registry.artifactName ?? 'Hiện vật #${registry.artifactId}',
          style: const TextStyle(fontWeight: FontWeight.bold),
        ),
        subtitle: Text(registry.siteName ?? ''),
        trailing: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            const Icon(Icons.timer, size: 14, color: Colors.green),
            Text(
              '${elapsed}s',
              style: const TextStyle(fontSize: 12, color: Colors.green),
            ),
          ],
        ),
      ),
    );
  }
}

class _BeaconTile extends StatelessWidget {
  final RegistryBeacon registryBeacon;
  final int avgRssi;
  final bool isActive;
  final DateTime lastSeen;

  const _BeaconTile({
    required this.registryBeacon,
    required this.avgRssi,
    required this.isActive,
    required this.lastSeen,
  });

  String _rssiLabel(int rssi) {
    if (rssi >= BeaconConfig.rssiEnter) return 'Trong vùng';
    if (rssi >= BeaconConfig.rssiExit) return 'Gần vùng';
    return 'Ngoài vùng';
  }

  Color _rssiColor(int rssi) {
    if (rssi >= BeaconConfig.rssiEnter) return Colors.green.shade700;
    if (rssi >= BeaconConfig.rssiExit) return Colors.orange.shade700;
    return Colors.red.shade400;
  }

  @override
  Widget build(BuildContext context) {
    final ago = DateTime.now().difference(lastSeen).inSeconds;
    return Card(
      child: ListTile(
        leading: Icon(
          isActive ? Icons.bluetooth_connected : Icons.bluetooth,
          color: isActive ? Colors.green : Colors.blueGrey,
        ),
        title: Text(
          registryBeacon.artifactName ?? 'Chưa gán hiện vật',
          style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 14),
        ),
        subtitle: Text(
          'UUID: ${registryBeacon.uuid.substring(0, 8)}…\n'
          'Major ${registryBeacon.major} · Minor ${registryBeacon.minor} · ${ago}s trước',
          style: const TextStyle(fontSize: 11),
        ),
        isThreeLine: true,
        trailing: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          crossAxisAlignment: CrossAxisAlignment.end,
          children: [
            Text(
              '$avgRssi dBm',
              style: TextStyle(
                fontWeight: FontWeight.bold,
                fontSize: 13,
                color: _rssiColor(avgRssi),
              ),
            ),
            const SizedBox(height: 2),
            Text(
              _rssiLabel(avgRssi),
              style: TextStyle(
                fontSize: 11,
                color: _rssiColor(avgRssi),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
