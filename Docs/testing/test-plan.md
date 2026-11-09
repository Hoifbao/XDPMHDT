# Test Plan – Smart Heritage (bản 1.0)

| Mục | Nội dung |
|---|---|
| Phiên bản | 1.0 (Sprint 6, dùng cho System Testing lần 1) |
| Người lập | Tường (đầu mối QA) |
| Ngày | 09/11/2026 (bản 0.2: 01/11/2026; bản 0.1: 30/10/2026) |
| Căn cứ | User story US01–US24 trong `readme.md`; `task_w4_chi_tiet` mục III; `task_w5_chi_tiet` mục III (route Gateway, luồng quét beacon, NiFi); `task_w6_chi_tiet` mục II, III, V (sự kiện RabbitMQ, mức độ lỗi) và tiêu chí nghiệm thu của từng thành viên |
| Trạng thái | Đã chốt để Ân chạy System Testing lần 1; test case có việc chưa xong ghi ở mục 10 |

**Thay đổi so với bản 0.2**

- Thêm 45 test case tuần 6 (TC-87 → TC-131): 5 sự kiện RabbitMQ, FCM, pin beacon, thống kê thời gian thực,
  xuất báo cáo, hoàn thiện Mobile/Web, cảnh báo, load test, trường hợp đặc biệt, phân quyền và smoke test bản chốt.
- TC-86 (xuất báo cáo) chuyển vào nhóm tuần 6, không còn là test case đặt trước.
- Mức độ lỗi đổi sang Blocker, Critical, Major, Minor kèm hạn sửa theo `task_w6_chi_tiet` mục V.
- Thêm mục 3.1: cách xem sự kiện trên RabbitMQ UI.

**Thay đổi của bản 0.2 so với bản 0.1**

- Thêm 35 test case tuần 5 (TC-51 → TC-85): luồng quét beacon, ngoại tuyến, bản đồ, chi tiết hiện vật đa phương tiện,
  đánh giá trên app và Web, phân quyền route mới, Swagger/giám sát tại Gateway, NiFi, smoke test.
- Thêm TC-86 (US17, xuất báo cáo PDF/Excel) đặt trước cho tuần 6 để ma trận truy vết phủ đủ 24 user story.
- TC-17, TC-34 → TC-36, TC-47 chuyển sang chạy qua Gateway 8080 và cập nhật theo feedback-service tuần 5
  (lọc theo hiện vật, "Ẩn" = REJECTED, gửi lại thì ghi đè).
- Ma trận truy vết (mục 9) phủ đủ US01–US24 (US17 chạy ở tuần 6).
- Thêm mục 10: test case phụ thuộc việc chưa xong của tuần 5.

## 1. Mục tiêu

- Kiểm tra các chức năng đã làm ở tuần 1–4 vẫn chạy đúng khi chuyển sang kiến trúc microservice
  (client gọi qua Gateway `http://localhost:8080`).
- Giữ nguyên kết quả phân quyền 401/403 của tuần 3, bổ sung luật cho route mới tuần 5.
- Tuần 5: kiểm tra 7 service chạy sau Gateway, luồng du khách (đăng nhập → quét beacon → xem hiện vật → yêu thích
  → đánh giá), luồng Admin (duyệt đánh giá, bản đồ beacon, dashboard) và đồng bộ NiFi sang `analytics_db`.
- Đo 2 yêu cầu hiệu năng của Đề tài: nhận diện hiện vật ≤ 2 giây, tạo Dashboard ≤ 5 giây.
- Tuần 6: kiểm tra toàn luồng du khách trên Mobile, luồng Admin trên Web, sự kiện RabbitMQ giữa các service,
  cảnh báo giám sát, và đối chiếu yêu cầu 500 người dùng đồng thời, p95 ≤ 2 giây.
- Làm danh sách test case nền để viết Test Report.

## 2. Phạm vi

**Trong phạm vi**

| Nhóm | Chức năng |
|---|---|
| Visitor (app Flutter, API) | Đăng ký/đăng nhập, hồ sơ và ngôn ngữ, bản đồ di tích + chỉ đường, quét iBeacon tự hiện hiện vật, chi tiết hiện vật đa ngôn ngữ (ảnh, audio, video), xem lại offline, yêu thích, lịch sử tham quan, hàng đợi ngoại tuyến, gửi/sửa đánh giá, "Đánh giá của tôi", thông báo khi đánh giá được duyệt |
| Administrator (Web Admin, API) | Đăng nhập, CRUD di tích/hiện vật, media, bản dịch, danh sách ngôn ngữ, quản lý và gán iBeacon, bản đồ vị trí beacon, duyệt/ẩn đánh giá, số đánh giá chờ duyệt trên menu, dashboard thống kê, xuất báo cáo PDF/Excel, cảnh báo pin beacon |
| System | JWT + RBAC tại Gateway, chống giả mạo header, định tuyến Gateway, Swagger gom tại Gateway, fallback ngôn ngữ, chống báo trùng beacon, đồng bộ offline không trùng, cô lập database theo service, giám sát Prometheus/Grafana, xử lý khi service phụ thuộc bị tắt, luồng NiFi, Redis cache, thông báo FCM, sự kiện RabbitMQ và dead-letter queue, cảnh báo Grafana, kiểm thử tải k6 |

**Ngoài phạm vi bản 1.0**

- iOS (thư viện iBeacon chỉ demo trên Android).
- Quét beacon khi app chạy nền lâu (Android giới hạn); demo chính với app đang mở.
- Kiểm thử bảo mật chuyên sâu.

## 3. Môi trường

| Thành phần | Địa chỉ / lệnh |
|---|---|
| MySQL 8 + Redis 7 | `docker compose -f Infra/docker-compose.yml up -d` (MySQL cổng 3307, Redis 6379). Dữ liệu sạch: `down -v` rồi `up -d` |
| Cả hệ thống | `--profile core up -d --build` (Backend cũ, Gateway, auth/site/content/feedback-service) hoặc `Infra\dev\start-local.ps1` |
| api-gateway | `http://localhost:8080` (client chỉ gọi vào đây); Swagger chung `http://localhost:8080/swagger-ui.html` |
| auth / site / content / beacon | 8081 / 8082 / 8083 / 8084 (Swagger ở cổng riêng: `/swagger-ui.html`) |
| personalization / feedback / analytics | 8085 / 8086 / 8087 (tách ở tuần 5, route qua Gateway) |
| Backend cũ (monolith) | `http://localhost:8090`, schema `smart_heritage_db`. Service chưa tách thì Gateway chuyển về đây |
| RabbitMQ (bắt buộc tuần 6) | `--profile mq`: AMQP 5672, giao diện `http://localhost:15672` (`smartheritage` / `change-me-rabbit`, nếu `Infra/.env` không đổi) |
| Monitoring (tuỳ chọn) | `--profile monitoring`: Prometheus 9090, Grafana 3000, mysqld-exporter, redis-exporter |
| NiFi (tuỳ chọn) | `--profile nifi`: `https://localhost:8443/nifi` |
| Web Admin | Mở `WebAdmin/src/pages/login.html` bằng Live Server |
| Mobile | Android thật (máy ảo không quét được BLE) + điện thoại thứ 2 chạy Beacon Simulator; không có beacon thì dùng nút "Giả lập phát hiện beacon". Điện thoại thật: `--dart-define=API_BASE_URL=http://<IP máy>:8080`, cùng Wi-Fi |
| Unit test | `mvn test` (từng service), `flutter test` (thư mục `Interface`) |

**Tài khoản và dữ liệu mẫu** (`Infra/db`, mật khẩu `Test@1234`):

| Tài khoản | Vai trò |
|---|---|
| `admin` | ADMIN |
| `visitor_an` (id 2) | VISITOR |
| `visitor_mai` (id 3) | VISITOR |
| `visitor_john` (id 4, ngôn ngữ en) | VISITOR |
| `visitor1` / `123456` | VISITOR, tự đăng ký |

- Hiện vật: 1 Khuê Văn Các, 2 Bia Tiến sĩ, 3 Đoan Môn. Hiện vật 3 chỉ có bản tiếng Việt.
- Beacon UUID `E2C56DB5-DFFB-48D2-B060-D0F5A71096E0`: 1/1, 1/2, 2/1 đang ACTIVE; 1/3 là INACTIVE.
- Đánh giá (`feedback_db`): APPROVED id 1 (hiện vật 1, 5★), 2 (hiện vật 2, 4★), 4 (hiện vật 3, 5★); PENDING id 3, 5;
  REJECTED id 6. Điểm TB đúng (chỉ APPROVED): hiện vật 1 = 5.0, hiện vật 2 = 4.0, hiện vật 3 = 5.0.

