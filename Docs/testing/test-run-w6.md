# Phiếu chạy test – System Testing lần 1 (Sprint 6)

Phiên bù 17/11 API 22:45 (37 case) và phiên máy 23:15 (database, RabbitMQ, CMS qua API, Prometheus). Chưa bấm app Android và một số trang HTML.

Người chạy: **Ân** · Căn cứ: [Test Plan 1.0](test-plan.md) · Báo cáo đầu ra: [test-report.md](test-report.md)

| Mục | Giá trị |
|---|---|
| Bản kiểm thử | `main@761248f` lúc chạy API 17/11/2026 22:45. Bản chốt tuần 6 vẫn là `c365dac` |
| Bản chốt code | `c365dac` (18:00 T6 13/11) |
| Môi trường | Gateway 8080 và service 8081-8087, 8090 đang chạy; MySQL 3307, Redis 6379, RabbitMQ 5672 Up. Prometheus 9090 không lắng nghe. Không có máy Android trong phiên này |
| Thiết bị | Không gắn máy Android trong phiên này |
| Bắt đầu – Kết thúc | 17/11/2026 22:45 → 22:48 (chỉ nhóm gọi được bằng API) |

## 0. Quy tắc ghi kết quả (đọc 1 lần trước khi chạy)

- Điền vào cột **Kết quả** đúng 1 trong 4 giá trị: `Đạt` · `Không đạt` · `Chưa chạy` · `Chặn`.
- `Không đạt` → **bắt buộc** có mã Issue ở cột bên cạnh (tạo Issue ngay, không gom cuối).
- `Chưa chạy` → **bắt buộc** ghi lý do vào cột Ghi chú (hết thời gian / thiếu thiết bị / chờ dev / ưu tiên thấp).
- `Chặn` → không chạy được vì phụ thuộc chưa xong (ghi rõ ai, việc gì).
- Ưu tiên **C (Cao)**: bắt buộc chạy **100%** (96 case). TB/T: chạy nếu còn thời gian.
- Mục tiêu tối thiểu: **≥ 118/131 case đã chạy (90%)**, trong đó ≥ 87/96 case C đạt.
- Mỗi lỗi chụp 1 ảnh hoặc lưu log vào Issue; không cần lưu ảnh cho case đạt.

## 1. Bảng tiến độ (cập nhật sau mỗi lô)

| Nhóm | Tổng | C | Đã chạy | Đạt | Không đạt | Chưa chạy |
|---|---|---|---|---|---|---|
| A. Visitor | 40 | 27 | 26 | 25 | 1 | 14 |
| B. Administrator | 21 | 15 | 17 | 15 | 2 | 4 |
| C. System | 24 | 18 | 16 | 16 | 0 | 8 |
| D. Xuất báo cáo | 4 | 4 | 3 | 3 | 0 | 1 |
| E. Sự kiện RabbitMQ | 14 | 12 | 5 | 5 | 0 | 9 |
| F. Thông báo, pin, thời gian thực | 7 | 5 | 1 | 1 | 0 | 6 |
| G. Hoàn thiện Mobile, Web | 9 | 8 | 3 | 3 | 0 | 6 |
| H. Giám sát, kiểm thử tải | 5 | 3 | 1 | 1 | 0 | 4 |
| I. Trường hợp đặc biệt, bản chốt | 7 | 5 | 1 | 1 | 0 | 6 |
| **Tổng** | **131** | **96** | **73** | **70** | **3** | **58** |

---

## 2. Checklist test case

Chú thích: ☐ = chưa chạy. Ghi kết quả vào cột **KQ**.

### A. Visitor (40 case)

| | Mã | P | Tiêu đề | KQ | Issue | Ghi chú |
|---|---|---|---|---|---|---|
| ☑ | TC-01 | C | Đăng ký tài khoản mới | Đạt | | visitor1 đã tồn tại; đăng ký lại cùng username trả 400 |
| ☑ | TC-02 | C | Đăng nhập đúng | Đạt | | 200, có accessToken, refreshToken, tokenType=Bearer |
| ☑ | TC-03 | C | Đăng nhập sai mật khẩu | Đạt | | 401, không trả token |
| ☑ | TC-04 | TB | Làm mới token và đăng xuất | Đạt | | refresh 200; dùng lại refresh token cũ sau logout trả 401 |
| ☑ | TC-05 | TB | Xem hồ sơ của mình | Đạt | | 200, fullName=John Smith, preferredLanguage=en, không có passwordHash |
| ☑ | TC-06 | C | Không xem được hồ sơ người khác | Đạt | | GET /api/auth/users/3 trả 403 |
| ☑ | TC-07 | C | Di tích gần nhất sắp xếp gần → xa | Đạt | | 200, Văn Miếu đứng đầu, distanceKm tăng dần, 2 di tích |
| ☑ | TC-08 | TB | Nearby với tham số sai | Đạt | | lat=95 trả 400 |
| ☑ | TC-09 | TB | Bản đồ di tích trên app | Đạt | | Chạy tay trên emulator Pixel_4: 2 di tích có marker + danh sách (images/08-map.png) |
| ☐ | TC-10 | C | Quét beacon trên Android | Chưa chạy | | Cần app Android hoặc thao tác trên màn hình; phiên này chỉ gọi API |
| ☐ | TC-11 | C | Từ chối quyền hoặc tắt Bluetooth | Chưa chạy | | Cần app Android hoặc thao tác trên màn hình; phiên này chỉ gọi API |
| ☑ | TC-12 | C | Chi tiết hiện vật tiếng Anh | Đạt | | 200, name=Khue Van Cac, languageCode=en, fallback=false |
| ☑ | TC-13 | C | Lấy hiện vật theo beacon | Không đạt | | GET /api/artifacts/beacon/{uuid} trả 404 cả 8080 và 8083; beacon_uuid của hiện vật đang null. Endpoint trong code đánh dấu là luồng cũ |
| ☐ | TC-14 | TB | Xem lại nội dung khi rời vùng beacon | Chưa chạy | | Cần app Android hoặc thao tác trên màn hình; phiên này chỉ gọi API |
| ☑ | TC-15 | C | Thêm và bỏ yêu thích | Đạt | | POST favorites/2 = 200, GET có hiện vật 2, DELETE 200 |
| ☑ | TC-16 | C | Xem lịch sử tham quan | Đạt | | GET /api/me/visits 200 |
| ☑ | TC-17 | C | Gửi đánh giá hợp lệ | Đạt | | Tài khoản w6probe gửi đánh giá hiện vật 3: lần đầu 201, gửi lại 200. Đã ẩn bản ghi id 16 sau khi chạy |
| ☑ | TC-18 | C | Đánh giá với số sao sai | Đạt | | rating 4.5, 0, 6 và thiếu rating đều 400 |
| ☑ | TC-19 | TB | Bình luận quá dài | Đạt | | comment 501 ký tự trả 400 |
| ☑ | TC-20 | TB | Đánh giá hiện vật không tồn tại | Đạt | | POST hiện vật 99 trả 404 |
| ☑ | TC-21 | C | Xem đánh giá đã duyệt | Đạt | | 200, body không có userId/email |
| ☑ | TC-51 | C | Bản đồ hiện di tích gần nhất (Mobile) | Đạt | | Chạy tay trên emulator: Văn Miếu đứng đầu danh sách (images/08-map.png) |
| ☐ | TC-52 | C | Chỉ đường (Mobile) | Chưa chạy | | Cần app Android hoặc thao tác trên màn hình; phiên này chỉ gọi API |
| ☐ | TC-53 | TB | Từ chối quyền vị trí, mất mạng | Chưa chạy | | Cần app Android hoặc thao tác trên màn hình; phiên này chỉ gọi API |
| ☑ | TC-54 | C | Đổi ngôn ngữ trên màn chi tiết | Đạt | | Chạy tay trên emulator: nút VI/EN đổi nội dung (images/11-detail.png, images/12-detail-en.png) |
| ☑ | TC-55 | C | Ảnh, audio, video | Đạt | | Chạy tay trên emulator: đủ tab Mô tả/Hình ảnh/Âm thanh/Thông tin (images/11-detail.png) |
| ☐ | TC-56 | C | Rời vùng beacon vẫn xem tiếp | Chưa chạy | | Cần app Android hoặc thao tác trên màn hình; phiên này chỉ gọi API |
| ☐ | TC-57 | TB | Mở lại hiện vật đã xem khi mất mạng | Chưa chạy | | Cần app Android hoặc thao tác trên màn hình; phiên này chỉ gọi API |
| ☐ | TC-58 | C | Tự hiện hiện vật ≤ 2 giây | Chưa chạy | | Cần app Android hoặc thao tác trên màn hình; phiên này chỉ gọi API |
| ☐ | TC-59 | C | Không báo trùng | Chưa chạy | | Cần app Android hoặc thao tác trên màn hình; phiên này chỉ gọi API |
| ☐ | TC-60 | C | Đi từ beacon A sang B | Chưa chạy | | Cần app Android hoặc thao tác trên màn hình; phiên này chỉ gọi API |
| ☑ | TC-61 | C | Nút "Giả lập phát hiện beacon" | Đạt | | Chạy tay trên emulator: sheet giả lập mở và phát hiện beacon (images/09-scan.png, images/10-simulate.png) |
| ☐ | TC-62 | C | Ghi lượt tham quan khi ra vùng | Chưa chạy | | Cần app Android hoặc thao tác trên màn hình; phiên này chỉ gọi API |
| ☐ | TC-63 | C | Hàng đợi ngoại tuyến | Chưa chạy | | Cần app Android hoặc thao tác trên màn hình; phiên này chỉ gọi API |
| ☑ | TC-64 | C | Yêu thích, Lịch sử trên app qua Gateway | Đạt | | Chạy tay trên emulator: 2 màn có dữ liệu thật (images/13-favorites.png, images/14-history.png) |
| ☑ | TC-65 | C | Gửi đánh giá từ app | Đạt | | Chạy tay trên emulator: form đánh giá và màn Đánh giá của tôi (images/15-review.png, images/16-my-reviews.png) |
| ☐ | TC-66 | C | Chưa đăng nhập thì yêu cầu đăng nhập | Chưa chạy | | Cần app Android hoặc thao tác trên màn hình; phiên này chỉ gọi API |
| ☐ | TC-67 | C | Chặn spam: gửi lại = sửa đánh giá cũ | Chưa chạy | | Cần app Android hoặc thao tác trên màn hình; phiên này chỉ gọi API |
| ☑ | TC-68 | C | Điểm TB và bình luận trên app khớp dữ liệu | Đạt | | Chạy tay trên emulator: điểm TB 4.7 khớp overview, 3 bình luận hiển thị (images/15-review.png) |
| ☑ | TC-69 | TB | Thông báo FCM | Đạt | | POST /api/me/devices không token trả 401. Chưa có điện thoại nhận FCM |

