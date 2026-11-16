# Đặc tả yêu cầu người dùng (URS) – Smart Heritage

| Mục | Nội dung |
|---|---|
| Sản phẩm | Smart Heritage – Hệ thống du lịch thông minh ứng dụng iBeacon hỗ trợ tương tác hiện vật tại di tích lịch sử, văn hoá |
| Phiên bản tài liệu | Bản cuối – Sprint 7 (XDPMHDT-96) |
| Căn cứ | Đề tài (mục 3.2 a–g), Task_main.docx, code trên `main` (`c365dac`) và 4 patch tuần 7 |
| Người tổng hợp | Đỗ Lê Hoài Bảo |
| Tài liệu liên quan | [SRS](SRS.md) (đặc tả chi tiết, mã FR/NFR), [Kiến trúc](../architecture/architecture.md), [Deployment Guide](../deployment/deployment-guide.md) |

## 1. Mục đích và phạm vi

Tài liệu này ghi lại **người dùng cần gì** từ hệ thống, viết theo ngôn ngữ nghiệp vụ, không đi vào cách làm kỹ thuật. Mỗi yêu cầu có mã `UR-xx`, tiêu chí chấp nhận nhìn thấy được và được truy vết sang yêu cầu phần mềm trong [SRS](SRS.md).

Trong phạm vi: ứng dụng di động cho du khách; trang quản trị web cho ban quản lý di tích; hạ tầng iBeacon; thống kê và báo cáo.

Ngoài phạm vi: định vị trong nhà chính xác theo mét, thực tế tăng cường (AR), gợi ý lộ trình bằng AI, dịch máy tự động, bán vé và thanh toán.

## 2. Bối cảnh

- Nhiều di tích vẫn giới thiệu hiện vật bằng bảng thông tin, tờ rơi hoặc hướng dẫn viên; du khách tiếp nhận thụ động, ít tương tác.
- Thông tin chủ yếu bằng tiếng Việt; khách quốc tế khó tiếp cận.
- Mã QR buộc du khách quét từng hiện vật, làm gián đoạn trải nghiệm.
- Ban quản lý thiếu công cụ tập trung để cập nhật nội dung, quản lý thiết bị và xem số liệu khách tham quan; quyết định dựa vào quan sát thủ công.

Smart Heritage dùng iBeacon (Bluetooth Low Energy) để điện thoại **tự nhận biết** du khách đang đứng gần hiện vật nào và hiện nội dung đa phương tiện, đa ngôn ngữ; đồng thời cung cấp trang quản trị và bảng thống kê cho ban quản lý.

## 3. Nhóm người dùng

| Nhóm | Mô tả | Đặc điểm cần lưu ý |
|---|---|---|
| Du khách (Visitor) | Khách tham quan trong nước và quốc tế, dùng điện thoại Android hoặc iOS | Thao tác smartphone cơ bản; có thể không đọc tiếng Việt; muốn ít thao tác (3–5 lần chạm) |
| Ban quản lý di tích (Administrator) | Nhân viên quản lý nội dung, thiết bị và số liệu, dùng máy tính | Không cần kiến thức về BLE; cần thao tác trên một trang quản trị thống nhất |
| Hệ thống (System) | Các tiến trình tự động: dò beacon, chống thông báo lặp, đồng bộ, ghi nhận tương tác | Chạy nền trên ứng dụng và máy chủ |

## 4. Yêu cầu người dùng

Mức ưu tiên: **Bắt buộc** (thiếu thì không demo được luồng chính), **Cần có**, **Nên có**.

### 4.1 Du khách