### 3.1 Cách xem sự kiện trên RabbitMQ UI

Sự kiện chỉ giữ lại khi có queue bind vào exchange, nên trước khi chạy TC-87 → TC-100:

1. Mở `http://localhost:15672`, vào **Queues and Streams** → **Add a new queue**, tên `qa.events`, bấm **Add queue**.
2. Vào **Exchanges** → `sh.events` → mục **Add binding from this exchange**: To queue `qa.events`, Routing key `#`, bấm **Bind**.
3. Sau mỗi bước test, vào queue `qa.events` → **Get messages** (Ack Mode: *Automatic ack*, Messages: 10) để xem nội dung JSON.
4. Test xong xóa queue `qa.events` để không tích tin.

Exchange `sh.events` được tạo ở lần gửi sự kiện đầu tiên. Chưa thấy exchange thì gửi 1 đánh giá mới trước.
Mỗi tin phải có đủ 5 trường: `eventId`, `type`, `version`, `occurredAt`, `data`.

## 4. Vai trò

| Người | Trách nhiệm |
|---|---|
| Tường | Lập và duy trì Test Plan, tổng hợp kết quả, quản lý danh sách lỗi; test Feedback và Database |
| Ân | Chạy System Testing lần 1 trên Mobile và Web, tạo Issue, ghi kết quả vào `Docs/testing/test-report.md` |
| Thành viên phụ trách module | Chạy test case của module mình trước khi mở PR, ghi kết quả vào PR |
| Bảo | Test tích hợp qua Gateway (Web Admin + Mobile), xác nhận 401/403 |
| Người review PR | Chạy lại ít nhất các test case ưu tiên Cao của module được review |

## 5. Cách tiếp cận

| Mức | Cách làm | Công cụ |
|---|---|---|
| Unit | Test service/logic, mock repository và client | JUnit 5, Mockito (`mvn test`), `flutter test` |
| API | Gọi qua Gateway 8080, kiểm tra mã HTTP và JSON `{success, message, data}` | Swagger UI, Postman, curl |
| Giao diện | Thao tác trên Web Admin và app theo bước của test case | Trình duyệt, điện thoại Android |
| Tích hợp | Luồng nhiều service: đăng nhập → gọi API ở service khác; tắt 1 service để xem xử lý lỗi | docker compose, Gateway |
| Database | Kiểm tra schema, quyền user, dữ liệu mẫu sau `down -v` → `up -d` | MySQL CLI trong container |

## 6. Tiêu chí bắt đầu và kết thúc

**Bắt đầu test khi:**

- Code đã ở trên nhánh và build được: `mvn -q -DskipTests install`; `flutter analyze` không có error.
- Database tạo mới từ `Infra/db` không lỗi.
- Gateway, auth-service và service cần test đều chạy; `/actuator/health` trả `UP`.
- Có dữ liệu mẫu đúng như mục 3.

**Kết thúc vòng test khi:**

- Đã chạy ít nhất 90% tổng số test case, trong đó 100% test case ưu tiên Cao và ít nhất 90% số đó đạt.
- Không còn lỗi Blocker hoặc Critical đang mở.
- Lỗi Major/Minor còn mở đều đã có Issue, mức độ và người phụ trách.
- Kết quả đã ghi vào `Docs/testing/test-report.md` (Đạt / Không đạt / Chưa chạy) và gửi nhóm.

## 7. Mẫu báo lỗi

Tạo GitHub Issue với tiêu đề `[BUG][<module>] <mô tả ngắn>`, nội dung:

```
Mã lỗi        : BUG-<số>
Test case     : TC-xx
Mức độ        : Blocker | Critical | Major | Minor
Service       : tên service liên quan
Môi trường    : nhánh/commit, Gateway hay cổng riêng, thiết bị (nếu là Mobile)
Bước tái hiện : 1. ... 2. ... 3. ...
Dữ liệu       : tài khoản, id, body request
Kết quả mong đợi :
Kết quả thực tế  : (mã HTTP, JSON, log)
Ảnh/video        :
Người phụ trách  :
```

| Mức độ | Ý nghĩa | Hạn sửa |
|---|---|---|
| Blocker | Chặn luồng demo chính (không đăng nhập được, quét beacon không mở hiện vật, Gateway lỗi) | Trong 24 giờ |
| Critical | Sai dữ liệu, lỗi phân quyền, mất dữ liệu | Trước Thứ 3 tuần 7 |
| Major | Chức năng chạy nhưng sai một phần, giao diện vỡ trên màn hình phổ biến | Trước Thứ 5 tuần 7 |
| Minor | Lỗi chữ, căn lề, thông báo chưa rõ | Nếu còn thời gian |

Sau 18:00 Thứ 6 13/11 chỉ merge PR sửa lỗi có Issue, mô tả PR ghi `Fixes #<số Issue>`.

## 8. Danh sách test case

Ưu tiên: C = Cao, TB = Trung bình, T = Thấp. Mọi API gọi qua `http://localhost:8080`, trừ khi ghi cổng khác.

### 8.1 Visitor

| Mã TC | US | Tiêu đề | Tiền điều kiện | Bước | Dữ liệu | Kết quả mong đợi | Ưu tiên |
|---|---|---|---|---|---|---|---|
| TC-01 | US01 | Đăng ký tài khoản mới | Chưa có username `visitor1` | `POST /api/auth/register` | `{"username":"visitor1","email":"v1@example.com","password":"123456","fullName":"Visitor Một"}` | 201/200, tài khoản role VISITOR; đăng ký lại cùng username → 400 | C |
| TC-02 | US01 | Đăng nhập đúng | Có `visitor_an` | `POST /api/auth/login` | `visitor_an` / `Test@1234` | 200, có `accessToken`, `refreshToken`, `tokenType = Bearer` | C |
| TC-03 | US01 | Đăng nhập sai mật khẩu | Có `visitor_an` | `POST /api/auth/login` | `visitor_an` / `sai123` | 401 (hoặc 400) kèm thông báo; không trả token | C |
| TC-04 | US01 | Làm mới token và đăng xuất | Đã đăng nhập (TC-02) | 1. `POST /api/auth/refresh` với refreshToken 2. `POST /api/auth/logout` 3. Gọi lại refresh với token cũ | refreshToken của TC-02 | Bước 1 trả accessToken mới; bước 3 → 401 (token đã thu hồi) | TB |
| TC-05 | US02 | Xem hồ sơ của mình | Đã đăng nhập `visitor_john` | `GET /api/auth/me` kèm Bearer | — | 200, `fullName = John Smith`, `preferredLanguage = en`; không có `passwordHash` | TB |
| TC-06 | US02 | Không xem được hồ sơ người khác | Đăng nhập `visitor_an` (id 2) | `GET /api/auth/users/3` | — | 403 | C |
| TC-07 | US03 | Di tích gần nhất sắp xếp gần → xa | Có 2 di tích mẫu | `GET /api/heritage-sites/nearby?lat=21.0278&lng=105.8355&radius=5` | Toạ độ Văn Miếu | 200; Văn Miếu đứng đầu, `distanceKm` tăng dần, làm tròn 2 chữ số | C |
| TC-08 | US03 | Nearby với tham số sai | — | `GET /api/heritage-sites/nearby?lat=95&lng=105` | lat ngoài [-90, 90] | 400 kèm thông báo | TB |
| TC-09 | US03 | Bản đồ di tích trên app | App đã đăng nhập | Mở màn hình bản đồ | — | Hiện marker 2 di tích, bấm marker xem tên và địa chỉ | TB |
| TC-10 | US04, US18 | Quét beacon trên Android | Bluetooth bật, đã cấp quyền; Beacon Simulator phát 1/1 | Mở màn hình "Quét beacon", đứng gần rồi đi xa | UUID dự án, Major 1, Minor 1 | Hiện UUID/Major/Minor/RSSI, cập nhật khoảng 1 giây/lần; RSSI thay đổi khi di chuyển | C |
| TC-11 | US04 | Từ chối quyền hoặc tắt Bluetooth | Thu hồi quyền vị trí/Bluetooth | Mở màn hình quét | — | App hiện hướng dẫn mở Cài đặt, không crash | C |
| TC-12 | US05, US23 | Chi tiết hiện vật tiếng Anh | Hiện vật 1 có bản `en` | `GET /api/artifacts/1?lang=en` | — | 200, nội dung tiếng Anh "Khue Van Cac", `fallbackUsed`/`fallback = false`, có media | C |
| TC-13 | US05 | Lấy hiện vật theo beacon | Beacon 1/1 gán hiện vật 1 | `GET /api/artifacts/beacon/{uuid}?lang=vi` | UUID dự án | 200, đúng hiện vật Khuê Văn Các | C |
| TC-14 | US06 | Xem lại nội dung khi rời vùng beacon | Đã mở chi tiết hiện vật 1 | Tắt Beacon Simulator / bật chế độ máy bay | — | Màn hình chi tiết vẫn hiển thị nội dung đã tải, không bị đóng | TB |
| TC-15 | US07 | Thêm và bỏ yêu thích | Đăng nhập `visitor_an` | 1. `POST /api/me/favorites/2` 2. `GET /api/me/favorites` 3. `DELETE /api/me/favorites/2` | artifactId 2 | Bước 2 có hiện vật 2 kèm tên; bước 3 xong thì danh sách không còn hiện vật 2; thêm trùng không tạo 2 dòng | C |
| TC-16 | US07 | Xem lịch sử tham quan | Đăng nhập `visitor_an` | `GET /api/me/visits` | — | 200, có lượt tham quan hiện vật 1 và 2 của user 2, mới nhất trước; không thấy dữ liệu của user khác | C |
| TC-17 | US08 | Gửi đánh giá hợp lệ | Đăng nhập `visitor_mai` | `POST /api/artifacts/2/feedbacks` | `{"rating":4,"comment":"Rất hay"}` | 201, `status = PENDING`, `reviewerName = Trần Thị Mai`; chưa hiện trong danh sách công khai. `visitor_mai` đã có đánh giá id 5 cho hiện vật 2 nên lần này trả **200** và ghi đè id 5 (xem TC-67); muốn thấy 201 thì dùng hiện vật 3 | C |
| TC-18 | US08 | Đánh giá với số sao sai | Đăng nhập visitor | POST như TC-17 | `rating` = 4.5, 0, 6, thiếu | 400, thông báo rõ lỗi rating; không lưu | C |
| TC-19 | US08 | Bình luận quá dài | Đăng nhập visitor | POST như TC-17 | `comment` 501 ký tự | 400 "Bình luận tối đa 500 ký tự" | TB |
| TC-20 | US08 | Đánh giá hiện vật không tồn tại | Đăng nhập visitor | `POST /api/artifacts/99/feedbacks` | `{"rating":5}` | 404 | TB |
| TC-21 | US08 | Xem đánh giá đã duyệt | — (không cần đăng nhập) | `GET /api/artifacts/1/feedbacks` | — | 200, chỉ có đánh giá APPROVED (id 1), mới nhất trước, không lộ userId/email | C |