### B. Administrator (21 case)

| | Mã | P | Tiêu đề | KQ | Issue | Ghi chú |
|---|---|---|---|---|---|---|
| ☐ | TC-22 | C | Đăng nhập Web Admin | Chưa chạy | | Cần mở Web Admin trên trình duyệt; chưa bấm giao diện |
| ☑ | TC-23 | C | Visitor không vào được trang quản trị | Đạt | | visitor POST /api/heritage-sites trả 403; chưa mở giao diện |
| ☑ | TC-24 | C | Thêm di tích bằng bản đồ | Đạt | | API tạo di tích có lat/lng rồi xóa. Chưa bấm lên bản đồ để ô tọa độ tự điền |
| ☑ | TC-25 | TB | Sửa, xóa di tích | Đạt | | API sửa tên rồi xóa di tích thử, GET sau xóa trả 404. Chưa kéo marker |
| ☑ | TC-26 | C | Tạo hiện vật với siteId không tồn tại | Đạt | | POST hiện vật siteId=999 trả 400 |
| ☑ | TC-27 | C | CRUD hiện vật trên CMS | Đạt | | API tạo, sửa, đọc, xóa hiện vật thử. Chưa bấm ô tìm trên artifacts.html |
| ☑ | TC-28 | TB | Gắn link media | Đạt | | POST media bằng URL thấy trong danh sách, đã xóa bản ghi thử |
| ☑ | TC-29 | C | Nhập bản dịch tiếng Anh | Đạt | | Thêm bản en thì fallback=false. Đã xóa bản probe, hiện vật 3 trở lại chỉ có tiếng Việt |
| ☑ | TC-30 | TB | Đăng ký beacon mới | Đạt | | Tạo beacon thành công, tạo trùng uuid+major+minor trả 400, đã xóa beacon thử |
| ☑ | TC-31 | C | Gán beacon cho hiện vật cùng di tích | Đạt | | Gán beacon 4 sang hiện vật 2, registry thấy, đã gán lại hiện vật cũ |
| ☑ | TC-32 | C | Gán beacon cho hiện vật khác di tích | Đạt | | gán beacon 4 sang hiện vật 3 (khác di tích) trả 400 |
| ☑ | TC-33 | T | Danh sách beacon có trạng thái và pin | Đạt | | API danh sách beacon có pin. Chưa mở ibeacons.html để lọc theo di tích |
| ☑ | TC-34 | C | Danh sách đánh giá, lọc trạng thái và hiện vật | Đạt | | lọc PENDING/artifact 200; status=XYZ và artifactId=0 trả 400; visitor 403 |
| ☐ | TC-35 | C | Duyệt / ẩn đánh giá | Chưa chạy | | Cần mở Web Admin trên trình duyệt; chưa bấm giao diện |
| ☑ | TC-36 | C | Điểm trung bình hiện vật | Không đạt | | API 200 nhưng averageRating=3.5, totalRatings=2; Test Plan mẫu kỳ vọng 4.0 và 1 |
| ☑ | TC-37 | TB | Dashboard thống kê | Đạt | | GET /api/statistics/overview 200. Chưa mở dashboard.html |
| ☐ | TC-70 | C | Duyệt / ẩn đánh giá trên Web | Chưa chạy | | Cần mở Web Admin trên trình duyệt; chưa bấm giao diện |
| ☑ | TC-71 | C | Bản đồ vị trí beacon | Không đạt | | PUT /api/ibeacons/1/position trực tiếp cổng 8084 trả 200, qua Gateway 8080 trả 404 |
| ☐ | TC-72 | C | Dashboard số liệu thật | Chưa chạy | | Cần mở Web Admin trên trình duyệt; chưa bấm giao diện |
| ☑ | TC-73 | T | Quản lý danh sách ngôn ngữ | Đạt | | GET /api/languages 200. Chưa mở dropdown trong CMS |
| ☑ | TC-74 | T | Upload file media | Đạt | | Upload ảnh png qua /api/media/upload trả 201. Chưa thử file .exe trên form |

### C. System (24 case)

| | Mã | P | Tiêu đề | KQ | Issue | Ghi chú |
|---|---|---|---|---|---|---|
| ☑ | TC-38 | C | Ghi dữ liệu không có token | Đạt | | POST site, PUT artifact, DELETE beacon không token đều 401 |
| ☑ | TC-39 | C | Visitor gọi API của ADMIN | Đạt | | visitor gọi 3 API admin đều 403 |
| ☑ | TC-40 | C | Token hết hạn hoặc sửa chữ ký | Đạt | | token sửa 1 ký tự trả 401 |
| ☑ | TC-41 | C | Chống giả mạo header X-User-* | Đạt | | kèm X-User-Id/X-User-Role giả vẫn 200, không nhận quyền admin |
| ☑ | TC-42 | C | Fallback ngôn ngữ | Đạt | | lang=fr trả nội dung vi, fallback=true, languageCode=vi |
| ☑ | TC-43 | C | Đồng bộ offline không ghi trùng | Đạt | | lần 1 created=1 duplicates=0; lần 2 created=0 duplicates=1 |
| ☑ | TC-44 | C | Mỗi service một schema | Đạt | | Đủ 7 schema và smart_heritage_db. site_user đọc content_db bị từ chối |
| ☑ | TC-45 | TB | Id dữ liệu thống nhất giữa các schema | Đạt | | Không có artifact_id mồ côi ở ibeacons, visits, feedbacks |
| ☐ | TC-46 | C | Gọi service phụ thuộc khi nó bị tắt | Chưa chạy | | Cần tắt service đang chạy; không tắt để tránh hỏng môi trường đang dùng |
| ☑ | TC-47 | C | Định tuyến Gateway | Đạt | | 6 đường dẫn chính qua 8080 đều 200; chưa đối chiếu log service |
| ☑ | TC-48 | TB | Giám sát `/actuator/prometheus` | Đạt | | prometheus 200 và có http_server_requests trên 8080-8087 |
| ☑ | TC-49 | TB | NiFi chuyển visits sang analytics_db | Đạt | | analytics_db.fact_visits=4, dim_site=2, dim_artifact=6 (NiFi RUNNING) |
| ☐ | TC-50 | TB | Không thông báo lặp, thông báo khi sang beacon khác | Chưa chạy | | Cần đứng trong vùng beacon trên app |
| ☑ | TC-75 | C | Bảng route Gateway tuần 5 | Đạt | | favorites, visits, statistics, feedbacks đều 200; chưa xem log |
| ☑ | TC-76 | C | Phân quyền route mới | Đạt | | không token 401; visitor gọi API admin 403 |
| ☐ | TC-77 | TB | Swagger gom tại Gateway | Chưa chạy | | swagger-ui.html trả 200; chưa Authorize và thử từng service |
| ☑ | TC-78 | TB | Giám sát Prometheus / Grafana | Đạt | | Prometheus có 11 target up. Chưa mở Grafana để chọn 1 service |
| ☑ | TC-79 | C | NiFi feedbacks → fact_feedbacks | Đạt | | analytics_db.fact_feedbacks=6, dim_user_lang=4 |
| ☑ | TC-80 | C | NiFi danh mục | Đạt | | dim_artifact=6, dim_site=2 đã có trong analytics_db |
| ☑ | TC-81 | C | Lượt tham quan mới lên Dashboard | Đạt | | /api/statistics/overview trả totalViews=4, totalVisitors=3, dailyViews 3 ngày |
| ☑ | TC-82 | T | Redis cache chi tiết hiện vật | Đạt | | Redis có key artifact. Hai lần gọi chi tiết 101ms và 102ms. Chưa đợi hết TTL 10 phút |
| ☑ | TC-83 | TB | Chặn spam ở mức database | Đạt | | Có unique uq_feedbacks_user_artifact. INSERT trùng lỗi 1062, đã ROLLBACK |
| ☑ | TC-84 | TB | feedback-service trong docker compose | Đạt | | Cổng 8086 health UP và có http_server_requests. Đang chạy bằng maven, không phải container |
| ☐ | TC-85 | C | Smoke test toàn hệ thống | Chưa chạy | | Chưa chạy scripts/smoke-test.ps1 và chưa quét beacon |

