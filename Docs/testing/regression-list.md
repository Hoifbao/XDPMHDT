# Danh sách test case hồi quy – Smart Heritage (Sprint 7)

| Mục | Nội dung |
|---|---|
| Người lập | Nguyễn Văn Ân |
| Ngày lập | 17/11/2026 (cập nhật theo `test-plan.md` sau commit `761248f`) |
| Nguồn | [test-plan.md](test-plan.md) mục 8 – 136 test case (13 nhóm), ưu tiên C / TB / T giữ nguyên |
| Phạm vi | Toàn bộ 136 case; chia 3 mức hồi quy R1 / R2 / R3 |
| Kết quả chạy | Ghi vào [test-run-w6.md](test-run-w6.md) và [test-report.md](test-report.md), **không** ghi vào file này |

File này chỉ là **danh mục chọn case để chạy lại**; nội dung bước, dữ liệu và kết quả mong đợi giữ nguyên theo `test-plan.md` mục 8 để tránh hai bản mô tả lệch nhau.

## 1. Ba mức hồi quy

| Mức | Số case | Khi nào chạy | Thời gian ước tính |
|---|---|---|---|
| **R1 – Smoke** | 20 | Mỗi lần merge vào `main`, mỗi buổi trước demo, sau khi `down -v` dựng lại DB | ~40 phút |
| **R2 – Theo vùng** | 82 | Khi PR chạm vào vùng ảnh hưởng ở mục 2 (case ưu tiên Cao còn lại) | ~4 giờ |
| **R3 – Đầy đủ** | 34 | Cuối sprint, trước khi đóng bản chốt (case ưu tiên TB/T) | ~2 giờ |

- R1 chọn theo đường đi chính của demo: đăng nhập → chọn hiện vật → nghe/xem nội dung → yêu thích → ghi lượt → duyệt đánh giá → quản lý người dùng → dashboard. Một case R1 hỏng là **chặn demo**, xử lý trước mọi case khác.
- R2 gồm các case ưu tiên Cao không nằm trong R1.
- R3 gồm case ưu tiên Trung bình và Thấp; chạy khi đã xanh R1 + R2.
- Case đang `Chặn` vì thiếu thiết bị (máy Android thật + Beacon Simulator) vẫn giữ trong danh mục: chạy được ngay khi có thiết bị, không cần sửa tiêu chí.

## 2. Chọn case theo vùng ảnh hưởng