### 8.2 Administrator

| Mã TC | US | Tiêu đề | Tiền điều kiện | Bước | Dữ liệu | Kết quả mong đợi | Ưu tiên |
|---|---|---|---|---|---|---|---|
| TC-22 | US09 | Đăng nhập Web Admin | Có `admin` | Mở `login.html`, nhập tài khoản, bấm Đăng nhập | `admin` / `Test@1234` | Vào được dashboard; token lưu trong trình duyệt; gọi API qua 8080 | C |
| TC-23 | US09 | Visitor không vào được trang quản trị | Có `visitor_an` | Đăng nhập Web Admin bằng visitor rồi thao tác thêm dữ liệu | — | API ghi trả 403; giao diện báo không đủ quyền | C |
| TC-24 | US10 | Thêm di tích bằng bản đồ | Đăng nhập admin | Mở `heritage-sites.html`, bấm Thêm, bấm lên bản đồ, nhập tên, Lưu | Tên "Chùa Một Cột" | Ô latitude/longitude tự điền; lưu thành công; tải lại trang vẫn còn (DB thật) | C |
| TC-25 | US10 | Sửa, xóa di tích | Có di tích TC-24 | Sửa tên, kéo marker; sau đó Xóa | — | Thay đổi lưu vào DB; xóa xong không còn trong danh sách | TB |
| TC-26 | US10 | Tạo hiện vật với siteId không tồn tại | Đăng nhập admin | `POST /api/artifacts` | `siteId = 999` | 400 "di tích không tồn tại" | C |
| TC-27 | US10 | CRUD hiện vật trên CMS | Đăng nhập admin | Tạo, sửa, xóa hiện vật trên `artifacts.html`; dùng ô tìm kiếm và lọc theo di tích | Hiện vật mới thuộc di tích 1 | Mỗi thao tác lưu thật; tìm kiếm và lọc ra đúng kết quả | C |
| TC-28 | US11 | Gắn link media | Có hiện vật 1 | `POST /api/artifacts/1/media` rồi `GET /api/artifacts/1/media` | `{"mediaType":"IMAGE","fileUrl":"https://example.com/a.jpg","caption":"Ảnh"}` | Media mới xuất hiện trong danh sách và trong chi tiết hiện vật | TB |
| TC-29 | US14 | Nhập bản dịch tiếng Anh | Hiện vật 3 chỉ có `vi` | Thêm bản `en` trên CMS (`POST /api/artifacts/3/contents`) | title "Doan Mon" | `GET /api/artifacts/3?lang=en` trả tiếng Anh, `fallback = false` | C |
| TC-30 | US12 | Đăng ký beacon mới | Đăng nhập admin | `POST /api/ibeacons` | UUID dự án, Major 2, Minor 2, site 2 | 201/200; tạo trùng (uuid, major, minor) → 400 | TB |
| TC-31 | US12 | Gán beacon cho hiện vật cùng di tích | Beacon 4 (site 1) chưa gán | Gán beacon 4 → hiện vật 2 trên `ibeacons.html` (`POST /api/ibeacons/4/assign`) | `{"artifactId":2}` | Thành công; cột "hiện vật đang gán" cập nhật ngay; bỏ gán bằng `artifactId = null` | C |
| TC-32 | US12 | Gán beacon cho hiện vật khác di tích | Beacon 4 thuộc site 1 | `POST /api/ibeacons/4/assign` | `{"artifactId":3}` (site 2) | 400 | C |
| TC-33 | US13 | Danh sách beacon có trạng thái và pin | Đăng nhập admin | Mở `ibeacons.html`, lọc theo di tích 1 | — | Hiện cột trạng thái, % pin; beacon 4 hiển thị INACTIVE, pin 15% | T |
| TC-34 | US15 | Danh sách đánh giá, lọc trạng thái và hiện vật | Đăng nhập admin; feedback-service 8086 | `GET /api/admin/feedbacks?status=PENDING` rồi `?artifactId=1` rồi `?status=PENDING&artifactId=1` qua 8080 (token admin) | — | 200; lần lượt: id 3, 5 / id 1, 3, 6 / id 3. `status=XYZ` hoặc `artifactId=0` → 400; token visitor → 403 | C |
| TC-35 | US15 | Duyệt / ẩn đánh giá | Có đánh giá PENDING (TC-17) | `PATCH` (hoặc `PUT`) `/api/admin/feedbacks/{id}/status` qua 8080 | `{"status":"APPROVED"}`, sau đó `{"status":"REJECTED"}` (= Ẩn) | 200; APPROVED thì hiện trong TC-21, REJECTED thì biến khỏi TC-21; `updatedAt` đổi; gửi `PENDING` → 400; id không tồn tại → 404 | C |
| TC-36 | US08, US16 | Điểm trung bình hiện vật | Đánh giá mẫu | `GET /api/artifacts/2/rating-summary` qua 8080 | — | `averageRating = 4.0`, `totalRatings = 1`, chỉ tính APPROVED (đối chiếu tay mục 3); sau khi duyệt/ẩn ở TC-35 thì số liệu cập nhật | C |
| TC-37 | US16 | Dashboard thống kê | Đăng nhập admin | Mở `dashboard.html` (`GET /api/statistics/overview`) | — | Hiện tổng lượt tham quan, hiện vật nổi bật, đánh giá gần đây khớp dữ liệu mẫu | TB |

### 8.3 System

