# Xuân Anh W5 — chi tiết hiện vật, ngôn ngữ và upload

## Khởi chạy

- Gateway: `8080`; content-service: `8083`; personalization-service: `8085`; feedback-service: `8086`.
- Gateway mặc định chuyển `/api/artifacts/**`, `/api/media/**`, `/api/languages` và `/api/admin/languages/**` tới content-service. Các route đánh giá vẫn ưu tiên feedback-service.
- Với Docker: `docker compose -f Infra/docker-compose.yml --profile core up -d --build`. File upload được giữ trong volume `content_uploads`.
- Với Maven: `mvn -pl services/content-service -am install -DskipTests`, sau đó chạy từng service. File upload nằm trong `./uploads` của thư mục chạy; đổi bằng `MEDIA_DIRECTORY`.
- Schema `content_db.languages` được Hibernate tạo khi khởi động với cấu hình `ddl-auto: update`; service bổ sung VI/EN nếu chưa có, không ghi đè trạng thái đã lưu. Có thể chạy riêng `Infra/db/08_languages.sql` cho cơ sở dữ liệu hiện hữu. Không cần xóa volume dữ liệu.
- Mở `WebAdmin/src/pages/artifacts.html` qua web server, đăng nhập ADMIN. Trang có quản lý ngôn ngữ, chọn bản dịch và tải file từ máy.
- Android emulator: `flutter run --dart-define=API_BASE_URL=http://10.0.2.2:8080`; máy thật dùng IP LAN của máy chạy Gateway.

## Hợp đồng API

| API | Quyền tại Gateway | Chức năng |
|---|---|---|
| `GET /api/artifacts/{id}?lang=vi` | Công khai | Chi tiết, bản dịch và các URL media |
| `GET /api/languages` | Công khai | Ngôn ngữ đang active |
| `GET /api/admin/languages` | ADMIN | Tất cả ngôn ngữ, kể cả đã tắt |
| `POST /api/admin/languages` | ADMIN | `{ "code": "fr", "name": "Français", "active": true }` |
| `PUT /api/admin/languages/{code}` | ADMIN | Sửa `name`, `active`; mã là định danh cố định |
| `POST /api/media/upload` | ADMIN | Multipart, field `file`; trả `fileUrl`, `mediaType`, `size` |
| `GET /api/media/files/{name}` | Công khai | Đọc file; hỗ trợ HTTP Range để tua audio/video |

Tiếng Việt luôn active để fallback. Ngôn ngữ đã đăng ký nhưng bị tắt hoặc chưa có bản dịch sẽ trả tiếng Việt. Mã ngôn ngữ chưa đăng ký trả HTTP 400. Cả `fallback` và `fallbackUsed` được trả để tương thích client cũ/mới. Tắt ngôn ngữ không xóa nội dung cũ.

Upload kiểm tra phần mở rộng và chữ ký đầu file, tạo tên UUID; không dùng tên do người dùng cung cấp để ghi lên máy chủ. Giới hạn: ảnh JPG/JPEG/PNG/WebP **10 MB**; audio MP3/WAV/OGG/M4A **30 MB**; video MP4/WebM **100 MB**. File rỗng, định dạng không hỗ trợ, giả định dạng hoặc vượt giới hạn bị từ chối. Multipart toàn request tối đa 101 MB. URL file được tạo từ origin Gateway đã chuyển tiếp; app dùng origin `API_BASE_URL` cho file do content-service quản lý, tương thích emulator và máy thật.

## Nghiệm thu trên app

1. Đăng nhập user có `preferredLanguage=en`; mở hiện vật có bản EN. Kiểm tra nội dung EN và request `?lang=en`. Đổi VI/EN: tiêu đề, mô tả và media tải lại. Đổi liên tiếp nhanh không được hiển thị kết quả request cũ.
2. Mở hiện vật chưa có EN: thấy nhãn “Chưa có bản dịch, đang hiển thị tiếng Việt”.
3. Trong Hình ảnh: lướt trái/phải, bấm ảnh và phóng to/thu nhỏ. URL ảnh hỏng phải hiển thị placeholder ở cả thư viện và trang phóng to.
4. Audio: phát/tạm dừng, kéo thanh tua, ±10 giây, hiển thị vị trí/thời lượng. URL hỏng hoặc lỗi phát phải hiển thị thông báo. Chuyển sang màn hình khác/đưa app xuống nền: audio tạm dừng.
5. Video: phát/tạm dừng, kéo tua, bật/tắt toàn màn hình. Rời màn video hoặc đưa app xuống nền phải dừng/tạm dừng. Ngắt mạng khi đang phát hoặc dùng URL hỏng: thông báo lỗi, không crash. Bấm Thử lại không để player cũ tiếp tục hoạt động.
6. Mở 20 hiện vật, mỗi hiện vật bằng cả VI/EN; mở hiện vật thứ 21: chỉ hiện vật ít được xem gần nhất bị loại cùng mọi bản ngôn ngữ. Mở lại bài offline cũng cập nhật thứ tự gần nhất.
7. Đóng/mở lại app, tắt mạng, chọn biểu tượng lịch sử trên trang chủ → “Hiện vật đã xem” → mở bài đã xem: đọc được nội dung và thấy banner offline. Bài chưa lưu hiển thị lỗi có nút thử lại. Cache này lưu nội dung bài và URL; không tải toàn bộ audio/video xuống máy. Ảnh đã tải dùng cache ảnh riêng.
8. Yêu thích: đăng nhập rồi thêm/xóa, kiểm tra danh sách của Phú và trạng thái sau khi mở lại. Khách được nhắc đăng nhập. Đánh giá: mở khu vực đánh giá của Tường đúng `artifactId`, gửi đánh giá thật và xem trạng thái chờ duyệt.
9. Từ luồng quét của Khoa, mở hiện vật theo `artifactId`; đi ra khỏi vùng beacon: màn chi tiết giữ nguyên, vẫn đọc/phát media được nếu còn mạng. Ra vùng không gọi thao tác đóng màn hình.