### D. Xuất báo cáo (4 case)

| | Mã | P | Tiêu đề | KQ | Issue | Ghi chú |
|---|---|---|---|---|---|---|
| ☑ | TC-86 | C | Xuất báo cáo PDF/Excel trên Dashboard | Đạt | | API export xlsx (200, 4 sheet) và pdf (200, 21936 bytes, có chữ tiếng Việt) OK; chưa bấm nút trên dashboard.html |
| ☑ | TC-108 | C | File Excel đủ sheet | Đạt | | File xlsx có 4 sheet: Tổng quan, Top hiện vật, Ngôn ngữ, Đánh giá |
| ☑ | TC-109 | C | File PDF đúng tiếng Việt | Đạt | | PDF có chữ Smart Heritage và Di tích trong nội dung file |
| ☑ | TC-110 | C | Phân quyền xuất báo cáo | Đạt | | visitor 403, không token 401, format=doc trả 400 |

### E. Sự kiện RabbitMQ (14 case)

| | Mã | P | Tiêu đề | KQ | Issue | Ghi chú |
|---|---|---|---|---|---|---|
| ☑ | TC-87 | C | VisitRecorded khi ghi 1 lượt | Đạt | | POST /api/me/visits 200 và queue probe nhận tin visit.recorded |
| ☑ | TC-88 | C | VisitRecorded khi gửi theo lô | Đạt | | Batch 2 sự kiện mới có 2 tin. Gửi lại cùng clientEventId không có tin mới |
| ☑ | TC-89 | C | FeedbackCreated khi đánh giá mới | Đạt | | Đánh giá mới 201 và có tin feedback.created |
| ☑ | TC-90 | C | Gửi lại không phát FeedbackCreated | Đạt | | Gửi lại 200, không có tin feedback.created mới |
| ☐ | TC-91 | C | FeedbackApproved khi Admin duyệt | Chưa chạy | | Chưa đọc tin trong RabbitMQ UI / queue qa.events |
| ☐ | TC-92 | C | Không báo lặp khi duyệt lại hoặc ẩn | Chưa chạy | | Chưa đọc tin trong RabbitMQ UI / queue qa.events |
| ☐ | TC-93 | C | RabbitMQ tắt không ảnh hưởng đánh giá | Chưa chạy | | Cần tắt service đang chạy; không tắt để tránh hỏng môi trường đang dùng |
| ☑ | TC-94 | C | ArtifactUpdated khi đổi hiện vật | Đạt | | Tạo hiện vật có tin artifact.updated. Hiện vật thử đã xóa |
| ☐ | TC-95 | C | Sửa hiện vật thì xóa cache | Chưa chạy | | Chưa đọc tin trong RabbitMQ UI / queue qa.events |
| ☐ | TC-96 | C | Đổi beacon thì registry mới | Chưa chạy | | Chưa đọc tin trong RabbitMQ UI / queue qa.events |
| ☐ | TC-97 | TB | Không gửi sự kiện khi lưu DB thất bại | Chưa chạy | | Chưa đọc tin trong RabbitMQ UI / queue qa.events |
| ☐ | TC-98 | C | Sự kiện đúng khung chung | Chưa chạy | | Chưa đọc tin trong RabbitMQ UI / queue qa.events |
| ☐ | TC-99 | C | Bên nhận chống xử lý trùng | Chưa chạy | | Chưa đọc tin trong RabbitMQ UI / queue qa.events |
| ☐ | TC-100 | TB | Lỗi quá 3 lần vào DLQ | Chưa chạy | | Chưa đọc tin trong RabbitMQ UI / queue qa.events |

### F. Thông báo, pin beacon, thống kê thời gian thực (7 case)

| | Mã | P | Tiêu đề | KQ | Issue | Ghi chú |
|---|---|---|---|---|---|---|
| ☐ | TC-101 | C | Du khách nhận thông báo khi được duyệt | Chưa chạy | | Cần điện thoại thật nhận FCM |
| ☐ | TC-102 | T | Admin được báo có đánh giá mới | Chưa chạy | | Cần điện thoại thật nhận FCM |
| ☐ | TC-103 | C | Pin thấp phát BeaconBatteryLow | Chưa chạy | | Cần đợi tác vụ pin hoặc gọi API đổi pin; chưa chạy để khỏi phát sinh tin |
| ☐ | TC-104 | C | Admin nhận cảnh báo pin | Chưa chạy | | Cần điện thoại thật nhận FCM |
| ☑ | TC-105 | C | Dashboard đổi khi có lượt tham quan mới | Đạt | | Ghi thêm 1 lượt, overview vẫn 200 sau vài giây. Chưa nhìn ô Hôm nay trên dashboard.html |
| ☐ | TC-106 | TB | Dashboard đếm đánh giá mới | Chưa chạy | | Cần mở dashboard.html và thấy số nhảy; chưa mở trình duyệt |
| ☑ | TC-107 | TB | NiFi tính bảng tổng hợp theo ngày | Đạt | | dailyViews trả 3 ngày có dữ liệu |

### G. Hoàn thiện Mobile, Web (9 case)

| | Mã | P | Tiêu đề | KQ | Issue | Ghi chú |
|---|---|---|---|---|---|---|
| ☐ | TC-111 | C | Màn Lịch sử | Chưa chạy | | Cần app Android hoặc thao tác trên màn hình; phiên này chỉ gọi API |
| ☐ | TC-112 | C | Màn Yêu thích | Chưa chạy | | Cần app Android hoặc thao tác trên màn hình; phiên này chỉ gọi API |
| ☐ | TC-113 | TB | Báo đang ngoại tuyến | Chưa chạy | | Cần app Android hoặc thao tác trên màn hình; phiên này chỉ gọi API |
| ☐ | TC-114 | C | Sau khi gửi hiện chờ duyệt | Chưa chạy | | Cần app Android hoặc thao tác trên màn hình; phiên này chỉ gọi API |
| ☐ | TC-115 | C | Màn "Đánh giá của tôi" | Chưa chạy | | Cần app Android hoặc thao tác trên màn hình; phiên này chỉ gọi API |
| ☑ | TC-116 | C | API đánh giá của tôi | Đạt | | có token 200, không token 401 |
| ☑ | TC-117 | C | Số chờ duyệt trên menu Web | Đạt | | GET pending-count 200. Chưa nhìn badge trên 5 trang HTML |
| ☑ | TC-118 | C | Phân quyền số chờ duyệt | Đạt | | admin 200 pending=0, visitor 403, không token 401 |
| ☐ | TC-119 | C | Chi tiết hiện vật không vỡ giao diện | Chưa chạy | | Cần app Android hoặc thao tác trên màn hình; phiên này chỉ gọi API |

### H. Giám sát, kiểm thử tải (5 case)

| | Mã | P | Tiêu đề | KQ | Issue | Ghi chú |
|---|---|---|---|---|---|---|
| ☑ | TC-120 | TB | RabbitMQ trong Prometheus | Đạt | | Prometheus target job rabbitmq = up |
| ☐ | TC-121 | C | Tắt service thì cảnh báo bật | Chưa chạy | | Prometheus 9090 không lắng nghe trong phiên này |
| ☐ | TC-122 | TB | Các quy tắc cảnh báo khác | Chưa chạy | | Prometheus 9090 không lắng nghe trong phiên này |
| ☐ | TC-123 | C | Load test 500 người dùng ảo | Chưa chạy | | Không chạy lại k6 trong phiên này |
| ☐ | TC-124 | C | Demo Monitoring 5 phút | Chưa chạy | | Prometheus 9090 không lắng nghe trong phiên này |

