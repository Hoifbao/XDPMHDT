import CoreLocation
import Flutter
import Foundation

final class IosBeaconRanger: NSObject, CLLocationManagerDelegate, FlutterStreamHandler {
  private lazy var locationManager: CLLocationManager = {
    let manager = CLLocationManager()
    manager.delegate = self
    return manager
  }()
  private var constraints: [UUID: CLBeaconIdentityConstraint] = [:]
  private var samples: [UUID: [[String: Any]]] = [:]
  private var pendingDelivery: DispatchWorkItem?
  private var pendingError: FlutterError?
  private var eventSink: FlutterEventSink?

  func register(with messenger: FlutterBinaryMessenger) {
    FlutterEventChannel(
      name: "smartheritage/beacon/ranging",
      binaryMessenger: messenger
    ).setStreamHandler(self)

    FlutterMethodChannel(
      name: "smartheritage/beacon",
      binaryMessenger: messenger
    ).setMethodCallHandler { [weak self] call, result in
      guard let self = self else {
        result(FlutterMethodNotImplemented)
        return
      }
      switch call.method {
      case "start":
        guard let values = call.arguments as? [String] else {
          result(FlutterError(code: "invalid_uuids", message: nil, details: nil))
          return
        }
        self.pendingError = nil
        result(self.updateConstraints(values))
      case "stop":
        self.stop()
        result(nil)
      default:
        result(FlutterMethodNotImplemented)
      }
    }
  }

  private func updateConstraints(_ values: [String]) -> FlutterError? {
    let requested = Set(values.compactMap { UUID(uuidString: $0) })
    if requested.isEmpty {
      stop()
      return nil
    }
    guard CLLocationManager.isRangingAvailable() else {
      return FlutterError(code: "ranging_unavailable", message: nil, details: nil)
    }
    if let error = permissionError(locationManager) {
      return error
    }

    for uuid in Set(constraints.keys).subtracting(requested) {
      if let constraint = constraints.removeValue(forKey: uuid) {
        locationManager.stopRangingBeacons(satisfying: constraint)
      }
      samples.removeValue(forKey: uuid)
    }
    for uuid in requested.subtracting(Set(constraints.keys)) {
      let constraint = CLBeaconIdentityConstraint(uuid: uuid)
      constraints[uuid] = constraint
      locationManager.startRangingBeacons(satisfying: constraint)
    }
    return nil
  }

  private func stop() {
    pendingDelivery?.cancel()
    pendingDelivery = nil
    pendingError = nil
    samples.removeAll()
    for constraint in constraints.values {
      locationManager.stopRangingBeacons(satisfying: constraint)
    }
    constraints.removeAll()
  }

  private func fail(_ error: FlutterError) {
    stop()
    if let eventSink = eventSink {
      eventSink(error)
    } else {
      pendingError = error
    }
  }

  private func permissionError(_ manager: CLLocationManager) -> FlutterError? {
    let authorization = manager.authorizationStatus
    guard authorization == .authorizedWhenInUse || authorization == .authorizedAlways else {
      return FlutterError(code: "location_permission", message: nil, details: nil)
    }
    guard manager.accuracyAuthorization == .fullAccuracy else {
      return FlutterError(code: "precise_location", message: nil, details: nil)
    }
    return nil
  }

  func onListen(
    withArguments arguments: Any?,
    eventSink events: @escaping FlutterEventSink
  ) -> FlutterError? {
    eventSink = events
    if let pendingError = pendingError {
      self.pendingError = nil
      events(pendingError)
    }
    return nil
  }

  func onCancel(withArguments arguments: Any?) -> FlutterError? {
    eventSink = nil
    return nil
  }

  func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
    guard !constraints.isEmpty, let error = permissionError(manager) else { return }
    fail(error)
  }

  func locationManager(
    _ manager: CLLocationManager,
    didRange beacons: [CLBeacon],
    satisfying beaconConstraint: CLBeaconIdentityConstraint
  ) {
    let uuid = beaconConstraint.uuid
    guard constraints[uuid] != nil else { return }
    var strongest: [String: CLBeacon] = [:]
    for beacon in beacons where beacon.rssi < 0 {
      let key = "\(beacon.major.intValue):\(beacon.minor.intValue)"
      if let previous = strongest[key], previous.rssi >= beacon.rssi {
        continue
      }
      strongest[key] = beacon
    }
    samples[uuid] = strongest.values.map { beacon in
      [
        "uuid": beacon.uuid.uuidString,
        "major": beacon.major.intValue,
        "minor": beacon.minor.intValue,
        "rssi": beacon.rssi,
        "accuracy": beacon.accuracy
      ]
    }

    // Core Location reports a batch for each UUID. Combine callbacks from the
    // same scan interval so Flutter can compare nearby beacons across UUIDs.
    guard pendingDelivery == nil else { return }
    let delivery = DispatchWorkItem { [weak self] in
      guard let self = self else { return }
      self.pendingDelivery = nil
      let readings = self.samples.values.flatMap { $0 }
      self.samples.removeAll()
      self.eventSink?(readings)
    }
    pendingDelivery = delivery
    DispatchQueue.main.asyncAfter(deadline: .now() + .milliseconds(180), execute: delivery)
  }

  func locationManager(
    _ manager: CLLocationManager,
    didFailRangingFor beaconConstraint: CLBeaconIdentityConstraint,
    error: Error
  ) {
    guard constraints[beaconConstraint.uuid] != nil else { return }
    let code = (error as? CLError)?.code == .denied ? "location_permission" : "ranging_failed"
    fail(FlutterError(code: code, message: error.localizedDescription, details: nil))
  }
}
