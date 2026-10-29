import 'dart:async';

import 'package:flutter/material.dart';

import 'beacon_service.dart';

/// Màn hình quét iBeacon xung quanh và đối chiếu với registry của Backend.
class BeaconScanScreen extends StatefulWidget {
  const BeaconScanScreen({super.key});

  @override
  State<BeaconScanScreen> createState() => _BeaconScanScreenState();
}

class _BeaconScanScreenState extends State<BeaconScanScreen> {
  static const Duration _staleAfter = Duration(seconds: 10);

  final Map<String, RegistryBeacon> _registry = {};
  final Map<String, ScannedBeacon> _scanned = {};
  StreamSubscription<List<ScannedBeacon>>? _subscription;
  Timer? _refreshTimer;
  bool _loading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _start();
    // Cập nhật lại giao diện mỗi giây để beacon mất sóng sẽ biến khỏi danh sách
    _refreshTimer = Timer.periodic(const Duration(seconds: 1), (_) {
      if (mounted) setState(() {});
    });
  }

  @override
  void dispose() {
    _subscription?.cancel();
    _refreshTimer?.cancel();
    BeaconService.stopScan();
    super.dispose();
  }

  Future<void> _start() async {
    await _subscription?.cancel();
    setState(() {
      _loading = true;
      _error = null;
      _scanned.clear();
    });

    try {
      final items = await BeaconService.fetchRegistry();
      _registry
        ..clear()
        ..addEntries(items.map((b) => MapEntry(b.key, b)));

      await BeaconService.initialize();
      await BeaconService.startScan();

      _subscription = BeaconService.ranging().listen(
        (list) {
          if (!mounted) return;
          setState(() {
            for (final beacon in list) {
              _scanned[beacon.key] = beacon;
            }
          });
        },
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

  @override
  Widget build(BuildContext context) {
    final now = DateTime.now();
    final visible = _scanned.values
        .where((b) => now.difference(b.lastSeen) < _staleAfter)
        .toList()
      ..sort((a, b) => b.rssi.compareTo(a.rssi));

    return Scaffold(
      appBar: AppBar(
        title: const Text('Quét iBeacon'),
        actions: [
          IconButton(
            tooltip: 'Quét lại',
            icon: const Icon(Icons.refresh),
            onPressed: _loading ? null : _start,
          ),
        ],
      ),
      body: _buildBody(visible),
    );
  }

  Widget _buildBody(List<ScannedBeacon> visible) {
    if (_loading) {
      return const Center(child: CircularProgressIndicator());
    }
    if (_error != null) {
      return Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Text(_error!, textAlign: TextAlign.center),
        ),
      );
    }

    final matched = visible.where((b) => _registry.containsKey(b.key)).length;
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        Text('Đang thấy ${visible.length} beacon · Khớp registry: $matched'),
        const SizedBox(height: 12),
        if (visible.isEmpty)
          const Text(
            'Chưa thấy beacon nào. Hãy bật Bluetooth và Vị trí, rồi đến gần beacon.',
          ),
        ...visible.map(_tile),
      ],
    );
  }

  Widget _tile(ScannedBeacon beacon) {
    final registered = _registry[beacon.key];
    final String status;
    if (registered == null) {
      status = 'Ngoài registry';
    } else {
      status = registered.artifactName ?? 'Chưa gán hiện vật';
    }

    return Card(
      child: ListTile(
        isThreeLine: true,
        leading: Icon(
          registered != null ? Icons.check_circle : Icons.help_outline,
          color: const Color(0xFF718B78),
        ),
        title: Text(beacon.uuid, style: const TextStyle(fontSize: 13)),
        subtitle: Text(
          'Major ${beacon.major} · Minor ${beacon.minor}\n'
          'RSSI ${beacon.rssi} dBm · ~${beacon.accuracy.toStringAsFixed(1)} m',
        ),
        trailing: SizedBox(
          width: 90,
          child: Text(
            status,
            textAlign: TextAlign.end,
            style: const TextStyle(fontSize: 12, fontWeight: FontWeight.w600),
          ),
        ),
      ),
    );
  }
}