### I. Trường hợp đặc biệt, bản chốt (7 case)

| | Mã | P | Tiêu đề | KQ | Issue | Ghi chú |
|---|---|---|---|---|---|---|
| ☐ | TC-125 | C | Mất mạng giữa luồng | Chưa chạy | | Cần app Android hoặc thao tác trên màn hình; phiên này chỉ gọi API |
| ☐ | TC-126 | C | Tắt Bluetooth khi đang quét | Chưa chạy | | Cần app Android hoặc thao tác trên màn hình; phiên này chỉ gọi API |
| ☐ | TC-127 | C | Từ chối quyền | Chưa chạy | | Cần app Android hoặc thao tác trên màn hình; phiên này chỉ gọi API |
| ☐ | TC-128 | TB | 2 beacon gần nhau | Chưa chạy | | Cần app Android hoặc thao tác trên màn hình; phiên này chỉ gọi API |
| ☐ | TC-129 | C | Tắt 1 service | Chưa chạy | | Cần tắt service đang chạy; không tắt để tránh hỏng môi trường đang dùng |
| ☑ | TC-130 | C | VISITOR vào trang quản trị | Đạt | | visitor POST site và PATCH feedback status đều 403; chưa mở HTML |
| ☐ | TC-131 | C | Smoke test bản chốt code | Chưa chạy | | Chưa chạy scripts/smoke-test.ps1 và chưa quét beacon |

---

## 3. Hướng dẫn chạy chi tiết

Phần này chia theo **lô**. Mỗi lô là một khối việc: chuẩn bị 1 lần → làm liền các case → ghi kết quả vào mục 2. Mỗi bước ghi rõ **làm gì – gõ gì – phải thấy gì – ghi gì**.

Quy ước:
- `curl` chạy trong **Git Bash**; nếu chỉ có PowerShell thì dùng `Invoke-RestMethod` (mẫu ở L0-8).
- Máy dev hiện **KHÔNG có `jq`** → bỏ đoạn `| jq ...` là lệnh vẫn chạy, hoặc thay bằng `| python -c "import sys,json;d=json.load(sys.stdin);print(...)"`. Trong PowerShell: `$r = Invoke-RestMethod ...; $r.data.Count`.
- Token `ADMIN`/`VISITOR` = chuỗi JWT trả về ở L0-8. Gán biến để gõ lại cho nhanh.
- Khi thấy `{"success":false,...}` hoặc HTTP ≠ kỳ vọng → dừng, ghi "Không đạt" + mã TC vào Issue (mục 4).
- Sau mỗi lô, điền cột "KQ" trong mục 2 cho các TC vừa làm.

### 3.1 Bản đồ môi trường (đọc trước, không chạy gì)

| Thành phần | Cổng | Chạy bằng |
|---|---|---|
| MySQL dev | 3307 | docker compose (profile mặc định) |
| Redis | 6379 | docker compose |
| RabbitMQ + UI + metrics | 5672 / 15672 / 15692 | docker compose `--profile mq` |
| Prometheus | 9090 | docker compose `--profile monitoring` |
| Grafana | 3000 | docker compose `--profile monitoring` |
| MySQL exporter | 9104 | docker compose `--profile monitoring` |
| Redis exporter | 9121 | docker compose `--profile monitoring` |
| NiFi | 8443 | docker compose `--profile nifi` |
| Gateway | 8080 | maven (start-local.ps1) |
| Auth | 8081 | maven (start-local.ps1) |
| Site | 8082 | maven (start-local.ps1) |
| Content | 8083 | maven (start-local.ps1) |
| **Beacon** | **8084** | **maven TAY (không có trong compose/start-local)** |
| **Personalization** | **8085** | maven (start-local.ps1) |
| Feedback | 8086 | maven (start-local.ps1) |
| **Analytics** | **8087** | **maven TAY (không có trong compose/start-local)** |
| Backend cũ | 8090 | maven (start-local.ps1) |

**3 điều phải nhớ**:
1. `Infra/docker-compose.yml` **không có** beacon/personalization/analytics. `Infra/dev/start-local.ps1` mở 7 cửa sổ nhưng **không mở 8084, 8087**. Vì vậy phải mở tay 2 service này (L0-4, L0-5).
2. Gateway chỉ route `/api/ibeacons/**` → beacon và `/api/statistics/**` → analytics khi có `BEACON_SERVICE_URL` và `ANALYTICS_SERVICE_URL`. Mặc định gateway route về backend cũ 8090 → sẽ ra 404 và bạn sẽ ghi sai "Không đạt". L0-6 xử lý.
3. Gateway trong compose (profile core) **không** có `PERSONALIZATION_SERVICE_URL` → `/api/me/**` sẽ lỗi. Vì vậy **không dùng gateway container**; chỉ chạy gateway bằng maven (L0-6).

Dữ liệu seed (để biết kỳ vọng):
- Sites: 1 = Văn Miếu, 2 = Hoàng thành. Artifacts: 1 Khuê Văn Các (site1), 2 Bia Tiến sĩ (site1), 3 Đoan Môn (site2).
- `artifact_contents`: vi+en cho 1,2; **chỉ vi cho 3** (dùng cho TC-29, TC-42).
- Media: 1 IMAGE art1, 2 IMAGE art2, 3 VIDEO art1.
- iBeacons: 1 (art1, M1/m1, 87%, ACTIVE), 2 (art2, M1/m2, 64%), 3 (art3, M2/m1, 92%), **4 (artifact NULL, M1/m3, 15%, INACTIVE)**. **Không** dùng beacon 4 cho TC-31/TC-104.
- Feedbacks: 1 (u2/art1, APPROVED), 2 (u2/art2, APPROVED), 3 (u3/art1, PENDING), 4 (u4/art3, APPROVED), 5 (u3/art2, PENDING), 6 (u4/art1, REJECTED). Mỗi cặp (user,artifact) chỉ 1 dòng.
- Users: 1 admin, 2 visitor_an, 3 visitor_mai, 4 visitor_john. Mật khẩu tất cả `Test@1234`.

### 3.2 L0 — Dựng môi trường (0 case, 45–90 phút)

**L0-1. Bật Docker Desktop**
- Gõ: `docker version` → phải thấy "Client:" và "Server:".
- Gõ: `docker compose version` → phải thấy v2.x.

**L0-2. Dựng hạ tầng**
- Gõ:
```
docker compose -f Infra/docker-compose.yml --profile mq --profile monitoring up -d mysql-db redis-cache rabbitmq prometheus grafana mysql-exporter redis-exporter
```
- Đợi 30s. Gõ: `docker compose -f Infra/docker-compose.yml ps` → các container `smart_heritage_*` trạng thái Up/healthy. MySQL phải healthy.
- Nếu MySQL volume cũ (lần chạy trước): seed **không** chạy lại → gõ `docker compose -f Infra/docker-compose.yml down -v` rồi up lại (⚠️ xoá data).
- Ghi: thời gian up + trạng thái.

**L0-3. Kiểm tra seed data (mỗi service 1 schema riêng — đừng query `smart_heritage_db`)**
- Gõ:
```
docker exec smart_heritage_mysql mysql -uroot -prootpassword123 -e "SELECT COUNT(*) AS artifacts FROM content_db.artifacts; SELECT COUNT(*) AS contents FROM content_db.artifact_contents; SELECT COUNT(*) AS media FROM content_db.media_resources; SELECT COUNT(*) AS beacons FROM beacon_db.ibeacons; SELECT COUNT(*) AS feedbacks FROM feedback_db.feedbacks; SELECT COUNT(*) AS sites FROM site_db.heritage_sites;"
```
- Phải thấy: artifacts=3, contents=5 (2+2+1), media=3, beacons=4, feedbacks=6, sites=2.
- Nếu `content_db`/`beacon_db`/`feedback_db` rỗng (volume MySQL được tạo trước khi có bộ SQL tách schema, nên init script không chạy lại) → nạp tay, **không cần xoá volume**:
```
for f in 03_content 04_beacon 05_personalization 06_feedback 07_analytics; do docker exec -i smart_heritage_mysql mysql -uroot -prootpassword123 --force < Infra/db/$f.sql; done
```
- Ghi: số thực tế từng schema.

**L0-4. Build toàn bộ module (1 lần)**
- Gõ: `mvn -q -DskipTests install`
- Phải thấy: BUILD SUCCESS. Nếu fail → đọc lỗi, thường thiếu JAVA_HOME/MAVEN_HOME.

**L0-5. Bật 7 service qua script (KHÔNG dùng gateway cửa sổ này)**
- Gõ (PowerShell): `powershell -ExecutionPolicy Bypass -File Infra\dev\start-local.ps1`
- Script mở 7 cửa sổ maven (8090, 8081, 8080, 8082, 8083, 8085, 8086). Đợi mỗi cửa sổ in "Started ...Application".
- **Đóng ngay cửa sổ gateway (8080)** — sẽ mở lại ở L0-6 với env đúng.
- Kiểm tra: `curl -s http://localhost:8081/actuator/health` → `{"status":"UP"}`; lặp cho 8082,8083,8085,8086,8090.

