# Báo cáo kiểm thử cuối – Smart Heritage (Test Report v1.0)

| Mục | Nội dung |
|---|---|
| Người lập | Tường (đầu mối QA), Ân (System Testing) |
| Căn cứ | [Test Plan](test-plan.md) (136 test case); `task_w7_chi_tiet` mục III.5 |
| Lần 1 (System Testing) | 10/11/2026 – 15/11/2026, bản chốt code 18:00 13/11 |
| Lần 2 (hồi quy) | 19/11/2026 (Ân, Tường) |
| Bản kiểm thử cuối | tag `v1.0`, commit: *(điền khi Bảo gắn tag, Thứ 6 20/11)* |
| Môi trường | Windows 11, JDK 25, MySQL 8 / Redis / RabbitMQ chạy Docker (`--profile mq`), 7 service + Gateway chạy jar; thiết bị Android: *(model, phiên bản Android, phiên bản app)* |

**Tình trạng bản này (17/11):** đã có kết quả cấp Unit và Integration chạy trên `main` (commit `533e64c` lúc Tường đo; `main` hiện tại `761248f`),
và 5 test case quản lý người dùng mới. Lần 1 System Testing đã chạy 65/131 case trên máy này tối 17/11 (API, database, RabbitMQ, Prometheus). Chưa đủ 90%. Cột lần 2 điền sau buổi hồi quy 19/11.

## 1. Tổng hợp theo 3 cấp kiểm thử

| Cấp | Cách chạy | Kết quả |
|---|---|---|
| Unit | `mvn test` (10 module Java), `flutter test` (app) | **330/330 đạt** (Java 269, Flutter 61) – mục 2 |
| Integration | `Docs/testing/integration-smoke.ps1 -Events` qua Gateway | **12/12 đạt** – mục 3 |
| System | Test Plan, chạy tay trên Web Admin, app Android và API | Lần 1: **65/131 đã chạy, 62 đạt, 3 không đạt** (17/11, `main@761248f`); lần 2: *(19/11)* – mục 4 |

## 2. Unit test

Chạy ngày 17/11/2026 trên commit `533e64c` (`main`).

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
| content-service | 23 | 23 | 0 | 0 |
| beacon-service | 46 | 46 | 0 | 0 |
| personalization-service | 8 | 8 | 0 | 0 |
| feedback-service | 66 | 66 | 0 | 0 |
| analytics-service | 16 | 16 | 0 | 0 |
| Backend cũ (monolith) | 41 | 41 | 0 | 0 |
| **Java tổng** | **269** | **269** | **0** | **0** |
| App Flutter (`Interface`) | 61 | 61 | 0 | 0 |

Ghi chú: lần chạy `flutter test` đầu tiên có 1 file (`artifact_detail_screen_test.dart`) không chạy được vì Windows
Smart App Control chặn `flutter_tester.exe`; chạy lại thì đạt. Đây là chính sách bảo mật của máy, không phải lỗi code.

## 3. Integration test

Chạy ngày 17/11/2026, 7 service + Gateway + MySQL, Redis, RabbitMQ, dữ liệu mẫu.

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
`sh_events_published_total`) đã kiểm với RabbitMQ thật ở tuần 6, PR #59.

## 4. System test (theo Test Plan)

