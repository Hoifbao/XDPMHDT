# Gắn FCM (Firebase Cloud Messaging) vào app Android — Khoa, Sprint 4

> Mục tiêu nghiệm thu: **Điện thoại nhận được 1 thông báo thử từ Firebase Console**.
> Phần này cần tài khoản Google cá nhân của bạn; tài liệu này là kịch bản các bước,
> thiêt lập đã ghi rõ từng file cần sửa trong nhánh.

## Nguyên tắc bảo mật của nhóm

1. **`google-services.json` KHÔNG commit** — `.gitignore` đã thêm dòng
   `**/google-services.json`; nếu lỡ `git add`, kiểm tra lại bằng `git status` trước khi commit.
2. Gửi `google-services.json` cho nhóm qua **kênh riêng** (Zalo/Drive), không paste lên chat công khai.
3. Token FCM là dữ liệu của từng thiết bị — chỉ in ra log để demo; chưa gửi lên Backend
   (để dành làm nâng cấp tuần 5, khi personalization-service đi qua Gateway).

## Bước 0. Chuẩn bị

- Máy cài Flutter (`flutter doctor` không có báo lỗi đỏ).
- 1 điện thoại Android thật (chính là máy chạy plugin quét BLE tuần 3).
- `flutter doctor` kiểm tra trước khi bắt đầu.

## Bước 1. Tạo Firebase project