| Mã TC | US | Tiêu đề | Tiền điều kiện | Bước | Dữ liệu | Kết quả mong đợi | Ưu tiên |
|---|---|---|---|---|---|---|---|
| TC-38 | US24 | Ghi dữ liệu không có token | Gateway chạy | `POST /api/heritage-sites`, `PUT /api/artifacts/1`, `DELETE /api/ibeacons/1` không có header Authorization | — | Cả 3 trả 401 tại Gateway, JSON `{success:false}` | C |
| TC-39 | US24 | Visitor gọi API của ADMIN | Token `visitor_an` | `POST /api/heritage-sites`, `GET /api/statistics/overview`, `GET /api/admin/visits` | — | 403 | C |
| TC-40 | US24 | Token hết hạn hoặc sửa chữ ký | Token đã sửa 1 ký tự | `GET /api/me/favorites` | — | 401 | C |
| TC-41 | US24 | Chống giả mạo header X-User-* | Token `visitor_an` (id 2) | `GET /api/me/favorites` kèm thêm header `X-User-Id: 3`, `X-User-Role: ADMIN` | — | Trả yêu thích của user 2 (Gateway xóa header giả, gắn lại từ token); không có quyền ADMIN | C |
| TC-42 | US23 | Fallback ngôn ngữ | Hiện vật 3 không có bản `en`, `fr` | `GET /api/artifacts/3?lang=fr` | — | Trả nội dung tiếng Việt, cờ fallback = true | C |
| TC-43 | US21, US22 | Đồng bộ offline không ghi trùng | Đăng nhập `visitor_an` | Gửi 2 lần `POST /api/me/visits/batch` cùng `clientEventId` | `{"events":[{"artifactId":2,"source":"BEACON","clientEventId":"0b8e6f3a-6a1d-4c8e-9f2b-3d4e5f6a7b8c"}]}` | Lần 1 `created = 1`; lần 2 `created = 0`, `duplicates = 1`; DB chỉ có 1 dòng, `duration_seconds` = NULL | C |
| TC-44 | US24 | Mỗi service một schema | Volume mới (`down -v` → `up -d`) | `SHOW DATABASES`; đếm dòng mẫu; đăng nhập MySQL bằng `site_user` đọc `content_db.artifacts` | `site_user` / `site_pass_dev` | Đủ 7 schema + `smart_heritage_db`; số dòng khớp mẫu; câu lệnh đọc schema khác → ERROR 1142 | C |
| TC-45 | US21 | Id dữ liệu thống nhất giữa các schema | Như TC-44 | So sánh `artifact_id` ở beacon_db, personalization_db, feedback_db với `content_db.artifacts` | — | Không có id nào không tồn tại ở schema gốc | TB |
| TC-46 | — | Gọi service phụ thuộc khi nó bị tắt | Tắt site-service (hoặc content/Backend cũ với feedback-service) | Tạo hiện vật mới; gửi đánh giá trên 8086 | — | 503 kèm thông báo rõ, trong khoảng 2 giây; service gọi không bị treo hay sập | C |
| TC-47 | — | Định tuyến Gateway | Gateway + các service đã tách | Gọi `/api/heritage-sites`, `/api/artifacts/1`, `/api/ibeacons/registry`, `/api/artifacts/1/feedbacks`, `/api/artifacts/1/rating-summary`, `/api/admin/feedbacks` qua 8080 | — | Mỗi đường dẫn tới đúng service (xem log), đặc biệt `/feedbacks` và `/rating-summary` không bị chuyển nhầm sang content-service, `/api/admin/feedbacks` không về Backend cũ (bảng đầy đủ: TC-75) | C |
| TC-48 | — | Giám sát | Service đang chạy | `GET /actuator/prometheus` trên 8080, 8081 và mỗi service đã tách | — | 200, có số liệu `http_server_requests_*` | TB |
| TC-49 | US22 | NiFi chuyển visits sang analytics_db | `--profile nifi`, flow của Tuấn | Chạy flow 2 lần | 4 dòng visits mẫu | `analytics_db.fact_visits` có đủ 4 dòng, lần 2 không nhân đôi | TB |
| TC-50 | US19, US20 | Không thông báo lặp, thông báo khi sang beacon khác | App quét beacon (tuần 5) | Đứng yên ở beacon 1/1 30 giây, rồi chuyển sang 1/2 | — | Chỉ 1 thông báo cho 1/1; có thông báo mới khi vào vùng 1/2 | TB |

### 8.4 Tuần 5 – Visitor (Mobile)

Tên người trong ngoặc là người phụ trách tính năng, chạy test case trước khi mở PR.

