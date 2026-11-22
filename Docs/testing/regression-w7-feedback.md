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

## Case chạy tay trên app và Web (phần D, 22/11/2026)

Bảo chạy trên `main` `31853e6` (= `v1.0` + bản iOS, backend và app Android không đổi), Docker `--profile all`, DB sạch + dữ liệu
demo; máy ảo Android Pixel 7, Android 17 (API 37), app 1.0.0+1 debug; Web Admin trên Chrome. Mỗi bước có ảnh chụp và được đối
chiếu bằng script đọc API và MySQL (`Kiem_case_Tuong_v1.ps1`, không sửa dữ liệu): 27/27 kiểm tra đạt.

| Mã TC | Nội dung | Kết quả | Ghi chú |
|---|---|---|---|
| TC-65 | Gửi đánh giá từ app | Đạt | Gửi 4 sao, trạng thái chờ duyệt; ô bình luận dừng ở 500/500 ký tự |
| TC-66 | Chưa đăng nhập thì yêu cầu đăng nhập | Đạt | Có nút "Đăng nhập để đánh giá"; đăng nhập xong quay về đúng màn |
| TC-67 | Gửi lại = sửa đánh giá cũ | Đạt | Cùng id, `created_at` giữ, `updated_at` đổi, về PENDING. Danh sách bình luận cùng màn chưa tự làm mới (Minor) |
| TC-68 | Điểm TB và bình luận trên app khớp dữ liệu | Đạt | 5.0 / 4.0 / 5.0; sau khi duyệt, ẩn: Khuê Văn Các 3.0, 1 lượt |
| TC-70 | Duyệt / ẩn đánh giá trên Web | Đạt | Thiếu huy hiệu "Dữ liệu từ API" trên trang Phản hồi (Minor); giờ hiện UTC (Minor) |
| TC-101 | Thông báo FCM khi được duyệt | Chặn | Thiếu cấu hình Firebase và điện thoại thật |
| TC-114 | Sau khi gửi hiện chờ duyệt | Đạt | Có "Đánh giá đang chờ duyệt" và nút "Xem đánh giá của tôi" |
| TC-115 | Màn "Đánh giá của tôi" | Đạt | Thẻ hiện ngày cập nhật thay cho ngày gửi (Minor); tài khoản mới hiện màn trống có hướng dẫn |
| TC-117 | Số chờ duyệt trên menu Web | Đạt | Số đúng trên mọi trang, giảm ngay khi duyệt; tắt feedback-service thì menu vẫn dùng được, không hiện số. Gateway trả 500, nên là 503 (Minor) |

**Tổng phần D: 8 đạt, 1 chặn.** Lỗi Minor: xem Test Report mục 6.

## Lần 1 không đạt nhưng không thuộc phần Đánh giá (chạy lại 19/11)

| Mã TC | Phụ trách | Lần 2 | Ghi chú |
|---|---|---|---|
| TC-13 | Xuân Anh, Khoa | Đạt | `GET /api/artifacts/beacon/{uuid}` là endpoint cũ (các beacon dùng chung UUID). Test Plan đã đổi TC-13 sang luồng app đang dùng: registry trả `artifactId = 1`, `GET /api/artifacts/1` ra Khuê Văn Các |
| TC-71 | Bảo, Khoa | Đạt | Chạy toàn hệ thống bằng docker-compose: `PUT /api/ibeacons/1/position` qua Gateway trả 200. Lần 1 lỗi vì Gateway chạy Maven thiếu `BEACON_SERVICE_URL` |

## Chạy lại trên dữ liệu demo bản cuối (19/11, tối)

Sau khi nạp `Infra/db/09_demo_seed.sql` và `Infra/seed/media` (toàn hệ thống chạy Docker `--profile core --profile mq`):
`regression-feedback.ps1` 17/17 kiểm tra đạt, `integration-smoke.ps1 -Events` 12/12 đạt. Dữ liệu demo không làm đổi kết quả
phần Đánh giá (hiện vật 1..3 giữ nguyên id).