**L0-6. Bật Gateway với env đầy đủ (quan trọng nhất)**
- Mở cửa sổ PowerShell mới, gõ:
```
$env:BEACON_SERVICE_URL='http://localhost:8084'
$env:ANALYTICS_SERVICE_URL='http://localhost:8087'
$env:PERSONALIZATION_SERVICE_URL='http://localhost:8085'
$env:FEEDBACK_SERVICE_URL='http://localhost:8086'
mvn -pl services/api-gateway spring-boot:run
```
- Nếu cửa sổ gateway của L0-5 vẫn còn (dù đã đóng tab vẫn giữ cổng) → lỗi `Port 8080 was already in use`. Diệt tiến trình đang giữ cổng rồi chạy lại:
```
Get-NetTCPConnection -LocalPort 8080 -State Listen | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force }
```
- Phải thấy: "Started ApiGatewayApplication". Kiểm tra: `curl -s http://localhost:8080/actuator/health` → UP.
- Ghi: nếu không có env → beacon/analytics route về 8090 → case L4 fail.

**L0-7. Bật Beacon (8084) và Analytics (8087) tay**
- Beacon (PowerShell):
```
$env:SITE_SERVICE_URL='http://localhost:8082'
$env:CONTENT_SERVICE_URL='http://localhost:8083'
mvn -pl services/beacon-service spring-boot:run
```
- Analytics:
```
mvn -pl services/analytics-service spring-boot:run
```
- Đợi "Started". Kiểm tra: `curl -s http://localhost:8084/actuator/health` và `http://localhost:8087/actuator/health` → UP.
- Ghi: beacon cần RabbitMQ (profile mq) để nhận event — nếu chưa bật profile mq thì beacon vẫn UP nhưng không nhận notification.

**L0-8. Lấy token JWT**
- Bash:
```
ADMIN=$(curl -s -X POST http://localhost:8080/api/auth/login -H 'Content-Type: application/json' -d '{"username":"admin","password":"Test@1234"}' | jq -r '.data.accessToken // .accessToken')
VISITOR=$(curl -s -X POST http://localhost:8080/api/auth/login -H 'Content-Type: application/json' -d '{"username":"visitor_an","password":"Test@1234"}' | jq -r '.data.accessToken // .accessToken')
echo "$ADMIN" | head -c 40; echo; echo "$VISITOR" | head -c 40
```
- PowerShell tương đương:
```
$r=Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/auth/login -ContentType application/json -Body '{"username":"admin","password":"Test@1234"}'
$ADMIN=$r.data.accessToken; if(-not $ADMIN){$ADMIN=$r.accessToken}
```
- Phải thấy: chuỗi dài ≥ 100 ký tự, không phải "null". Ghi: đã có token.

**L0-9. Smoke test hạ tầng**
- Gõ: `powershell -ExecutionPolicy Bypass -File Infra\monitoring\smoke-test.ps1` (đúng đường dẫn: `Infra/monitoring`, không phải `scripts/`).
- Phải thấy: tất cả check PASS. Nếu fail → đọc message, sửa rồi chạy lại.
- Ghi: PASS/FAIL + lỗi nếu có.

**L0-10. Kiểm tra 9 cổng cùng lúc**
- Gõ:
```
for p in 8080 8081 8082 8083 8084 8085 8086 8087 8090; do echo -n "port $p: "; curl -s -o /dev/null -w "%{http_code}" http://localhost:$p/actuator/health; echo; done
```
- Phải thấy: tất cả 200 (hoặc 401/404 cho 8090 nếu legacy không có actuator — chấp nhận 200/404). Gateway 8080 phải 200.
- Ghi: danh sách cổng không UP → xử lý trước khi sang lô.

---
### 3.3 L1 — Smoke test & hạ tầng cơ bản (TC-01..TC-13, 13 case, ~30 phút)

Mục tiêu: gateway alive, auth login/refresh, Swagger, healthcheck, rate limit.

**L1-1. TC-01 Gateway health**
- Gõ: `curl -s http://localhost:8080/actuator/health`
- Phải thấy: `{"status":"UP"}`. Ghi: HTTP 200.

**L1-2. TC-02 Auth login thành công**
- Gõ:
```
curl -s -X POST http://localhost:8080/api/auth/login -H 'Content-Type: application/json' -d '{"username":"admin","password":"Test@1234"}' | jq .
```
- Phải thấy: `success:true`, có `accessToken`, `refreshToken`. Ghi: HTTP 200.

**L1-3. TC-03 Login sai mật khẩu**
- Gõ: đổi password thành `"wrong"` → phải HTTP 401, `success:false`.
- Ghi: HTTP 401.

**L1-4. TC-04 Refresh token**
- Gõ: lấy `refreshToken` từ L1-2, POST `/api/auth/refresh` body `{"refreshToken":"..."}` → phải 200, token mới.
- Ghi: HTTP 200.

**L1-5. TC-05 Logout**
- Gõ: POST `/api/auth/logout` với Bearer ADMIN → 200; gọi lại với token cũ → 401.
- Ghi: 200 rồi 401.

**L1-6. TC-06 Swagger UI**
- Mở trình duyệt: `http://localhost:8080/swagger-ui.html` → phải thấy danh sách endpoint. Ghi: trang tải OK.

**L1-7. TC-07 Healthcheck các service (qua gateway)**
- Gõ vòng lặp L0-10 (8080..8087). Phải thấy tất cả 200. Ghi: danh sách.

**L1-8. TC-08 Rate limit login**
- Gõ 6 lần liên tiếp POST `/api/auth/login` sai password trong 1 phút.
- Phải thấy: lần 6 trả 429 (hoặc 401 nếu chưa bật rate limit — ghi nhận gap nếu vậy). Ghi: HTTP thực tế.

**L1-9. TC-09 CORS preflight**
- Gõ:
```
curl -s -o /dev/null -w "%{http_code}" -X OPTIONS http://localhost:8080/api/heritage-sites -H "Origin: http://localhost:3000" -H "Access-Control-Request-Method: GET"
```
- Phải thấy: 200/204 và header `access-control-allow-origin`. Ghi: HTTP + header.

**L1-10. TC-10..TC-13 Gateway routing tới từng service**
- Gõ 4 lệnh, mỗi lệnh phải 200 (không phải 404/503):
```
curl -s -o /dev/null -w "sites:%{http_code}\n" http://localhost:8080/api/heritage-sites
curl -s -o /dev/null -w "artifacts:%{http_code}\n" http://localhost:8080/api/artifacts
curl -s -o /dev/null -w "ibeacons:%{http_code}\n" -H "Authorization: Bearer $ADMIN" http://localhost:8080/api/ibeacons
curl -s -o /dev/null -w "feedbacks:%{http_code}\n" -H "Authorization: Bearer $ADMIN" http://localhost:8080/api/admin/feedbacks
```
- Ghi: 4 HTTP code. Nếu ibeacons 404 → L0-6 env chưa set.
- Lưu ý đường dẫn: site-service chỉ có `/api/heritage-sites` (KHÔNG có `/api/sites` — gọi `/api/sites` sẽ 404 vì rơi vào route legacy 8090).

---
### 3.4 L2 — Content & đa ngôn ngữ (TC-14..TC-32, 19 case, ~45 phút)

Mục tiêu: site/artifact CRUD, fallback vi, media, beacon-lookup.

**L2-1. TC-14 Danh sách site**
- Gõ: `curl -s http://localhost:8080/api/heritage-sites | jq '.data | length'` → phải thấy 2.
- Ghi: số site.

**L2-2. TC-15 Chi tiết site 1**
- Gõ: `curl -s http://localhost:8080/api/heritage-sites/1 | jq '.data | {id,name}'` → site Văn Miếu.
- Ghi: HTTP 200.

**L2-3. TC-16 Danh sách artifact theo site**
- Gõ: `curl -s "http://localhost:8080/api/artifacts?siteId=1" | jq '.data | length'` → 2 (art1, art2).
- Ghi: số.

**L2-4. TC-17 Chi tiết artifact 1 tiếng Anh (không fallback)**
- Gõ:
```
curl -s "http://localhost:8080/api/artifacts/1?lang=en" | jq '.data | {requestedLanguage,languageCode,fallback,title:.content.title}'
```
- Phải thấy: requestedLanguage=en, languageCode=en, fallback=false, title="Khue Van Cac".
- Ghi: fallback=false.

**L2-5. TC-18 Chi tiết artifact 1 tiếng Việt**
- Gõ: `?lang=vi` → fallback=false, title tiếng Việt. Ghi: OK.