1. Đăng nhập [console.firebase.google.com](https://console.firebase.google.com) bằng tài khoản Google cá nhân.
2. **Add project** → tên gợi ý `smart-heritage-khoa` → tắt Google Analytics (không cần cho demo) → **Create project**.

## Bước 2. Thêm app Android vào project

Trong Firebase Console — bấm icon **Android** ở trang chủ:

| Trường | Giá trị | Ghi chú |
|---|---|---|
| Android package name | `com.example.smartheritage` | Phải **trùng `applicationId`** trong `Interface/android/app/build.gradle.kts` (hiện tại vẫn là `com.example.smartheritage`). Sau này nhóm đổi lại thành package riêng thì phải tạo lại app trong Console + tải lại file json. |
| App nickname | Smart Heritage (tuỳ) | Không bắt buộc |
| Debug signing certificate SHA-1 | bỏ trống | Chỉ cần khi dùng login Google/Phone; demo FCM không cần |

Sau đó tải xuống **`google-services.json`** và đặt vào đúng:

```
Interface/android/app/google-services.json    (KHÔNG commit)
```

## Bước 3. Cấu hình Flutter

### 3.1. Dùng `flutterfire` CLI sinh `firebase_options.dart`

```powershell
cd Interface
dart pub global activate flutterfire_cli
flutterfire configure --project=<firebase-project-id> --platforms=android
```

CLI sẽ tự viết `lib/firebase_options.dart` và tự thêm `firebase_core` vào `pubspec.yaml`;
nó cũng tải lại `google-services.json` vào `android/app/` (vẫn không commit được do `.gitignore`).

### 3.2. Thêm `firebase_messaging` vào `pubspec.yaml`

```yaml
dependencies:
  firebase_core: ^3.15.2      # cập nhật theo `flutter pub outdated` nếu CLI đã thêm sẵn
  firebase_messaging: ^15.3.0
```

Sau đó: `flutter pub get` rồi chạy `flutter analyze` kiểm tra.

> ⚠️ Không commit `Interface/pubspec.lock` (quy định nhóm; file này đã bị tracked trước đó —
> khi mở PR, đề nghị cùng Bảo quyết: `git rm --cached Interface/pubspec.lock`).
> ⚠️ AGP của nhánh hiện tại là **9.1.0** (`android/settings.gradle.kts`) — nếu plugin
> Google Services xung đột với AGP 9 thì nâng phiên bản plugin và ghi rõ vào PR.

### 3.3. Bật plugin Google Services trên Android (Kotlin DSL)

`Interface/android/settings.gradle.kts` — thêm 1 dòng vào khối `plugins`:

```kotlin
plugins {
    id("dev.flutter.flutter-plugin-loader") version "1.0.0"
    id("com.android.application") version "9.1.0" apply false
    id("org.jetbrains.kotlin.android") version "2.4.0" apply false
    id("com.google.gms.google-services") version "4.4.14" apply false
}
```

`Interface/android/app/build.gradle.kts` — thêm plugin, giữ nguyên các plugin hiện có:

```kotlin
plugins {
    id("com.android.application")
    id("dev.flutter.flutter-gradle-plugin")
    id("com.google.gms.google-services")    // Kotlin DSL: version đã khai báo ở settings.gradle.kts
}
```

## Bước 4. Code: Firebase init + in token + nhận thông báo foreground

Sửa `Interface/lib/main.dart` (giữ nguyên phần app hiện tại):

```dart
// 1. import trên đầu file:
import 'package:firebase_core/firebase_core.dart';
import 'package:firebase_messaging/firebase_messaging.dart';
import 'firebase_options.dart';

// 2. Handler thông báo khi app chạy nền (buộc là hàm top-level, không nằm trong class):
@pragma('vm:entry-point')
Future<void> _firebaseMessagingBackgroundHandler(RemoteMessage message) async {
  await Firebase.initializeApp(options: DefaultFirebaseOptions.currentPlatform);
  debugPrint('[FCM] background: ${message.notification?.title} — ${message.notification?.body}');
}

// 3. Đổi `void main()` thành `Future<void> main() async` và thêm setup trước runApp:
Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await Firebase.initializeApp(options: DefaultFirebaseOptions.currentPlatform);

  final messaging = FirebaseMessaging.instance;
  await messaging.requestPermission(alert: true, badge: true, sound: true);

  final token = await messaging.getToken();
  debugPrint('[FCM] TOKEN = $token');   // copy token này để gửi thông báo thử ở bước 5

  FirebaseMessaging.onMessage.listen((message) {
    debugPrint('[FCM] foreground: ${message.notification?.title}');
  });
  // Tuần 5: gửi token này về Backend (beacon-service) để notification chủ động

  runApp(<runApp hiện tại của app, không đổi>);
}
```

> Sau bước 4 chạy lại `flutter analyze` — điều này báo nếu thiếu `@pragma('vm:entry-point')`
> hoặc import không dùng. Nếu main.dart đã bị Xuân Anh sửa trong PR W5 đến sớm,
> chỉ cần áp cùng các dòng setup này vào main() mới của nhánh đó.

## Bước 5. Test nhận thông báo

1. `flutter run` trên điện thoại thật — copy dòng log `[FCM] TOKEN = ...`.
2. Firebase Console → **Engage → Messaging → Create first campaign → Firebase Notification messages**:
   - Title: `Thông báo thử Smart Heritage`
   - Body: `App của Khoa nhận được thông báo từ Firebase Console`
   - Target: app `com.example.smartheritage` → **Send test message** → dán token.
3. Lưu 2 ảnh vào `Docs/research/firebase-demo/`:
   - `console-send.png` — Console sau bấm send.
   - `phone-notification.png` — thông báo trên điện thoại.
   Đây là minh chứng cho mục "Demo Firebase trên lớp".

## Tiêu chí nghiệm thu (đối chiếu task_w4_chi_tiet.docx mục 4)

- [ ] Điện thoại nhận được thông báo thử từ Firebase Console.
- [ ] Người reviewer nhìn ảnh Console + ảnh điện thoại là xác nhận được flow.
- [ ] `google-services.json` không có trong PR (kiểm tra `git status`).

## Recovery nhanh

Nếu chỉ riêng dòng plugin `com.google.gms.google-services` trong `build.gradle.kts`
gây lỗi build không rõ nguyên nhân, comment dòng đó lại là app build tiếp được;
ghi rõ trong PR để Bảo xem cùng (không tự ý đổi phiên bản AGP của nhóm).
