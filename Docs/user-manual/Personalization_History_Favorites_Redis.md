# Smart Heritage — Personalization Integration

Phạm vi: tích hợp phần việc của Người 5 vào **Backend dùng chung** và Flutter hiện có. Không dùng module Spring Boot độc lập, không tạo `VisitorAccount` hoặc `Artifact` riêng.

## Ánh xạ vào project hiện tại

| Phần | Đường dẫn | Vai trò |
|---|---|---|
| Entity lượt xem | `Backend/src/main/java/com/smartheritage/backend/entity/VisitHistory.java` | Ánh xạ bảng `visits` hiện có; FK tới `User` và `Artifact`; `client_event_id` chống retry trùng |
| Entity yêu thích | `Backend/src/main/java/com/smartheritage/backend/entity/Favorite.java` + `FavoriteId.java` | Ánh xạ bảng `likes` hiện có bằng PK `(user_id, artifact_id)` |
| API Favorites | `Backend/src/main/java/com/smartheritage/backend/controller/FavoriteController.java` | GET, POST/PUT, DELETE, exists |
| API History | `Backend/src/main/java/com/smartheritage/backend/controller/VisitHistoryController.java` | GET phân trang, POST lượt xem, POST batch outbox |
| Dữ liệu cho analytics | `Backend/src/main/java/com/smartheritage/backend/controller/AdminVisitHistoryController.java` | GET `/api/admin/visits`; chỉ role ADMIN |
| Redis demo | `Backend/src/main/java/com/smartheritage/backend/service/RedisDemoService.java` + controller tương ứng | Ghi/đọc/xóa key có namespace và TTL |
| Flutter service + cache | `Interface/lib/features/personalization/personalization_service.dart` | HTTP client, SharedPreferences cache, retry outbox cục bộ |
| Màn yêu thích | `Interface/lib/features/favorites/favorites_screen.dart` | Danh sách, refresh, xóa yêu thích |
| Màn lịch sử | `Interface/lib/features/visit_history/visit_history_screen.dart` | Danh sách lượt xem, trạng thái chờ đồng bộ, refresh |

## API chính

| Method | Endpoint | Ý nghĩa |
|---|---|---|
| GET | `/api/me/favorites?page=0&size=20` | Lấy trang yêu thích của người hiện tại |
| POST | `/api/me/favorites/{artifactId}` | Thêm yêu thích, idempotent; PUT cũng được hỗ trợ tương thích |
| DELETE | `/api/me/favorites/{artifactId}` | Xóa yêu thích, gọi nhiều lần không tạo lỗi |
| GET | `/api/me/favorites/{artifactId}/exists` | Kiểm tra trạng thái yêu thích |
| GET | `/api/me/visits?page=0&size=20` | Lịch sử mới nhất trước |
| POST | `/api/me/visits` | Ghi một lượt xem |
| POST | `/api/me/visits/batch` | Đồng bộ tối đa 100 event, giữ nguyên `clientEventId` khi retry |
| GET | `/api/admin/visits?from=2026-10-01&to=2026-10-31&siteId=1` | Dữ liệu đọc cho analytics-service |
| POST | `/api/cache/demo?key=hello&value=Smart%20Heritage&ttlSeconds=600` | Redis demo, chỉ bật khi được cấu hình |
| GET | `/api/cache/demo/hello` | Đọc key demo |

Mọi response nghiệp vụ tiếp tục dùng wrapper `ApiResponse` hiện có. `User` và `Artifact` lấy từ package `com.smartheritage.backend.entity`; không tạo entity demo trùng.

## Cơ sở dữ liệu

- Bảng `visits` được dùng lại; thêm `client_event_id` nullable và `source` (`BEACON`, `MAP`, `MANUAL`) để retry idempotent và phân biệt nguồn lượt xem.
- Bảng `likes` được dùng lại; PK hiện tại `(user_id, artifact_id)` đã ngăn favorite trùng.
- Database mới nhận cấu trúc từ `Infra/init_db.sql`. Với volume MySQL đã tồn tại trước khi sửa, xem `Infra/personalization_migration.sql` và kiểm tra cột/unique index trước khi chạy migration. Không chạy ALTER lần hai.

## Danh tính người dùng — điều quan trọng

Backend hiện tại có login demo nhưng **chưa phát JWT hoặc đặt principal xác thực**. Code tích hợp ưu tiên user từ Spring Security khi principal đã có. Để kiểm thử UI cục bộ trước khi nhóm auth tích hợp JWT, có fallback header `X-User-Id` nhưng mặc định bị tắt. Chỉ bật bằng biến môi trường `PERSONALIZATION_ALLOW_DEV_USER_HEADER=true` khi chạy local; tuyệt đối không bật ở production. Khi auth-service phát JWT, các API phải dùng principal/token thật thay vì header phát triển.

## Redis demo

`RedisDemoController` được vô hiệu hóa mặc định. Bật bằng `REDIS_DEMO_ENABLED=true`. Redis endpoint ghi key dưới tiền tố `smartheritage:personalization-demo:`; TTL phải từ 1 đến 86400 giây. Không lưu token, mật khẩu hoặc thông tin nhạy cảm vào demo cache.

## Kiểm thử thủ công

1. Đăng nhập trên Flutter bằng user có sẵn trong MySQL seed; User ID hiện trong response login.
2. Tạm bật dev header cho local demo và mở một hiện vật khi đăng nhập: event xuất hiện trong `visits`.
3. Nhấn **Lưu yêu thích**; kiểm tra `likes` có đúng một dòng cho user/artifact.
4. Mở **Yêu thích** từ drawer; xóa một mục rồi refresh.
5. Mở **Lịch sử tham quan**; bật chế độ máy bay để kiểm tra dữ liệu cache và event chờ đồng bộ; bật mạng lại, nhấn refresh.
6. Gọi POST Redis demo, GET lại cùng key và xác nhận giá trị/TTL; tắt Redis để thấy lỗi kết nối được ghi nhận.
7. Kiểm tra dữ liệu user A không trộn với user B; `X-User-Id` chỉ dùng ở local demo cho tới khi JWT principal sẵn sàng.
