# Tìm hiểu quét BLE iBeacon và quyền trên Android / iOS

> Phần việc tuần 2 của Người 4 (beacon-service). Mục đích: chuẩn bị cho tuần 3 (code module quét BLE trên app Flutter).
> Tài liệu là **ghi chú tìm hiểu**, các lựa chọn thư viện ở mục 4 là **đề xuất, cần thử thực tế** trước khi chốt.

---

## 1. Gói tin iBeacon gồm những gì

iBeacon là chuẩn quảng bá (advertising) BLE của Apple. Thiết bị chỉ **phát**, không cần kết nối. Mỗi gói tin có:

| Trường | Kích thước | Ý nghĩa trong Smart Heritage |
|---|---|---|
| UUID | 16 byte | Định danh chung cho cả hệ thống (mọi beacon của dự án dùng chung 1 UUID) |
| Major | 2 byte (0–65535) | Khu vực, ví dụ 1 = Văn Miếu, 2 = Hoàng thành |
| Minor | 2 byte (0–65535) | Từng thiết bị trong khu vực |
| Measured Power (TX power) | 1 byte | Cường độ tín hiệu đo ở 1 m, dùng để ước lượng khoảng cách |

App đọc thêm **RSSI** (cường độ tín hiệu nhận được, đơn vị dBm, ví dụ -60 là gần, -90 là xa). Bộ `(UUID, Major, Minor)` khớp với ràng buộc `UNIQUE (uuid, major, minor)` của bảng `ibeacons`.

Tài liệu gốc: Apple – *Getting Started with iBeacon* (developer.apple.com/ibeacon).

---

## 2. Quyền trên Android

Quyền khai báo trong `Interface/android/app/src/main/AndroidManifest.xml` và phải **xin lúc chạy** (runtime permission).

| Phiên bản Android | Quyền cần có | Ghi chú |
|---|---|---|
| Android 12 trở lên (API 31+) | `BLUETOOTH_SCAN`, `ACCESS_FINE_LOCATION` | iBeacon được dùng để suy ra vị trí, nên **không** được gắn cờ `neverForLocation` cho `BLUETOOTH_SCAN`, và vẫn cần quyền vị trí |
| Android 11 trở xuống (API ≤ 30) | `BLUETOOTH`, `BLUETOOTH_ADMIN`, `ACCESS_FINE_LOCATION` | Khai báo `BLUETOOTH`, `BLUETOOTH_ADMIN` với `android:maxSdkVersion="30"` |
| Quét khi app chạy nền | thêm `ACCESS_BACKGROUND_LOCATION` | Phải xin **riêng, sau** khi đã có quyền vị trí lúc dùng app. Đồ án nên chỉ quét khi app đang mở |

Lưu ý thêm:

- Người dùng phải **bật Bluetooth và bật Vị trí (GPS)** trên máy, thiếu một trong hai thì không quét được dù đã cấp quyền.
- Android giới hạn số lần bắt đầu/dừng quét trong thời gian ngắn (khoảng 5 lần / 30 giây); bắt đầu – dừng liên tục sẽ bị hệ điều hành chặn.

Khai báo mẫu:

```xml
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.BLUETOOTH" android:maxSdkVersion="30" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" android:maxSdkVersion="30" />
```

Tài liệu gốc: Android Developers – *Bluetooth permissions* (developer.android.com/develop/connectivity/bluetooth/bt-permissions).

---

## 3. Quyền trên iOS

**Điểm quan trọng nhất:** trên iOS, gói tin iBeacon **bị lọc khỏi CoreBluetooth**. Thư viện BLE thông thường (quét bằng CoreBluetooth) sẽ **không thấy** iBeacon. Phải dùng **CoreLocation** (`CLBeaconRegion`, *beacon ranging*), và phải biết trước UUID cần tìm.

| Mục cần khai báo trong `ios/Runner/Info.plist` | Khi nào cần |
|---|---|
| `NSLocationWhenInUseUsageDescription` | Bắt buộc, để quét beacon khi app đang mở |
| `NSLocationAlwaysAndWhenInUseUsageDescription` | Chỉ khi cần phát hiện beacon lúc app chạy nền (region monitoring) |
| `NSBluetoothAlwaysUsageDescription` | Khi app dùng thêm CoreBluetooth (ví dụ kiểm tra Bluetooth đang bật/tắt) |

Repo hiện **chưa có thư mục `Interface/ios/`**, nên trước mắt demo trên Android; khi thêm iOS phải bổ sung các khai báo trên.

Tài liệu gốc: Apple – *Determining the proximity to an iBeacon device* (CoreLocation, developer.apple.com/documentation/corelocation).

---

## 4. Thư viện Flutter (đề xuất, cần thử)

| Lựa chọn | Ưu điểm | Nhược điểm |
|---|---|---|
| Thư viện chuyên iBeacon (ví dụ `flutter_beacon` hoặc các bản fork còn cập nhật) | Dùng CoreLocation trên iOS, trả sẵn UUID/Major/Minor/RSSI | Cần kiểm tra bản còn được bảo trì và chạy được với Flutter/Android hiện tại |
| `flutter_blue_plus` + tự tách gói tin | Phổ biến, cập nhật thường xuyên | **Không thấy iBeacon trên iOS**; trên Android phải tự đọc manufacturer data (Apple company ID `0x004C`, tiền tố `0x02 0x15`) |
| `permission_handler` | Xin quyền runtime cho cả Android và iOS | Chỉ lo phần quyền, phải dùng kèm thư viện quét |

Đề xuất cho tuần 3: thử thư viện chuyên iBeacon trên Android trước; nếu không chạy được thì dùng `flutter_blue_plus` (Android) để kịp mốc "đọc được beacon thật".

---

## 5. Không có beacon thật thì test thế nào

- Dùng 1 điện thoại Android khác cài app phát iBeacon giả lập (ví dụ *Beacon Simulator*), nhập đúng UUID của dự án `E2C56DB5-DFFB-48D2-B060-D0F5A71096E0` và Major/Minor trùng dữ liệu mẫu trong `init_db.sql` (1/1 = Khuê Văn Các, 1/2 = Bia Tiến sĩ, 2/1 = Đoan Môn).
- Lưu ý: điện thoại phát phải hỗ trợ BLE advertising; một số máy cũ không hỗ trợ.

---

## 6. Những gì cần nhóm chốt

1. Demo bằng beacon thật hay điện thoại giả lập.
2. Có cần quét khi app chạy nền không (ảnh hưởng quyền và độ phức tạp). Đề xuất: **không**, chỉ quét khi app mở.
3. Thư viện quét chính thức (sau khi thử ở tuần 3).