| Mã TC | US | Tiêu đề | Tiền điều kiện | Bước | Dữ liệu | Kết quả mong đợi | Ưu tiên |
|---|---|---|---|---|---|---|---|
| TC-51 | US03 | Bản đồ hiện di tích gần nhất (Ân) | Đã cấp quyền vị trí; máy ảo đặt vị trí giả lập | Mở màn hình Bản đồ; bấm 1 marker; bấm "Xem hiện vật" | Toạ độ Văn Miếu 21.0278, 105.8355 | Hiện vị trí người dùng và marker từ `GET /api/heritage-sites/nearby`; bảng thông tin có tên, khoảng cách; danh sách hiện vật lọc đúng `siteId` | C |
| TC-52 | US03 | Chỉ đường (Ân) | Như TC-51 | Bấm marker → "Chỉ đường" | — | Mở Google Maps / ứng dụng bản đồ với đúng toạ độ đích | C |
| TC-53 | US03 | Từ chối quyền vị trí, mất mạng (Ân) | Thu hồi quyền vị trí; lần 2 bật chế độ máy bay | Mở Bản đồ; gõ tên di tích vào ô tìm kiếm | "Văn Miếu" | Vẫn hiện bản đồ và danh sách di tích, có thông báo hướng dẫn; mất mạng thì báo lỗi, không crash; tìm kiếm ra đúng di tích | TB |
| TC-54 | US05, US23 | Đổi ngôn ngữ trên màn chi tiết (Xuân Anh) | Hiện vật 1 có `vi`/`en`; hiện vật 3 chỉ có `vi` | Mở chi tiết hiện vật 1, bấm VI/EN; mở hiện vật 3 với EN | — | Tên, mô tả, audio đổi theo trong ≤ 2 giây; hiện vật 3 có nhãn "Chưa có bản dịch, đang hiển thị tiếng Việt" (`fallbackUsed = true`) | C |
| TC-55 | US05 | Ảnh, audio, video (Xuân Anh) | Hiện vật có bộ media mẫu thật (ảnh, mp3, mp4 ngắn) | Lướt và phóng to ảnh; phát/tạm dừng/tua audio; phát video, toàn màn hình, rời màn hình | 1 URL ảnh hỏng | Ảnh hỏng hiện ảnh thay thế; audio hiện thời lượng, tua được; video toàn màn hình được và dừng khi rời màn hình; URL lỗi/mất mạng → thông báo, không crash | C |
| TC-56 | US06 | Rời vùng beacon vẫn xem tiếp (Xuân Anh, Khoa) | Mở chi tiết từ luồng quét, audio đang phát | Tắt Beacon Simulator, chờ > 10 giây | — | Màn hình không tự đóng, audio vẫn phát | C |
| TC-57 | US06, US21 | Mở lại hiện vật đã xem khi mất mạng (Xuân Anh) | Đã xem hiện vật 1, 2 khi có mạng | Bật chế độ máy bay; mở lại hiện vật 1; mở 1 hiện vật chưa xem | — | Hiện vật đã xem mở được từ bộ nhớ máy (lưu tối đa 20 hiện vật gần nhất); hiện vật chưa xem báo mất mạng, không crash | TB |
| TC-58 | US04, US18 | Tự hiện hiện vật ≤ 2 giây (Khoa) | Registry đã tải khi mở app; Beacon Simulator phát 1/1 | Đi từ xa lại gần beacon; đo từ lúc vào vùng (TB RSSI 5 lần ≥ −75 dBm) đến lúc mở chi tiết; lặp 10 lần | Beacon 1/1 → hiện vật 1 | Mở đúng hiện vật 1; cả 10 lần ≤ 2 giây, ghi vào `Docs/research/ble-performance.md`; beacon không có trong registry bị bỏ qua | C |
| TC-59 | US19 | Không báo trùng (Khoa) | Như TC-58 | Đứng yên trong vùng 1/1 2 phút; sau đó ra vào liên tục trong 60 giây | — | Chỉ báo/mở chi tiết 1 lần; trong 60 giây không báo lại | C |
| TC-60 | US20 | Đi từ beacon A sang B (Khoa) | 2 beacon 1/1 và 1/2 cách nhau ≥ 3 m | Đứng ở 1/1 rồi đi sang 1/2 | — | Báo hiện vật của 1/2; không nhảy qua lại giữa A và B (ngưỡng ra −85 dBm) | C |
| TC-61 | US04 | Nút "Giả lập phát hiện beacon" (Khoa) | Bản debug hoặc bật trong cài đặt; không có beacon thật | Chọn beacon 1/2 trong registry | — | Chạy đủ luồng như beacon thật: mở chi tiết, chống báo trùng, "ra vùng" thì có lượt tham quan | C |
| TC-62 | US22 | Ghi lượt tham quan khi ra vùng (Khoa, Phú) | Đăng nhập `visitor_an`; ở trong vùng 1/1 khoảng 30 giây | Rời vùng, chờ > 10 giây; `GET /api/me/visits` | — | Có lượt mới `source = BEACON`, `durationSeconds` ≈ thời gian ở trong vùng, `clientEventId` là UUID | C |
| TC-63 | US21, US22 | Hàng đợi ngoại tuyến (Phú) | Đăng nhập `visitor_an` | Tắt mạng, đi qua 3 beacon (hoặc giả lập 3 lần); bật mạng lại; `GET /api/me/visits` | — | App gửi `POST /api/me/visits/batch`; đủ 3 lượt mới, không trùng; gửi lại hàng đợi lần nữa thì DB không thêm dòng | C |
| TC-64 | US07 | Yêu thích, Lịch sử trên app qua Gateway (Phú) | Đăng nhập `visitor_an`; app trỏ 8080 | Bấm Yêu thích trên màn chi tiết; mở màn Yêu thích, Lịch sử | — | Nút đổi trạng thái ngay; 2 màn hình hiện dữ liệu thật qua 8080; đăng xuất thì không xem được | C |
| TC-65 | US08 | Gửi đánh giá từ app (Tường) | Đăng nhập `visitor1` (chưa đánh giá hiện vật 1) | Chi tiết hiện vật 1 → "Đánh giá" → chọn 4 sao, nhập bình luận → Gửi | 4★, "Thuyết minh rõ ràng" | `POST` trả 201; app hiện "Đã gửi đánh giá", trạng thái Chờ duyệt; bình luận chưa có trong danh sách công khai; bình luận > 500 ký tự không nhập được | C |
| TC-66 | US08 | Chưa đăng nhập thì yêu cầu đăng nhập (Tường) | Đã đăng xuất | Mở màn Đánh giá của hiện vật 1; bấm "Đăng nhập để đánh giá", đăng nhập, quay lại | — | Vẫn xem được điểm TB và bình luận đã duyệt; thay form là nút đăng nhập, không có request POST; đăng nhập xong quay lại thì hiện form | C |
| TC-67 | US08 | Chặn spam: gửi lại = sửa đánh giá cũ (Tường) | Sau TC-65, Admin đã duyệt đánh giá đó | Gửi lại cho hiện vật 1 với 2 sao | 2★ | Trả 200, app hiện "Đã cập nhật đánh giá"; DB vẫn 1 dòng cho (user, hiện vật), `id` và `created_at` giữ nguyên, `status` về PENDING, `updated_at` đổi; đánh giá biến khỏi danh sách công khai tới khi duyệt lại | C |
| TC-68 | US08, US15 | Điểm TB và bình luận trên app khớp dữ liệu (Tường) | Dữ liệu mẫu (mục 3) | Mở màn Đánh giá của hiện vật 1, 2, 3; Admin ẩn id 1 (TC-70) rồi kéo xuống để tải lại | — | Điểm TB và số lượt theo mức sao đúng mục 3; chỉ hiện bình luận APPROVED; sau khi ẩn id 1, hiện vật 1 không còn bình luận đó và điểm TB đổi | C |
| TC-69 | US20 | Thông báo FCM (Khoa) | Firebase service account đặt ngoài repo (biến môi trường); app đã đăng nhập | App gửi `POST /api/me/devices` (FCM token); gọi API gửi FCM cho user đó | — | Token lưu theo userId; điện thoại nhận được thông báo; không có token đăng nhập → 401 | TB |

### 8.5 Tuần 5 – Administrator (Web)

| Mã TC | US | Tiêu đề | Tiền điều kiện | Bước | Dữ liệu | Kết quả mong đợi | Ưu tiên |
|---|---|---|---|---|---|---|---|
| TC-70 | US15 | Duyệt / ẩn đánh giá trên Web (Tường) | Đăng nhập `admin` trên Web Admin | Mở `feedback.html`; lọc "Chờ duyệt"; lọc hiện vật "Khuê Văn Các"; bấm Xem id 3 → Duyệt; bấm Ẩn id 1; tải lại trang | — | Huy hiệu "Dữ liệu từ API"; lọc đúng như TC-34; modal hiện đủ thông tin; sau khi tải lại id 3 "Đã duyệt", id 1 "Đã ẩn"; app (TC-68) không còn id 1. Tắt feedback-service thì trang báo lỗi, không hiện dữ liệu giả | C |
| TC-71 | US13 | Bản đồ vị trí beacon (Ân, Khoa) | Đăng nhập `admin`; di tích 1 có beacon | Mở bản đồ beacon, chọn di tích 1; kéo marker 1 beacon sang chỗ khác; tải lại trang; bấm marker | — | Thấy mọi beacon của di tích, màu theo trạng thái (ACTIVE, INACTIVE, pin yếu); kéo thả gọi `PUT /api/ibeacons/{id}/position`; tải lại vẫn đúng vị trí mới; bấm marker xem thông tin và hiện vật được gán | C |
| TC-72 | US16 | Dashboard số liệu thật (Tuấn) | Đăng nhập `admin`; NiFi đã đồng bộ | Mở `dashboard.html` qua 8080; lọc theo khoảng ngày, theo di tích; đo thời gian tải | Dữ liệu mẫu | Số liệu khớp tính tay từ dữ liệu nguồn (bảng đối chiếu trong PR của Tuấn); tải xong ≤ 5 giây | C |
| TC-73 | US14 | Quản lý danh sách ngôn ngữ (Xuân Anh, P1) | Đăng nhập `admin` | Thêm/tắt ngôn ngữ trong bảng `languages`; mở dropdown ngôn ngữ trong CMS | `fr`, Français | Dropdown chỉ hiện ngôn ngữ đang active | T |
| TC-74 | US11 | Upload file media (Xuân Anh, P2) | Đăng nhập `admin` | Upload ảnh/audio thật cho hiện vật 1; thử file quá dung lượng, sai định dạng | 1 ảnh jpg, 1 file .exe | File hợp lệ lưu được và phát/hiện được trên app; file sai bị từ chối kèm thông báo rõ | T |

### 8.6 Tuần 5 – System

