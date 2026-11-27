# Báo cáo kiểm thử cuối – Smart Heritage (Test Report v1.0)

| Mục | Nội dung |
|---|---|
| Người lập | Tường (đầu mối QA), Ân (System Testing) |
| Căn cứ | [Test Plan](test-plan.md) (136 test case); `task_w7_chi_tiet` mục III.5 |
| Lần 1 (System Testing) | 10/11/2026 – 15/11/2026, bản chốt code 18:00 13/11 |
| Lần 2 (hồi quy) | 19/11/2026 (API Đánh giá, Tường), 21/11/2026 (Thống kê trên máy sạch `v1.0`, Tuấn), 22/11/2026 (app, Web – phần D, Bảo chạy), 23/11/2026 (20 case R1, Bảo chạy thay Ân, mục 4.2), 24/11/2026 (giám sát, sự kiện, ngoại tuyến – Bảo, mục 4.3; app – Ân, mục 4.4) và 25/11/2026 (nghiệm thu lỗi Minor 10, 11; 30 case API, MySQL, RabbitMQ, Docker – Bảo, mục 4.5); 25–27/11/2026 (13 case app, 15 case Web Admin, 5 case NiFi, TC-123 – Bảo, mục 4.6 → 4.8) |
| Bản kiểm thử cuối | tag `v1.0` = `2a6e9e7`. Phần D chạy trên `main` `31853e6` = `v1.0` + bản iOS (#90); backend và app Android không đổi so với `v1.0` |
| Môi trường | Windows 11, JDK 25. Lần 2: toàn hệ thống chạy Docker (`--profile core --profile mq` ngày 19/11, `--profile all` ngày 22/11), DB sạch + dữ liệu demo bản cuối (`Infra/db/09_demo_seed.sql` + `Infra/seed/media`) |
| Thiết bị | Máy ảo Android Pixel 7, Android 17 (API 37, Google Play), app 1.0.0+1 bản debug, `DEMO_MODE=true` (nhóm không có điện thoại Android thật). Web Admin: Chrome, Live Server, tài khoản `admin` |

**Tình trạng bản này (25/11):** lần 2 đã có phần API của Tường (19/11), phần Thống kê trên máy sạch của Tuấn (21/11, mục 4.1)
và phần chạy tay trên app, Web (phần D, 22/11): 36 case đã chạy đều đạt, TC-101 bị chặn (thiếu Firebase và máy thật).
Không có lỗi Blocker, Critical, Major; 12 lỗi Minor ghi ở mục 6 (số 9 phát hiện khi chạy R1 23/11; số 10, 11 khi chạy TC-125 24/11; số 12 khi chạy TC-129 25/11), đã sửa cả 12.
Phần D có bằng chứng ảnh chụp và script đối chiếu dữ liệu `Kiem_case_Tuong_v1.ps1` (chỉ đọc API và MySQL): 27/27 kiểm tra đạt.
**Cập nhật 23/11:** đã chạy 20 case hồi quy R1 trên `main` sau #99 (mục 4.2): 19 đạt, TC-62 đạt một phần (giả lập ra vùng trên máy ảo, chưa có beacon thật).
**Cập nhật 24–25/11:** Bảo chạy 9 case giám sát, sự kiện, ngoại tuyến (mục 4.3): 8 đạt, TC-100 đạt một phần; Ân chạy thêm 3 case trên app (mục 4.4).
Khi chạy TC-125 phát hiện thêm 2 lỗi Minor (số 10, 11, mục 6), đã sửa và nghiệm thu 25/11. Ngày 25/11 chạy thêm 30 case bằng script (mục 4.5): 29 đạt, TC-46 đạt một phần; phát hiện và sửa lỗi Minor 12 (Gateway trả 500 khi service vừa tắt).
Lần 2 nâng lên **93/136 case: 90 đạt, 3 đạt một phần (TC-46, TC-62, TC-100), 1 chặn (TC-101)**; 12/12 lỗi Minor đã sửa.
**Cập nhật 27/11 (XDPMHDT-116):** chạy thêm 13 case app, 15 case Web Admin (mục 4.6, 4.7), 5 case NiFi (mục 4.8) và ghi kết quả k6 500 VU (TC-123, [load-test-w7.md](load-test-w7.md)).
Lần 2 đạt **127/136 = 93% (yêu cầu ≥ 90%): 122 đạt, 5 đạt một phần (TC-46, 53, 62, 100, 104), 1 chặn (TC-101), 8 chưa chạy** (cần điện thoại Android, beacon thật, Firebase – mục 7). Phát hiện lỗi Minor 14 (mục 6), sửa và nghiệm thu trong XDPMHDT-116 (TC-11, TC-127 chạy lại đạt); TC-27 đạt sau khi thêm tìm kiếm, lọc di tích.
**Cập nhật 27/11 (XDPMHDT-117):** ghi kết quả TC-69 (phần API, chạy 26/11, mục 4.7): **đạt một phần**, thiếu phần điện thoại nhận thông báo (Firebase + máy Android thật).
Lần 2 đạt **128/136 = 94%: 122 đạt, 6 đạt một phần (TC-46, 53, 62, 69, 100, 104), 1 chặn (TC-101), 7 chưa chạy** (mục 7).
**Cập nhật 27/11 (XDPMHDT-118):** sửa và chạy lại 3 case đạt một phần: TC-46 (503 sau 1.777 / 1.712 ms nhờ hạn chờ tổng 1,5 s khi gọi service khác), TC-53 (thêm ô tìm kiếm không dấu ở màn Bản đồ), TC-100 (thêm panel DLQ trên Grafana).
Lần 2 đạt **128/136 = 94%: 125 đạt, 3 đạt một phần (TC-62, 69, 104), 1 chặn (TC-101), 7 chưa chạy**.

## 1. Tổng hợp theo 3 cấp kiểm thử

| Cấp | Cách chạy | Kết quả |
|---|---|---|
| Unit | `mvn test` (10 module Java), `flutter test` (app) | **341/341 đạt** (Java 277, Flutter 64), 22/11; 27/11 (XDPMHDT-118): content-service 33/33, feedback-service 68/68, Flutter 89/89 – mục 2 |
| Integration | `Docs/testing/integration-smoke.ps1 -Events` qua Gateway | **12/12 đạt**, 19/11 – mục 3 |
| System | Test Plan, chạy tay trên Web Admin, app Android và API | Lần 1: **65/131 đã chạy, 62 đạt, 3 không đạt** (17/11, `main@761248f`); lần 2: **128 đã chạy, 125 đạt, 3 đạt một phần, 1 chặn, 7 chưa chạy** (19–27/11) – mục 4 |

## 2. Unit test

Chạy ngày 22/11/2026 trên `main` `31853e6` (code backend và app Android giống tag `v1.0`): 341/341, giống lần 19/11 (`66e8361`).
Ngày 27/11 (XDPMHDT-118, trước khi merge): `mvn -pl services/content-service,services/feedback-service -am test` 101/101 (content 33, feedback 68; +3 test hạn chờ tổng cho TC-46), `flutter test` 89/89, `flutter analyze` không có error/warning.
Lần 17/11 (`533e64c`): 330/330 (Java 269, Flutter 61); số test tăng do PR #84 (content-service +8, app +3).

```bash
mvn test -fae                    # ở thư mục gốc repo
cd Interface && flutter test     # app Flutter
```

| Module | Số test | Đạt | Lỗi | Bỏ qua |
|---|---|---|---|---|
| common (thư viện sự kiện) | 19 | 19 | 0 | 0 |
| api-gateway | 24 | 24 | 0 | 0 |
| auth-service | 9 | 9 | 0 | 0 |
| site-service | 17 | 17 | 0 | 0 |
| content-service | 31 | 31 | 0 | 0 |
| beacon-service | 46 | 46 | 0 | 0 |
| personalization-service | 8 | 8 | 0 | 0 |
| feedback-service | 66 | 66 | 0 | 0 |
| analytics-service | 16 | 16 | 0 | 0 |
| Backend cũ (monolith) | 41 | 41 | 0 | 0 |
| **Java tổng** | **277** | **277** | **0** | **0** |
| App Flutter (`Interface`) | 64 | 64 | 0 | 0 |

Ghi chú: ngày 17/11, lần chạy `flutter test` đầu tiên có 1 file (`artifact_detail_screen_test.dart`) không chạy được vì Windows
Smart App Control chặn `flutter_tester.exe`; chạy lại thì đạt. Đây là chính sách bảo mật của máy, không phải lỗi code. Lần 19/11 không gặp.

## 3. Integration test

Chạy ngày 19/11/2026 trên `main` `66e8361`, toàn hệ thống bằng Docker (`--profile core --profile mq`), dữ liệu demo bản cuối
vừa nạp (`09_demo_seed.sql`, media chép vào `content_uploads`). Lần 17/11 (7 service chạy jar, dữ liệu mẫu) cũng 12/12 đạt.

```powershell
powershell -ExecutionPolicy Bypass -File Docs/testing/integration-smoke.ps1 -Events
```

| # | Kiểm tra | Kết quả |
|---|---|---|
| 1 | auth-service: đăng nhập `admin`, `visitor_an` qua Gateway | Đạt |
| 2 | site-service: `GET /api/heritage-sites` | Đạt (200) |
| 3 | content-service: `GET /api/artifacts/1` | Đạt (200) |
| 4 | beacon-service: `GET /api/ibeacons/registry` | Đạt (200) |
| 5 | personalization-service: `GET /api/me/favorites` (VISITOR) | Đạt (200) |
| 6 | feedback-service: `GET /api/artifacts/1/rating-summary` | Đạt (200) |
| 7 | analytics-service: `GET /api/statistics/overview` (ADMIN) | Đạt (200) |
| 8 | auth-service: `GET /api/admin/users` (ADMIN) | Đạt (200) |
| 9 | Gateway: VISITOR gọi `/api/admin/users` | Đạt (403) |
| 10 | Gateway: không token gọi `/api/admin/users` | Đạt (401) |
| 11 | Gateway: VISITOR sửa di tích | Đạt (403) |
| 12 | RabbitMQ: gửi đánh giá mới → `feedback.created` → analytics-service tăng số đánh giá hôm nay | Đạt (1 → 2) |

Luồng sự kiện đánh giá đầy đủ (FeedbackCreated, FeedbackApproved, RabbitMQ tắt thì API vẫn chạy, metric
`sh_events_published_total`) kiểm lại ở lần 2 bằng `regression-feedback.ps1` (TC-89 → TC-93), xem mục 4.

Dữ liệu demo: 6 hiện vật, mỗi hiện vật bản `vi`/`en` đúng ngôn ngữ (không dùng bản dự phòng); 18 URL media
(ảnh, ảnh nhỏ, audio) đều trả 200 qua `http://localhost:8080/api/media/files/…`.

## 4. System test (theo Test Plan)

| Nhóm | Số case | Lần 1: đã chạy | Lần 1: đạt | Lần 2: đã chạy | Lần 2: đạt | Ghi chú |
|---|---|---|---|---|---|---|
| Visitor (TC-01 → TC-21, TC-51 → TC-69) | 40 | 18 | 17 | 36 | 34 | Lần 1: TC-13 không đạt. Lần 2: TC-13, TC-17 → TC-21 (API, 19/11); TC-65 → TC-68 (app, 22/11); R1 23/11: TC-02, 12, 15, 16, 54, 61, 62 (giả lập, **đạt một phần**), 64; Ân 24/11: TC-09, 51, 55; script 25/11: TC-01, 03 → 08; Bảo 26–27/11 (mục 4.6): TC-11 (lỗi 14, đạt sau XDPMHDT-116), 14, 52, 53 (đạt sau XDPMHDT-118), 56 (giả lập), 57, 63; Bảo 26/11 (mục 4.7, XDPMHDT-117): TC-69 (**đạt một phần**, phần API, thiếu Firebase). Chưa chạy: TC-10, 58, 59, 60 (cần máy thật, beacon) |
| Administrator (TC-22 → TC-37, TC-70 → TC-74) | 21 | 17 | 15 | 21 | 21 | Lần 1: TC-36, TC-71 không đạt. Lần 2: TC-34, TC-35, TC-36, TC-71 (API); TC-70 (Web); R1 23/11: TC-24, 37, 72; script 25/11: TC-26, 28, 29, 30, 32; Bảo 25–27/11 (mục 4.7): TC-22, 23, 25, 27 (đạt sau XDPMHDT-116), 31, 33, 73, 74 |
| System (TC-38 → TC-50, TC-75 → TC-85) | 24 | 16 | 16 | 23 | 23 | Lần 2 (Tường): TC-83; R1 23/11: TC-47, 75, 85; Bảo 24/11: TC-48, 78; script 25/11: TC-38 → 45, 46 (đạt sau XDPMHDT-118), 76, 82, 84; Bảo 25/11 (mục 4.7): TC-77; NiFi 27/11 (mục 4.8): TC-49, 79, 80, 81. Chưa chạy: TC-50 (cần máy thật, beacon) |
| Tuần 6: Xuất báo cáo (TC-86, TC-108 → TC-110) | 4 | 3 | 3 | 4 | 4 | Lần 1: TC-86 chưa bấm nút trên Dashboard. Lần 2 (Tuấn, máy sạch `v1.0`, 21/11): TC-86, TC-108 → TC-110 |
| Tuần 6: Sự kiện RabbitMQ (TC-87 → TC-100) | 14 | 5 | 5 | 14 | 14 | Lần 1: TC-87, 88, 89, 90, 94. Lần 2 (Tường): TC-89 → TC-93; Bảo 24/11: TC-98, TC-100 (đạt sau XDPMHDT-118); script 25/11: TC-87, 88, 99; Bảo 25–27/11 (mục 4.7): TC-94, 95, 96, 97 |
| Tuần 6: Thông báo, pin, thời gian thực (TC-101 → TC-107) | 7 | 1 | 1 | 5 | 4 | Lần 1: TC-69 chỉ phần 401. Lần 2: TC-105 (Tuấn, 21/11); script 25/11: TC-103; TC-101 **Chặn** (thiếu Firebase và máy thật); Bảo 25–26/11 (mục 4.7): TC-104 (**đạt một phần**, thiếu Firebase), 106; NiFi 27/11: TC-107. Chưa chạy: TC-102 |
| Tuần 6: Hoàn thiện Mobile, Web (TC-111 → TC-119) | 9 | 3 | 3 | 9 | 9 | Lần 1: TC-116, TC-117, TC-118. Lần 2: TC-116, TC-118 (API); TC-114, TC-115, TC-117 (app, Web); Bảo 26/11 (mục 4.6): TC-111, 112, 113, 119 |
| Tuần 6: Giám sát, kiểm thử tải (TC-120 → TC-124) | 5 | 1 | 1 | 5 | 5 | Lần 1: TC-120. Lần 2 (Bảo, 24/11): TC-120, 121, 122, 124; TC-123 (k6 500 VU) đạt theo số đo k6 21/11 (mục 4.3, [load-test-w7.md](load-test-w7.md)) |
| Tuần 6: Trường hợp đặc biệt, bản chốt (TC-125 → TC-131) | 7 | 1 | 1 | 6 | 6 | Lần 1: chỉ TC-130 (API 403). R1 23/11: TC-131; Bảo 24/11: TC-125; script 25/11: TC-129, 130; Bảo 26/11 (mục 4.6): TC-126, 127 (lỗi 14, đạt sau XDPMHDT-116). Chưa chạy: TC-128 (cần 2 beacon thật) |
| Tuần 7: Quản lý người dùng (TC-132 → TC-136) | 5 | — | — | 5 | 5 | Chạy 17/11 qua Gateway (Tường). Không thuộc lần 1 tuần 6 |
| **Tổng** | **136** | **65** | **62** | **128** | **125** | Lần 1: 66 case chưa chạy. Lần 2: phần của Tường (API + app, Web), TC-132 → TC-136, phần Thống kê của Tuấn, 20 case R1 (15 case mới; TC-13, 21, 70, 133, 134 đã tính trước), 9 case của Bảo (mục 4.3), 3 case của Ân (mục 4.4), 30 case bằng script (mục 4.5); 3 đạt một phần (TC-46, TC-62, TC-100); 1 chặn (TC-101); 27/11: 13 case app, 15 case Web, 5 case NiFi, TC-123 (mục 4.3, 4.6 → 4.8); XDPMHDT-117: TC-69 (phần API, 26/11); XDPMHDT-118: TC-46, 53, 100 từ đạt một phần lên đạt; 3 đạt một phần (TC-62, 69, 104); 1 chặn (TC-101); 7 chưa chạy (TC-10, 50, 58, 59, 60, 102, 128) |

- Tỉ lệ đã chạy lần 1: **65/131 = 50%** (yêu cầu ≥ 90%, chưa đạt).
- Tỉ lệ đạt trên số đã chạy lần 1: **62/65 = 95%**.
- Lần 2 (19–25/11): **93/136** case đã chạy: 90 đạt, 3 đạt một phần (TC-46, TC-62, TC-100); TC-101 bị chặn. Kết quả từng case phần Đánh giá: [regression-w7-feedback.md](regression-w7-feedback.md); phần Thống kê: mục 4.1; R1: mục 4.2; giám sát, sự kiện, ngoại tuyến: mục 4.3; app (Ân): mục 4.4; API, MySQL, RabbitMQ, Docker: mục 4.5.
- Lần 2 sau 27/11: **128/136** case đã chạy: 125 đạt, 3 đạt một phần (sau XDPMHDT-118); TC-101 bị chặn; 7 case chưa chạy. Chi tiết 27/11: app mục 4.6, Web Admin và TC-69 (API) mục 4.7, NiFi mục 4.8, k6 mục 4.3.
- Tỉ lệ đã chạy lần 2 (yêu cầu ≥ 90% = 123/136): **128/136 = 94%, đạt** (25/11 là 93/136 = 68%). 8 case còn lại cần điện thoại Android thật, beacon thật hoặc Firebase (mục 7).
- Tỉ lệ đạt của case ưu tiên Cao (yêu cầu ≥ 90%): 101 case C, đã chạy 96, đạt 94 = **98%, đạt** (2 đạt một phần: TC-62, 104; chưa chạy hoặc chặn: TC-10, 58, 59, 60, 101).

### 4.1 Chạy lại phần Thống kê trên máy sạch `v1.0` (Tuấn, 21/11/2026)

Chạy qua Gateway trên máy sạch (tag `v1.0` = `2a6e9e7`, Docker `--profile all`). Dashboard và file xuất dùng 1.000.000 lượt thử
nạp vào `analytics_db` (tên di tích, hiện vật lấy từ seed thật) vì máy sạch không bật NiFi. Bằng chứng: Hình 5.3 – 5.5 và mục 5.7
của 12_Final_Report, log `bench_1m.log`.

| Mã TC | Nội dung | Kết quả | Ghi chú |
|---|---|---|---|
| TC-86 | Xuất PDF/Excel trên Dashboard cùng bộ lọc | Đạt | Lọc "Hoàng thành Thăng Long": 2 file mở được; số khớp Dashboard (5.000 khách, 380.951 lượt, điểm TB 3,86, 10.000 phản hồi) |
| TC-108 | File Excel đủ sheet | Đạt | Có 4 sheet: Tổng quan, Top hiện vật, Ngôn ngữ, Đánh giá |
| TC-109 | File PDF đúng tiếng Việt | Đạt | 2 trang, có tiêu đề, khoảng ngày, di tích, bảng số; tiếng Việt có dấu đúng. Giờ "Xuất lúc" lệch 7 giờ (container chạy UTC) – lỗi Minor, mục 6 |
| TC-110 | Phân quyền và kiểm tra tham số xuất báo cáo | Đạt | `visitor_an` 403; không token 401; `format` sai 400; khoảng ngày ngược 400 |
| TC-105 | Số "Hôm nay" tăng khi có lượt mới | Đạt | Du khách ghi 1 lượt xem thì số "Hôm nay" tăng ngay (sự kiện RabbitMQ), không cần NiFi |

### 4.2 Hồi quy R1 – 20 case (Bảo chạy thay Ân, 23/11/2026)

Chạy trên `main` `dffc5c0` (sau #99: đã sửa 8 lỗi Minor, giờ VN, index `fact_visits`), Docker `--profile all`, máy ảo Pixel 7, Web Admin Chrome.
13 case API chạy tự động bằng script `Chay_R1_v1.ps1` (để ngoài repo, thư mục môn học); 7 case giao diện chạy tay. Danh sách R1: [regression-list.md](regression-list.md) mục 4.
TC-15 và TC-134 có ghi dữ liệu nhưng script tự trả lại như cũ (yêu thích hiện vật 2 của `visitor_an`; khoá rồi mở khoá `visitor_mai`).

| Mã TC | Nội dung | Cách chạy | Kết quả | Ghi chú |
|---|---|---|---|---|
| TC-02 | Đăng nhập đúng | Script | Đạt | 200, có `accessToken`, `refreshToken`, `tokenType = Bearer` |
| TC-12 | Chi tiết hiện vật tiếng Anh | Script | Đạt | `languageCode = en`, `fallback = false`, có media. Tiêu đề EN theo seed bản cuối là "Constellation of Literature Pavilion" (đã sửa test-plan theo, 25/11) |
| TC-13 | Lấy hiện vật theo beacon | Script | Đạt | Registry 1/1 → `artifactId = 1`; chi tiết đúng Khuê Văn Các |
| TC-15 | Thêm và bỏ yêu thích | Script | Đạt | Thêm 2 lần chỉ 1 dòng, có tên hiện vật; xoá xong không còn |
| TC-16 | Xem lịch sử tham quan | Script | Đạt | Có hiện vật 1 và 2, mới nhất trước |
| TC-21 | Xem đánh giá đã duyệt | Script | Đạt | Chỉ APPROVED, mới nhất trước, không lộ `userId`/email |
| TC-24 | Thêm di tích bằng bản đồ | Tay (Web) | Đạt | Bấm bản đồ tự điền vĩ độ/kinh độ, lưu, F5 vẫn còn; đã xoá sau khi thử. Ghi chú: ô vĩ độ/kinh độ dùng chữ gợi ý là số tọa độ thật, dễ tưởng đã có tọa độ |
| TC-37 | Dashboard thống kê | Script + tay | Đạt | API 200, đủ trường; máy không chạy NiFi nên số liệu bằng 0 |
| TC-47 | Định tuyến Gateway | Script | Đạt | 6 đường dẫn của test-plan 200; `/feedbacks`, `/rating-summary` trả dữ liệu feedback-service |
| TC-54 | Đổi ngôn ngữ trên màn chi tiết | Tay (app) | Đạt | VI ↔ EN đổi ngay, có phụ đề thuyết minh. Chưa kiểm nhãn "Chưa có bản dịch" vì cả 6 hiện vật seed đều có bản EN |
| TC-61 | Nút giả lập phát hiện beacon | Tay (app) | Đạt | Theo nghiệm thu PR #88 (EM-03, EM-05): mở chi tiết, ghi lượt BEACON, đồng bộ không trùng. Ảnh sheet giả lập: `Docs/user-manual/images/10-simulate.png` (chụp lại 25/11) |
| TC-62 | Ghi lượt tham quan khi ra vùng | Tay (app, giả lập) | Đạt một phần | Màn Quét iBeacon → nút giả lập → Vào vùng: app tự ra vùng sau 10 s mất sóng (đúng quy tắc `exitTimeout`) và ghi lượt `source = BEACON`, `durationSeconds = 10`, `clientEventId` là UUID. Chưa kiểm đứng trong vùng 30 s và quét BLE thật (cần điện thoại Android + beacon). Khi chạy phát hiện lỗi treo quyền lần đầu mở màn (mục 6, số 9) |
| TC-64 | Yêu thích, Lịch sử trên app qua Gateway | Tay (app) | Đạt | Theo nghiệm thu PR #88 (EM-01, EM-04, EM-07) |
| TC-70 | Duyệt / ẩn đánh giá trên Web | Tay (Web) | Đạt | Huy hiệu "Dữ liệu từ API"; Ẩn rồi Duyệt lại 1 đánh giá, F5 giữ trạng thái, thẻ số liệu cập nhật; giờ hiển thị đúng giờ VN |
| TC-72 | Dashboard số liệu thật | Script | Đạt | Tải ≤ 5 s (tổng 36 ms, lọc di tích 49 ms, lọc 30 ngày 85 ms). Phần khớp số liệu nguồn cần NiFi, đã kiểm ở mục 4.1 |
| TC-75 | Bảng route Gateway tuần 5 | Script | Đạt | 11 nhóm route qua 8080 đều 200 |
| TC-85 | Smoke test toàn hệ thống | Script | Đạt | Đăng nhập admin, visitor + 1 API chính của 7 service đều 200 |
| TC-131 | Smoke test bản chốt code | Script + tay | Đạt | Gộp TC-85, luồng app (đăng nhập, mô phỏng beacon, yêu thích, lịch sử) và TC-70 |
| TC-133 | Danh sách và tìm kiếm người dùng | Script | Đạt | `q=an` có `visitor_an`, đủ `role`, `locked`, không có `passwordHash` |
| TC-134 | Khoá và mở khoá tài khoản | Script | Đạt | Khoá 200; đăng nhập 403; refresh token cũ 401; mở khoá 200; đăng nhập lại 200 |

Cùng đợt, màn "Hiện Vật Di Sản" trên app (trước đây dùng dữ liệu mẫu gõ cứng) đã nối `GET /api/artifacts`: hiện đủ 6 hiện vật thật, lọc theo di tích thật.

### 4.3 Giám sát, sự kiện, ngoại tuyến (Bảo, 24/11/2026)

Chạy trên `main` `e2ff2fc`, Docker `--profile all`, máy ảo Pixel 7. TC-48, 78, 98, 100, 120, 121, 122, 124 chạy bằng script `Chay_Monitoring_v1.ps1` (để ngoài repo, thư mục môn học) kèm thao tác tay trên Prometheus, Grafana, RabbitMQ; TC-125 chạy tay trên app.

| Mã TC | Nội dung | Kết quả | Ghi chú |
|---|---|---|---|
| TC-48 | Metric Prometheus của từng service | Đạt | 8 cổng 8080 → 8087: `/actuator/prometheus` 200, có `http_server_requests_*` |
| TC-78 | Prometheus target và dashboard Grafana theo service | Đạt | Target `smart-heritage` 8/8, `mysql`, `redis` UP; dashboard `sh-services` chọn content-service thì mọi biểu đồ chỉ còn content-service. Ghi chú: ô "Độ trễ p95" trống khi 5 phút không có request |
| TC-98 | Định dạng sự kiện RabbitMQ | Đạt | Đủ 5 loại: `visit.recorded`, `artifact.updated`, `beacon.battery.low`, `feedback.created`, `feedback.approved`; có `eventId` (UUID), `type`, `version`, `occurredAt` (ISO), `data`; không có `__TypeId__`; `content_type = application/json`. `artifact.updated` chỉ phát khi có trường đổi (đúng thiết kế) |
| TC-100 | Tin hỏng vào DLQ | Đạt | Tin hỏng vào `analytics.visit-recorded` chuyển sang `analytics.dlq` sau 9 s, còn nguyên; Prometheus thấy DLQ tăng; cảnh báo `RabbitMQDeadLetter` bật. 24/11 thiếu panel Grafana (đạt một phần). 27/11 (XDPMHDT-118): thêm hàng "RabbitMQ – hàng đợi & DLQ" vào dashboard *Smart Heritage – Microservices*; publish `{"bad":true}` → ô "Tin trong DLQ" = 1 (đỏ), biểu đồ có `analytics.dlq`; đã Purge DLQ. Ảnh `Monitoring_nghiem_thu` TC-100_1 → 4 |
| TC-120 | RabbitMQ vào Prometheus | Đạt | Target `rabbitmq` UP, có số liệu 11 queue (6 chính + 5 DLQ) |
| TC-121 | Cảnh báo ServiceDown | Đạt | Tắt content-service: Pending sau 12 s, Firing ~72 s; bật lại: UP sau 10 s, cảnh báo tắt sau 30 s |
| TC-122 | Các quy tắc cảnh báo còn lại | Đạt | `RabbitMQDeadLetter` 51 s; `RabbitMQQueueBacklog` (> 100 tin, queue tạm) 161 s; `HighLatencyP95` (api-gateway p95 = 3,2 s) Firing; `HighErrorRate5xx` (api-gateway 94%) Pending rồi Firing (xem lịch sử `ALERTS`) |
| TC-124 | Phát hiện và khôi phục ≤ 5 phút | Đạt | Tắt → báo (~72 s) → bật lại → UP 10 s → tắt cảnh báo 30 s, tổng ≈ 2 phút |
| TC-125 | App khi mất mạng | Đạt | Chế độ máy bay: Yêu thích lưu trên máy ("sẽ đồng bộ khi có mạng"); Đánh giá, Đánh giá của tôi báo "Không kết nối được máy chủ" + Thử lại, giữ dữ liệu đã nhập; Lịch sử hiện bản đã lưu; không crash; có mạng lại tải ngay. Phát hiện lỗi Minor 10, 11 (mục 6) |

TC-123 (k6 500 VU): **Đạt**. Chạy 19:59 ngày 21/11 trên `v1.0` (`2a6e9e7`), Docker `--profile all`: giữ 500 VU, 81.479 request, p95 toàn bộ 31 ms (API chậm nhất đăng nhập 87 ms), lỗi 0%, HikariCP content-service tối đa 1 kết nối bận, 0 request chờ. Chi tiết và so sánh lần 1: [load-test-w7.md](load-test-w7.md) (điền 27/11).

### 4.4 App trên máy ảo (Ân, 24/11/2026)

Chạy tay trên máy ảo Android (Pixel 4), `main` `e2ff2fc`, khi chụp ảnh User Manual du khách (PR #103). Ảnh: `Docs/user-manual/images/`.
Ân chạy lại cả TC-54, 61, 64, 65, 68 (đã tính ở lần 2, cùng kết quả Đạt); 3 case mới:

| Mã TC | Nội dung | Kết quả | Ghi chú |
|---|---|---|---|
| TC-09 | Bản đồ di tích trên app | Đạt | 2 di tích có marker + danh sách (`08-map.png`) |
| TC-51 | Bản đồ hiện di tích gần nhất | Đạt | Văn Miếu đứng đầu danh sách (`08-map.png`) |
| TC-55 | Ảnh, audio, video | Đạt | Đủ tab Mô tả, Hình ảnh, Âm thanh, Thông tin (`11-detail.png`) |

### 4.5 Các case API, MySQL, RabbitMQ, Docker còn lại (Bảo, 25/11/2026)

Chạy bằng script `Chay_lan2_API_v1.ps1` (để ngoài repo, thư mục môn học) trên `main` `8e8feb7`, Docker `--profile all`. Phần C (tắt service) chạy lại sau khi sửa lỗi Minor 12 trong nhánh XDPMHDT-114.
Dữ liệu thử đều được xoá hoặc trả lại (media, beacon 9/9, pin beacon 3, ngôn ngữ `fr` tắt lại); chỉ còn tài khoản `visitor1` (có từ lần 1) và vài lượt tham quan mới của `visitor_an`.

| Mã TC | Nội dung | Kết quả | Ghi chú |
|---|---|---|---|
| TC-01 | Đăng ký tài khoản mới | Đạt | `visitor1` đã có từ lần 1: đăng ký lại 400 "Username đã tồn tại!" |
| TC-03 | Đăng nhập sai mật khẩu | Đạt | 401 "Sai tài khoản hoặc mật khẩu!", không có token |
| TC-04 | Làm mới token và đăng xuất | Đạt | refresh 200 (access token mới); logout 200; refresh lại token đã thu hồi 401 |
| TC-05 | Xem hồ sơ của mình | Đạt | `visitor_john`: `fullName = John Smith`, `preferredLanguage = en`, không có `passwordHash` |
| TC-06 | Không xem được hồ sơ người khác | Đạt | `visitor_an` gọi `/api/auth/users/3`: 403 |
| TC-07 | Di tích gần nhất sắp xếp gần → xa | Đạt | Văn Miếu đứng đầu; `distanceKm` 0.01, 0.85 |
| TC-08 | Nearby với tham số sai | Đạt | `lat=95`: 400 kèm thông báo |
| TC-26 | Tạo hiện vật với siteId không tồn tại | Đạt | 400 "Di tích không tồn tại: siteId=999" |
| TC-28 | Gắn link media | Đạt | 201; có trong danh sách media và chi tiết hiện vật; đã xoá |
| TC-29 | Nhập bản dịch tiếng Anh | Đạt | Seed bản cuối đã có bản `en` của hiện vật 3: `lang=en` trả "Doan Mon Gate", `fallback = false` |
| TC-30 | Đăng ký beacon mới | Đạt | 201; tạo trùng (uuid, major, minor) 400; dùng Major 9 Minor 9 để không đụng seed, đã xoá |
| TC-32 | Gán beacon cho hiện vật khác di tích | Đạt | 400 "Hiện vật ID 3 không thuộc di tích ID 1" |
| TC-38 | Ghi dữ liệu không có token | Đạt | 3 lệnh đều 401; body `{"success":false,"message":"Bạn chưa đăng nhập…"}` (kiểm bằng `curl.exe -i`) |
| TC-39 | Visitor gọi API của ADMIN | Đạt | 3 lệnh đều 403 |
| TC-40 | Token sửa chữ ký | Đạt | 401 |
| TC-41 | Chống giả mạo header X-User-* | Đạt | Kèm `X-User-Id: 3`, `X-User-Role: ADMIN`: vẫn trả yêu thích của user 2; `/api/admin/visits` 403 |
| TC-42 | Fallback ngôn ngữ | Đạt | Bảng `languages` chỉ bật `vi`, `en` nên `lang=fr` trả 400 "Ngôn ngữ chưa được hỗ trợ" (đúng thiết kế); bật tạm `fr`: 200, `languageCode = vi`, `fallback = true`; đã tắt lại `fr` |
| TC-43 | Đồng bộ offline không ghi trùng | Đạt | Lần 1 `created = 1`; lần 2 `duplicates = 1`; DB 1 dòng, `duration_seconds` NULL |
| TC-44 | Mỗi service một schema | Đạt | Đủ 7 schema + `smart_heritage_db`; `site_user` đọc `content_db.artifacts`: ERROR 1142 (chạy trên DB đang dùng, không `down -v`) |
| TC-45 | Id thống nhất giữa các schema | Đạt | 0 id hiện vật lạc ở `beacon_db`, `likes`, `visits`, `feedbacks` |
| TC-46 | Gọi service phụ thuộc khi nó bị tắt | Đạt | Tắt site-service: tạo hiện vật 503 "Không kết nối được dịch vụ di tích…"; tắt content-service + Backend cũ: gửi đánh giá 503 "Không kết nối được dịch vụ hiện vật…"; service gọi không treo, không sập. Thời gian **~4,1 – 4,3 s** (3 lần chạy), test-plan yêu cầu khoảng 2 s: timeout gọi service khác 2 s cộng thời gian phân giải tên container đã tắt. 27/11 (XDPMHDT-118): thêm hạn chờ tổng 1,5 s (`app.clients.deadline`, gồm cả bước tra tên) cho content-service và feedback-service; chạy lại `Chay_lan2_API_v1.ps1 -Phan C`: 503 sau **1.777 ms** (tắt site) và **1.712 ms** (tắt content) → **Đạt**; TC-129 chạy lại vẫn đạt (503 sau 2,0 – 2,6 s) |
| TC-76 | Phân quyền route mới | Đạt | Không token 401; visitor gọi API ADMIN 403; lịch sử `visitor_an` và `visitor_mai` không trùng id |
| TC-82 | Redis cache chi tiết hiện vật | Đạt | Lần 2: 34 ms; key `sh:content:detail:v1:{2}:data` có TTL (mặc định `app.detail-cache.ttl-seconds = 120` s, test-plan ghi 10 phút) nên hết hạn sẽ đọc lại DB |
| TC-84 | feedback-service trong docker compose | Đạt | Container healthy; DB user `feedback_user`; `:8086` health UP; có `http_server_requests_*` |
| TC-87 | VisitRecorded khi ghi 1 lượt | Đạt | Đúng 1 tin `visit.recorded`; `data` có `visitId, artifactId, detectedAt` – đúng hợp đồng analytics-service (`VisitEventData`, PR #63). Đã sửa kỳ vọng TC-87 trong test-plan (bản cũ ghi thêm `userId, siteId, durationSeconds, source`) |
| TC-88 | VisitRecorded khi gửi theo lô | Đạt | Lô 2 event: 2 tin; gửi lại trùng `clientEventId`: 0 tin |
| TC-99 | Bên nhận chống xử lý trùng | Đạt | Gửi lại đúng tin `visit.recorded` (cùng `eventId`) vào `analytics.visit-recorded`: số "Hôm nay" không đổi (21 → 21), `rt_processed_events` 1 dòng. Phần FCM chưa kiểm (chưa có Firebase) |
| TC-103 | Pin thấp phát BeaconBatteryLow | Đạt | Pin 15: 1 tin cho beacon 3, đủ `beaconId, siteId, batteryLevel, checkedAt`; pin 50: 0 tin; đã trả lại 92 |
| TC-129 | Tắt 1 service | Đạt | Sau khi sửa lỗi 12: tắt content, feedback, personalization lần lượt: 503 sau 2,0 – 3,0 s, service khác vẫn 200, bật lại dùng được ngay. Trước khi sửa: 500 sau ~14 s hoặc treo > 30 s (mục 6, số 12) |
| TC-130 | VISITOR vào trang quản trị | Đạt | API ghi `POST /api/heritage-sites`, `PATCH /api/admin/feedbacks/3/status`: 403 (phần trang HTML đã kiểm ở lần 1) |

### 4.6 App trên máy ảo (Bảo, 26–27/11/2026)

Chạy tay trên máy ảo Android Pixel 7, `DEMO_MODE=true`, `main` `38874a1`, Docker `--profile all`. Ảnh lưu ngoài repo (thư mục môn học `App_nghiem_thu`, tên ảnh theo mã TC). TC-63 và TC-119 bổ sung bằng chứng ngày 27/11.

| Mã TC | Nội dung | Kết quả | Ghi chú | Ảnh |
|---|---|---|---|---|
| TC-11 | Từ chối quyền hoặc tắt Bluetooth | Đạt | 26/11: không crash nhưng chỉ có "Thử lại", không chỉ vào Cài đặt → lỗi Minor 14. 27/11 chạy lại trên nhánh XDPMHDT-116: thông báo chỉ đường vào Cài đặt + nút "Mở Cài đặt" mở đúng App info; cấp Location, Nearby devices rồi quay lại app thì màn Quét tự quét lại (Registry 3) | TC-11_1 → 4 |
| TC-14 | Xem lại nội dung khi rời vùng beacon | Đạt | Chế độ máy bay: màn chi tiết giữ nội dung, có dòng "Đang xem nội dung đã tải – có thể thiếu cập nhật mới nhất" | TC-14_1 → 3 |
| TC-52 | Chỉ đường | Đạt | Mở Google Maps, đích Văn Miếu Quốc Tử Giám | TC-52_1 |
| TC-53 | Từ chối quyền vị trí, mất mạng | Đạt | Thiếu quyền: vẫn hiện bản đồ, 2 di tích và hướng dẫn "Vào Cài đặt → Ứng dụng → Smart Heritage → Quyền → Vị trí"; mất mạng: "Không tải được danh sách di tích. Kiểm tra kết nối mạng." + Thử lại, không crash. 26/11 màn Bản đồ chưa có ô tìm kiếm (đạt một phần). 27/11 (XDPMHDT-118) thêm ô tìm theo tên/địa chỉ, không phân biệt dấu: "Văn Miếu" và "van mieu" → Văn Miếu – Quốc Tử Giám (1/2 địa điểm, còn 1 marker); "xyz" → "Không có di tích khớp" + nút Xoá tìm kiếm | TC-53_1 → 6 |
| TC-56 | Rời vùng beacon vẫn xem tiếp | Đạt (giả lập) | Giả lập ra vùng trên máy ảo (chưa có beacon thật); 13:15 → 13:21 màn chi tiết không đóng, audio vẫn phát | TC-56_1 → 6 |
| TC-57 | Mở lại hiện vật đã xem khi mất mạng | Đạt | Hiện vật đã xem mở từ bộ nhớ máy; hiện vật chưa xem báo "Không có kết nối mạng và chưa có bản đã tải" + Thử lại; màn "Hiện vật đã xem" lưu 20 hiện vật gần nhất | TC-57_1 → 3 |
| TC-63 | Hàng đợi ngoại tuyến | Đạt | 4 lượt ghi lúc máy bay ("Chờ đồng bộ") lên server khi có mạng: `visits` id 10 → 13, mỗi dòng một `client_event_id`; kiểm trùng `(user_id, client_event_id)`: 0 dòng (27/11); khoá duy nhất `uq_visits_user_event` chặn gửi lại. Lượt id 10 có `duration_seconds` NULL vì mở từ hộp thoại demo ở màn chính (không đo được thời gian dừng, đúng thiết kế) | TC-63_1 → 5 |
| TC-111 | Màn Lịch sử | Đạt | Nhóm theo ngày, có thời gian dừng; bấm 1 dòng mở đúng chi tiết | TC-111_1 → 2 |
| TC-112 | Màn Yêu thích | Đạt | Bỏ tim: dòng mất ngay, báo "Đã bỏ yêu thích"; hết danh sách thì có hướng dẫn | TC-112_1 → 4 |
| TC-113 | Báo đang ngoại tuyến | Đạt | "Đang ngoại tuyến – sẽ đồng bộ sau"; có mạng lại thì đồng bộ và mất dòng báo | TC-113_1 → 2 |
| TC-119 | Chi tiết hiện vật không vỡ giao diện | Đạt | 27/11, màn hình nhỏ 720×1280, mật độ 320 (360 dp): Nội dung thuyết minh ~1.900 ký tự xuống dòng gọn, cuộn tới cuối được; Cột cờ Hà Nội (bản vi không có audio) hiện "Chưa có file âm thanh cho ngôn ngữ này."; bản đồ hiển thị đủ. Dữ liệu thử đã trả lại (181 ký tự) | TC-119_1 → 9 |
| TC-126 | Tắt Bluetooth khi đang quét | Đạt | "Bluetooth đã tắt. Bật Bluetooth để tiếp tục nhận diện hiện vật." + Thử lại; bật lại thì quét tiếp | TC-126_1 → 2 |
| TC-127 | Từ chối quyền | Đạt | 26/11: lỗi Minor 14 (như TC-11). 27/11 chạy lại trên nhánh XDPMHDT-116, cài mới app, từ chối mọi quyền: màn Quét hiện hướng dẫn vào Cài đặt + nút "Mở Cài đặt"; Bản đồ, Đăng nhập vẫn dùng được | TC-127_1 → 7 |

### 4.7 Web Admin (Bảo, 25–27/11/2026)

Chrome + Live Server, tài khoản `admin`, `main` `8a416bf` (25/11) và `38874a1` (26–27/11), Docker `--profile all`. Ảnh lưu ngoài repo (thư mục môn học `Web_nghiem_thu`). TC-74, 95, 97 bổ sung bằng chứng ngày 27/11.

| Mã TC | Nội dung | Kết quả | Ghi chú | Ảnh |
|---|---|---|---|---|
| TC-22 | Đăng nhập Web Admin | Đạt | Vào Dashboard; `sh_access_token`, `sh_refresh_token` trong localStorage; API gọi qua 8080 trả 200 | TC-22_1 → 3 |
| TC-23 | Visitor không vào được trang quản trị | Đạt | `visitor_an` đăng nhập Web Admin bị chặn: "Tài khoản hiện tại không có quyền quản trị"; API ghi trả 403 đã kiểm ở TC-130 | TC-23_1 |
| TC-25 | Sửa, xoá di tích | Đạt | Thêm "QA TC-25", sửa tên và toạ độ trên bản đồ, xoá: danh sách về 2 di tích | TC-25_1 → 3 |
| TC-27 | CRUD hiện vật trên CMS | Đạt | Tạo, sửa, xoá "QA TC-27" đều lưu thật; thiếu bản dịch thì báo "Bản dịch cần tiêu đề và nội dung thuyết minh". 25/11 trang chưa có tìm kiếm, lọc; 27/11 thêm trong XDPMHDT-116 và chạy lại: "bia tien si" (không dấu) → Bia Tiến sĩ; lọc Hoàng thành Thăng Long → 3 hiện vật; "kvc" → Khuê Văn Các; "xyz" → "Không có hiện vật khớp bộ lọc." | TC-27_1 → 10 |
| TC-31 | Gán beacon cho hiện vật cùng di tích | Đạt | Gán beacon 1/3 (id 4) cho Bia Tiến sĩ: cột Hiện vật đổi ngay; bỏ gán về "(Chưa gán)" | TC-31_1 → 3 |
| TC-33 | Danh sách beacon có trạng thái và pin | Đạt | Có cột Trạng thái, Pin; lọc Văn Miếu ra 3 beacon; beacon 1/3 "Không hoạt động", 15% | TC-33_1 → 2 |
| TC-69 | Thông báo FCM (phần API) | Đạt một phần | Chạy 26/11 bằng PowerShell qua Gateway: `POST /api/me/devices` không token → 401; `visitor_an` → 201 (`isNew` true, `welcomeNotificationSent` false), `beacon_db.devices` có dòng `user_id` 2; `GET /api/me/devices` chỉ trả `tokenPreview` (không lộ token đầy đủ); `/api/me/devices/test`: 0 thiết bị gửi được. Phần điện thoại nhận thông báo chưa kiểm (thiếu Firebase, máy Android thật). Thiết bị thử đã xoá | Log terminal |
| TC-73 | Quản lý danh sách ngôn ngữ | Đạt | Thêm `fr` đang hoạt động: dropdown có en, fr, vi; tắt `fr`: chỉ còn en, vi; đã tắt lại | TC-73_1 → 3 |
| TC-74 | Upload file media | Đạt | Ảnh hợp lệ tải lên, link qua Gateway (sau lỗi 13); file sai định dạng: "File rỗng hoặc định dạng không hỗ trợ"; 27/11: ảnh 12 MB báo "Tệp quá lớn: ảnh tối đa 10 MB, audio 30 MB, video 100 MB." | TC-74_1 → 4 |
| TC-77 | Swagger gom tại Gateway | Đạt | Chọn được 7 service + backend-cu; Authorize bằng token; thử API trả 200; API có mô tả | TC-77_1 → 3 |
| TC-94 | ArtifactUpdated khi đổi hiện vật | Đạt | Queue tạm `qa.tc94` (bind `artifact.updated`): 4 tin `ArtifactUpdated` cho hiện vật 7 khi thêm, sửa, xoá; đã xoá queue | TC-94_1 → 4 |
| TC-95 | Sửa hiện vật thì xoá cache | Đạt | 27/11: `GET /api/me/favorites` trả tên cũ → sửa Tiêu đề bản vi → trả "Bia Tiến sĩ QA" ngay; log personalization `ArtifactUpdated … cacheKey=artifact:summary:2 deleted=true` (00:05:26), Redis chỉ còn `artifact:summary:1`; đã trả tên cũ | TC-95_1 → 8 |
| TC-96 | Đổi beacon thì registry mới | Đạt | Gán beacon 4 → hiện vật 2: `/api/ibeacons/registry` trả beacon 4 trỏ `artifactId = 2`. Ghi chú: beacon-service không phát sự kiện khi gán; registry đọc thẳng DB nên cập nhật ngay | TC-96_1 → 4 |
| TC-97 | Không gửi sự kiện khi lưu DB thất bại | Đạt | 27/11, queue tạm `qa.tc97` (bind `feedback.created`): 2 request đồng thời → 201 + 409; rating 9 → 400 "Số sao (rating) phải nằm trong khoảng 1–5"; `feedbacks` 6 → 7; đúng 1 tin `FeedbackCreated` (`artifactId 4`). Đánh giá thử đã ẩn | TC-97_1 → 6 |
| TC-104 | Admin nhận cảnh báo pin | Đạt một phần | Web: banner "Cảnh báo pin beacon thấp" (beacon #3, 15%), thẻ "Pin thấp" = 1; đã trả pin về cũ. Phần điện thoại admin nhận FCM chưa kiểm (thiếu Firebase, máy thật) | TC-104_1 → 3 |
| TC-106 | Dashboard đếm đánh giá mới | Đạt | "Phản hồi mới hôm nay" 0 → 1; gửi lại (ghi đè) vẫn 1; đánh giá thử đã ẩn | TC-106_1 → 6 |

### 4.8 NiFi đồng bộ sang `analytics_db` (Bảo, 27/11/2026)

`main` `38874a1`, Docker `--profile all --profile nifi`, import `Infra/nifi/flows/analytics-sync.json` vào NiFi mới (volume `nifi_conf` trống), `analytics_db` trống trước khi chạy. Chạy từng processor bằng **Run Once** để đối chiếu; TC-79, 81 bật processor đọc chạy theo lịch 60 s. Ảnh lưu ngoài repo (thư mục môn học `NiFi_nghiem_thu`, ảnh tổng hợp `NiFi_tong_hop.png`).

| Mã TC | Nội dung | Kết quả | Ghi chú |
|---|---|---|---|
| TC-49 | NiFi chuyển visits sang analytics_db | Đạt | Lần 1: `fact_visits` 13/13 dòng (bằng nguồn); state `visits@!@visit_id = 13`, chạy lại không đọc thêm; Clear state rồi chạy lại: đọc lại 13 dòng, vẫn 13 (UPSERT, không nhân đôi) |
| TC-79 | NiFi feedbacks → fact_feedbacks | Đạt | Lần đầu 7/7 dòng, trạng thái khớp nguồn; duyệt id 9 lúc 01:07:23 → `fact_feedbacks` APPROVED trước 01:08:03 (≤ 40 s); ẩn → REJECTED sau ≤ 1 phút; vẫn 7 dòng, không trùng `feedback_id` |
| TC-80 | NiFi danh mục | Đạt | `dim_artifact` 6, `dim_site` 2, `dim_user_lang` 4 (bằng nguồn); `dim_user_lang` chỉ có `user_id`, `language_code`; chạy 2 lần không đổi (ghi đè theo id) |
| TC-81 | Lượt tham quan mới lên Dashboard | Đạt | Ghi 1 lượt qua `POST /api/me/visits` lúc 01:11:29 → `fact_visits` 14 lúc 01:11:57 (28 s); Dashboard "Lượt tương tác" 13 → 14, biểu đồ có thêm 27/11 |
| TC-107 | NiFi tính bảng tổng hợp theo ngày | Đạt | Run Once "Hen gio 01:00 tong hop ngay": `agg_daily_views` 5 ngày khớp số tính lại từ `fact_visits`; chạy lần 2 không đổi |

Ghi chú: `fact_feedbacks.created_at` làm tròn tới mili-giây (NiFi chuyển kiểu thời gian); `dim_artifact.site_name` để NULL vì flow không ghi cột này – Dashboard lấy tên di tích bằng cách nối `dim_site` nên không ảnh hưởng.

## 5. Lỗi theo mức độ

| Mức độ | Hạn sửa | Tổng (lần 1) | Đã đóng | Còn mở sau lần 2 |
|---|---|---|---|---|
| Blocker | Trong 24 giờ | 0 | – | 0 |
| Critical | Trước Thứ 3 tuần 7 | 0 | – | 0 |
| Major | Trước Thứ 5 tuần 7 | 0 | – | 0 |
| Minor | Nếu còn thời gian | 0 (lần 2 phát hiện 14) | 14 | 0 |

Ba case Không đạt ở lần 1 chưa xếp mức và chưa mở Issue: TC-13 (endpoint beacon cũ trả 404), TC-36 (số mẫu trên DB lệch Test Plan), TC-71 (đổi vị trí beacon qua Gateway trả 404, gọi thẳng cổng 8084 thì 200).
Lần 2 xác định cả ba **không phải lỗi code** (nguyên nhân ở mục 7), nên không mở Issue; đến 19/11 chưa có lỗi Blocker/Critical nào.

## 6. Lỗi còn tồn

Các lỗi Minor phát hiện ở lần 2 (Thống kê trên máy sạch 21/11, phần D 22/11 và khi chụp ảnh User Manual). Đều không chặn luồng demo; vì đã chốt tag `v1.0`
nên chưa sửa code ở lần 2, chuyển sang tuần 8. Ngày 23/11 đã sửa 6 lỗi trong PR #96 (XDPMHDT-108) và lỗi số 2 trong PR #98 (XDPMHDT-109); lỗi số 9 (phát hiện khi chạy R1) sửa trong PR #101 (XDPMHDT-111); lỗi số 10, 11 (phát hiện khi chạy TC-125, 24/11) sửa trong PR #88 (XDPMHDT-99) và PR #104 (XDPMHDT-112), nghiệm thu trên máy ảo 25/11; lỗi số 12 (phát hiện khi chạy TC-129, 25/11) sửa trong nhánh XDPMHDT-114; không còn lỗi Minor tồn. Lỗi số 13 (link upload media) sửa trong PR #107 (XDPMHDT-115); lỗi số 14 (phát hiện khi chạy TC-11, TC-127 ngày 26/11) sửa trong PR XDPMHDT-116.

| # | Lỗi | Test case | Mức độ | Service / file | Người phụ trách | Trạng thái | Ảnh hưởng demo |
|---|---|---|---|---|---|---|---|
| 1 | Trang Phản hồi mất huy hiệu "Dữ liệu từ API" (`admin-nav.js` thay khối `.nav` chứa huy hiệu) | TC-70 | Minor | WebAdmin `feedback.html`, `admin-nav.js` | Bảo, Tường | **Đã sửa** (23/11, PR #96): đưa huy hiệu ra ngoài khối `.nav` | Không |
| 2 | Cột "Ngày tạo", "Cập nhật" trên trang Phản hồi hiện giờ UTC (lệch 7 giờ): feedback-service trong container chạy múi giờ UTC | TC-70 | Minor | feedback-service | Tường | **Đã sửa** (23/11, PR #98): `TZ=Asia/Ho_Chi_Minh` cho cả 9 service Java trong `docker-compose.yml`; phản hồi cũ trên từng máy sửa bằng 1 lệnh cộng 7 giờ | Không |
| 3 | Màn "Đánh giá của tôi" hiện ngày cập nhật thay cho ngày gửi | TC-115 | Minor | App `Interface` | Tường | **Đã sửa** (23/11, PR #96): dùng `createdAt` | Không |
| 4 | Sau khi gửi lại đánh giá, danh sách bình luận trên cùng màn chưa tự làm mới (mở lại thì đúng) | TC-67 | Minor | App `Interface` | Tường | **Đã sửa** (23/11, PR #96): gửi thành công thì tải lại | Không |
| 5 | Tắt feedback-service thì Gateway trả 500 thay vì 503 | TC-117 | Minor | api-gateway | Bảo | **Đã sửa** (23/11, PR #96): `ServiceUnavailableHandler` trả 503 | Không |
| 6 | Trang chủ app: Row tràn 14 px (`main.dart` dòng 325); 1 ảnh Unsplash gõ cứng trả 404 | — | Minor | App `Interface` | Xuân Anh | **Đã sửa** (23/11, PR #96): `Expanded`, bỏ ảnh gõ cứng, có khung thay thế | Bản debug hiện dải sọc vàng đen |
| 7 | Tên ngôn ngữ "Tiếng Việt" lỗi dấu ("Tiáº¿ng Viá»‡t") ở mục Ngôn ngữ hỗ trợ: `08_languages.sql` thiếu `SET NAMES utf8mb4` | — | Minor | `Infra/db/08_languages.sql` | Tường | **Đã sửa** (22/11, chỉ script DB; DB đang chạy sửa bằng 1 lệnh trong `Infra/db/README.md`) | Không |
| 8 | Giờ UTC ở Thống kê: "Xuất lúc" trong file Excel/PDF lệch 7 giờ; số "Hôm nay" tính theo ngày UTC (analytics-service trong container chạy múi giờ UTC) | TC-105, TC-109 | Minor | analytics-service | Tuấn | **Đã sửa** (23/11, PR #96): `TZ=Asia/Ho_Chi_Minh` trong `docker-compose.yml` (chỉ khi chạy bằng Docker) | Không, trừ khi demo từ 0 h – 7 h sáng |
| 9 | Lần đầu mở màn Quét iBeacon sau khi cài app: xoay mãi "Đang tải registry và khởi động BLE...". App xin quyền thông báo (không chờ) và quyền Bluetooth/Vị trí cùng lúc; Android chỉ xử lý 1 yêu cầu, yêu cầu Bluetooth không bao giờ trả kết quả (log `Can request only one set of permissions at a time`) | TC-62 | Minor | App `beacon_scan_screen.dart` | Khoa | **Đã sửa** (23/11, PR XDPMHDT-111): chờ xin quyền thông báo xong mới xin quyền Bluetooth | Có, nếu cài app mới ngay trước demo (máy ảo hôm nay phải cấp quyền bằng `adb`) |
| 10 | Bỏ Yêu thích lúc mất mạng: có mạng lại, màn chi tiết hiện tim đỏ lại (server vẫn đang thích); chỉ khi mở màn Yêu thích mới đồng bộ | TC-125 | Minor | App `personalization_service.dart` | Phú | **Đã sửa** (25/11, PR #88): `_isFavorite` đọc hàng chờ trước khi hỏi server; hàng chờ chỉ giữ thao tác cuối của mỗi hiện vật; `PersonalizationSync` đồng bộ mỗi 30 s, khi mở lại app và khi đăng nhập. Nghiệm thu 25/11: server đang thích → máy bay, bỏ tim → có mạng: tim trắng, `likes` không còn dòng | Không |
| 11 | Mất mạng: Lịch sử hiện lượt xem tay bằng tên gõ cứng "Khue Van Cac" / "Di tích Smart Heritage" (có mạng thì đúng) | TC-125 | Minor | App `Interface/lib/main.dart` | Bảo | **Đã sửa** (25/11, PR #104): thêm `historyName`, `siteName` cho hiện vật màn chính, lượt `MANUAL` ghi cache bằng tên thật. Nghiệm thu 25/11: máy bay → "Khuê Văn Các · Văn Miếu - Quốc Tử Giám" | Không |
| 12 | Tắt feedback-service hoặc personalization-service (container vừa dừng): Gateway treo ~14 s rồi trả 500, có lần treo > 30 s; log `NoRouteToHostException: feedback-service/172.19.0.16:8086` (Gateway còn giữ IP cũ) | TC-129 | Minor | api-gateway `ServiceUnavailableHandler`, `application.yml` | Bảo | **Đã sửa** (25/11, XDPMHDT-114): bắt thêm `NoRouteToHostException` → 503; `connect-timeout` 2 s; thêm unit test (gateway 28/28). Nghiệm thu: tắt content, feedback, personalization → 503 sau 2,0 – 3,0 s | Có, nếu 1 service sập lúc demo (app chờ lâu rồi báo lỗi chung) |
| 13 | Upload ảnh/audio trên Web Admin trả link `http://content-service:8083/api/media/files/…` (host Docker nội bộ): trình duyệt và app không mở được | TC-74 | Minor | content-service `MediaUploadController` | Bảo | **Đã sửa** (25/11, PR #107, XDPMHDT-115): thêm `PUBLIC_BASE_URL=http://localhost:8080`, thêm unit test | Có, nếu demo upload media |
| 14 | Màn Quét iBeacon khi bị từ chối quyền Bluetooth/Vị trí chỉ có nút "Thử lại", không chỉ vào Cài đặt; Android từ chối 2 lần sẽ không hỏi lại nên người dùng bị kẹt (màn Bản đồ đã có hướng dẫn) | TC-11, TC-127 | Minor | App `beacon_service.dart`, `beacon_scan_screen.dart` | Bảo | **Đã sửa** (27/11, XDPMHDT-116): thông báo có đường dẫn vào Cài đặt + nút "Mở Cài đặt" (`openAppSettings`), quay lại app thì tự quét lại; thêm 2 unit test (`beacon_service_test` 22/22, `flutter analyze` sạch). Nghiệm thu 27/11 trên máy ảo: TC-11, TC-127 đạt | Có, nếu người xem demo bấm "Từ chối" |

**Quan sát khi chạy 25–27/11 (không tính là lỗi):**

- Ô "Tên hiện vật" trên CMS (`artifacts.name`) không phải tên app và API hiển thị: content-service trả `name` bằng Tiêu đề bản dịch `vi` (`ArtifactService.localize`). Sửa ô "Tên hiện vật" thì app không đổi (TC-95).
- Giới hạn ảnh 10 MB, audio 30 MB chỉ kiểm trên trình duyệt (`artifacts.js`); server chỉ chặn chung 100 MB (TC-74).
- Tên hiện vật trên ảnh đầu màn chi tiết là chữ tối trên nền nâu, khó đọc (TC-119).
- Màn hình rất hẹp (~274 dp) thì tiêu đề AppBar màn chính tràn 22 px; ở 360 dp (điện thoại nhỏ phổ biến) không tràn (TC-119).
- Gán beacon không phát sự kiện RabbitMQ; registry đọc DB nên vẫn đúng ngay (TC-96).
- Thẻ "Pin thấp (< 20%)" chỉ đếm beacon đang hoạt động; beacon "Không hoạt động" 15% không được đếm (TC-33, TC-104).

**Hiệu năng lọc theo di tích (không tính là lỗi Minor):** ở mục 4.1, với 1.000.000 lượt, Dashboard và file xuất khi lọc một di tích mất 24 – 27 s
(không lọc 1,5 – 3,3 s). Nguyên nhân: lọc di tích phải quay lại bảng `fact_visits` lấy `user_id`, `duration_seconds` cho từng dòng, và bộ nhớ đệm
InnoDB mặc định 128 MB nhỏ hơn index. Đã sửa 23/11 trong PR #98: index bao trùm `idx_fact_visits_artifact_cover (artifact_id, detected_at, user_id, duration_seconds)`
thay `idx_fact_visits_artifact_date` (DB cũ chạy `Infra/analytics_w8_cover_index.sql` một lần) và `innodb-buffer-pool-size=512M`.
Đo lại trên máy Bảo qua Gateway, 1.000.000 lượt (`Infra/loadtest/analytics_1m_bench.sql`), lọc Hoàng thành Thăng Long: `overview?siteId=2` 0,53 s, xuất Excel 1,24 s.
## 7. Test case không đạt (lần 1, 17/11) và kết quả lần 2

| Mã TC | Lần 1 | Lý do lần 1 | Lần 2 (19/11) | Kết luận |
|---|---|---|---|---|
| TC-13 | Không đạt | `GET /api/artifacts/beacon/{uuid}` trả 404 trên Gateway và content-service. Cột `beacon_uuid` của hiện vật đang null. Code ghi endpoint này là luồng cũ; luồng chuẩn là registry rồi `GET /api/artifacts/{id}` | **Đạt** | Test Plan kiểm endpoint cũ (các beacon dùng chung UUID nên không tra theo UUID được). Đã sửa TC-13 theo luồng app đang dùng: registry trả `artifactId = 1`, chi tiết ra Khuê Văn Các. Không phải lỗi code |
| TC-36 | Không đạt | `GET /api/artifacts/2/rating-summary` trả 200 với averageRating=3.5, totalRatings=2. Test Plan mẫu kỳ vọng 4.0 và 1 | **Đạt** | DB lúc chạy lần 1 đã có thêm đánh giá được duyệt từ các bài test khác. Trên DB vừa đưa về mẫu: `4.0` / `1`. Đã ghi tiền điều kiện vào Test Plan. Không phải lỗi code |
| TC-71 | Không đạt | `PUT /api/ibeacons/1/position` qua Gateway 8080 trả 404. Gọi thẳng beacon-service cổng 8084 với cùng body trả 200 | **Đạt** | Lần 1 Gateway chạy bằng Maven thiếu `BEACON_SERVICE_URL` nên route về Backend cũ. Chạy bằng docker-compose (đủ biến): 200. Lỗi môi trường, không phải lỗi code |

**Case bị chặn ở lần 2:**

| Mã TC | Kết quả | Lý do | Cần để chạy |
|---|---|---|---|
| TC-101 | Chặn | Du khách nhận thông báo FCM khi được duyệt: chưa có `Interface/android/app/google-services.json`, khoá service account Firebase và điện thoại Android thật. Code gửi sự kiện (feedback-service) và nhận, gửi FCM (beacon-service, PR #62) đã có | File cấu hình Firebase + 1 điện thoại Android thật |

**Case chưa chạy ở lần 2 (7):** cần thiết bị nhóm không có (không ai có điện thoại Android; máy ảo không có BLE thật).

| Mã TC | Nội dung | Cần để chạy |
|---|---|---|
| TC-10 | Quét beacon trên Android | Điện thoại Android + Beacon Simulator |
| TC-50 | Không thông báo lặp, thông báo khi sang beacon khác | Điện thoại Android + 2 beacon |
| TC-58 | Tự hiện hiện vật ≤ 2 giây | Điện thoại Android + beacon (đo 10 lần) |
| TC-59 | Không báo trùng | Điện thoại Android + beacon |
| TC-60 | Đi từ beacon A sang B | Điện thoại Android + 2 beacon cách ≥ 3 m |
| TC-102 | Admin được báo có đánh giá mới | Firebase + điện thoại Android |
| TC-128 | 2 beacon gần nhau | Điện thoại Android + 2 beacon |

TC-69 đã chạy phần API ngày 26/11 (đạt một phần, mục 4.7); phần điện thoại nhận thông báo vẫn cần Firebase + điện thoại Android.

Phần quét đã kiểm thay bằng giả lập trên máy ảo (TC-56, TC-62, TC-126) và unit test chống nhảy beacon (PR #92).

66 case còn lại của lần 1 ghi `Chưa chạy` trong [test-run-w6.md](test-run-w6.md): app Android, vài thao tác chỉ có trên trang HTML, NiFi, tắt service để xem cảnh báo. Lần 2 chạy theo [regression-list.md](regression-list.md).

## 8. Kết luận

- Đạt tiêu chí kết thúc kiểm thử lần 1 (chạy ≥ 90% Test Plan, mọi lỗi có Issue): **Không**. Đã chạy 65/131 case.
- Lần 2 (19–25/11, trên `v1.0` rồi `main` sau các PR sửa lỗi): 93 case đã chạy: 90 đạt, 3 đạt một phần (TC-46 chậm ~4 s, TC-62 giả lập, TC-100 thiếu panel Grafana), 1 case chặn (TC-101). 3 case không đạt của lần 1 đều đạt
  và đều không phải lỗi code. Luồng Đánh giá đầy đủ trên app và Web (gửi, sửa, chờ duyệt, duyệt, ẩn, điểm trung bình, số chờ duyệt) đạt;
  Thống kê trên máy sạch 5/5 đạt; giám sát và cảnh báo (TC-48, 78, 120 → 122, 124) đạt.
- Đạt tiêu chí kết thúc kiểm thử lần 2 (chạy ≥ 90% Test Plan): **đạt** sau 27/11: 128/136 = 94% (125 đạt, 3 đạt một phần, 1 chặn, 7 chưa chạy vì thiếu thiết bị); case ưu tiên Cao đạt 94/96 = 98%. XDPMHDT-118 đưa TC-46 (hạn chờ tổng 1,5 s), TC-53 (tìm kiếm Bản đồ), TC-100 (panel DLQ Grafana) lên đạt. Hồi quy R1 (20 case, mục 4.2): 19 đạt, 1 đạt một phần (TC-62, giả lập). (Bản 25/11: 93/136 = 68%.)
- Lỗi còn tồn: không có Blocker, Critical, Major; 14 lỗi Minor đều đã sửa: 1 ngày 22/11 (script DB), 6 ngày 23/11 (PR #96), 1 ngày 23/11 (PR #98, giờ UTC), 1 ngày 23/11 (PR #101, treo quyền màn Quét iBeacon), 2 ngày 25/11 (PR #88, #104, Yêu thích và Lịch sử khi mất mạng), 1 ngày 25/11 (XDPMHDT-114, Gateway trả 503 khi service vừa tắt). Lọc di tích với 1.000.000 lượt giảm từ ~25 s xuống 0,53 s (PR #98). Thêm lỗi 13 (PR #107, link upload media) và lỗi 14 (XDPMHDT-116, màn Quét khi bị từ chối quyền).
- Rủi ro cho buổi bảo vệ: thông báo FCM chưa chạy trên máy thật (TC-69 mới kiểm phần API, TC-101 chặn); app chỉ kiểm trên máy ảo; các phần lần 1 chưa chạy
  (NiFi, k6 500 VU lần 2) đã chạy và đạt ngày 27/11 (mục 4.3, 4.8); 7 case cần điện thoại, beacon thật, Firebase chưa chạy (mục 7).