| Mã | Người dùng cần | Tiêu chí chấp nhận | Ưu tiên | Truy vết SRS |
|---|---|---|---|---|
| UR-01 | Đăng ký, đăng nhập trên ứng dụng để lưu lịch sử và yêu thích của mình | Đăng ký bằng tên đăng nhập, email, mật khẩu; đăng nhập lại trên máy khác vẫn thấy dữ liệu của mình | Bắt buộc | FR-AUTH-01, 02, 03 |
| UR-02 | Chọn ngôn ngữ ưu tiên và cập nhật hồ sơ | Đổi sang tiếng Anh thì nội dung hiện vật hiển thị tiếng Anh; thiếu bản dịch thì hiện tiếng Việt kèm nhãn | Bắt buộc | FR-AUTH-05, FR-ART-03 |
| UR-03 | Xem bản đồ các di tích và tìm di tích gần mình | Bản đồ có marker di tích; danh sách di tích gần sắp xếp từ gần đến xa; có nút chỉ đường | Cần có | FR-SITE-03, 04 |
| UR-04 | Đến gần hiện vật thì ứng dụng tự hiện thông tin, không phải quét QR | Từ lúc vào vùng phủ beacon tới lúc hiện nội dung ≤ 2 giây | Bắt buộc | FR-BCN-03 → 06, NFR-01 |
| UR-05 | Xem mô tả, ảnh, audio, video của hiện vật | Mở được đủ 4 loại nội dung; rời màn hình thì audio/video dừng | Bắt buộc | FR-ART-02, 06 |
| UR-06 | Tiếp tục xem nội dung khi đã đi khỏi vùng beacon hoặc mất mạng | Tắt mạng vẫn mở được hiện vật đã xem, có nhãn đang xem bản đã tải | Bắt buộc | FR-PER-04, NFR-06 |
| UR-07 | Lưu hiện vật yêu thích và xem lại lịch sử tham quan | Bấm yêu thích 2 lần không tạo trùng; lịch sử mới nhất ở đầu; dữ liệu lúc mất mạng được đồng bộ khi có mạng | Cần có | FR-PER-01 → 03 |
| UR-08 | Chấm sao và gửi nhận xét về hiện vật | Chọn 1–5 sao; đánh giá chỉ hiện công khai sau khi được duyệt; được báo khi đánh giá được duyệt | Cần có | FR-FB-01, 02, FR-BCN-08 |

### 4.2 Ban quản lý di tích

| Mã | Người dùng cần | Tiêu chí chấp nhận | Ưu tiên | Truy vết SRS |
|---|---|---|---|---|
| UR-09 | Đăng nhập an toàn vào trang quản trị | Chỉ tài khoản quản trị vào được; chưa đăng nhập hoặc không đủ quyền bị đưa về trang đăng nhập | Bắt buộc | FR-AUTH-02, 04, 07 |
| UR-10 | Đi tới mọi chức năng quản trị từ một menu chung | Sau đăng nhập vào trang Tổng quan; menu có Tổng quan, Di tích, Hiện vật, Beacon, Phản hồi, Người dùng, Tài khoản | Bắt buộc | FR-ADM-01 |
| UR-11 | Quản lý tài khoản người dùng | Xem, tìm, đổi vai trò, khoá/mở tài khoản; tài khoản bị khoá không đăng nhập được; không tự khoá chính mình | Bắt buộc | FR-AUTH-06 |
| UR-12 | Thêm, sửa, xoá di tích và hiện vật | Chọn toạ độ di tích trên bản đồ; mã hiện vật không trùng; hiện vật luôn thuộc một di tích | Bắt buộc | FR-SITE-01, 02, FR-ART-01 |
| UR-13 | Tải lên ảnh, audio, video và nhập nội dung theo từng ngôn ngữ | Tạo xong hiện vật có 2 ngôn ngữ trong một form; file sai định dạng hoặc quá dung lượng bị từ chối | Bắt buộc | FR-ART-02, 04, 05 |
| UR-14 | Quản lý danh sách ngôn ngữ hỗ trợ và bản dịch | Bật/tắt ngôn ngữ; bản dịch nhập trong form hiện vật | Cần có | FR-ART-02, 03 |
| UR-15 | Đăng ký, cấu hình beacon và gán cho hiện vật | Bộ UUID/Major/Minor không trùng; chỉ gán được hiện vật cùng di tích | Bắt buộc | FR-BCN-01, 02 |
| UR-16 | Theo dõi vị trí, trạng thái và pin của beacon trên bản đồ | Thấy vị trí beacon trên bản đồ; pin dưới 20% có cảnh báo | Cần có | FR-BCN-07 |
| UR-17 | Duyệt hoặc ẩn đánh giá của du khách | Lọc đánh giá chờ duyệt; số chờ duyệt hiện cạnh menu Phản hồi | Cần có | FR-FB-03 |
| UR-18 | Xem bảng thống kê: lượt khách, hiện vật nổi bật, ngôn ngữ, đánh giá | Lọc theo khoảng ngày và di tích; tải trong ≤ 5 giây | Bắt buộc | FR-ANA-01, 02, NFR-02 |
| UR-19 | Xuất báo cáo PDF hoặc Excel | File tải về có đúng số liệu đang lọc | Cần có | FR-ANA-03 |

### 4.3 Hệ thống

