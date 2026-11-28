import 'package:flutter/foundation.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:smartheritage/features/beacon/beacon_service.dart';

const _uuidBytes = [
  0xE2, 0xC5, 0x6D, 0xB5, 0xDF, 0xFB, 0x48, 0xD2,
  0xB0, 0x60, 0xD0, 0xF5, 0xA7, 0x10, 0x96, 0xE0,
];

/// Tạo manufacturer data iBeacon: 0x02 0x15 | UUID | Major | Minor | TX Power.
List<int> iBeaconPayload({
  List<int> uuid = _uuidBytes,
  int major = 1,
  int minor = 1,
  int txPower = -59,
}) {
  return [
    0x02,
    0x15,
    ...uuid,
    (major >> 8) & 0xFF,
    major & 0xFF,
    (minor >> 8) & 0xFF,
    minor & 0xFF,
    txPower & 0xFF,
  ];
}

void main() {
  group('BeaconService.parseIBeacon', () {
    test('đọc đúng UUID, Major, Minor và RSSI', () {
      final beacon = BeaconService.parseIBeacon(
        {0x004C: iBeaconPayload(major: 2, minor: 300)},
        -70,
      );

      expect(beacon, isNotNull);
      expect(beacon!.uuid, 'E2C56DB5-DFFB-48D2-B060-D0F5A71096E0');
      expect(beacon.major, 2);
      expect(beacon.minor, 300);
      expect(beacon.rssi, -70);
    });

    test('ước lượng khoảng cách bằng 1 m khi RSSI bằng TX Power', () {
      final beacon = BeaconService.parseIBeacon(
        {0x004C: iBeaconPayload(txPower: -59)},
        -59,
      );

      expect(beacon!.accuracy, closeTo(1.0, 1e-9));
    });

    test('khoảng cách tăng khi RSSI yếu hơn', () {
      final near = BeaconService.parseIBeacon({0x004C: iBeaconPayload()}, -59)!;
      final far = BeaconService.parseIBeacon({0x004C: iBeaconPayload()}, -79)!;

      expect(far.accuracy, greaterThan(near.accuracy));
    });

    test('TX Power âm được giải mã đúng (0xC5 = -59)', () {
      final beacon = BeaconService.parseIBeacon(
        {0x004C: iBeaconPayload(txPower: -59)},
        -59,
      );

      expect(beacon!.accuracy, closeTo(1.0, 1e-9));
    });

    test('bỏ qua manufacturer data không phải của Apple', () {
      final beacon = BeaconService.parseIBeacon(
        {0x0059: iBeaconPayload()},
        -60,
      );

      expect(beacon, isNull);
    });

    test('bỏ qua gói Apple không phải iBeacon (prefix sai)', () {
      final payload = iBeaconPayload();
      payload[0] = 0x07;

      expect(BeaconService.parseIBeacon({0x004C: payload}, -60), isNull);
    });

    test('bỏ qua gói quá ngắn', () {
      expect(
        BeaconService.parseIBeacon({0x004C: [0x02, 0x15, 0x01]}, -60),
        isNull,
      );
    });

    test('bỏ qua khi không có manufacturer data', () {
      expect(BeaconService.parseIBeacon({}, -60), isNull);
    });
  });

  group('BeaconService.needsAppSettings (lỗi 14)', () {
    test('thiếu quyền thì cần mở Cài đặt', () {
      final message = BeaconService.userMessage(Exception(BeaconService.permissionDeniedMessage));
      expect(BeaconService.needsAppSettings(message), isTrue);
      expect(message, contains('Cài đặt'));
    });

    test('lỗi khác (GPS tắt, null) thì không', () {
      expect(BeaconService.needsAppSettings('Hãy bật Định vị (GPS) để quét beacon.'), isFalse);
      expect(BeaconService.needsAppSettings(null), isFalse);
    });
  });

  group('BeaconService.userMessage', () {
    test('bỏ tiền tố "Exception: " khỏi thông báo lỗi', () {
      expect(
        BeaconService.userMessage(Exception('Hãy bật Định vị (GPS) để quét beacon.')),
        'Hãy bật Định vị (GPS) để quét beacon.',
      );
    });

    test('giữ nguyên lỗi không phải Exception', () {
      expect(BeaconService.userMessage(StateError('boom')), 'Bad state: boom');
    });
  });

  group('BeaconSwitchGuard (chống nhảy qua lại)', () {
    test('không đổi khi beacon khác chỉ mạnh hơn dưới 5 dBm', () {
      final guard = BeaconSwitchGuard();
      for (var i = 0; i < 6; i++) {
        final next = guard.update(
          currentKey: 'A',
          averages: {'A': -70, 'B': -66},
        );
        expect(next, isNull);
      }
    });

    test('đổi sau đúng 3 lần đọc liên tiếp đạt ngưỡng 5 dBm', () {
      final guard = BeaconSwitchGuard();
      final averages = {'A': -72, 'B': -65};

      expect(guard.update(currentKey: 'A', averages: averages), isNull);
      expect(guard.update(currentKey: 'A', averages: averages), isNull);
      expect(guard.update(currentKey: 'A', averages: averages), 'B');
    });

    test('một lần đọc không đạt làm đếm lại từ đầu', () {
      final guard = BeaconSwitchGuard();

      guard.update(currentKey: 'A', averages: {'A': -72, 'B': -65});
      guard.update(currentKey: 'A', averages: {'A': -72, 'B': -65});
      guard.update(currentKey: 'A', averages: {'A': -72, 'B': -70});
      expect(guard.update(currentKey: 'A', averages: {'A': -72, 'B': -65}), isNull);
      expect(guard.update(currentKey: 'A', averages: {'A': -72, 'B': -65}), isNull);
      expect(guard.update(currentKey: 'A', averages: {'A': -72, 'B': -65}), 'B');
    });

    test('beacon vắng mặt trong lượt quét không làm mất streak', () {
      final guard = BeaconSwitchGuard();

      guard.update(currentKey: 'A', averages: {'A': -72, 'B': -65});
      guard.update(currentKey: 'A', averages: {'A': -72});
      guard.update(currentKey: 'A', averages: {'A': -72, 'B': -65});
      expect(guard.update(currentKey: 'A', averages: {'A': -72, 'B': -65}), 'B');
    });

    test('khi có nhiều ứng viên đạt ngưỡng, chọn beacon mạnh nhất', () {
      final guard = BeaconSwitchGuard();
      final averages = {'A': -80, 'B': -70, 'C': -68};

      guard.update(currentKey: 'A', averages: averages);
      guard.update(currentKey: 'A', averages: averages);
      expect(guard.update(currentKey: 'A', averages: averages), 'C');
    });

    test('không đổi khi beacon hiện tại không có trong lượt quét', () {
      final guard = BeaconSwitchGuard();

      expect(guard.update(currentKey: 'A', averages: {'B': -60}), isNull);
    });

    test('không đổi sang beacon chưa đạt ngưỡng vào vùng', () {
      final guard = BeaconSwitchGuard();
      // B mạnh hơn A 10 dBm nhưng -80 < rssiEnter (-75) → không đổi
      for (var i = 0; i < 4; i++) {
        expect(
          guard.update(currentKey: 'A', averages: {'A': -90, 'B': -80}),
          isNull,
        );
      }
    });

    test('reset xóa streak đang đếm', () {
      final guard = BeaconSwitchGuard();
      final averages = {'A': -72, 'B': -65};

      guard.update(currentKey: 'A', averages: averages);
      guard.update(currentKey: 'A', averages: averages);
      guard.reset();
      expect(guard.update(currentKey: 'A', averages: averages), isNull);
    });
  });

  group('RegistryBeacon', () {
    test('fromJson chuẩn hóa UUID về chữ hoa và tạo key đúng', () {
      final beacon = RegistryBeacon.fromJson({
        'id': 1,
        'uuid': 'e2c56db5-dffb-48d2-b060-d0f5a71096e0',
        'major': 1,
        'minor': 2,
        'artifactId': 1,
        'artifactName': 'Khuê Văn Các',
        'siteName': 'Văn Miếu',
      });

      expect(beacon.uuid, 'E2C56DB5-DFFB-48D2-B060-D0F5A71096E0');
      expect(beacon.artifactId, 1);
      expect(beacon.artifactName, 'Khuê Văn Các');
      expect(beacon.key, 'E2C56DB5-DFFB-48D2-B060-D0F5A71096E0-1-2');
    });

    test('artifactName và siteName có thể null khi beacon chưa gán hiện vật', () {
      final beacon = RegistryBeacon.fromJson({
        'id': 4,
        'uuid': 'E2C56DB5-DFFB-48D2-B060-D0F5A71096E0',
        'major': 1,
        'minor': 3,
        'artifactId': null,
        'artifactName': null,
        'siteName': null,
      });

      expect(beacon.artifactId, isNull);
      expect(beacon.artifactName, isNull);
      expect(beacon.siteName, isNull);
    });
  });

  group('requiredPermissions', () {
    test('iOS chi xin Bluetooth + Vi tri (khong co quyen rieng cua Android)', () {
      expect(
        BeaconService.requiredPermissions(TargetPlatform.iOS),
        [Permission.bluetooth, Permission.locationWhenInUse],
      );
    });

    test('Android xin bluetoothScan, bluetoothConnect va Vi tri', () {
      expect(
        BeaconService.requiredPermissions(TargetPlatform.android),
        [
          Permission.bluetoothScan,
          Permission.bluetoothConnect,
          Permission.locationWhenInUse,
        ],
      );
    });
  });
}