| Nhóm | Số case | Lần 1: đã chạy | Lần 1: đạt | Lần 2: đã chạy | Lần 2: đạt | Ghi chú |
|---|---|---|---|---|---|---|
| Visitor (TC-01 → TC-21, TC-51 → TC-69) | 40 | 18 | 17 | | | TC-13 không đạt |
| Administrator (TC-22 → TC-37, TC-70 → TC-74) | 21 | 17 | 15 | | | TC-36, TC-71 không đạt |
| System (TC-38 → TC-50, TC-75 → TC-85) | 24 | 16 | 16 | | | |
| Tuần 6: Xuất báo cáo (TC-86, TC-108 → TC-110) | 4 | 3 | 3 | | | TC-86 chưa bấm nút trên Dashboard |
| Tuần 6: Sự kiện RabbitMQ (TC-87 → TC-100) | 14 | 5 | 5 | | | TC-87, 88, 89, 90, 94. Chưa đọc DLQ và tin sai |
| Tuần 6: Thông báo, pin, thời gian thực (TC-101 → TC-107) | 7 | 1 | 1 | | | TC-69 chỉ phần 401. Còn FCM và Dashboard |
| Tuần 6: Hoàn thiện Mobile, Web (TC-111 → TC-119) | 9 | 3 | 3 | | | TC-116, TC-117, TC-118 |
| Tuần 6: Giám sát, kiểm thử tải (TC-120 → TC-124) | 5 | 1 | 1 | | | TC-120. Chưa tắt service để xem cảnh báo, chưa chạy lại k6 |
| Tuần 6: Trường hợp đặc biệt, bản chốt (TC-125 → TC-131) | 7 | 1 | 1 | | | Chỉ TC-130 (API 403) |
| Tuần 7: Quản lý người dùng (TC-132 → TC-136) | 5 | — | — | 5 | 5 | Chạy 17/11 qua Gateway (Tường). Không thuộc lần 1 tuần 6 |
| **Tổng lần 1 (131 case)** | **131** | **65** | **62** | | | 66 case chưa chạy |

- Tỉ lệ đã chạy lần 1: **65/131 = 50%** (yêu cầu ≥ 90%, chưa đạt).
- Tỉ lệ đạt trên số đã chạy lần 1: **62/65 = 95%**.
- Tỉ lệ đã chạy lần 2 (yêu cầu ≥ 90%): chưa chạy.
- Tỉ lệ đạt của case ưu tiên Cao (yêu cầu ≥ 90%): chưa đủ số case Cao đã chạy để kết luận.

## 5. Lỗi theo mức độ

| Mức độ | Hạn sửa | Tổng (lần 1) | Đã đóng | Còn mở sau lần 2 |
|---|---|---|---|---|
| Blocker | Trong 24 giờ | 0 | | |
| Critical | Trước Thứ 3 tuần 7 | 0 | | |
| Major | Trước Thứ 5 tuần 7 | 0 | | |
| Minor | Nếu còn thời gian | 0 | | |

Ba case Không đạt chưa xếp mức và chưa mở Issue: TC-13 (endpoint beacon cũ trả 404), TC-36 (số mẫu trên DB lệch Test Plan), TC-71 (đổi vị trí beacon qua Gateway trả 404, gọi thẳng cổng 8084 thì 200).

## 6. Lỗi còn tồn

| Issue | Test case | Mức độ | Service | Người phụ trách | Lý do chưa sửa | Ảnh hưởng demo |
|---|---|---|---|---|---|---|
| | | | | | | |

## 7. Test case không đạt (lần 1, 17/11)

| Mã TC | Kết quả | Lý do | Issue |
|---|---|---|---|
| TC-13 | Không đạt | `GET /api/artifacts/beacon/{uuid}` trả 404 trên Gateway và content-service. Cột `beacon_uuid` của hiện vật đang null. Code ghi endpoint này là luồng cũ; luồng chuẩn là registry rồi `GET /api/artifacts/{id}` | Chưa mở |
| TC-36 | Không đạt | `GET /api/artifacts/2/rating-summary` trả 200 với averageRating=3.5, totalRatings=2. Test Plan mẫu kỳ vọng 4.0 và 1 | Chưa mở |
| TC-71 | Không đạt | `PUT /api/ibeacons/1/position` qua Gateway 8080 trả 404. Gọi thẳng beacon-service cổng 8084 với cùng body trả 200 | Chưa mở |

66 case còn lại ghi `Chưa chạy` trong [test-run-w6.md](test-run-w6.md): app Android, vài thao tác chỉ có trên trang HTML, NiFi, tắt service để xem cảnh báo. Lần 2 (19/11) chưa chạy.

## 8. Kết luận

- Đạt tiêu chí kết thúc kiểm thử lần 1 (chạy ≥ 90% Test Plan, mọi lỗi có Issue): **Không**. Đã chạy 65/131 case.
- Lỗi còn tồn và lý do: xem mục 6 và mục 7. Chưa có lỗi Blocker/Critical từ phiên này.
- Rủi ro cho buổi bảo vệ: luồng app, beacon, FCM, Dashboard trên trình duyệt và cảnh báo Prometheus chưa được ghi nhận trong lần 1.
