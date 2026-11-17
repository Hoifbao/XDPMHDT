# Content-service — nội dung cho SRS/báo cáo tuần 7

Content-service quản lý hiện vật, bản dịch và tài nguyên ảnh/audio/video trong schema `content_db`, chạy ở cổng 8083 sau API Gateway 8080. API quản trị do Gateway giới hạn quyền ADMIN; API đọc cung cấp nội dung theo ngôn ngữ yêu cầu, dùng tiếng Việt khi bản dịch không có hoặc ngôn ngữ bị tắt. File upload được kiểm tra loại/kích thước và lưu trong thư mục hoặc volume upload. Mobile đọc lại nội dung đã lưu khi mất mạng; phản hồi 404/410 được xử lý riêng để không hiển thị bản cache của hiện vật đã bị xóa.

Đường đọc được tách thành giao dịch DB ngắn để tạo DTO; việc gọi site-service và Redis diễn ra ngoài giao dịch đó. Chi tiết chưa cache dùng 4 câu truy vấn (hiện vật, bản dịch, media, ngôn ngữ), không truy vấn lại bản dịch khi chọn ngôn ngữ. Tên di tích được gộp theo ID trong danh sách, có cache và khoảng chờ ngắn khi site-service lỗi. Redis lưu DTO gốc với TTL 120 giây; bản ngôn ngữ được chọn trên DTO độc lập để không làm lẫn VI/EN. Thay đổi hiện vật/bản dịch/media xóa cache sau commit. Script Lua dùng phiên bản để chặn một lần đọc cũ ghi lại cache sau khi bị invalidation.

Redis là tối ưu đọc, không phải nguồn dữ liệu chính. Khi Redis lỗi, service quay về DB với timeout Redis 200 ms và khoảng nghỉ thử lại 2 giây. Cache invalidation lỗi được nhớ trong tiến trình để xử lý trước lần đọc tiếp theo; nếu tiến trình khởi động lại trong lúc Redis gián đoạn, TTL giới hạn thời gian tồn tại của bản cache cũ. Hikari vẫn giữ mặc định tối đa 10 kết nối, tối thiểu 2, thời gian chờ kết nối 2 giây; có biến môi trường để Bảo điều chỉnh khi đo 500 VU, không tăng pool khi chưa có số đo.

RabbitMQ truyền `ArtifactUpdated` trong `EventEnvelope` dùng chung sau commit; outbox giữ eventId khi gửi lại và chỉ đánh dấu đã gửi sau broker confirm. Khi broker lỗi, dispatcher ghi một dòng WARN cho đợt gián đoạn và dừng batch ở lần gửi lỗi đầu tiên; phục hồi được ghi bằng INFO. NiFi đồng bộ dữ liệu theo lô cho báo cáo; Redis là cache đọc nhanh. Đây là ba vai trò khác nhau.

## Đo local trước/sau cache

Đo riêng `GET /api/artifacts/1?lang=vi` trên content-service, MySQL/Redis local, 20 luồng trong 10 giây, một request warm-up. Hai lượt dùng cùng code tuần 7 và cùng pool 10; lượt đối chứng tắt cache, lượt sau bật cache. Không phải phép đo trước/sau toàn bộ code tuần 6 và không thay k6 9 phút/500 VU của nhóm.

| Chỉ số | Cache tắt | Cache bật |
|---|---:|---:|
| Request | 1.011 | 2.243 |
| Lỗi | 0 | 0 |
| Request/giây | 99,32 | 222,91 |
| p50 | 171,72 ms | 69,60 ms |
| p95 | 429,47 ms | 193,22 ms |
| p99 | 553,63 ms | 310,68 ms |
| Hikari pending lớn nhất lấy mẫu | 10 | 0 |

Lệnh tái hiện sau khi chạy service trên 18083 với `--app.detail-cache.enabled=false` hoặc `true`:

```powershell
node services/content-service/scripts/benchmark-detail.mjs --users 20 --seconds 10
```

Kiểm thử H2 + Redis thật trên namespace riêng xác nhận: cache hit giảm truy vấn chi tiết từ 4 xuống 1 (chỉ còn kiểm tra ngôn ngữ); commit xóa cache, rollback giữ cache, xóa hiện vật không trả bản cũ và lần đọc đang chạy không ghi lại cache bị invalidation. Unit test kiểm tra Redis mất kết nối, JSON cache hỏng, DTO độc lập và chống nhiều cache miss đồng thời.

Kết quả p95 ≤ 2 giây ở **500 VU trên toàn hệ thống vẫn chờ Bảo chạy k6 lần 2**. Chưa đánh dấu tiêu chí này đạt từ bài đo local.

## Dữ liệu demo

Manifest tại `services/content-service/demo-data/manifest.json` chuẩn bị 2 di tích/6 hiện vật và bản VI/EN. Ảnh dùng Wikimedia Commons, có nguồn/tác giả/giấy phép trong `media-credits.json`; thumbnail 320 px khoảng 20 KB so với ảnh preview 280–314 KB. Audio là tiếng Anh tổng hợp local, không phải bản thu chính thức, chưa có thuyết minh tiếng Việt.

Hiện chỉ có 3/6 ảnh tải thành công; 3 ảnh còn lại bị nguồn trả HTTP 429 sau các lượt chờ/thử lại. Script import kiểm đủ media trước khi gọi API, không ghi một bộ thiếu ảnh vào CMS và không thay dữ liệu hiện hữu. Khi đủ ảnh, chạy prepare-demo-thumbnails rồi import-demo-data; script dùng API upload/CRUD, giữ ID theo artifactCode, không xuất SQL seed thay Tường. Tường cần xuất seed và đóng gói file upload sau khi nhóm chốt dữ liệu.