## Nghiệm thu CMS

1. Thêm FR, sửa tên, tắt/bật; dropdown bản dịch lấy từ API, không giới hạn cố định hai ngôn ngữ. VI không tắt được. Bản dịch đã có của ngôn ngữ bị tắt vẫn được giữ khi sửa hiện vật.
2. Chọn ảnh/audio/video thật ở trường tương ứng, chờ upload xong rồi Lưu hiện vật. Mở lại CMS và app, kiểm tra URL đã lưu có thể đọc được.
3. Thử file `.svg`/`.exe`, file đổi đuôi giả PNG, ảnh >10 MB/audio >30 MB/video >100 MB và file rỗng: báo lỗi, không thay URL đang có.
4. Khách/visitor gọi API quản trị hoặc upload qua Gateway phải nhận 401/403; API đọc media và ngôn ngữ active vẫn công khai.

## Kiểm tra tự động

```powershell
mvn -B -pl services/content-service,services/api-gateway -am test
cd Interface
flutter pub get
flutter test
flutter analyze --no-pub lib/core/artifact_service.dart lib/core/media_lifecycle.dart lib/screens/artifact_detail_screen.dart lib/screens/recent_artifacts_screen.dart test/core/artifact_cache_test.dart test/artifact_detail_screen_test.dart test/artifact_media_test.dart
flutter build apk --debug --no-pub
```

Nếu Kotlin báo `this and base files have different roots` trên Windows (Pub cache ở C, dự án ở D), dùng cho lần build đó:

```powershell
flutter build apk --debug --no-pub --android-project-arg=kotlin.incremental=false --android-project-arg=kotlin.compiler.execution.strategy=in-process
```

Kiểm tra CMS bằng Chrome headless, không cần dữ liệu thật:

```powershell
# Cài Playwright ngoài repository hoặc trong môi trường test riêng.
$env:CHROME_EXECUTABLE='C:/Program Files/Google/Chrome/Application/chrome.exe'
node Docs/testing/xa-w5-cms.browser.cjs
```

Script tự tạo web server và API giả lập, kiểm tra thêm ngôn ngữ thứ ba, tải ảnh/audio qua multipart, từ chối file sai định dạng, giữ URL cũ khi upload lỗi và lưu hiện vật không có lỗi JavaScript.

## Kết quả kiểm tra trong phiên hoàn thiện

- Java: **27 test đạt** (common 4, Gateway 13, content-service 10); không failure/error/skipped.
- Flutter: **44 test đạt**, gồm đọc offline, 20 ID duy nhất với nhiều ngôn ngữ, truy cập đồng thời, migration cache, preferredLanguage, VI/EN, fallback, URL audio lỗi, lỗi video khi đang phát, fullscreen, pause khi mở màn hình khác, retry/dispose, Yêu thích và truyền ID sang đánh giá thật.
- CMS Chrome headless: đạt kiểm tra ngôn ngữ động, ba bản dịch, multipart ảnh/audio, giữ URL khi upload lỗi, lưu dữ liệu và không có lỗi JavaScript.
- Android: build được APK debug bằng lệnh tắt Kotlin incremental ở trên. Máy hiện tại có cache Kotlin/Pub trên ổ C và repository trên ổ D.
- Chưa chạy nghiệm thu với beacon vật lý, âm thanh/codec trên thiết bị thật hoặc Docker daemon trong phiên này; máy không có lệnh Docker. Cấu hình Compose bổ sung volume lưu upload; không thay dữ liệu MySQL thật.

Test Java dùng H2 và thư mục upload tạm, không tác động dữ liệu MySQL thật. Test Flutter dùng API/cache giả lập và video platform giả lập để kiểm tra mất mạng, retry, pause/dispose, đổi ngôn ngữ và giới hạn cache. Các bước với beacon vật lý, codec và loa thiết bị cần nghiệm thu trên máy thật; test tự động không thay thế các bước đó.