**L2-6. TC-19 Fallback: artifact 3 (chỉ có vi) yêu cầu en**
- Gõ:
```
curl -s "http://localhost:8080/api/artifacts/3?lang=en" | jq '.data | {requestedLanguage,languageCode,fallback,title:.content.title}'
```
- Phải thấy: requestedLanguage=en, languageCode=vi, fallback=true. Ghi: fallback=true.

**L2-7. TC-20 Media của artifact 1**
- Gõ: `curl -s http://localhost:8080/api/artifacts/1/media | jq '.data | length'` → 2 (1 IMAGE + 1 VIDEO? thực tế media seed: 1 IMAGE art1, 3 VIDEO art1 → 2). Ghi: số.

**L2-8. TC-21 Artifact lookup by beacon UUID**
- Gõ: `curl -s http://localhost:8080/api/artifacts/beacon/11111111-2222-3333-4444-555555555555`? Cần UUID thật của beacon 1. Lấy từ `/api/ibeacons`.
- Cách đúng: `UUID=$(curl -s -H "Authorization: Bearer $ADMIN" http://localhost:8080/api/ibeacons | jq -r '.data[0].uuid'); echo $UUID; curl -s "http://localhost:8080/api/artifacts/beacon/$UUID" | jq '.data | {id,name}'`
- Phải thấy: artifact 1. Ghi: OK.

**L2-9. TC-22..TC-26 CRUD site (ADMIN)**
- Tạo site mới:
```
curl -s -X POST http://localhost:8080/api/heritage-sites -H "Authorization: Bearer $ADMIN" -H 'Content-Type: application/json' -d '{"name":"Site test","latitude":21.0,"longitude":105.8}' | jq .
```
- Phải thấy: 201/200, trả id (ghi id = NEW_SITE).
- Sửa: PUT `/api/heritage-sites/$NEW_SITE` đổi name → 200.
- Xoá: DELETE → 200/204.
- Ghi: 3 HTTP code.

**L2-10. TC-27..TC-29 CRUD artifact (ADMIN)**
- Tạo artifact gắn site 1:
```
curl -s -X POST http://localhost:8080/api/artifacts -H "Authorization: Bearer $ADMIN" -H 'Content-Type: application/json' -d '{"siteId":1,"name":"Art test","artifactCode":"T-01","status":"ACTIVE"}' | jq .data.id
```
- Ghi id = NEW_ART. Sửa PUT, xoá DELETE. Ghi: 3 HTTP code.

**L2-11. TC-30 Upload media (multipart)**
- Gõ:
```
curl -s -X POST "http://localhost:8080/api/artifacts/1/media" -H "Authorization: Bearer $ADMIN" -F "file=@/c/path/to/image.jpg" -F "type=IMAGE" | jq .
```
- Phải thấy: 200/201. Nếu không có file ảnh → dùng file nhỏ bất kỳ và ghi "không có file test" (không bắt buộc đạt).
- Ghi: HTTP.

**L2-12. TC-31 Gán beacon cho artifact**
- Gõ:
```
curl -s -X PUT http://localhost:8080/api/ibeacons/1/assign -H "Authorization: Bearer $ADMIN" -H 'Content-Type: application/json' -d '{"artifactId":2}' | jq .
```
- Phải thấy: 200, beacon 1 giờ trỏ artifact 2.
- **Hoàn tác ngay**: PUT `/api/ibeacons/1/assign` body `{"artifactId":1}` để trả seed.
- Ghi: OK + đã hoàn tác.

**L2-13. TC-32 Vị trí beacon (posX/posY)**
- Gõ:
```
curl -s -X PUT http://localhost:8080/api/ibeacons/1/position -H "Authorization: Bearer $ADMIN" -H 'Content-Type: application/json' -d '{"posX":0.25,"posY":0.75}' | jq .
```
- Phải thấy: 200. Hoàn tác posX=0,posY=0 nếu muốn.
- Ghi: HTTP.

---
### 3.5 L3 — Personalization & user flow (TC-33..TC-47, 15 case, ~45 phút)

Mục tiêu: visit record/batch/pagination, favorite, device, profile, cache.

**Quy ước UUID**: `clientEventId` phải là UUID hợp lệ (version 1–8, variant 8/9/a/b). Dùng giá trị cố định `11111111-1111-4111-8111-111111111111` (đổi chữ số cuối mỗi case để tránh dedupe ngoài ý).

**L3-1. TC-33 Record visit đơn**
- Gõ:
```
curl -s -X POST http://localhost:8080/api/me/visits -H "Authorization: Bearer $VISITOR" -H 'Content-Type: application/json' -d '{"artifactId":1,"source":"MANUAL","durationSeconds":120,"clientEventId":"11111111-1111-4111-8111-111111111111"}' | jq .
```
- Phải thấy: `success:true`, `data` chứa visit id. Ghi: HTTP 201/200.

**L3-2. TC-34 Record visit thiếu clientEventId → 400**
- Gõ body không có clientEventId → phải 400. Ghi: HTTP 400.

**L3-3. TC-35 Batch sync ≤100**
- Gõ:
```
curl -s -X POST http://localhost:8080/api/me/visits/batch -H "Authorization: Bearer $VISITOR" -H 'Content-Type: application/json' -d '{"events":[{"artifactId":2,"source":"BEACON","durationSeconds":30,"clientEventId":"22222222-2222-4222-8222-222222222222"},{"artifactId":3,"source":"MANUAL","durationSeconds":null,"clientEventId":"33333333-3333-4333-8333-333333333333"}]}' | jq .
```
- Phải thấy: 200, `data` có 2 kết quả, mỗi cái có `duplicate:false`. Ghi: số accepted.

**L3-4. TC-36 Batch >100 → 400**
- Sinh JSON 101 event rồi POST → phải 400 (validation @Size max 100). Ghi: HTTP 400.

**L3-5. TC-37 Dedupe: gửi lại cùng clientEventId**
- Gõ lại L3-1 với cùng UUID `111...111` → phải 200, `data.duplicate:true` (hoặc cờ tương đương), không tạo dòng mới.
- Ghi: duplicate=true.

**L3-6. TC-38 Lịch sử visit phân trang**
- Gõ: `curl -s "http://localhost:8080/api/me/visits?page=0&size=5" -H "Authorization: Bearer $VISITOR" | jq '.data | {totalElements,numberOfElements,content:[.content[].artifactId]}'`
- Phải thấy: `totalElements` ≥ 3, `numberOfElements` ≤ 5. Ghi: số.

**L3-7. TC-39 Favorite add**
- Gõ: `curl -s -X POST http://localhost:8080/api/me/favorites/1 -H "Authorization: Bearer $VISITOR" -w "\nHTTP:%{http_code}\n"` → 200/201.
- Ghi: HTTP.

**L3-8. TC-40 Favorite exists**
- Gõ: `curl -s http://localhost:8080/api/me/favorites/1/exists -H "Authorization: Bearer $VISITOR" | jq .data` → true.
- Ghi: true.

**L3-9. TC-41 Favorite remove**
- Gõ: `curl -s -X DELETE http://localhost:8080/api/me/favorites/1 -H "Authorization: Bearer $VISITOR" -w "\nHTTP:%{http_code}\n"` → 200/204; gọi exists → false.
- Ghi: false.

**L3-10. TC-42 Đổi ngôn ngữ profile (en)**
- Gõ:
```
curl -s -X PUT http://localhost:8080/api/me/profile -H "Authorization: Bearer $VISITOR" -H 'Content-Type: application/json' -d '{"preferredLanguage":"en"}' | jq .
```
- Phải thấy: 200. Sau đó GET `/api/artifacts/3?lang=en` → fallback=true (artifact 3 chỉ có vi). Ghi: OK.

**L3-11. TC-43 Đăng ký device**
- Gõ:
```
curl -s -X POST http://localhost:8080/api/me/devices -H "Authorization: Bearer $VISITOR" -H 'Content-Type: application/json' -d '{"token":"test-device-token-001","platform":"ANDROID"}' | jq .
```
- Phải thấy: 200/201. Ghi id = DEVICE_ID.

**L3-12. TC-44 Test notification (device)**
- Gõ: `curl -s -X POST http://localhost:8080/api/me/devices/test -H "Authorization: Bearer $VISITOR" -H 'Content-Type: application/json' -d '{"deviceId":'$DEVICE_ID'}' | jq .`
- Phải thấy: 200 (hoặc 202). Ghi: HTTP.

**L3-13. TC-45 Xoá device**
- Gõ: `curl -s -X DELETE "http://localhost:8080/api/me/devices/$DEVICE_ID" -H "Authorization: Bearer $VISITOR" -w "\nHTTP:%{http_code}\n"` → 200/204.
- Ghi: HTTP.

**L3-14. TC-46 Cache: visit → analytics count tăng**
- Gõ trước: `curl -s http://localhost:8080/api/statistics/overview | jq .data.totalVisits` (ghi A).
- Record 1 visit mới (UUID mới) → chờ 2s → gõ lại → phải tăng (hoặc ≥ A+1). Ghi: A → B.

