# Báo cáo kiểm thử cuối – Smart Heritage (Test Report v1.0)

| Mục | Nội dung |
|---|---|
| Người lập | Tường (đầu mối QA), Ân (System Testing) |
| Căn cứ | [Test Plan](test-plan.md) (136 test case); `task_w7_chi_tiet` mục III.5 |
| Lần 1 (System Testing) | 10/11/2026 – 15/11/2026, bản chốt code 18:00 13/11 |
| Lần 2 (hồi quy) | 19/11/2026 (API, Tường) và 22/11/2026 (app, Web – phần D, Bảo chạy) |
| Bản kiểm thử cuối | tag `v1.0` = `2a6e9e7`. Phần D chạy trên `main` `31853e6` = `v1.0` + bản iOS (#90); backend và app Android không đổi so với `v1.0` |
| Môi trường | Windows 11, JDK 25. Lần 2: toàn hệ thống chạy Docker (`--profile core --profile mq` ngày 19/11, `--profile all` ngày 22/11), DB sạch + dữ liệu demo bản cuối (`Infra/db/09_demo_seed.sql` + `Infra/seed/media`) |
| Thiết bị | Máy ảo Android Pixel 7, Android 17 (API 37, Google Play), app 1.0.0+1 bản debug, `DEMO_MODE=true` (nhóm không có điện thoại Android thật). Web Admin: Chrome, Live Server, tài khoản `admin` |

**Tình trạng bản này (22/11):** lần 2 đã có phần API của Tường (19/11) và phần chạy tay trên app, Web (phần D, 22/11):
31 case đã chạy đều đạt, TC-101 bị chặn (thiếu Firebase và máy thật). Không có lỗi Blocker, Critical, Major; 6 lỗi Minor ghi ở mục 6.
Phần D có bằng chứng ảnh chụp và script đối chiếu dữ liệu `Kiem_case_Tuong_v1.ps1` (chỉ đọc API và MySQL): 27/27 kiểm tra đạt.
Các case Ân phụ trách ở lần 2 chưa có kết quả trong báo cáo này.

## 1. Tổng hợp theo 3 cấp kiểm thử

| Cấp | Cách chạy | Kết quả |
|---|---|---|
| Unit | `mvn test` (10 module Java), `flutter test` (app) | **341/341 đạt** (Java 277, Flutter 64), 22/11 – mục 2 |
| Integration | `Docs/testing/integration-smoke.ps1 -Events` qua Gateway | **12/12 đạt**, 19/11 – mục 3 |
| System | Test Plan, chạy tay trên Web Admin, app Android và API | Lần 1: **65/131 đã chạy, 62 đạt, 3 không đạt** (17/11, `main@761248f`); lần 2: **31 đã chạy, 31 đạt, 1 chặn** (19/11 và 22/11) – mục 4 |

## 2. Unit test

Chạy ngày 22/11/2026 trên `main` `31853e6` (code backend và app Android giống tag `v1.0`): 341/341, giống lần 19/11 (`66e8361`).
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
| Visitor (TC-01 → TC-21, TC-51 → TC-69) | 40 | 18 | 17 | 10 | 10 | Lần 1: TC-13 không đạt. Lần 2: TC-13, TC-17 → TC-21 (API, 19/11); TC-65 → TC-68 (app, 22/11) |
| Administrator (TC-22 → TC-37, TC-70 → TC-74) | 21 | 17 | 15 | 5 | 5 | Lần 1: TC-36, TC-71 không đạt. Lần 2: TC-34, TC-35, TC-36, TC-71 (API); TC-70 (Web) |
| System (TC-38 → TC-50, TC-75 → TC-85) | 24 | 16 | 16 | 1 | 1 | Lần 2 (Tường): TC-83 |
| Tuần 6: Xuất báo cáo (TC-86, TC-108 → TC-110) | 4 | 3 | 3 | | | TC-86 chưa bấm nút trên Dashboard |
| Tuần 6: Sự kiện RabbitMQ (TC-87 → TC-100) | 14 | 5 | 5 | 5 | 5 | Lần 1: TC-87, 88, 89, 90, 94. Lần 2 (Tường): TC-89 → TC-93 |
| Tuần 6: Thông báo, pin, thời gian thực (TC-101 → TC-107) | 7 | 1 | 1 | 0 | 0 | Lần 1: TC-69 chỉ phần 401. Lần 2: TC-101 **Chặn** (thiếu Firebase và máy thật) |
| Tuần 6: Hoàn thiện Mobile, Web (TC-111 → TC-119) | 9 | 3 | 3 | 5 | 5 | Lần 1: TC-116, TC-117, TC-118. Lần 2: TC-116, TC-118 (API); TC-114, TC-115, TC-117 (app, Web) |
| Tuần 6: Giám sát, kiểm thử tải (TC-120 → TC-124) | 5 | 1 | 1 | | | TC-120. Chưa tắt service để xem cảnh báo, chưa chạy lại k6 |
| Tuần 6: Trường hợp đặc biệt, bản chốt (TC-125 → TC-131) | 7 | 1 | 1 | | | Chỉ TC-130 (API 403) |
| Tuần 7: Quản lý người dùng (TC-132 → TC-136) | 5 | — | — | 5 | 5 | Chạy 17/11 qua Gateway (Tường). Không thuộc lần 1 tuần 6 |
| **Tổng** | **136** | **65** | **62** | **31** | **31** | Lần 1: 66 case chưa chạy. Lần 2: phần của Tường (API + app, Web) và TC-132 → TC-136; 1 chặn (TC-101) |

- Tỉ lệ đã chạy lần 1: **65/131 = 50%** (yêu cầu ≥ 90%, chưa đạt).
- Tỉ lệ đạt trên số đã chạy lần 1: **62/65 = 95%**.
- Lần 2: **31/136** case đã chạy, đạt 31/31; TC-101 bị chặn. Kết quả từng case phần Đánh giá: [regression-w7-feedback.md](regression-w7-feedback.md).
- Tỉ lệ đã chạy lần 2 (yêu cầu ≥ 90%): **chưa đạt** – báo cáo này chưa có các case Ân phụ trách ở lần 2.
- Tỉ lệ đạt của case ưu tiên Cao (yêu cầu ≥ 90%): chưa đủ số case Cao đã chạy để kết luận.

## 5. Lỗi theo mức độ

| Mức độ | Hạn sửa | Tổng (lần 1) | Đã đóng | Còn mở sau lần 2 |
|---|---|---|---|---|
| Blocker | Trong 24 giờ | 0 | – | 0 |
| Critical | Trước Thứ 3 tuần 7 | 0 | – | 0 |
| Major | Trước Thứ 5 tuần 7 | 0 | – | 0 |
| Minor | Nếu còn thời gian | 0 (lần 2 phát hiện 7) | 1 | 6 |

Ba case Không đạt ở lần 1 chưa xếp mức và chưa mở Issue: TC-13 (endpoint beacon cũ trả 404), TC-36 (số mẫu trên DB lệch Test Plan), TC-71 (đổi vị trí beacon qua Gateway trả 404, gọi thẳng cổng 8084 thì 200).
Lần 2 xác định cả ba **không phải lỗi code** (nguyên nhân ở mục 7), nên không mở Issue; đến 19/11 chưa có lỗi Blocker/Critical nào.

## 6. Lỗi còn tồn

Các lỗi Minor phát hiện ở lần 2 (phần D, 22/11 và khi chụp ảnh User Manual). Đều không chặn luồng demo; vì đã chốt tag `v1.0`
nên chưa sửa code, chuyển sang tuần 8.

| # | Lỗi | Test case | Mức độ | Service / file | Người phụ trách | Trạng thái | Ảnh hưởng demo |
|---|---|---|---|---|---|---|---|
| 1 | Trang Phản hồi mất huy hiệu "Dữ liệu từ API" (`admin-nav.js` thay khối `.nav` chứa huy hiệu) | TC-70 | Minor | WebAdmin `feedback.html`, `admin-nav.js` | Bảo, Tường | Còn tồn | Không |
| 2 | Cột "Ngày tạo", "Cập nhật" trên trang Phản hồi hiện giờ UTC (lệch 7 giờ): feedback-service trong container chạy múi giờ UTC | TC-70 | Minor | feedback-service | Tường | Còn tồn | Không |
| 3 | Màn "Đánh giá của tôi" hiện ngày cập nhật thay cho ngày gửi | TC-115 | Minor | App `Interface` | Tường | Còn tồn | Không |
| 4 | Sau khi gửi lại đánh giá, danh sách bình luận trên cùng màn chưa tự làm mới (mở lại thì đúng) | TC-67 | Minor | App `Interface` | Tường | Còn tồn | Không |
| 5 | Tắt feedback-service thì Gateway trả 500 thay vì 503 | TC-117 | Minor | api-gateway | Bảo | Còn tồn | Không |
| 6 | Trang chủ app: Row tràn 14 px (`main.dart` dòng 325); 1 ảnh Unsplash gõ cứng trả 404 | — | Minor | App `Interface` | Bảo | Còn tồn | Không |
| 7 | Tên ngôn ngữ "Tiếng Việt" lỗi dấu ("Tiáº¿ng Viá»‡t") ở mục Ngôn ngữ hỗ trợ: `08_languages.sql` thiếu `SET NAMES utf8mb4` | — | Minor | `Infra/db/08_languages.sql` | Tường | **Đã sửa** (22/11, chỉ script DB; DB đang chạy sửa bằng 1 lệnh trong `Infra/db/README.md`) | Không |

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

66 case còn lại của lần 1 ghi `Chưa chạy` trong [test-run-w6.md](test-run-w6.md): app Android, vài thao tác chỉ có trên trang HTML, NiFi, tắt service để xem cảnh báo. Lần 2 chạy theo [regression-list.md](regression-list.md).

## 8. Kết luận

- Đạt tiêu chí kết thúc kiểm thử lần 1 (chạy ≥ 90% Test Plan, mọi lỗi có Issue): **Không**. Đã chạy 65/131 case.
- Lần 2 (19/11 và 22/11, trên `v1.0`): 31 case đã chạy đều đạt, 1 case chặn (TC-101). 3 case không đạt của lần 1 đều đạt
  và đều không phải lỗi code. Luồng Đánh giá đầy đủ trên app và Web (gửi, sửa, chờ duyệt, duyệt, ẩn, điểm trung bình, số chờ duyệt) đạt.
- Đạt tiêu chí kết thúc kiểm thử lần 2 (chạy ≥ 90% Test Plan): **chưa đạt** trong báo cáo này (31/136), vì chưa có kết quả các case Ân phụ trách.
- Lỗi còn tồn: không có Blocker, Critical, Major; 6 lỗi Minor chuyển tuần 8 (mục 6), 1 lỗi Minor đã sửa (script DB).
- Rủi ro cho buổi bảo vệ: thông báo FCM chưa chạy trên máy thật (TC-101); app chỉ kiểm trên máy ảo; các phần lần 1 chưa chạy
  (NiFi, cảnh báo Prometheus khi tắt service) cần người phụ trách xác nhận trước buổi bảo vệ.
