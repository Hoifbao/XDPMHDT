# Kiểm thử hồi quy lần 2 – phần Đánh giá (Tường, 19/11/2026)

Thuộc kiểm thử lần 2 (hồi quy) theo [regression-list](regression-list.md) của Ân. File này ghi các case API của
feedback-service; case trên app Android và trang Web chạy tay cùng Ân, ghi trong Test Report.

| Mục | Nội dung |
|---|---|
| Bản kiểm thử | `main` `e681bce` |
| Môi trường | Windows 11, JDK 25; MySQL, Redis, RabbitMQ chạy Docker (`--profile mq`); 7 service + Gateway chạy jar |
| Dữ liệu | DB vừa đưa về dữ liệu mẫu (`down -v` rồi `up -d`) |
| Cách chạy | `powershell -ExecutionPolicy Bypass -File Docs/testing/regression-feedback.ps1` |

## Kết quả

| Mã TC | Nội dung | Kết quả | Ghi chú |
|---|---|---|---|
| TC-36 | Điểm trung bình hiện vật 2 | Đạt | `4.0` / `1`. Lần 1 ghi Không đạt (3.5 / 2) vì DB lúc đó đã có thêm đánh giá được duyệt từ các bài test trước, không phải lỗi code. Đã ghi rõ tiền điều kiện trong Test Plan. Duyệt thêm id 5 thì ra `3.0` / `2`, đúng |
| TC-21 | Đánh giá công khai chỉ có APPROVED | Đạt | Chỉ id 1; không lộ `userId`, email |
| TC-34 | Lọc danh sách quản trị | Đạt | PENDING: 3, 5; hiện vật 1: 1, 3, 6; cả hai: 3. Tham số sai 400, token visitor 403 |
| TC-18 | Số sao sai (4.5, 0, 6, thiếu) | Đạt | Đều 400, không phát sự kiện |
| TC-19 | Bình luận 501 ký tự | Đạt | 400 |
| TC-20 | Hiện vật không tồn tại | Đạt | 404 |
| TC-116 | Đánh giá của tôi | Đạt | Chỉ đánh giá của `visitor_mai`; header `X-User-Id: 2` giả bị bỏ qua; không token 401 |
| TC-118 | Phân quyền số chờ duyệt | Đạt | admin 200 `pending = 2`; visitor 403; không token 401 |
| TC-89 | Đánh giá mới phát `FeedbackCreated` | Đạt | 201, đúng 1 tin `feedback.created`, `userId = 3`, `artifactId = 3`, `rating = 4` |
| TC-90 | Gửi lại không phát sự kiện | Đạt | 200, về PENDING, 0 tin |
| TC-17 | Gửi lại hiện vật 2 ghi đè | Đạt | 200, ghi đè id 5, `reviewerName = Trần Thị Mai` |
| TC-91 | Duyệt phát `FeedbackApproved` | Đạt | Đúng 1 tin, `feedbackId = 3`, `artifactName = Khuê Văn Các` |
| TC-92 | Duyệt lại, ẩn không báo lặp | Đạt | 200, 200, 0 tin |
| TC-35 | Duyệt, ẩn, trạng thái sai | Đạt | Duyệt thì hiện công khai, ẩn thì biến mất; `PENDING` 400; id không tồn tại 404 |
| TC-83 | Chặn trùng ở mức database | Đạt | MySQL báo lỗi 1062 trên `uq_feedbacks_user_artifact` |
| TC-93 | RabbitMQ tắt không ảnh hưởng đánh giá | Đạt | Chạy tay: 201 trong 0,11 giây; health UP; log `Không gửi được sự kiện FeedbackCreated eventId=…`; `sh_events_published_total{result="failed"}` 0 → 1 |

**Tổng: 16/16 case đạt** (script: 15 case, 17 kiểm tra đều đạt; TC-93 chạy tay).

## Case của Tường chạy tay cùng Ân (app, Web)

| Mã TC | Nội dung | Kết quả |
|---|---|---|
| TC-65 → TC-68 | Gửi, sửa đánh giá, điểm TB trên app | *(điền sau buổi hồi quy)* |
| TC-70 | Duyệt / ẩn đánh giá trên Web | |
| TC-101 | Du khách nhận thông báo khi được duyệt (cùng Khoa) | |
| TC-114, TC-115 | Màn "Đánh giá đang chờ duyệt", "Đánh giá của tôi" trên app | |
| TC-117 | Số chờ duyệt trên menu Web | |

## Lần 1 không đạt nhưng không thuộc phần Đánh giá

| Mã TC | Phụ trách | Ghi chú |
|---|---|---|
| TC-13 | Xuân Anh, Khoa | `GET /api/artifacts/beacon/{uuid}` là luồng cũ; luồng chuẩn là registry rồi `GET /api/artifacts/{id}` |
| TC-71 | Bảo, Khoa | `PUT /api/ibeacons/{id}/position` qua Gateway 404 khi Gateway chạy thiếu `BEACON_SERVICE_URL` |
