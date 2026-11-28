import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:smartheritage/features/beacon/beacon_service.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();
  final messenger =
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger;
  const permissions = MethodChannel('flutter.baseflow.com/permissions/methods');
  const commands = MethodChannel('smartheritage/beacon');
  const uuid = 'E2C56DB5-DFFB-48D2-B060-D0F5A71096E0';

  setUp(() => debugDefaultTargetPlatformOverride = TargetPlatform.iOS);
  tearDown(() {
    debugDefaultTargetPlatformOverride = null;
    messenger.setMockMethodCallHandler(permissions, null);
    messenger.setMockMethodCallHandler(commands, null);
  });

  test('iPhone asks for iOS location and Bluetooth separately', () async {
    final requested = <int>[];
    messenger.setMockMethodCallHandler(permissions, (call) async {
      if (call.method == 'checkServiceStatus') {
        expect(call.arguments, Permission.locationWhenInUse.value);
        return ServiceStatus.enabled.index;
      }
      expect(call.method, 'requestPermissions');
      final permission = (call.arguments as List<Object?>).single as int;
      requested.add(permission);
      return {
        permission: permission == Permission.bluetooth.value
            ? PermissionStatus.permanentlyDenied.index
            : PermissionStatus.granted.index,
      };
    });

    await expectLater(
      BeaconService.initialize(),
      throwsA(
        predicate<Object>((error) {
          final message = BeaconService.userMessage(error);
          return message.contains('Bluetooth') &&
              BeaconService.needsAppSettings(message);
        }),
      ),
    );
    expect(requested, [
      Permission.locationWhenInUse.value,
      Permission.bluetooth.value,
    ]);
  });

  test(
    'iPhone ranges each registered UUID once and stops via Core Location',
    () async {
      final calls = <MethodCall>[];
      messenger.setMockMethodCallHandler(commands, (call) async {
        calls.add(call);
        return null;
      });

      await BeaconService.startScan([
        const RegistryBeacon(id: 1, uuid: uuid, major: 1, minor: 1),
        RegistryBeacon(id: 2, uuid: uuid.toLowerCase(), major: 1, minor: 2),
      ]);
      await BeaconService.stopScan();

      expect(calls.map((call) => call.method), ['start', 'stop']);
      expect(calls.first.arguments, [uuid]);
    },
  );

  test('matches iOS readings to registry and ignores RSSI zero', () {
    final sample = <String, Object>{
      'uuid': uuid.toLowerCase(),
      'major': 1,
      'minor': 2,
      'rssi': -63,
      'accuracy': 1.4,
    };
    final beacon = ScannedBeacon.fromIosSample(sample);
    expect(beacon?.key, '$uuid-1-2');
    expect(beacon?.rssi, -63);
    expect(beacon?.accuracy, 1.4);
    expect(ScannedBeacon.fromIosSample({...sample, 'rssi': 0}), isNull);
  });

  test('precise location error offers to open settings', () {
    final message = BeaconService.userMessage(
      PlatformException(code: 'precise_location'),
    );
    expect(message, contains('Vị trí chính xác'));
    expect(BeaconService.needsAppSettings(message), isTrue);
  });
}