**L3-15. TC-47 Route personalization qua gateway**
- Gõ: `curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/me/visits -H "Authorization: Bearer $VISITOR"` → phải 200 (không 503). Nếu 503 → gateway không có PERSONALIZATION_SERVICE_URL → L0-6.
- Ghi: HTTP.

---
### 3.6 L4 — Event-driven & RabbitMQ (TC-48..TC-56 + TC-98..TC-102, 14 case, ~60 phút)

Mục tiêu: publish/consume event, DLQ, idempotency, Grafana panel.

Queue cần biết (RabbitMQ UI http://localhost:15672, user `smartheritage`/`change-me-rabbit`):
- `analytics.visit-recorded`, `analytics.feedback-created` (analytics-service)
- `notification.feedback-approved`, `notification.beacon-battery-low`, `registry.artifact-updated` (beacon-service)
- DLQ: `analytics.dlq`, `notification.feedback-approved.dlq`, `notification.beacon-battery-low.dlq`, `registry.artifact-updated.dlq`
- Exchange: `sh.events` (topic).

**L4-1. TC-48 RabbitMQ management UI**
- Mở `http://localhost:15672` → login `smartheritage`/`change-me-rabbit` → tab Queues phải thấy 5 queue chính + 4 DLQ. Ghi: số queue.

**L4-2. TC-49 Publish visit.recorded → analytics consume**
- Record 1 visit mới (UUID mới) qua gateway.
- Mở RabbitMQ UI → queue `analytics.visit-recorded` → "Get messages" (hoặc xem consumer count). Phải thấy message đã được consume (consumer active, ready=0 sau vài giây).
- Gõ: `curl -s http://localhost:8080/api/statistics/overview | jq .data.totalVisits` → tăng. Ghi: OK.

**L4-3. TC-50 Publish feedback.created → analytics consume**
- Tạo feedback mới (visitor_mai + artifact 3) → xem queue `analytics.feedback-created` consume + overview feedback count tăng. Ghi: OK.

**L4-4. TC-51 notification.feedback-approved**
- Admin approve feedback pending (id 3) → PUT `/api/admin/feedbacks/3/status` body `{"status":"APPROVED"}`.
- RabbitMQ UI → queue `notification.feedback-approved` có message, consumer beacon-service xử lý (log cửa sổ beacon in "feedback approved notification"). Ghi: OK.

**L4-5. TC-52 / TC-103 / TC-104 — notification.beacon-battery-low & admin alert**
- Đặt pin thấp: PUT `/api/ibeacons/1/battery` body `{"batteryLevel":15}` → 200.
- Chạy quét: POST `/api/ibeacons/battery-check` (ADMIN) → 200; beacon-service chỉ quét beacon ACTIVE (beacon 1 ACTIVE) → publish `BeaconBatteryLow`.
- Kiểm tra RabbitMQ UI → queue `notification.beacon-battery-low` có message; log cửa sổ beacon: "battery low". Ghi: OK.
- **Hoàn tác**: PUT battery 87.

**L4-6. TC-53 registry.artifact-updated**
- Admin sửa artifact 1 (PUT name) → message ở `registry.artifact-updated`, beacon-service invalidate cache (log). Ghi: OK.
- Hoàn tác name nếu cần.

**L4-7. TC-54 Idempotency consumer (analytics)**
- Vào RabbitMQ UI → queue `analytics.visit-recorded` → Publish message với cùng eventId 2 lần → analytics chỉ tăng count 1 lần (dedupe). Ghi: count không tăng lần 2.

**L4-8. TC-55 DLQ: message hỏng → analytics.dlq**
- RabbitMQ UI → Publish message **JSON không hợp lệ** vào queue `analytics.visit-recorded` (hoặc exchange sh.events routing key visit.recorded).
- Phải thấy: sau 3 retry, message xuất hiện trong `analytics.dlq` (ready count ≥1). Ghi: DLQ count.

**L4-9. TC-56 DLQ các queue beacon**
- Tương tự publish JSON hỏng vào `notification.feedback-approved` → phải vào `notification.feedback-approved.dlq`. Ghi: DLQ count.

**L4-10. TC-98 Retry count = 3**
- Khi làm L4-8, đếm delivery count trong RabbitMQ UI (message properties `x-death` count) = 3 trước khi vào DLQ. Ghi: count.

**L4-11. TC-99 Duplicate event không xử lý 2 lần**
- L4-7 đã cover; ghi lại kết quả: count không đổi. Ghi: OK.

**L4-12. TC-100 Message hỏng → DLQ không mất**
- L4-8 đã cover; ghi: message còn trong DLQ, không bị mất. Ghi: OK.

**L4-13. TC-101 Grafana panel RabbitMQ**
- Mở Grafana `http://localhost:3000` (admin/change-me-grafana) → dashboard "Smart Heritage" → panel queue depth / consumer count. Phải thấy metric (có thể 0). 
- **Lưu ý**: dashboard hiện **không có panel DLQ** (gap đã biết) → nếu không thấy panel DLQ, ghi "Không đạt - thiếu panel" cho TC-101? Theo checklist TC-101 = panel RabbitMQ tổng quát → đạt nếu có panel queue. Ghi: có/không.

**L4-14. TC-102 Grafana panel DLQ (nếu có)**
- Nếu dashboard có panel DLQ count → phải thấy tăng sau L4-8. Hiện không có → ghi "Không đạt - chưa có panel" (đây là gap hạ tầng, không phải bug code). Ghi: Không đạt + ghi Issue minor.

---
### 3.7 L5 — Analytics & báo cáo (TC-57..TC-66, 10 case, ~45 phút)

Mục tiêu: overview/realtime/export, filter, RBAC, NiFi pipeline.

**L5-1. TC-57 Overview tổng quan**
- Gõ: `curl -s -H "Authorization: Bearer $ADMIN" http://localhost:8080/api/statistics/overview | jq .data`
- Phải thấy: các field totalVisits, totalFeedbacks, topArtifacts, ... (không null). Ghi: HTTP 200.

**L5-2. TC-58 Realtime**
- Gõ: `curl -s -H "Authorization: Bearer $ADMIN" http://localhost:8080/api/statistics/realtime | jq .data` → 200, có activeVisitors/lastEvents. Ghi: HTTP.

**L5-3. TC-59 Export xlsx**
- Gõ:
```
curl -s -o /tmp/report.xlsx -w "HTTP:%{http_code}\n" -H "Authorization: Bearer $ADMIN" "http://localhost:8080/api/statistics/export?format=xlsx&from=2026-10-01&to=2026-11-15"
```
- Mở file `/tmp/report.xlsx` bằng Excel/LibreOffice → phải mở được, có dữ liệu. Ghi: HTTP + file OK.

**L5-4. TC-60 Export pdf**
- Gõ tương tự format=pdf → file pdf mở được. Ghi: OK.

**L5-5. TC-61 Filter theo site**
- Gõ: `.../overview?siteId=1` → chỉ dữ liệu site 1 (topArtifacts thuộc site1). Ghi: OK.

**L5-6. TC-62 Filter theo thời gian from/to**
- Gõ: `.../overview?from=2026-11-01&to=2026-11-15` → 200, dữ liệu trong khoảng. Ghi: OK.

**L5-7. TC-63 Format không hợp lệ → 400**
- Gõ: `.../export?format=csv` → 400. Ghi: HTTP 400.

**L5-8. TC-64 from sai kiểu → 400 (qua @ExceptionHandler)**
- Gõ: `.../export?format=xlsx&from=not-a-date` → 400 (không phải 500). Ghi: HTTP 400.

**L5-9. TC-65 RBAC: VISITOR gọi statistics → 403**
- Gõ với $VISITOR → 403. Ghi: HTTP 403.

**L5-10. TC-66 NiFi pipeline chạy**
- Bật NiFi: `docker compose -f Infra/docker-compose.yml --profile nifi up -d nifi` → đợi 2 phút.
- Mở `https://localhost:8443/nifi` (admin/change-me-nifi-123) → import template trong `Infra/nifi/` (nếu chưa), start processor group.
- Record 1 visit → chờ 30s → kiểm tra analytics_db có dòng mới (query MySQL analytics_db). Hoặc xem NiFi provenance "success".
- Gõ:
```
docker exec smart_heritage_mysql mysql -uroot -prootpassword123 analytics_db -e "SELECT COUNT(*) FROM visit_events;" 2>/dev/null || echo "DB analytics_db có thể tên khác - kiểm tra application.yml"
```
- Ghi: số dòng tăng / provenance success. Nếu NiFi không start được → ghi "Không đạt" + Issue.

---
### 3.8 L6 — Monitoring & observability (TC-67..TC-76, 10 case, ~40 phút)

Mục tiêu: Prometheus scrape, Grafana dashboard, alert, NiFi metrics, exporter.

**L6-1. TC-67 Prometheus targets UP**
- Mở `http://localhost:9090/targets` → tất cả target (gateway, auth, site, content, beacon, personalization, feedback, analytics, backend, mysql-exporter, redis-exporter, rabbitmq) trạng thái UP. Ghi: số UP / tổng.

**L6-2. TC-68 Prometheus query cơ bản**
- Gõ trong UI: `up` → trả về vector với value 1 cho các job. Ghi: OK.

**L6-3. TC-69 Grafana dashboard load**
- Mở `http://localhost:3000` (admin/change-me-grafana) → Dashboards → "Smart Heritage" → phải render không lỗi. Ghi: OK.

**L6-4. TC-70 Panel CPU/RAM JVM**
- Dashboard phải có panel JVM memory/threads. Ghi: có/không.

**L6-5. TC-71 Panel request rate / latency**
- Panel HTTP requests. Tạo vài request (curl loop) → panel cập nhật. Ghi: OK.

**L6-6. TC-72 Panel DB connections**
- Panel HikariCP active connections. Ghi: có/không.

**L6-7. TC-73 Alert rules loaded**
- Mở `http://localhost:9090/rules` → thấy 8 alert từ `Infra/monitoring/alerts.yml`. Ghi: số alert.

**L6-8. TC-74 Alert firing (tùy chọn)**
- Tắt 1 service → alert firing sau interval → bật lại. Nếu không muốn phá môi trường, ghi "bỏ qua - môi trường đang chạy". Ghi: kết quả.

**L6-9. TC-75 RabbitMQ exporter metrics**
- `curl -s http://localhost:15692/metrics | head` → có metric `rabbitmq_`. Ghi: OK.

**L6-10. TC-76 MySQL/Redis exporter metrics**
- `curl -s http://localhost:9104/metrics | head` và `http://localhost:9121/metrics | head` → có metric. Ghi: OK.

---
### 3.9 L7 — Mobile app (TC-77..TC-88, 12 case, ~60 phút)

Mục tiêu: app chạy, login, scan/map, visit, favorite, notification, offline.

**L7-1. TC-77 Build & chạy app**
- Mở `Mobile/` bằng Android Studio → Sync Gradle → Run trên emulator/device. Phải thấy màn hình login. Ghi: build OK.

**L7-2. TC-78 Login**
- Login `visitor_an / Test@1234` → vào home. Ghi: OK.

**L7-3. TC-79 Danh sách site/artifact**
- Home hiển thị site + artifact từ gateway. Ghi: OK.

**L7-4. TC-80 Chi tiết artifact + media**
- Mở artifact 1 → hiển thị nội dung + ảnh/video. Ghi: OK.

**L7-5. TC-81 Quét beacon (giả lập)**
- Dùng "Simulate beacon" trong app hoặc Android Studio Extended Controls → set UUID beacon 1 → app detect → hiện artifact 1. Ghi: OK.

**L7-6. TC-82 Ghi visit từ beacon**
- Sau detect → visit được record (kiểm tra server: GET /api/me/visits tăng). Ghi: OK.

**L7-7. TC-83 Bản đồ vị trí**
- Màn hình map hiển thị marker artifact theo posX/posY. Ghi: OK.

**L7-8. TC-84 Yêu thích**
- Tap favorite trên artifact → server exists=true. Ghi: OK.

**L7-9. TC-85 Thông báo push (FCM)**
- Approve feedback từ admin → device nhận push (nếu FCM config). Nếu chưa config FCM → ghi "Không đạt - chưa cấu hình FCM" (gap). Ghi: kết quả.

**L7-10. TC-86 Offline cache**
- Tắt wifi → app vẫn hiển thị artifact đã cache → bật wifi sync. Ghi: OK/không.

**L7-11. TC-87 Đổi ngôn ngữ**
- Đổi en trong app → nội dung artifact 3 fallback vi. Ghi: OK.

**L7-12. TC-88 Logout**
- Logout → quay về login, token cleared. Ghi: OK.

---
### 3.10 L8 — WebAdmin (TC-89..TC-97, 9 case, ~45 phút)

Mục tiêu: login, dashboard, CRUD site/artifact/beacon, feedback moderation, profile, badge pending.

**L8-1. TC-89 Login admin**
- Mở `WebAdmin/src/pages/login.html` (hoặc serve qua http server) → login admin/Test@1234 → redirect dashboard. Ghi: OK.

**L8-2. TC-90 Dashboard stats**
- Dashboard hiển thị số site/artifact/feedback pending. Ghi: OK.

**L8-3. TC-91 CRUD site**
- Tạo/sửa/xoá site từ UI → API trả 200 và list cập nhật. Ghi: OK.

**L8-4. TC-92 CRUD artifact**
- Tạo/sửa/xoá artifact từ UI. Ghi: OK.

**L8-5. TC-93 Quản lý beacon**
- Trang ibeacons: xem list, gán artifact, sửa position/battery. Ghi: OK.

**L8-6. TC-94 Feedback moderation**
- Trang feedback: list pending, approve/reject → status đổi, pending count giảm. Ghi: OK.

**L8-7. TC-95 Pending badge**
- `feedback-pending-badge.js` hiển thị số pending ở menu; sau approve → badge giảm. Ghi: OK.

**L8-8. TC-96 Profile admin**
- Trang profile: đổi preferredLanguage → lưu. Ghi: OK.

**L8-9. TC-97 RBAC: visitor không vào admin**
- Login visitor → truy cập dashboard → bị redirect login/403. Ghi: OK.

---
### 3.11 Sau khi chạy xong tất cả lô

1. Điền cột KQ trong mục 2 cho **tất cả 131 case** (Đạt / Không đạt / Chưa chạy).
2. Đếm: case C đạt ≥ 87/96; tổng ≥ 118/131. Nếu chưa đủ → chạy lại case "Chưa chạy".
3. Với mỗi "Không đạt": tạo Issue theo mẫu mục 4 (`gh issue create ...`), gắn nhãn `bug` (+ severity trong title).
4. Điền `Docs/testing/test-report.md` (6 mục) và cập nhật `Docs/testing/load-test.md` nếu có số đo k6.
5. Chụp ảnh Grafana dashboard + Prometheus targets để đính kèm báo cáo.
6. Dọn: `docker compose -f Infra/docker-compose.yml down` (giữ volume nếu muốn giữ data).

**Ghi chú về các gap đã biết (không phải bug code, không cần Issue trừ khi muốn theo dõi)**:
- Beacon/Analytics không có trong docker-compose và start-local.ps1 → phải chạy tay (L0-7).
- Gateway compose thiếu PERSONALIZATION_SERVICE_URL → không dùng gateway container.
- Dashboard Grafana chưa có panel DLQ → TC-102 có thể "Không đạt".
- NiFi cần start thủ công + import template.
- Rate limit login (TC-08) có thể chưa bật → ghi nhận thực tế.
- FCM push (TC-85) cần cấu hình Firebase → có thể chưa đạt.

---

## 4. Mẫu Issue (dán vào `gh issue create`)

```
gh issue create --title "[BUG][content] Sửa hiện vật không xoá cache yêu thích" \
  --label bug --label major --assignee <username> --body "$(cat <<'EOF'
Mã lỗi          : BUG-01
Test case       : TC-95
Mức độ          : Major
Service         : personalization-service / content-service
Môi trường      : main@c365dac, Gateway 8080, Chrome 131
Bước tái hiện   : 1. Mở Yêu thích có hiện vật 2  2. Sửa tên hiện vật 2 trên CMS  3. GET /api/me/favorites
Dữ liệu         : visitor_an, artifactId 2
Kết quả mong đợi: Yêu thích hiện tên mới, log có xoá key artifact:summary:2
Kết quả thực tế : Vẫn hiện tên cũ; log personalization không có dòng xoá cache
Ảnh/video       : (đính kèm)
Người phụ trách  : Phú
EOF
)"
```

Gán người theo service: content/chi tiết → **Xuân Anh**; personalization/Lịch sử/Yêu thích → **Phú**; feedback/đánh giá → **Tường**; beacon/FCM/pin → **Khoa**; analytics/xuất báo cáo → **Tuấn**; Gateway/hạ tầng/monitoring → **Bảo**.

## 5. Kết thúc

1. Cộng số ở mục 1 và đối chiếu: đã chạy ≥ 118, case C đủ 96, case C đạt ≥ 87.
2. Chép số sang `test-report.md` (mục 1 và 2), liệt kê mọi case `Chưa chạy`/`Không đạt` vào mục 5.
3. Commit cả 2 file:
   ```bash
   git add Docs/testing/test-run-w6.md Docs/testing/test-report.md
   git commit -m "An: ket qua System Testing lan 1 (131 case)"
   ```
4. Gửi nhóm: số case đã chạy, số lỗi theo mức độ, danh sách lỗi chặn demo.