| Vùng | Số case | Chạy lại khi thay đổi | Case |
|---|---|---|---|
| A1. Visitor – API/app | 21 | auth-service, personalization-service, feedback-service, content-service qua Gateway | TC-01, TC-02, TC-03, TC-04, TC-05, TC-06, TC-07, TC-08, TC-09, TC-10, TC-11, TC-12, TC-13, TC-14, TC-15, TC-16, TC-17, TC-18, TC-19, TC-20, TC-21 |
| A2. Administrator – Web Admin | 16 | WebAdmin/src/pages (heritage-sites, artifacts, ibeacons, feedback, dashboard) | TC-22, TC-23, TC-24, TC-25, TC-26, TC-27, TC-28, TC-29, TC-30, TC-31, TC-32, TC-33, TC-34, TC-35, TC-36, TC-37 |
| S1. System – nền tảng | 13 | Gateway AccessRules/routes, JWT, schema DB, NiFi, Prometheus | TC-38, TC-39, TC-40, TC-41, TC-42, TC-43, TC-44, TC-45, TC-46, TC-47, TC-48, TC-49, TC-50 |
| A3. Mobile – luồng du khách | 19 | Interface/lib/features (map, beacon, artifact detail, favorites, visit_history, feedback) | TC-51, TC-52, TC-53, TC-54, TC-55, TC-56, TC-57, TC-58, TC-59, TC-60, TC-61, TC-62, TC-63, TC-64, TC-65, TC-66, TC-67, TC-68, TC-69 |
| A4. Mobile/Web – tuần 5 | 5 | feedback.html, ibeacons.html, dashboard.js, bảng languages, upload media | TC-70, TC-71, TC-72, TC-73, TC-74 |
| S2. System – tuần 5 | 11 | Gateway route tuần 5, Prometheus/Grafana, NiFi, Redis cache, docker compose | TC-75, TC-76, TC-77, TC-78, TC-79, TC-80, TC-81, TC-82, TC-83, TC-84, TC-85 |
| D1. Xuất báo cáo | 4 | analytics-service (StatisticsController, export xlsx/pdf) | TC-86, TC-108, TC-109, TC-110 |
| E1. Sự kiện RabbitMQ | 14 | publisher/consumer + DLQ của personalization, feedback, beacon, content | TC-87, TC-88, TC-89, TC-90, TC-91, TC-92, TC-93, TC-94, TC-95, TC-96, TC-97, TC-98, TC-99, TC-100 |
| F1. Thông báo, pin, thời gian thực | 7 | beacon-service notification, FCM, NiFi agg_daily_views, analytics realtime | TC-101, TC-102, TC-103, TC-104, TC-105, TC-106, TC-107 |
| G1. Hoàn thiện Mobile/Web | 9 | màn Lịch sử, Yêu thích, Đánh giá của tôi; badge chờ duyệt trên WebAdmin | TC-111, TC-112, TC-113, TC-114, TC-115, TC-116, TC-117, TC-118, TC-119 |
| H1. Giám sát, tải | 5 | Prometheus/Grafana, alert rules, k6 load test | TC-120, TC-121, TC-122, TC-123, TC-124 |
| I1. Đặc biệt, bản chốt | 7 | kịch bản mạng/BLE, tắt service, phân quyền, smoke bản chốt | TC-125, TC-126, TC-127, TC-128, TC-129, TC-130, TC-131 |
| J1. Quản lý người dùng (tuần 7) | 5 | auth-service AdminUserController, cột `users.locked`, WebAdmin users.html/users.js | TC-132, TC-133, TC-134, TC-135, TC-136 |

## 3. Danh sách chi tiết