| Mã TC | US | Tiêu đề | Tiền điều kiện | Bước | Dữ liệu | Kết quả mong đợi | Ưu tiên |
|---|---|---|---|---|---|---|---|
| TC-75 | US21, US24 | Bảng route Gateway tuần 5 (Bảo) | Đủ 7 service | Gọi qua 8080 từng nhóm đường dẫn; xem log service nhận | `/api/artifacts/*/feedbacks`, `/api/artifacts/*/rating-summary`, `/api/admin/feedbacks/**` → 8086; `/api/me/favorites/**`, `/api/me/visits/**`, `/api/admin/visits/**` → 8085; `/api/me/devices/**` → 8084; `/api/statistics/**` → 8087 | Mỗi đường dẫn tới đúng service; route feedback đứng trước `/api/artifacts/**` | C |
| TC-76 | US24 | Phân quyền route mới | Token `visitor_an`, `visitor_mai`, `admin` | Không token: `GET /api/me/visits`, `POST /api/artifacts/1/feedbacks`. Token visitor: `GET /api/admin/feedbacks`, `GET /api/admin/visits`, `GET /api/statistics/overview`. `visitor_an` xem lịch sử | — | Không token → 401; visitor gọi API ADMIN → 403; mỗi người chỉ thấy lịch sử của mình | C |
| TC-77 | — | Swagger gom tại Gateway (Bảo, Tường rà) | Đủ 7 service | Mở `http://localhost:8080/swagger-ui.html`, chọn từng service, Authorize bằng token admin, thử 1 API | — | Chọn được đủ 7 service; thử API bằng token được; mọi API có mô tả và ví dụ (danh sách thiếu gửi từng người) | TB |
| TC-78 | — | Giám sát Prometheus / Grafana (Bảo) | `--profile monitoring` | Mở Prometheus → Status → Targets; mở dashboard Grafana, chọn 1 service | — | Mọi target UP (gateway, 7 service, MySQL, Redis); chọn service thì biểu đồ chỉ hiện số liệu của service đó (request/giây, p95, tỉ lệ 5xx, JVM, uptime) | TB |
| TC-79 | US21, US22 | NiFi feedbacks → fact_feedbacks (Tuấn) | `--profile nifi`; flow feedbacks dùng `updated_at` làm cột mốc | Duyệt 1 đánh giá PENDING trên Web (TC-70); chờ ≤ 2 phút; chạy lại flow | — | `analytics_db.fact_feedbacks` có dòng đó với status mới; chạy lại không nhân đôi (khoá `feedback_id`) | C |
| TC-80 | US22, US24 | NiFi danh mục (Tuấn) | Như TC-79 | Chạy flow artifacts, heritage_sites, users | — | `dim_artifact`, `dim_site`, `dim_user_lang` đủ dòng, ghi đè theo id; `dim_user_lang` chỉ có id và preferred_language, không có email/mật khẩu | C |
| TC-81 | US16, US22 | Lượt tham quan mới lên Dashboard (Tuấn, Bảo – kịch bản demo) | NiFi + analytics-service chạy | Thêm 1 lượt tham quan (TC-62 hoặc giả lập); mở lại Dashboard | — | Tổng lượt tham quan tăng 1 sau tối đa 1–2 phút | C |
| TC-82 | — | Redis cache chi tiết hiện vật (Phú) | Redis chạy | Gọi chi tiết hiện vật 2 lần liên tiếp; chờ hết TTL (10 phút) rồi gọi lại | — | Lần 2 nhanh hơn rõ (số liệu trong `Docs/research/redis-demo.md`); hết TTL thì đọc lại DB | T |
| TC-83 | US24 | Chặn spam ở mức database (Tường) | Volume mới | Chèn tay 2 dòng cùng (user_id, artifact_id) vào `feedback_db.feedbacks`; gửi đồng thời 2 lần đầu qua API | user 2, hiện vật 1 | Lần chèn thứ 2 lỗi trùng khoá `uq_feedbacks_user_artifact`; qua API: 1 lần 201, lần kia 200 hoặc 409, không có lỗi 500 | TB |
| TC-84 | — | feedback-service trong docker compose (Tường) | `--profile core up -d --build` | `docker compose ps`; `GET :8086/actuator/health`; `GET :8086/actuator/prometheus` | — | Container `smart_heritage_feedback` chạy bằng user `feedback_user`; health UP; có số liệu `http_server_requests_*` | TB |
| TC-85 | — | Smoke test toàn hệ thống (Bảo) | main mới nhất, đủ service | Chạy `scripts/smoke-test.ps1` (hoặc `.sh`) | — | Đăng nhập và 1 API chính của mỗi service qua Gateway đều đạt; chạy lại sau mỗi lần merge và tối Chủ nhật | C |

### 8.7 Tuần 6 – Xuất báo cáo

| Mã TC | US | Tiêu đề | Tiền điều kiện | Bước | Dữ liệu | Kết quả mong đợi | Ưu tiên |
|---|---|---|---|---|---|---|---|
| TC-86 | US17 | Xuất báo cáo thống kê PDF/Excel (Tuấn, tuần 6) | Đăng nhập `admin`; Dashboard có số liệu thật (TC-72) | Trên Dashboard chọn khoảng ngày, bấm Xuất PDF rồi Xuất Excel; mở 2 file | Khoảng ngày có dữ liệu mẫu | Tải về 2 file mở được; số liệu khớp Dashboard cùng bộ lọc; tiếng Việt hiển thị đúng; visitor gọi API xuất → 403 | C |
| TC-108 | US17 | File Excel đủ sheet (Tuấn) | Đăng nhập `admin` | `GET /api/statistics/export?format=xlsx&from=2026-10-01&to=2026-11-30` | Khoảng ngày có dữ liệu mẫu | Tải về file mở được bằng Excel, có 4 sheet Tổng quan, Top hiện vật, Ngôn ngữ, Đánh giá; số khớp Dashboard | C |
| TC-109 | US17 | File PDF đúng tiếng Việt (Tuấn) | Đăng nhập `admin` | `GET /api/statistics/export?format=pdf&from=2026-10-01&to=2026-11-30&siteId=1` | Di tích 1 | Có tiêu đề, khoảng ngày, bảng số liệu; tiếng Việt có dấu hiển thị đúng | C |
| TC-110 | US17, US24 | Phân quyền xuất báo cáo (Tuấn) | Token `visitor_an` | Gọi API như TC-108 bằng token visitor, sau đó không token; thử `format=doc` bằng token admin | — | Visitor → 403; không token → 401; `format` sai → 400 | C |

### 8.8 Tuần 6 – Sự kiện RabbitMQ

Chuẩn bị theo mục 3.1. Sau mỗi bước, xem tin trong queue `qa.events`.

| Mã TC | US | Tiêu đề | Tiền điều kiện | Bước | Dữ liệu | Kết quả mong đợi | Ưu tiên |
|---|---|---|---|---|---|---|---|
| TC-87 | US22 | VisitRecorded khi ghi 1 lượt (Phú) | Đăng nhập `visitor_an` | `POST /api/me/visits` | `{"artifactId":1,"source":"BEACON"}` | Có đúng 1 tin routing key `visit.recorded`; `data` có `visitId, userId, artifactId, siteId, detectedAt, durationSeconds, source` | C |
| TC-88 | US21, US22 | VisitRecorded khi gửi theo lô (Phú) | Đăng nhập `visitor_an` | `POST /api/me/visits/batch` 2 lần cùng `clientEventId` | 2 event có `clientEventId` khác nhau | Lần 1: 2 tin `visit.recorded`; lần 2 (trùng): không có tin mới | C |
| TC-89 | US08 | FeedbackCreated khi đánh giá mới (Tường) | Đăng nhập `visitor_mai`, chưa đánh giá hiện vật 3 | `POST /api/artifacts/3/feedbacks` | `{"rating":4,"comment":"Rất hay"}` | 201; có đúng 1 tin `feedback.created`, `type = FeedbackCreated`; `data` có `feedbackId, userId = 3, artifactId = 3, rating = 4, createdAt` | C |
| TC-90 | US08 | Gửi lại không phát FeedbackCreated (Tường) | Sau TC-89 | Gửi lại `POST /api/artifacts/3/feedbacks` | `{"rating":2}` | 200, đánh giá về PENDING; không có tin `feedback.created` mới | C |
| TC-91 | US15, US20 | FeedbackApproved khi Admin duyệt (Tường) | Đánh giá PENDING id 3 | `PATCH /api/admin/feedbacks/3/status` bằng token admin | `{"status":"APPROVED"}` | 200; có đúng 1 tin `feedback.approved`; `data` có `feedbackId = 3, userId = 3, artifactId = 1`, `artifactName` là tên hiện vật 1 | C |
| TC-92 | US15 | Không báo lặp khi duyệt lại hoặc ẩn (Tường) | Sau TC-91 | Gửi lại APPROVED cho id 3; gửi REJECTED cho id 5 | — | Cả 2 trả 200; không có tin `feedback.approved` mới | C |
| TC-93 | — | RabbitMQ tắt không ảnh hưởng đánh giá (Tường) | `docker stop smart_heritage_rabbitmq` | Gửi đánh giá, duyệt đánh giá; xem `:8086/actuator/health` và log | — | API vẫn 200/201 trong khoảng 2 giây; dữ liệu đã lưu; health UP; log ghi "Không gửi được sự kiện" kèm `eventId` | C |
| TC-94 | US10, US11, US14 | ArtifactUpdated khi đổi hiện vật (Xuân Anh) | Đăng nhập `admin` | Tạo, sửa, xóa hiện vật; sửa nội dung đa ngôn ngữ; thêm media trên CMS | — | Mỗi thao tác có 1 tin `artifact.updated`; `data` có `artifactId, siteId, changedFields, updatedAt`; `changedFields` đúng phần vừa sửa | C |
| TC-95 | US07 | Sửa hiện vật thì xóa cache (Phú, Xuân Anh) | Đã mở Yêu thích có hiện vật 2 | Sửa tên hiện vật 2 trên CMS; `GET /api/me/favorites`; xem log 2 service | Tên mới | Log personalization có xóa key `artifact:summary:2`; Yêu thích hiện tên mới ngay | C |
| TC-96 | US12, US18 | Đổi beacon thì registry mới (Xuân Anh, Khoa) | Beacon 4 chưa gán | Gán beacon 4 cho hiện vật 2; `GET /api/ibeacons/registry` | `{"artifactId":2}` | Có sự kiện tương ứng; registry trả beacon 4 trỏ hiện vật 2 | C |
| TC-97 | — | Không gửi sự kiện khi lưu DB thất bại (Tường, các bên gửi) | Dữ liệu sạch | Gửi đồng thời 2 lần đầu cùng user, cùng hiện vật (như TC-83); gửi đánh giá với `rating` sai | — | Lần bị 409 hoặc 400 không có tin; tổng số tin `feedback.created` đúng bằng số dòng mới | TB |
| TC-98 | — | Sự kiện đúng khung chung (Bảo) | Đã có tin của cả 5 loại | Đọc tin trong `qa.events` | — | Mọi tin có `eventId` (UUID), `type`, `version`, `occurredAt`, `data` | C |
| TC-99 | — | Bên nhận chống xử lý trùng (Tuấn, Khoa, Phú) | Có tin đã xử lý | Trên RabbitMQ UI publish lại đúng tin đó vào queue của bên nhận | Cùng `eventId` | Không đếm thêm, không gửi FCM lần 2, log ghi bỏ qua | C |
| TC-100 | — | Lỗi quá 3 lần vào DLQ (Bảo, các bên nhận) | Tạo lỗi xử lý ở bên nhận (ví dụ publish tin sai `data`) | Publish tin vào queue của bên nhận; xem DLQ và Grafana | Tin sai | Sau tối đa 3 lần xử lý lỗi, tin nằm trong DLQ; Grafana hiện số tin DLQ tăng | TB |

