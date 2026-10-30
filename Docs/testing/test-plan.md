# Test Plan – Smart Heritage (bản 0.1)

| Mục | Nội dung |
|---|---|
| Phiên bản | 0.1 (Sprint 4) |
| Người lập | Tường (đầu mối QA) |
| Ngày | 30/10/2026 |
| Căn cứ | User story US01–US24 trong `readme.md`; `task_w4_chi_tiet` mục III (Gateway, service, schema) |
| Trạng thái | Nháp: cập nhật mỗi sprint khi tách thêm service |

## 1. Mục tiêu

- Kiểm tra các chức năng đã làm ở tuần 1–4 vẫn chạy đúng khi chuyển sang kiến trúc microservice
  (client gọi qua Gateway `http://localhost:8080`).
- Giữ nguyên kết quả phân quyền 401/403 của tuần 3.
- Làm danh sách test case nền để viết Test Report ở tuần sau.

## 2. Phạm vi

**Trong phạm vi**

| Nhóm | Chức năng |
|---|---|
| Visitor (app Flutter, API) | Đăng ký/đăng nhập, hồ sơ và ngôn ngữ, di tích gần đây, quét iBeacon, xem chi tiết hiện vật đa ngôn ngữ, xem lại offline, yêu thích, lịch sử tham quan, gửi đánh giá |
| Administrator (Web Admin, API) | Đăng nhập, CRUD di tích/hiện vật, media, bản dịch, quản lý và gán iBeacon, duyệt đánh giá, xem thống kê |
| System | JWT + RBAC tại Gateway, chống giả mạo header, định tuyến Gateway, fallback ngôn ngữ, đồng bộ offline không trùng, cô lập database theo service, giám sát Prometheus, xử lý khi service phụ thuộc bị tắt, luồng NiFi |

**Ngoài phạm vi bản 0.1**

- iOS (thư viện iBeacon chỉ demo trên Android).
- Upload file media thật (tuần 4 mới nhập URL).
- Xuất báo cáo PDF/Excel (US17).
- Kiểm thử hiệu năng/tải và kiểm thử bảo mật chuyên sâu (để bản 0.2).

## 3. Môi trường

| Thành phần | Địa chỉ / lệnh |
|---|---|
| MySQL 8 + Redis 7 | `docker compose -f Infra/docker-compose.yml up -d` (MySQL cổng 3307, Redis 6379). Dữ liệu sạch: `down -v` rồi `up -d` |
| api-gateway | `http://localhost:8080` (client chỉ gọi vào đây) |
| auth / site / content / beacon | 8081 / 8082 / 8083 / 8084 (Swagger ở cổng riêng: `/swagger-ui.html`) |
| personalization / feedback / analytics | 8085 / 8086 / 8087 (tuần 5 mới route qua Gateway) |
| Backend cũ (monolith) | `http://localhost:8090`, schema `smart_heritage_db` |
| Monitoring (tuỳ chọn) | `--profile monitoring`: Prometheus 9090, Grafana 3000 |
| NiFi (tuỳ chọn) | `--profile nifi`: `https://localhost:8443/nifi` |
| Web Admin | Mở `WebAdmin/src/pages/login.html` bằng Live Server |
| Mobile | Android thật (máy ảo không quét được BLE) + điện thoại thứ 2 chạy Beacon Simulator |

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

## 4. Vai trò

| Người | Trách nhiệm |
|---|---|
| Tường | Lập và duy trì Test Plan, tổng hợp kết quả, quản lý danh sách lỗi; test Feedback và Database |
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

- 100% test case ưu tiên Cao đã chạy, ít nhất 90% đạt.
- Không còn lỗi mức Nghiêm trọng hoặc Cao đang mở.
- Lỗi mức Trung bình/Thấp còn mở đều đã ghi vào danh sách và có người nhận.
- Kết quả đã ghi vào cột "Kết quả" (Đạt / Không đạt / Chưa chạy) và gửi nhóm.

## 7. Mẫu báo lỗi

Tạo GitHub Issue với tiêu đề `[BUG][<module>] <mô tả ngắn>`, nội dung:

```
Mã lỗi        : BUG-<số>
Test case     : TC-xx
Mức độ        : Nghiêm trọng | Cao | Trung bình | Thấp
Môi trường    : nhánh/commit, Gateway hay cổng riêng, thiết bị (nếu là Mobile)
Bước tái hiện : 1. ... 2. ... 3. ...
Dữ liệu       : tài khoản, id, body request
Kết quả mong đợi :
Kết quả thực tế  : (mã HTTP, JSON, ảnh chụp màn hình / log)
Người phụ trách  :
```