| Mã | Ưu tiên | Mức | Tiêu đề | Vùng |
|---|---|---|---|---|
| TC-01 | C | R2 | Đăng ký tài khoản mới | A1. Visitor – API/app |
| TC-02 | C | R1 | Đăng nhập đúng | A1. Visitor – API/app |
| TC-03 | C | R2 | Đăng nhập sai mật khẩu | A1. Visitor – API/app |
| TC-04 | TB | R3 | Làm mới token và đăng xuất | A1. Visitor – API/app |
| TC-05 | TB | R3 | Xem hồ sơ của mình | A1. Visitor – API/app |
| TC-06 | C | R2 | Không xem được hồ sơ người khác | A1. Visitor – API/app |
| TC-07 | C | R2 | Di tích gần nhất sắp xếp gần → xa | A1. Visitor – API/app |
| TC-08 | TB | R3 | Nearby với tham số sai | A1. Visitor – API/app |
| TC-09 | TB | R3 | Bản đồ di tích trên app | A1. Visitor – API/app |
| TC-10 | C | R2 | Quét beacon trên Android | A1. Visitor – API/app |
| TC-11 | C | R2 | Từ chối quyền hoặc tắt Bluetooth | A1. Visitor – API/app |
| TC-12 | C | R1 | Chi tiết hiện vật tiếng Anh | A1. Visitor – API/app |
| TC-13 | C | R1 | Lấy hiện vật theo beacon | A1. Visitor – API/app |
| TC-14 | TB | R3 | Xem lại nội dung khi rời vùng beacon | A1. Visitor – API/app |
| TC-15 | C | R1 | Thêm và bỏ yêu thích | A1. Visitor – API/app |
| TC-16 | C | R1 | Xem lịch sử tham quan | A1. Visitor – API/app |
| TC-17 | C | R2 | Gửi đánh giá hợp lệ | A1. Visitor – API/app |
| TC-18 | C | R2 | Đánh giá với số sao sai | A1. Visitor – API/app |
| TC-19 | TB | R3 | Bình luận quá dài | A1. Visitor – API/app |
| TC-20 | TB | R3 | Đánh giá hiện vật không tồn tại | A1. Visitor – API/app |
| TC-21 | C | R1 | Xem đánh giá đã duyệt | A1. Visitor – API/app |
| TC-22 | C | R2 | Đăng nhập Web Admin | A2. Administrator – Web Admin |
| TC-23 | C | R2 | Visitor không vào được trang quản trị | A2. Administrator – Web Admin |
| TC-24 | C | R1 | Thêm di tích bằng bản đồ | A2. Administrator – Web Admin |
| TC-25 | TB | R3 | Sửa, xóa di tích | A2. Administrator – Web Admin |
| TC-26 | C | R2 | Tạo hiện vật với siteId không tồn tại | A2. Administrator – Web Admin |
| TC-27 | C | R2 | CRUD hiện vật trên CMS | A2. Administrator – Web Admin |
| TC-28 | TB | R3 | Gắn link media | A2. Administrator – Web Admin |
| TC-29 | C | R2 | Nhập bản dịch tiếng Anh | A2. Administrator – Web Admin |
| TC-30 | TB | R3 | Đăng ký beacon mới | A2. Administrator – Web Admin |
| TC-31 | C | R2 | Gán beacon cho hiện vật cùng di tích | A2. Administrator – Web Admin |
| TC-32 | C | R2 | Gán beacon cho hiện vật khác di tích | A2. Administrator – Web Admin |
| TC-33 | T | R3 | Danh sách beacon có trạng thái và pin | A2. Administrator – Web Admin |
| TC-34 | C | R2 | Danh sách đánh giá, lọc trạng thái và hiện vật | A2. Administrator – Web Admin |
| TC-35 | C | R2 | Duyệt / ẩn đánh giá | A2. Administrator – Web Admin |
| TC-36 | C | R2 | Điểm trung bình hiện vật | A2. Administrator – Web Admin |
| TC-37 | TB | R1 | Dashboard thống kê | A2. Administrator – Web Admin |
| TC-38 | C | R2 | Ghi dữ liệu không có token | S1. System – nền tảng |
| TC-39 | C | R2 | Visitor gọi API của ADMIN | S1. System – nền tảng |
| TC-40 | C | R2 | Token hết hạn hoặc sửa chữ ký | S1. System – nền tảng |
| TC-41 | C | R2 | Chống giả mạo header X-User-* | S1. System – nền tảng |
| TC-42 | C | R2 | Fallback ngôn ngữ | S1. System – nền tảng |
| TC-43 | C | R2 | Đồng bộ offline không ghi trùng | S1. System – nền tảng |
| TC-44 | C | R2 | Mỗi service một schema | S1. System – nền tảng |
| TC-45 | TB | R3 | Id dữ liệu thống nhất giữa các schema | S1. System – nền tảng |
| TC-46 | C | R2 | Gọi service phụ thuộc khi nó bị tắt | S1. System – nền tảng |
| TC-47 | C | R1 | Định tuyến Gateway | S1. System – nền tảng |
| TC-48 | TB | R3 | Giám sát | S1. System – nền tảng |
| TC-49 | TB | R3 | NiFi chuyển visits sang analytics_db | S1. System – nền tảng |
| TC-50 | TB | R3 | Không thông báo lặp, thông báo khi sang beacon khác | S1. System – nền tảng |
| TC-51 | C | R2 | Bản đồ hiện di tích gần nhất (Ân) | A3. Mobile – luồng du khách |
| TC-52 | C | R2 | Chỉ đường (Ân) | A3. Mobile – luồng du khách |
| TC-53 | TB | R3 | Từ chối quyền vị trí, mất mạng (Ân) | A3. Mobile – luồng du khách |
| TC-54 | C | R1 | Đổi ngôn ngữ trên màn chi tiết (Xuân Anh) | A3. Mobile – luồng du khách |
| TC-55 | C | R2 | Ảnh, audio, video (Xuân Anh) | A3. Mobile – luồng du khách |
| TC-56 | C | R2 | Rời vùng beacon vẫn xem tiếp (Xuân Anh, Khoa) | A3. Mobile – luồng du khách |
| TC-57 | TB | R3 | Mở lại hiện vật đã xem khi mất mạng (Xuân Anh) | A3. Mobile – luồng du khách |
| TC-58 | C | R2 | Tự hiện hiện vật ≤ 2 giây (Khoa) | A3. Mobile – luồng du khách |
| TC-59 | C | R2 | Không báo trùng (Khoa) | A3. Mobile – luồng du khách |
| TC-60 | C | R2 | Đi từ beacon A sang B (Khoa) | A3. Mobile – luồng du khách |
| TC-61 | C | R1 | Nút "Giả lập phát hiện beacon" (Khoa) | A3. Mobile – luồng du khách |
| TC-62 | C | R1 | Ghi lượt tham quan khi ra vùng (Khoa, Phú) | A3. Mobile – luồng du khách |
| TC-63 | C | R2 | Hàng đợi ngoại tuyến (Phú) | A3. Mobile – luồng du khách |
| TC-64 | C | R1 | Yêu thích, Lịch sử trên app qua Gateway (Phú) | A3. Mobile – luồng du khách |
| TC-65 | C | R2 | Gửi đánh giá từ app (Tường) | A3. Mobile – luồng du khách |
| TC-66 | C | R2 | Chưa đăng nhập thì yêu cầu đăng nhập (Tường) | A3. Mobile – luồng du khách |
| TC-67 | C | R2 | Chặn spam: gửi lại = sửa đánh giá cũ (Tường) | A3. Mobile – luồng du khách |
| TC-68 | C | R2 | Điểm TB và bình luận trên app khớp dữ liệu (Tường) | A3. Mobile – luồng du khách |
| TC-69 | TB | R3 | Thông báo FCM (Khoa) | A3. Mobile – luồng du khách |
| TC-70 | C | R1 | Duyệt / ẩn đánh giá trên Web (Tường) | A4. Mobile/Web – tuần 5 |
| TC-71 | C | R2 | Bản đồ vị trí beacon (Ân, Khoa) | A4. Mobile/Web – tuần 5 |
| TC-72 | C | R1 | Dashboard số liệu thật (Tuấn) | A4. Mobile/Web – tuần 5 |
| TC-73 | T | R3 | Quản lý danh sách ngôn ngữ (Xuân Anh, P1) | A4. Mobile/Web – tuần 5 |
| TC-74 | T | R3 | Upload file media (Xuân Anh, P2) | A4. Mobile/Web – tuần 5 |
| TC-75 | C | R1 | Bảng route Gateway tuần 5 (Bảo) | S2. System – tuần 5 |
| TC-76 | C | R2 | Phân quyền route mới | S2. System – tuần 5 |
| TC-77 | TB | R3 | Swagger gom tại Gateway (Bảo, Tường rà) | S2. System – tuần 5 |
| TC-78 | TB | R3 | Giám sát Prometheus / Grafana (Bảo) | S2. System – tuần 5 |
| TC-79 | C | R2 | NiFi feedbacks → fact_feedbacks (Tuấn) | S2. System – tuần 5 |
| TC-80 | C | R2 | NiFi danh mục (Tuấn) | S2. System – tuần 5 |
| TC-81 | C | R2 | Lượt tham quan mới lên Dashboard (Tuấn, Bảo – kịch bản demo) | S2. System – tuần 5 |
| TC-82 | T | R3 | Redis cache chi tiết hiện vật (Phú) | S2. System – tuần 5 |
| TC-83 | TB | R3 | Chặn spam ở mức database (Tường) | S2. System – tuần 5 |
| TC-84 | TB | R3 | feedback-service trong docker compose (Tường) | S2. System – tuần 5 |
| TC-85 | C | R1 | Smoke test toàn hệ thống (Bảo) | S2. System – tuần 5 |
| TC-86 | C | R2 | Xuất báo cáo thống kê PDF/Excel (Tuấn, tuần 6) | D1. Xuất báo cáo |
| TC-108 | C | R2 | File Excel đủ sheet (Tuấn) | D1. Xuất báo cáo |
| TC-109 | C | R2 | File PDF đúng tiếng Việt (Tuấn) | D1. Xuất báo cáo |
| TC-110 | C | R2 | Phân quyền xuất báo cáo (Tuấn) | D1. Xuất báo cáo |
| TC-87 | C | R2 | VisitRecorded khi ghi 1 lượt (Phú) | E1. Sự kiện RabbitMQ |
| TC-88 | C | R2 | VisitRecorded khi gửi theo lô (Phú) | E1. Sự kiện RabbitMQ |
| TC-89 | C | R2 | FeedbackCreated khi đánh giá mới (Tường) | E1. Sự kiện RabbitMQ |
| TC-90 | C | R2 | Gửi lại không phát FeedbackCreated (Tường) | E1. Sự kiện RabbitMQ |
| TC-91 | C | R2 | FeedbackApproved khi Admin duyệt (Tường) | E1. Sự kiện RabbitMQ |
| TC-92 | C | R2 | Không báo lặp khi duyệt lại hoặc ẩn (Tường) | E1. Sự kiện RabbitMQ |
| TC-93 | C | R2 | RabbitMQ tắt không ảnh hưởng đánh giá (Tường) | E1. Sự kiện RabbitMQ |
| TC-94 | C | R2 | ArtifactUpdated khi đổi hiện vật (Xuân Anh) | E1. Sự kiện RabbitMQ |
| TC-95 | C | R2 | Sửa hiện vật thì xóa cache (Phú, Xuân Anh) | E1. Sự kiện RabbitMQ |
| TC-96 | C | R2 | Đổi beacon thì registry mới (Xuân Anh, Khoa) | E1. Sự kiện RabbitMQ |
| TC-97 | TB | R3 | Không gửi sự kiện khi lưu DB thất bại (Tường, các bên gửi) | E1. Sự kiện RabbitMQ |
| TC-98 | C | R2 | Sự kiện đúng khung chung (Bảo) | E1. Sự kiện RabbitMQ |
| TC-99 | C | R2 | Bên nhận chống xử lý trùng (Tuấn, Khoa, Phú) | E1. Sự kiện RabbitMQ |
| TC-100 | TB | R3 | Lỗi quá 3 lần vào DLQ (Bảo, các bên nhận) | E1. Sự kiện RabbitMQ |
| TC-101 | C | R2 | Du khách nhận thông báo khi được duyệt (Tường, Khoa) | F1. Thông báo, pin, thời gian thực |
| TC-102 | T | R3 | Admin được báo có đánh giá mới (Khoa, P1) | F1. Thông báo, pin, thời gian thực |
| TC-103 | C | R2 | Pin thấp phát BeaconBatteryLow (Khoa) | F1. Thông báo, pin, thời gian thực |
| TC-104 | C | R2 | Admin nhận cảnh báo pin (Khoa) | F1. Thông báo, pin, thời gian thực |
| TC-105 | C | R2 | Dashboard đổi khi có lượt tham quan mới (Tuấn) | F1. Thông báo, pin, thời gian thực |
| TC-106 | TB | R3 | Dashboard đếm đánh giá mới (Tuấn) | F1. Thông báo, pin, thời gian thực |
| TC-107 | TB | R3 | NiFi tính bảng tổng hợp theo ngày (Tuấn) | F1. Thông báo, pin, thời gian thực |
| TC-111 | C | R2 | Màn Lịch sử (Phú) | G1. Hoàn thiện Mobile/Web |
| TC-112 | C | R2 | Màn Yêu thích (Phú) | G1. Hoàn thiện Mobile/Web |
| TC-113 | TB | R3 | Báo đang ngoại tuyến (Phú) | G1. Hoàn thiện Mobile/Web |
| TC-114 | C | R2 | Sau khi gửi hiện chờ duyệt (Tường) | G1. Hoàn thiện Mobile/Web |
| TC-115 | C | R2 | Màn "Đánh giá của tôi" (Tường) | G1. Hoàn thiện Mobile/Web |
| TC-116 | C | R2 | API đánh giá của tôi (Tường) | G1. Hoàn thiện Mobile/Web |
| TC-117 | C | R2 | Số chờ duyệt trên menu Web (Tường) | G1. Hoàn thiện Mobile/Web |
| TC-118 | C | R2 | Phân quyền số chờ duyệt (Tường) | G1. Hoàn thiện Mobile/Web |
| TC-119 | C | R2 | Chi tiết hiện vật không vỡ giao diện (Xuân Anh) | G1. Hoàn thiện Mobile/Web |
| TC-120 | TB | R3 | RabbitMQ trong Prometheus (Bảo) | H1. Giám sát, tải |
| TC-121 | C | R2 | Tắt service thì cảnh báo bật (Bảo) | H1. Giám sát, tải |
| TC-122 | TB | R3 | Các quy tắc cảnh báo khác (Bảo) | H1. Giám sát, tải |
| TC-123 | C | R2 | Load test 500 người dùng ảo (Bảo) | H1. Giám sát, tải |
| TC-124 | C | R2 | Demo Monitoring 5 phút (Bảo) | H1. Giám sát, tải |
| TC-125 | C | R2 | Mất mạng giữa luồng | I1. Đặc biệt, bản chốt |
| TC-126 | C | R2 | Tắt Bluetooth khi đang quét | I1. Đặc biệt, bản chốt |
| TC-127 | C | R2 | Từ chối quyền | I1. Đặc biệt, bản chốt |
| TC-128 | TB | R3 | 2 beacon gần nhau | I1. Đặc biệt, bản chốt |
| TC-129 | C | R2 | Tắt 1 service | I1. Đặc biệt, bản chốt |
| TC-130 | C | R2 | VISITOR vào trang quản trị | I1. Đặc biệt, bản chốt |
| TC-131 | C | R1 | Smoke test bản chốt code (Ân, Bảo) | I1. Đặc biệt, bản chốt |
| TC-132 | C | R2 | Cột trạng thái có sẵn khi tạo DB mới | J1. Quản lý người dùng (tuần 7) |
| TC-133 | C | R1 | Danh sách và tìm kiếm người dùng | J1. Quản lý người dùng (tuần 7) |
| TC-134 | C | R1 | Khoá và mở khoá tài khoản | J1. Quản lý người dùng (tuần 7) |
| TC-135 | C | R2 | Đổi vai trò và chặn tự đổi/tự khoá | J1. Quản lý người dùng (tuần 7) |
| TC-136 | C | R2 | VISITOR không gọi được API quản lý người dùng | J1. Quản lý người dùng (tuần 7) |

