import 'package:flutter_test/flutter_test.dart';
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

  group('RegistryBeacon', () {
    test('fromJson chuẩn hóa UUID về chữ hoa và tạo key đúng', () {
      final beacon = RegistryBeacon.fromJson({
        'id': 1,
        'uuid': 'e2c56db5-dffb-48d2-b060-d0f5a71096e0',
        'major': 1,
        'minor': 2,
        'artifactName': 'Khuê Văn Các',
        'siteName': 'Văn Miếu',
      });

      expect(beacon.uuid, 'E2C56DB5-DFFB-48D2-B060-D0F5A71096E0');
      expect(beacon.artifactName, 'Khuê Văn Các');
      expect(beacon.key, 'E2C56DB5-DFFB-48D2-B060-D0F5A71096E0-1-2');
    });

    test('artifactName và siteName có thể null khi beacon chưa gán hiện vật', () {
      final beacon = RegistryBeacon.fromJson({
        'id': 4,
        'uuid': 'E2C56DB5-DFFB-48D2-B060-D0F5A71096E0',
        'major': 1,
        'minor': 3,
        'artifactName': null,
        'siteName': null,
      });

      expect(beacon.artifactName, isNull);
      expect(beacon.siteName, isNull);
    });
  });
}