### 8.9 Tuần 6 – Thông báo, pin beacon, thống kê thời gian thực

| Mã TC | US | Tiêu đề | Tiền điều kiện | Bước | Dữ liệu | Kết quả mong đợi | Ưu tiên |
|---|---|---|---|---|---|---|---|
| TC-101 | US15, US20 | Du khách nhận thông báo khi được duyệt (Tường, Khoa) | Điện thoại Android đăng nhập `visitor_mai`, đã có FCM token | Admin duyệt đánh giá PENDING của `visitor_mai` trên Web | Id 3, hiện vật 1 | Trong ≤ 1 phút điện thoại nhận "Đánh giá của bạn về <tên hiện vật> đã được duyệt" | C |
| TC-102 | US20 | Admin được báo có đánh giá mới (Khoa, P1) | Điện thoại đăng nhập `admin` | Du khách gửi đánh giá mới | — | Admin nhận thông báo; gửi lại (ghi đè) thì không báo | T |
| TC-103 | US13 | Pin thấp phát BeaconBatteryLow (Khoa) | Đăng nhập `admin`, profile mq | `PUT /api/ibeacons/{id}/battery` đặt 15%; chờ tác vụ 15 phút hoặc chạy ngay bằng API của Khoa | Mức pin 15 (đối chiếu Swagger) | Có tin `beacon.battery.low` với `beaconId, siteId, batteryLevel, checkedAt`; đặt 50% thì không có tin | C |
| TC-104 | US13, US20 | Admin nhận cảnh báo pin (Khoa) | Sau TC-103 | Xem điện thoại trong topic `admins`; mở Web iBeacon | Beacon pin 15% | Điện thoại nhận thông báo; Web hiện biểu tượng cảnh báo cạnh beacon | C |
| TC-105 | US16, US22 | Dashboard đổi khi có lượt tham quan mới (Tuấn) | Mở `dashboard.html`, tắt NiFi | Ghi 1 lượt như TC-87, không tải lại trang | — | "Lượt xem hôm nay" tăng 1 và top hiện vật hôm nay cập nhật trong ≤ 30 giây | C |
| TC-106 | US16 | Dashboard đếm đánh giá mới (Tuấn) | Như TC-105 | Gửi 1 đánh giá mới (TC-89) | — | Số đánh giá thời gian thực tăng 1; gửi lại không tăng | TB |
| TC-107 | US16, US22 | NiFi tính bảng tổng hợp theo ngày (Tuấn) | `--profile nifi` | Chạy flow định kỳ bằng tay; chạy lại lần 2 | Dữ liệu mẫu | Bảng tổng hợp có số đúng theo ngày; chạy lại không nhân đôi | TB |

### 8.10 Tuần 6 – Hoàn thiện Mobile và Web

| Mã TC | US | Tiêu đề | Tiền điều kiện | Bước | Dữ liệu | Kết quả mong đợi | Ưu tiên |
|---|---|---|---|---|---|---|---|
| TC-111 | US07 | Màn Lịch sử (Phú) | Đăng nhập `visitor_an` | Mở Lịch sử; kéo xuống; bấm 1 dòng | — | Nhóm theo ngày, có thời gian dừng; tải lại được; mở đúng chi tiết hiện vật | C |
| TC-112 | US07 | Màn Yêu thích (Phú) | Đăng nhập `visitor_an` | Bỏ yêu thích ngay trên danh sách đến khi hết | — | Dòng biến mất ngay; danh sách trống có hướng dẫn | C |
| TC-113 | US21 | Báo đang ngoại tuyến (Phú) | Bật chế độ máy bay, có lượt trong hàng đợi | Mở Lịch sử | — | Hiện "Đang ngoại tuyến – sẽ đồng bộ sau"; có mạng lại thì đồng bộ và mất dòng này | TB |
| TC-114 | US08 | Sau khi gửi hiện chờ duyệt (Tường) | Đăng nhập `visitor1`, chưa đánh giá hiện vật 2 | Chi tiết hiện vật 2 → Đánh giá → 5 sao → Gửi | 5★ | Hiện "Đã gửi đánh giá", trạng thái "Đánh giá đang chờ duyệt", có nút "Xem đánh giá của tôi" | C |
| TC-115 | US08 | Màn "Đánh giá của tôi" (Tường) | Đăng nhập `visitor_mai`; có đánh giá đủ trạng thái | Menu bên → "Đánh giá của tôi"; Admin đổi trạng thái; kéo xuống; bấm 1 dòng | Id 3 PENDING, id 5 REJECTED | Hiện tên hiện vật, số sao, bình luận, ngày và nhãn Chờ duyệt/Đã duyệt/Không được duyệt; nhãn đổi sau khi tải lại; bấm mở đúng màn đánh giá; tài khoản mới thấy hướng dẫn trống | C |
| TC-116 | US08, US24 | API đánh giá của tôi (Tường) | Token `visitor_mai` | `GET /api/feedbacks/me` có token; không token; thêm header `X-User-Id: 2` giả | — | Có token: 200, chỉ đánh giá của user 3; không token: 401; header giả bị bỏ qua | C |
| TC-117 | US15 | Số chờ duyệt trên menu Web (Tường) | Đăng nhập `admin`, có đánh giá PENDING | Mở `admin.html` và `heritage-sites.html`; duyệt hết trên `feedback.html`; tải lại | — | Nút "Phản hồi" hiện đúng số PENDING; hết chờ duyệt thì không hiện số; tắt feedback-service thì menu vẫn dùng được | C |
| TC-118 | US15, US24 | Phân quyền số chờ duyệt (Tường) | — | `GET /api/admin/feedbacks/pending-count` bằng token admin, visitor, không token | — | Lần lượt 200 `{"pending":n}`, 403, 401 | C |
| TC-119 | US05 | Chi tiết hiện vật không vỡ giao diện (Xuân Anh) | Hiện vật mô tả rất dài; hiện vật không có media | Mở trên màn hình nhỏ | — | Không tràn chữ, không lỗi layout; không có media vẫn hiển thị gọn | C |

### 8.11 Tuần 6 – Giám sát và kiểm thử tải