## 4. Checklist R1 trước demo

Đánh dấu theo thứ tự dưới đây; dừng ở case đầu tiên không đạt để sửa trước khi demo.

- [ ] 1. TC-02 – Đăng nhập đúng
- [ ] 2. TC-12 – Chi tiết hiện vật tiếng Anh
- [ ] 3. TC-13 – Lấy hiện vật theo beacon
- [ ] 4. TC-54 – Đổi ngôn ngữ trên màn chi tiết (Xuân Anh)
- [ ] 5. TC-61 – Nút "Giả lập phát hiện beacon" (Khoa)
- [ ] 6. TC-62 – Ghi lượt tham quan khi ra vùng (Khoa, Phú)
- [ ] 7. TC-15 – Thêm và bỏ yêu thích
- [ ] 8. TC-16 – Xem lịch sử tham quan
- [ ] 9. TC-21 – Xem đánh giá đã duyệt
- [ ] 10. TC-64 – Yêu thích, Lịch sử trên app qua Gateway (Phú)
- [ ] 11. TC-24 – Thêm di tích bằng bản đồ
- [ ] 12. TC-70 – Duyệt / ẩn đánh giá trên Web (Tường)
- [ ] 13. TC-133 – Danh sách và tìm kiếm người dùng
- [ ] 14. TC-134 – Khoá và mở khoá tài khoản
- [ ] 15. TC-37 – Dashboard thống kê
- [ ] 16. TC-72 – Dashboard số liệu thật (Tuấn)
- [ ] 17. TC-47 – Định tuyến Gateway
- [ ] 18. TC-75 – Bảng route Gateway tuần 5 (Bảo)
- [ ] 19. TC-85 – Smoke test toàn hệ thống (Bảo)
- [ ] 20. TC-131 – Smoke test bản chốt code (Ân, Bảo)