Mức độ: **Nghiêm trọng** = sập service, mất dữ liệu, lộ quyền; **Cao** = chức năng chính không dùng được;
**Trung bình** = sai một phần, có cách đi vòng; **Thấp** = giao diện, câu chữ.

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
| TC-17 | US08 | Gửi đánh giá hợp lệ | Đăng nhập `visitor_mai` | `POST /api/artifacts/2/feedbacks` | `{"rating":4,"comment":"Rất hay"}` | 201, `status = PENDING`, `reviewerName = Trần Thị Mai`; chưa hiện trong danh sách công khai | C |
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
| TC-34 | US15 | Danh sách đánh giá chờ duyệt | Đăng nhập admin; feedback-service 8086 | `GET /api/admin/feedbacks?status=PENDING` (tuần 4 gọi thẳng 8086, tự gắn `X-User-Role: ADMIN`) | — | 200, có đánh giá id 3, 5; `status=XYZ` → 400 | C |
| TC-35 | US15 | Duyệt / từ chối đánh giá | Có đánh giá PENDING (TC-17) | `PUT /api/admin/feedbacks/{id}/status` (Backend cũ: `PATCH`) | `{"status":"APPROVED"}` | 200; đánh giá hiện trong TC-21; gửi `PENDING` → 400; id không tồn tại → 404 | C |
| TC-36 | US08, US16 | Điểm trung bình hiện vật | Đánh giá mẫu | `GET /api/artifacts/2/rating-summary` (8086) | — | `averageRating = 4.0`, `totalRatings = 1`, chỉ tính APPROVED; sau khi duyệt TC-35 thì số liệu cập nhật | TB |
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
| TC-47 | — | Định tuyến Gateway | Gateway + các service đã tách | Gọi `/api/heritage-sites`, `/api/artifacts/1`, `/api/ibeacons/registry`, `/api/artifacts/1/feedbacks` qua 8080 | — | Mỗi đường dẫn tới đúng service (xem log), đặc biệt `/feedbacks` không bị chuyển nhầm sang content-service | TB |
| TC-48 | — | Giám sát | Service đang chạy | `GET /actuator/prometheus` trên 8080, 8081 và mỗi service đã tách | — | 200, có số liệu `http_server_requests_*` | TB |
| TC-49 | US22 | NiFi chuyển visits sang analytics_db | `--profile nifi`, flow của Tuấn | Chạy flow 2 lần | 4 dòng visits mẫu | `analytics_db.fact_visits` có đủ 4 dòng, lần 2 không nhân đôi | TB |
| TC-50 | US19, US20 | Không thông báo lặp, thông báo khi sang beacon khác | App quét beacon (tuần 5) | Đứng yên ở beacon 1/1 30 giây, rồi chuyển sang 1/2 | — | Chỉ 1 thông báo cho 1/1; có thông báo mới khi vào vùng 1/2 | TB |

**Tổng: 50 test case** (Visitor 21, Administrator 16, System 13). TC-34 đến TC-36 chạy trên feedback-service (8086), tuần 5 sẽ chạy qua Gateway; TC-50 sẽ chạy khi có luồng thông báo ở tuần 5.

## 9. Truy vết yêu cầu

| User story | Test case |
|---|---|
| US01 | TC-01 → TC-04 |
| US02 | TC-05, TC-06 |
| US03 | TC-07 → TC-09 |
| US04, US18 | TC-10, TC-11 |
| US05 | TC-12, TC-13 |
| US06 | TC-14 |
| US07 | TC-15, TC-16 |
| US08 | TC-17 → TC-21, TC-36 |
| US09 | TC-22, TC-23 |
| US10 | TC-24 → TC-27 |
| US11 | TC-28 |
| US12, US13 | TC-30 → TC-33 |
| US14 | TC-29 |
| US15 | TC-34, TC-35 |
| US16 | TC-36, TC-37 |
| US19, US20 | TC-50 |
| US21, US22 | TC-43, TC-45, TC-49 |
| US23 | TC-12, TC-42 |
| US24 | TC-38 → TC-41, TC-44 |
| US17 | Chưa có (ngoài phạm vi bản 0.1) |