| Mã TC | US | Tiêu đề | Tiền điều kiện | Bước | Dữ liệu | Kết quả mong đợi | Ưu tiên |
|---|---|---|---|---|---|---|---|
| TC-120 | — | RabbitMQ trong Prometheus (Bảo) | `--profile monitoring --profile mq` | Mở Prometheus → Status → Targets | — | Target RabbitMQ UP | TB |
| TC-121 | — | Tắt service thì cảnh báo bật (Bảo) | Grafana đang mở | Tắt content-service; chờ; bật lại | — | Cảnh báo bật trong ≤ 2 phút; bật lại thì cảnh báo tắt | C |
| TC-122 | — | Các quy tắc cảnh báo khác (Bảo) | Như TC-121 | Tạo điều kiện: 5xx > 5% trong 5 phút; p95 > 2 giây; queue tồn > 100 tin; DLQ có tin | — | Từng cảnh báo hiện trên Grafana | TB |
| TC-123 | — | Load test 500 người dùng ảo (Bảo) | Kịch bản k6 qua Gateway | Tăng dần tới 500 VU, gọi chi tiết hiện vật, nearby, registry, đăng nhập | — | `Docs/testing/load-test.md` ghi p95, tỉ lệ lỗi đã đo thật, so với p95 ≤ 2 giây; máy không đủ thì ghi rõ mức đã đạt và giới hạn phần cứng | C |
| TC-124 | — | Demo Monitoring 5 phút (Bảo) | Theo kịch bản demo | Grafana → tắt content-service → cảnh báo bật → bật lại → cảnh báo tắt; bấm giờ | — | Chạy trọn trong 5 phút, không phải sửa gì lúc demo | C |

### 8.12 Tuần 6 – Trường hợp đặc biệt, phân quyền, bản chốt (Ân)

| Mã TC | US | Tiêu đề | Tiền điều kiện | Bước | Dữ liệu | Kết quả mong đợi | Ưu tiên |
|---|---|---|---|---|---|---|---|
| TC-125 | US21 | Mất mạng giữa luồng | App đang mở hiện vật | Bật chế độ máy bay; thao tác Yêu thích, Đánh giá, Lịch sử, Đánh giá của tôi | — | Có thông báo rõ, không crash; có mạng lại dùng được ngay | C |
| TC-126 | US04 | Tắt Bluetooth khi đang quét | Đang ở màn quét | Tắt Bluetooth; bật lại | — | Hiện hướng dẫn, không crash; bật lại thì quét tiếp | C |
| TC-127 | US04 | Từ chối quyền | Cài mới app | Từ chối quyền Bluetooth/Vị trí/Thông báo | — | Có hướng dẫn mở Cài đặt, các màn khác vẫn dùng được | C |
| TC-128 | US20 | 2 beacon gần nhau | 2 thiết bị phát 1/1 và 1/2 đặt gần nhau | Đứng giữa 2 beacon 1 phút | — | Không nhảy qua lại liên tục; chỉ mở 1 hiện vật tại một thời điểm | TB |
| TC-129 | — | Tắt 1 service | Hệ thống đang chạy | Lần lượt tắt content, feedback, personalization; dùng Web và app | — | Chức năng liên quan báo lỗi rõ (503), không treo; chức năng khác vẫn chạy; bật lại dùng được | C |
| TC-130 | US24 | VISITOR vào trang quản trị | Token `visitor_an` | Mở `admin.html`, `feedback.html`; gọi API ghi `POST /api/heritage-sites`, `PATCH /api/admin/feedbacks/3/status` | — | Trang chuyển về đăng nhập hoặc báo không đủ quyền; API trả 403 | C |
| TC-131 | — | Smoke test bản chốt code (Ân, Bảo) | Bản main lúc 18:00 Thứ 6 13/11 | Chạy `scripts/smoke-test.ps1` và lại luồng chính du khách, Admin | — | Đăng nhập, quét/giả lập beacon mở hiện vật, gửi và duyệt đánh giá đều đạt | C |

**Tổng: 131 test case.** Bản 0.1: 50 (Visitor 21, Administrator 16, System 13). Tuần 5 thêm 35 (Visitor 19, Administrator 5, System 11).
Tuần 6: 46 (TC-86 → TC-131).
Unit test tự động đi kèm: feedback-service `mvn test` (59 test), api-gateway `mvn test` (`AccessRulesTest`, `GatewayRoutesConfigTest`),
app `flutter test` (gồm thư mục `test/features/feedback`, 26 test).

## 9. Truy vết yêu cầu

| User story | Test case | Ghi chú |
|---|---|---|
| US01 | TC-01 → TC-04 | |
| US02 | TC-05, TC-06 | |
| US03 | TC-07 → TC-09, TC-51 → TC-53 | Bản đồ + chỉ đường (tuần 5) |
| US04 | TC-10, TC-11, TC-58, TC-61 | |
| US05 | TC-12, TC-13, TC-54, TC-55 | |
| US06 | TC-14, TC-56, TC-57 | |
| US07 | TC-15, TC-16, TC-64, TC-95, TC-111, TC-112 | |
| US08 | TC-17 → TC-21, TC-36, TC-65 → TC-68, TC-89, TC-90, TC-114 → TC-116 | |
| US09 | TC-22, TC-23 | |
| US10 | TC-24 → TC-27 | |
| US11 | TC-28, TC-74 | TC-74 là việc P2 |
| US12 | TC-30 → TC-32 | |
| US13 | TC-33, TC-71, TC-103, TC-104 | |
| US14 | TC-29, TC-73 | TC-73 là việc P1 |
| US15 | TC-34, TC-35, TC-68, TC-70, TC-91, TC-92, TC-101, TC-117, TC-118 | |
| US16 | TC-36, TC-37, TC-72, TC-81, TC-105 → TC-107 | |
| US17 | TC-86, TC-108 → TC-110 | Xuất PDF/Excel |
| US18 | TC-10, TC-58 | |
| US19 | TC-50, TC-59 | |
| US20 | TC-50, TC-60, TC-69, TC-101, TC-102, TC-104, TC-128 | |
| US21 | TC-43, TC-45, TC-57, TC-63, TC-75, TC-79, TC-88, TC-113, TC-125 | |
| US22 | TC-43, TC-49, TC-62, TC-63, TC-79 → TC-81, TC-87, TC-88, TC-105, TC-107 | |
| US23 | TC-12, TC-42, TC-54 | |
| US24 | TC-38 → TC-41, TC-44, TC-75, TC-76, TC-80, TC-83, TC-110, TC-116, TC-118, TC-130 | |
| Phi chức năng | TC-46 → TC-48, TC-77, TC-78, TC-82, TC-84, TC-85, TC-93, TC-97 → TC-100, TC-120 → TC-124, TC-129, TC-131 | Định tuyến, giám sát, Swagger, cache, triển khai, sự kiện, cảnh báo, kiểm thử tải |

Phủ 24/24 user story.

## 10. Test case phụ thuộc việc chưa xong (tình trạng 09/11/2026)

| Việc chưa xong | Người | Test case bị chặn | Cách chạy tạm |
|---|---|---|---|
| Thư viện sự kiện dùng chung (EventEnvelope, gửi sau commit) | Bảo | TC-98 (phần khung chung của module common) | feedback-service đang dùng khung tạm cùng định dạng JSON; đổi sang thư viện chung khi có trên main |
| VisitRecorded, xóa cache, Lịch sử/Yêu thích | Phú | TC-87, TC-88, TC-95, TC-111 → TC-113 | Theo PR của Phú |
| ArtifactUpdated, hoàn thiện chi tiết | Xuân Anh | TC-94 → TC-96, TC-119 | Theo PR của Xuân Anh |
| Nhận sự kiện gửi FCM, pin beacon, registry | Khoa | TC-96, TC-99 → TC-104 | TC-91 kiểm tra được phần gửi sự kiện, chưa có thông báo tới điện thoại |
| Đếm thời gian thực, NiFi định kỳ, xuất báo cáo | Tuấn | TC-86, TC-99, TC-100, TC-105 → TC-110 | Theo PR của Tuấn |
| Prometheus RabbitMQ, cảnh báo, k6, demo | Bảo | TC-100, TC-120 → TC-124 | Theo PR của Bảo |

Bản build Mobile chưa ổn định đầu tuần thì test Web và API trước, Mobile từ Thứ 4 (`task_w6_chi_tiet`, mục 7).