## 5. Điều kiện môi trường tối thiểu để hồi quy

| Thành phần | Bắt buộc cho | Ghi chú |
|---|---|---|
| MySQL + Redis (docker compose) | tất cả | MySQL healthy trước khi chạy; dữ liệu mẫu theo test-plan mục 3; DB mới phải có cột `users.locked` (TC-132) |
| RabbitMQ (`--profile mq`) | R2 nhóm E1, F1 | Thiếu RabbitMQ thì case sự kiện không kết luận được |
| Gateway 8080 + 7 service | tất cả | `docker-compose.yml` từ `533e64c` đã đủ 7 service + healthcheck; nếu chạy bằng maven thì phải set `BEACON_SERVICE_URL`, `ANALYTICS_SERVICE_URL`, `PERSONALIZATION_SERVICE_URL`, `FEEDBACK_SERVICE_URL` trước khi bật Gateway, nếu không `/api/ibeacons/**` và `/api/statistics/**` rơi về Backend cũ và trả 404 |
| NiFi (`--profile nifi`) | TC-49, TC-79, TC-80, TC-81, TC-107 | Cần import flow và bật controller service trước khi chạy |
| Prometheus + Grafana (`--profile monitoring`) | H1 | Cảnh báo chỉ bật sau khoảng 80–100 giây |
| Web Admin serve tĩnh | A2, A4, J1 | `python -m http.server 5500` tại thư mục `WebAdmin` |
| Android thật + Beacon Simulator | A3, A4 (case quét beacon) | Emulator **không** quét được BLE; dùng nút "Giả lập phát hiện beacon" trong app cho luồng không cần BLE |

## 6. Đường chạy nhanh bằng script có sẵn

- `Docs/testing/integration-smoke.ps1 -Events` – 12 check tích hợp qua Gateway (dùng trước khi chạy tay R1).
- `Docs/monitoring/smoke-test.ps1` – kiểm tra hạ tầng/monitoring.
- `Infra/dev/start-local.ps1` – mở 9 cửa sổ service (từ `533e64c` đã có beacon + analytics).

## 7. Quy ước ghi kết quả

- 4 giá trị: `Đạt` · `Không đạt` · `Chưa chạy` · `Chặn` (như `test-run-w6.md` mục 0).
- `Không đạt` phải kèm mã Issue; mô tả lỗi theo mẫu `test-plan.md` mục 7.
- Case hồi quy đạt **không** cần chụp ảnh; case không đạt phải có ảnh/log trong Issue.
- Sau mỗi vòng, cập nhật lại mục 3 (cột Mức) nếu có case đổi ưu tiên hoặc bị gỡ khỏi phạm vi.
