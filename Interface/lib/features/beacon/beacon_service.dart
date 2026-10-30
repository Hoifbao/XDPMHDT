import 'dart:convert';
import 'dart:math' as math;

import 'package:flutter_blue_plus/flutter_blue_plus.dart';
import 'package:http/http.dart' as http;
import 'package:permission_handler/permission_handler.dart';

import '../auth/auth_service.dart';

/// Một beacon trong registry của Backend (GET /api/ibeacons/registry).
class RegistryBeacon {
  final int id;
  final String uuid;
  final int major;
  final int minor;
  final String? artifactName;
  final String? siteName;

  const RegistryBeacon({
    required this.id,
    required this.uuid,
    required this.major,
    required this.minor,
    this.artifactName,
    this.siteName,
  });

  factory RegistryBeacon.fromJson(Map<String, dynamic> json) {
    return RegistryBeacon(
      id: (json['id'] as num).toInt(),
      uuid: json['uuid'].toString().toUpperCase(),
      major: (json['major'] as num).toInt(),
      minor: (json['minor'] as num).toInt(),
      artifactName: json['artifactName']?.toString(),
      siteName: json['siteName']?.toString(),
    );
  }

  String get key => beaconKey(uuid, major, minor);
}

/// Một beacon vừa quét được từ BLE.
class ScannedBeacon {
  final String uuid;
  final int major;
  final int minor;
  final int rssi;

  /// Khoảng cách ước lượng (mét), tính từ RSSI và TX Power của beacon.
  final double accuracy;
  final DateTime lastSeen;

  const ScannedBeacon({
    required this.uuid,
    required this.major,
    required this.minor,
    required this.rssi,
    required this.accuracy,
    required this.lastSeen,
  });

  String get key => beaconKey(uuid, major, minor);
}

String beaconKey(String uuid, int major, int minor) {
  return '${uuid.toUpperCase()}-$major-$minor';
}

/// Quét iBeacon bằng flutter_blue_plus và tải registry từ Backend.
///
/// iBeacon là gói quảng bá BLE của Apple (company ID 0x004C) với
/// cấu trúc: 0x02 0x15 | UUID (16 byte) | Major (2) | Minor (2) | TX Power (1).
class BeaconService {
  BeaconService._();

  static const Duration _timeout = Duration(seconds: 10);
  static const int _appleCompanyId = 0x004C;
  static const int _iBeaconLength = 23;

  /// Lấy danh sách beacon ACTIVE từ Backend.
  static Future<List<RegistryBeacon>> fetchRegistry() async {
    final response = await http
        .get(Uri.parse('${AuthService.baseUrl}/api/ibeacons/registry'))
        .timeout(_timeout);
    final body = jsonDecode(utf8.decode(response.bodyBytes)) as Map<String, dynamic>;
    if (response.statusCode != 200) {
      throw Exception(body['message'] ?? 'Không tải được registry (mã ${response.statusCode}).');
    }
    final items = body['data'] as List<dynamic>;
    return items
        .map((item) => RegistryBeacon.fromJson(item as Map<String, dynamic>))
        .toList();
  }

  /// Xin quyền, kiểm tra thiết bị và bật Bluetooth nếu cần.
  static Future<void> initialize() async {
    if (!await FlutterBluePlus.isSupported) {
      throw Exception('Thiết bị không hỗ trợ Bluetooth Low Energy.');
    }

    final statuses = await [
      Permission.bluetoothScan,
      Permission.bluetoothConnect,
      Permission.locationWhenInUse,
    ].request();
    if (statuses.values.any((status) => !status.isGranted)) {
      throw Exception('Cần cấp quyền Bluetooth và Vị trí để quét beacon.');
    }

    if (await FlutterBluePlus.adapterState.first != BluetoothAdapterState.on) {
      await FlutterBluePlus.turnOn();
    }
    await FlutterBluePlus.adapterState
        .where((state) => state == BluetoothAdapterState.on)
        .first
        .timeout(
          const Duration(seconds: 10),
          onTimeout: () => throw Exception('Bluetooth chưa bật.'),
        );
  }

  /// Bắt đầu quét liên tục. Gọi [stopScan] khi thoát màn hình.
  static Future<void> startScan() {
    return FlutterBluePlus.startScan(
      continuousUpdates: true,
      androidUsesFineLocation: true,
    );
  }

  static Future<void> stopScan() => FlutterBluePlus.stopScan();

  /// Luồng beacon iBeacon đang thấy. Mỗi lần có kết quả quét mới sẽ phát danh sách hiện tại.
  static Stream<List<ScannedBeacon>> ranging() {
    return FlutterBluePlus.scanResults.map((results) {
      return results
          .map((result) => parseIBeacon(result.advertisementData.manufacturerData, result.rssi))
          .whereType<ScannedBeacon>()
          .toList();
    });
  }

  /// Phân tích gói iBeacon từ manufacturer data. Trả về null nếu không phải iBeacon.
  static ScannedBeacon? parseIBeacon(Map<int, List<int>> manufacturerData, int rssi) {
    final data = manufacturerData[_appleCompanyId];
    if (data == null || data.length < _iBeaconLength) return null;
    if (data[0] != 0x02 || data[1] != 0x15) return null;

    final hex = data
        .sublist(2, 18)
        .map((b) => b.toRadixString(16).padLeft(2, '0'))
        .join()
        .toUpperCase();
    final uuid = '${hex.substring(0, 8)}-${hex.substring(8, 12)}-'
        '${hex.substring(12, 16)}-${hex.substring(16, 20)}-${hex.substring(20)}';
    final major = (data[18] << 8) | data[19];
    final minor = (data[20] << 8) | data[21];
    final txPower = data[22] > 127 ? data[22] - 256 : data[22];

    final distance = math.pow(10, (txPower - rssi) / 20).toDouble();

    return ScannedBeacon(
      uuid: uuid,
      major: major,
      minor: minor,
      rssi: rssi,
      accuracy: distance,
      lastSeen: DateTime.now(),
    );
  }
}
