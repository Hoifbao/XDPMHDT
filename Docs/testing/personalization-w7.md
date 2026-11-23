# Phú — kiểm chứng tuần 7

Jira: **XDPMHDT-99 — [Phú] Rớt mạng & độ tin cậy dữ liệu cá nhân**.

Ngày kiểm tra nền: 20/11/2026, commit `c6ec6c8f1e66158dd292bc7f7413e3144f15bbf0`. **Bổ sung CI 22/11/2026** trên nhánh PR #88; so sánh với `main` commit `31853e64d4cdbf6725fb7de4b7dfb88b8267d779`. Các kết quả được ghi rõ theo từng môi trường.
Đây là báo cáo bằng chứng và phần còn thiếu, không phải xác nhận nghiệm thu toàn bộ ứng dụng.

## Kết quả đã thực thi

| Hạng mục | Kết quả | Giới hạn |
|---|---|---|
| Java common | 19 test đạt, không lỗi/bỏ qua | Unit tests |
| Java personalization | 15 test đạt, không lỗi/bỏ qua | Có 7 test mới về fallback |
| Build parent/common/Gateway/personalization | Thành công | Đóng gói sau khi chạy test |
| API qua Gateway, MySQL 8.0.46 thật | 13 kiểm tra đạt | Content dùng HTTP fixture; Redis/RabbitMQ chủ động không chạy |
| Favorites khi Redis tắt | Thêm 2, thêm lại không trùng, đọc/xóa đạt | Không kiểm tra UI Flutter |
| Retry lượt tham quan | Gửi 3 UUID, bỏ phản hồi đầu, gửi lại: 0 mới, 3 trùng | Mô phỏng mất phản hồi ở HTTP, không phải mất sóng trên điện thoại |
| Content và Redis cùng tắt | Đọc favorites/history giữ dữ liệu cá nhân | Thiếu metadata dùng `Hiện vật #id`; thêm mới trả 503 để retry |
| JWT | Thiếu token bị 401; tài khoản khác không đọc được history | JWT kiểm thử tự ký; không kiểm tra màn hình đăng nhập |
| Swagger/OpenAPI/Prometheus | Endpoint phục vụ được | Không thay thế kiểm thử tương tác trình duyệt |
| Migration chỉ mục | Tạo lại từ trạng thái chưa có chỉ mục, chạy lại an toàn | DB thử riêng, không động DB demo |
| Flutter tests nhánh PR | **10/10 offline + 4 widget/grouping = 14/14 đạt** trên CI; full `flutter test` **74/74 đạt** ([run #44991384974](https://github.com/phuht0462-a11y/XDPMHDT/actions/runs/44991384974)) | Đạt trong CI Linux; **chưa** nghiệm thu Android Emulator/BLE thực tế |
| Flutter analyze | Đã chạy: **39 vấn đề** trên nhánh và **39** trên main đối chứng; exit code **1** | CI sử dụng `continue-on-error` cho bước analyze; **không ghi analyzer sạch** |
| Integration CI qua stack thật | **14/14 kiểm tra đạt** ([run #44991030284](https://github.com/phuht0462-a11y/XDPMHDT/actions/runs/44991030284)) | Auth, Content, Gateway, MySQL, Redis, RabbitMQ, Analytics đều thật **trong stack CI tạm**; không chứng minh test thao tác UI Emulator |

Bằng chứng thô: [Java](evidence/phu-w7-java-tests.json), [API cô lập](evidence/phu-w7-isolated-smoke.json), [chỉ mục](evidence/phu-w7-index-results.json).
Bằng chứng RabbitMQ/Analytics của run CI mới đã được kiểm tra riêng trong `smoke_live.py`: 3 UUID, không tăng Analytics khi retry, chống đọc chéo tài khoản, fallback Redis/Content. Các ảnh/test của lần chạy cũ không được cộng vào số liệu CI mới.

## Số đo chỉ mục

MySQL 8.0.46, dữ liệu tổng hợp 300.000 dòng, user mục tiêu 200.000 dòng; 3 lượt làm nóng, 20 lần đo mỗi truy vấn bằng `EXPLAIN ANALYZE`. Cùng tập dữ liệu và trang kết quả trước/sau.

| Truy vấn | Trước median / p95 (ms) | Sau median / p95 (ms) |
|---|---:|---:|
| History theo user, thời gian giảm dần, LIMIT 20 | 410 / 525 | 0,9445 / 1,32 |
| COUNT theo user | 51,8 / 79 | 39 / 41,8 |

Thêm `idx_visits_user_detected (user_id, detected_at)` giúp truy vấn trang dùng thứ tự chỉ mục. Đây là thời gian iterator truy vấn trên máy thử với cache đã làm nóng, **không phải** độ trễ API, hiệu năng thiết bị thật hoặc cam kết production. File JSON lưu đủ 20 mẫu và execution plan. COUNT vẫn cần xử lý nhiều dòng.

DB có dữ liệu: chạy SQL `Infra/personalization_w7_indexes.sql` bằng MySQL client đã đăng nhập, không reset/reseed. Schema mới cũng đã khai báo chỉ mục. Không dùng `docker compose down -v`.

## Tái lập

Tại thư mục gốc repository:

```powershell
mvn -pl services/personalization-service -am test
mvn -pl services/api-gateway,services/personalization-service -am -DskipTests package
cd Interface
flutter pub get
flutter test test/features/personalization/personalization_offline_test.dart
flutter test
flutter analyze
```

Java đã chạy bằng Temurin 17 và Maven 3.9.11. Môi trường kiểm thử không hỗ trợ Mockito tự attach nên preload `mockito-core-5.17.0.jar` bằng JVM `-javaagent`; không bỏ assertion. Máy khác hỗ trợ attach có thể chạy lệnh thông thường.

Harness Linux dùng Python standard library, MySQL distribution và Java; tạo DB tạm, dừng tiến trình và dọn DB thử khi kết thúc:

```bash
python services/personalization-service/scripts/smoke_isolated.py --java /path/to/java --mysql-home /path/to/mysql --output /tmp/phu-smoke.json
python services/personalization-service/scripts/benchmark_history.py --help
```

Benchmark mặc định dùng container `smart_heritage_mysql`, hỏi mật khẩu hoặc lấy `MYSQL_PWD` từ môi trường. Nó tạo schema ngẫu nhiên riêng rồi xóa đúng schema thử. Không ghi mật khẩu vào repository. Xem `--help` để chọn MySQL client/port và đường dẫn JSON.

## Thao tác trên app

1. Đăng nhập, mở danh sách hiện vật khi có mạng để có dữ liệu cục bộ.
2. Ngắt mạng; đi qua 3 beacon và đánh dấu 2 hiện vật yêu thích. Kiểm tra lịch sử/yêu thích được hiển thị cục bộ.
3. Bật mạng, giữ app ở foreground hoặc mở lại app. Bộ đồng bộ thử lại khi resume/đăng nhập và mỗi 30 giây lúc foreground. Không cam kết đồng bộ khi app đã bị đóng.
4. Khi có mạng nhưng còn lượt chưa gửi, thông báo phải là **“Có lượt tham quan đang chờ đồng bộ.”**; chỉ hiển thị ngoại tuyến khi thực sự mất mạng. Giữ sửa lỗi của nhóm trưởng, không phục hồi câu cũ.
5. Kiểm tra server có đúng 3 UUID, 2 yêu thích; retry không thêm lượt. Đổi tài khoản không thấy dữ liệu tài khoản trước.
6. Tắt Redis rồi lặp thao tác; khi Content cũng tắt, dữ liệu cá nhân vẫn đọc được, thêm mới chờ xác minh khi dịch vụ hồi phục.

## Các cổng nghiệm thu chưa đạt

- Flutter CI **đã chạy** và đạt **14/14** test chọn lọc, **74/74** toàn suite. `flutter analyze` đã chạy nhưng trả exit code 1 với **39 vấn đề**, bằng số vấn đề trên main đối chứng. Cần ghi nhận lint tồn tại, không gắn nhãn analyze pass. Kết quả trực tiếp trên máy Windows và Android Emulator chưa có.
- **Chưa kiểm thử Android Emulator/thiết bị**: 3 beacon giả lập + 2 yêu thích offline, ngắt mạng giữa lúc gửi, reconnect, khởi động lại và đổi tài khoản. Lưu video/log với UUID và kết quả server theo [biên bản emulator](personalization-w7-emulator-acceptance.md).
- CI đã chạy kiểm thử tích hợp với Content/Auth/RabbitMQ/Analytics thật trong stack riêng, đã thấy broker giao 3 visit tới Analytics và retry không tăng thêm; **vẫn cần** kiểm thử hồi quy trên môi trường demo nhóm, admin report và giao diện Flutter trên emulator. Chưa chứng minh chịu được mất broker sau commit hoặc đảm bảo delivery tuyệt đối.
- `publishAfterCommit` chưa có producer outbox: broker mất kết nối có thể mất sự kiện; phải phối hợp Bảo nếu yêu cầu đảm bảo giao sự kiện. Retry HTTP tuần tự đã kiểm chứng; không chứng nhận xử lý xung đột ghi đồng thời từ nhiều thiết bị.
- Bảo review phần wrapper trong `Interface/lib/main.dart` và đoạn SRS; không tự xác nhận thay nhóm trưởng.
- Mã Jira đã xác nhận: `XDPMHDT-99`; nhánh chính thức `fix/XDPMHDT-99-phu-w7-personalization`. PR giữ trạng thái Draft tới khi nghiệm thu emulator và review phần tích hợp dùng chung. Không tự chuyển Jira sang Done khi còn mục nghiệm thu chưa đạt.
- Chưa xóa được nhánh GitHub cũ `feature/personalization-history-favorites-redis`: thao tác Git từ môi trường thiếu xác thực ghi. Maintainer cần xác nhận PR63/64 đã merge và xóa đúng nhánh cũ; không xóa nhánh đang làm tuần 7.
- Diễn tập, phản biện và ký nghiệm thu là việc của nhóm trên môi trường demo; tài liệu không thay thế thao tác đó.

## Bàn giao

Nội dung SRS, phân biệt Redis/RabbitMQ/NiFi và câu hỏi bảo vệ ở [tài liệu yêu cầu](../requirements/personalization-w7.md). Sơ đồ ở `Diagram/personalization-service`. Đã giữ một mục Sprint 6 trong README. Các kết quả đạt có file bằng chứng; các mục chưa chạy phải tiếp tục trước khi tuyên bố hoàn thành 100%.