| Mã | Yêu cầu | Tiêu chí chấp nhận | Ưu tiên | Truy vết SRS |
|---|---|---|---|---|
| UR-20 | Dò beacon liên tục khi ứng dụng đang mở | Nhận được beacon thật hoặc giả lập; hướng dẫn cấp quyền Bluetooth, vị trí | Bắt buộc | FR-BCN-04 |
| UR-21 | Không báo lặp khi du khách đứng yên; báo khi sang vùng beacon khác | Đứng yên trong vùng không hiện lại; đi qua 2 beacon gần nhau không nhảy qua lại | Bắt buộc | FR-BCN-06, NFR-07 |
| UR-22 | Đồng bộ dữ liệu giữa ứng dụng và trang quản trị | Admin sửa hiện vật thì ứng dụng thấy nội dung mới; lượt xem lúc mất mạng được gửi lại không trùng | Bắt buộc | FR-PER-03, FR-EVT-01 → 05 |
| UR-23 | Ghi nhận lượt tham quan, tương tác để làm thống kê | Lượt tham quan mới xuất hiện trên Dashboard | Bắt buộc | FR-PER-02, FR-ANA-01 |
| UR-24 | Bảo vệ dữ liệu người dùng | Mật khẩu không lưu dạng đọc được; mỗi người chỉ xem dữ liệu của mình | Bắt buộc | FR-AUTH-04, NFR-04 |

## 5. Yêu cầu chất lượng (người dùng cảm nhận được)

| Mã | Yêu cầu | Ngưỡng (theo Đề tài) | Truy vết SRS |
|---|---|---|---|
| UQ-01 | Nhanh | Hiện nội dung hiện vật ≤ 2 giây sau khi vào vùng beacon; ≥ 95% thao tác web ≤ 2 giây; Dashboard ≤ 5 giây | NFR-01, 02 |
| UQ-02 | Dễ dùng | Việc chính (xem hiện vật, mở bản đồ, đổi ngôn ngữ) trong 3–5 lần chạm; giao diện dùng được trên nhiều kích thước màn hình | NFR-03 |
| UQ-03 | An toàn | Kết nối mã hoá HTTPS/TLS 1.2+; phân quyền theo vai trò | NFR-04 |
| UQ-04 | Chịu tải | ≥ 500 người dùng đồng thời; nhiều di tích, hàng nghìn hiện vật, hàng trăm beacon | NFR-05 |
| UQ-05 | Sẵn sàng | ≥ 99% thời gian vận hành; mất beacon vẫn xem được nội dung đã tải | NFR-06 |
| UQ-06 | Tin cậy | Không mất, không trùng dữ liệu khi mạng chập chờn | NFR-07 |
| UQ-07 | Dễ bảo trì, mở rộng | Module tách bạch; API có tài liệu Swagger | NFR-08 |

## 6. Ràng buộc và giả định

- Làm trong 8 tuần theo Agile/Scrum, phân tích thiết kế theo UML 2.0.
- Điện thoại cần Bluetooth 4.0 (BLE) và bật định vị; trên iOS iBeacon chỉ đọc qua CoreLocation.
- Khu di tích có mạng di động hoặc Wi-Fi cơ bản; khi mất mạng ứng dụng dùng dữ liệu đã tải.
- Mỗi hiện vật gắn tối đa một beacon đang hoạt động.
- Nội dung (văn bản, ảnh, audio, video) do ban quản lý cung cấp; bản demo dùng dữ liệu mẫu.

## 7. Mức đáp ứng tại thời điểm viết

Trạng thái chi tiết từng yêu cầu ở [SRS mục 3](SRS.md#3-yêu-cầu-chức-năng). Tóm tắt các điểm **chưa đạt hoặc chưa có bằng chứng** (16/11/2026):

| Yêu cầu | Tình trạng | Kế hoạch |
|---|---|---|
| UR-02 cập nhật hồ sơ | Xem hồ sơ được; chưa có API sửa hồ sơ | Ngoài phạm vi bản v1.0 nếu không kịp (ghi trong báo cáo) |
| UR-04, UR-21 trên điện thoại thật | Có trong code; đo ≤ 2 giây và chồng vùng đang làm | Khoa, tuần 7 |
| UR-10, UR-11 | Đã viết (patch W7 01, 02), chờ merge | Bảo, T2–T3 tuần 7 |
| UQ-03 HTTPS | Chạy HTTP trên máy local | Bật HTTPS khi triển khai server, tuần 8 |
| UQ-04 500 người dùng | k6 lần 1 đạt (p95 toàn bộ 1,09 giây, 0% lỗi); content-service nghẽn pool kết nối DB | Đo lại lần 2 sáng T5 19/11 |
