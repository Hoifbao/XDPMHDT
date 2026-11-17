# Báo cáo kiểm thử cuối – Smart Heritage (Test Report v1.0)

| Mục | Nội dung |
|---|---|
| Người lập | Tường (đầu mối QA), Ân (System Testing) |
| Căn cứ | [Test Plan](test-plan.md) (136 test case); `task_w7_chi_tiet` mục III.5 |
| Lần 1 (System Testing) | 10/11/2026 – 15/11/2026, bản chốt code 18:00 13/11 |
| Lần 2 (hồi quy) | 19/11/2026 (Ân, Tường) |
| Bản kiểm thử cuối | tag `v1.0`, commit: *(điền khi Bảo gắn tag, Thứ 6 20/11)* |
| Môi trường | Windows 11, JDK 25, MySQL 8 / Redis / RabbitMQ chạy Docker (`--profile mq`), 7 service + Gateway chạy jar; thiết bị Android: *(model, phiên bản Android, phiên bản app)* |

**Tình trạng bản này (17/11):** đã có kết quả cấp Unit và Integration chạy trên `main` mới nhất (commit `533e64c`),
và 5 test case quản lý người dùng mới. Cột lần 1 chờ kết quả Ân đã chạy; cột lần 2 điền sau buổi hồi quy 19/11.

## 1. Tổng hợp theo 3 cấp kiểm thử

| Cấp | Cách chạy | Kết quả |
|---|---|---|
| Unit | `mvn test` (10 module Java), `flutter test` (app) | **330/330 đạt** (Java 269, Flutter 61) – mục 2 |
| Integration | `Docs/testing/integration-smoke.ps1 -Events` qua Gateway | **12/12 đạt** – mục 3 |
| System | Test Plan, chạy tay trên Web Admin, app Android và API | Lần 1: *(chờ Ân)*; lần 2: *(19/11)* – mục 4 |

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
| Visitor (TC-01 → TC-21, TC-51 → TC-69) | 40 | | | | | |
| Administrator (TC-22 → TC-37, TC-70 → TC-74) | 21 | | | | | |
| System (TC-38 → TC-50, TC-75 → TC-85) | 24 | | | | | |
| Tuần 6: Xuất báo cáo (TC-86, TC-108 → TC-110) | 4 | | | | | |
| Tuần 6: Sự kiện RabbitMQ (TC-87 → TC-100) | 14 | | | | | |
| Tuần 6: Thông báo, pin, thời gian thực (TC-101 → TC-107) | 7 | | | | | |
| Tuần 6: Hoàn thiện Mobile, Web (TC-111 → TC-119) | 9 | | | | | |
| Tuần 6: Giám sát, kiểm thử tải (TC-120 → TC-124) | 5 | | | | | |
| Tuần 6: Trường hợp đặc biệt, bản chốt (TC-125 → TC-131) | 7 | | | | | |
| Tuần 7: Quản lý người dùng (TC-132 → TC-136) | 5 | — | — | 5 | 5 | Chạy 17/11 qua Gateway (Tường) |
| **Tổng** | **136** | | | | | |

- Tỉ lệ đã chạy lần 2 (yêu cầu ≥ 90%):
- Tỉ lệ đạt trên số đã chạy:
- Tỉ lệ đạt của case ưu tiên Cao (yêu cầu ≥ 90%):

## 5. Lỗi theo mức độ

| Mức độ | Hạn sửa | Tổng (lần 1) | Đã đóng | Còn mở sau lần 2 |
|---|---|---|---|---|
| Blocker | Trong 24 giờ | | | |
| Critical | Trước Thứ 3 tuần 7 | | | |
| Major | Trước Thứ 5 tuần 7 | | | |
| Minor | Nếu còn thời gian | | | |

## 6. Lỗi còn tồn

| Issue | Test case | Mức độ | Service | Người phụ trách | Lý do chưa sửa | Ảnh hưởng demo |
|---|---|---|---|---|---|---|
| | | | | | | |

## 7. Test case không đạt hoặc chưa chạy (lần 2)

| Mã TC | Kết quả | Lý do | Issue |
|---|---|---|---|
| | | | |

## 8. Kết luận

- Đạt tiêu chí kết thúc kiểm thử (Test Plan mục 6): Có / Không
- Lỗi còn tồn và lý do: xem mục 6
- Rủi ro cho buổi bảo vệ:
