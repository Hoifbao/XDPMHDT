# Báo cáo cuối kỳ – Smart Heritage

*Hệ thống du lịch thông minh ứng dụng công nghệ iBeacon hỗ trợ tương tác hiện vật tại các di tích lịch sử và văn hóa*

> **Bản khung** (Sprint 7, XDPMHDT-97). Người tổng hợp: Bảo. Ô có ký hiệu **[Tên – hạn]** là phần người đó gửi; Bảo ghép vào và chỉnh văn phong thống nhất. Hạn gửi phần mô tả service: **T5 19/11 22:00** (task_w7_chi_tiet_v5, mục III). Bản hoàn chỉnh: T7 21/11; nộp: CN 22/11 20:00 (`12_Final_Report.docx`).
>
> Quy ước viết: câu ngắn, có số liệu hoặc bằng chứng (ảnh chụp, link PR, kết quả test); không ghi "đã hoàn thành" nếu chưa có bằng chứng chạy thật.

| Mục | Nội dung |
|---|---|
| Môn học | XD_PMHDT – … (điền tên đầy đủ) |
| Giảng viên hướng dẫn | … |
| Nhóm | Đỗ Lê Hoài Bảo (nhóm trưởng), Đoàn Phương Thiên Ân, Nguyễn Phạm Xuân Anh, Nguyễn Huỳnh Đăng Khoa, Huỳnh Thiên Phú, Võ Duy Tường, Nguyễn Thanh Tuấn |
| Kho mã nguồn | https://github.com/Hoifbao/XDPMHDT (tag `v1.0`) |

## Tóm tắt

Smart Heritage là nền tảng du lịch thông minh cho di tích: điện thoại của du khách tự nhận diện hiện vật gần nhất qua iBeacon (BLE) và hiển thị nội dung đa phương tiện, đa ngôn ngữ, không cần quét QR; ban quản lý dùng Web Admin để quản lý nội dung, thiết bị, người dùng, phản hồi và xem thống kê. Hệ thống gồm ứng dụng Flutter, Web Admin, API Gateway và 7 microservice Spring Boot, MySQL (7 schema), Redis, RabbitMQ, Apache NiFi, Prometheus/Grafana, đóng gói Docker Compose. Kết quả chính: … (điền sau khi có số liệu cuối: số yêu cầu đạt / tổng, k6 lần 2, kiểm thử hồi quy).

## 1. Giới thiệu

### 1.1 Bối cảnh và vấn đề

Nguồn: Đề tài mục 3.2a; [URS mục 2](../requirements/URS.md#2-bối-cảnh). (Bảo viết – đã có nội dung trong URS, rút gọn còn 1 trang.)

### 1.2 Mục tiêu

- Du khách: nhận nội dung hiện vật tự động ≤ 2 giây, đa ngôn ngữ, xem được khi mất mạng.
- Ban quản lý: một nền tảng quản trị thống nhất; số liệu để ra quyết định.
- Kỹ thuật: kiến trúc microservice áp dụng 7 công nghệ của môn học; chịu tải 500 người dùng đồng thời.

### 1.3 Phạm vi

Trong và ngoài phạm vi: [SRS 1.2](../requirements/SRS.md#12-phạm-vi-sản-phẩm).

### 1.4 Cấu trúc báo cáo

…

## 2. Cơ sở lý thuyết và công nghệ

| Mục | Nội dung cần viết (½–1 trang mỗi mục) | Người viết |
|---|---|---|
| 2.1 iBeacon, BLE, RSSI | Gói quảng bá UUID/Major/Minor; ước lượng gần xa bằng RSSI; nhiễu và chống báo trùng | **[Khoa – T5]** |
| 2.2 Kiến trúc microservice, API Gateway | Lý do tách service; Gateway, định tuyến, JWT, RBAC | Bảo (lấy từ [Kiến trúc](../architecture/architecture.md) mục 1, 3, 8) |
| 2.3 Swagger / OpenAPI | Tài liệu hoá API; Swagger chung tại Gateway | **[Tường – T5]** |
| 2.4 Redis | Cache, TTL, chống xử lý trùng | **[Phú – T5]** |
| 2.5 RabbitMQ | Exchange topic, queue, DLQ, khác gì NiFi và Redis | **[Phú – T5]** |
| 2.6 Firebase Cloud Messaging | Token thiết bị, gửi thông báo từ máy chủ | **[Khoa – T5]** |
| 2.7 Apache NiFi | Luồng chép dữ liệu, chống trùng 2 lớp | **[Tuấn – T5]** |
| 2.8 Prometheus, Grafana | Metric, p95, cảnh báo | Bảo |
| 2.9 Flutter, Spring Boot, MySQL, Docker | Vai trò từng công nghệ trong hệ thống | Bảo |

## 3. Phân tích yêu cầu

- 3.1 Tác nhân và user story: [URS mục 3–4](../requirements/URS.md).
- 3.2 Yêu cầu chức năng (bảng rút gọn theo module, mã FR): [SRS mục 3](../requirements/SRS.md#3-yêu-cầu-chức-năng).
- 3.3 Yêu cầu phi chức năng: [SRS mục 4](../requirements/SRS.md#4-yêu-cầu-phi-chức-năng).
- 3.4 Biểu đồ Use Case tổng quát và đặc tả UC-01 → UC-05: [SRS mục 7](../requirements/SRS.md#7-đặc-tả-ca-sử-dụng-chính).

## 4. Thiết kế hệ thống

| Mục | Nội dung | Nguồn | Người |
|---|---|---|---|
| 4.1 Kiến trúc tổng thể | Sơ đồ container, bảng service, quyết định kiến trúc | [Kiến trúc](../architecture/architecture.md) | Bảo |
| 4.2 Luồng xác thực, luồng sự kiện, luồng NiFi | 3 sơ đồ | [Kiến trúc](../architecture/architecture.md) mục 3–4 | Bảo |
| 4.3 Biểu đồ UML | Activity, Sequence, Class, Component, Deployment (bản đã cập nhật) | `04_UML` | Người vẽ tuần 1 – **T4** |
| 4.4 Thiết kế CSDL | ERD, 7 schema, ràng buộc | `05_ERD_Database` | **[Tường – T4]** |
| 4.5 Thiết kế API | Quy ước, bảng API, phân quyền | `06_API_Docs`, [SRS 6.2](../requirements/SRS.md#62-giao-diện-phần-mềm-rest-api-qua-gateway-httpmáy-chủ8080) | **[Tường – T4]** |
| 4.6 Thiết kế giao diện | Wireframe tuần 1 và ảnh bản cuối | `07_UI_Wireframes` | **[Ân – T5]** |

## 5. Hiện thực

Mỗi người viết 1–2 trang theo cùng khung: **(a)** chức năng đã làm (bảng: chức năng – API/màn hình – mã FR); **(b)** điểm kỹ thuật đáng chú ý (thuật toán, xử lý lỗi, chống trùng, cache…); **(c)** ảnh chụp màn hình thật; **(d)** PR chính (số PR, link); **(e)** việc chưa làm và lý do.

| Mục | Phần | Người | Hạn |
|---|---|---|---|
| 5.1 | Nền tảng: Gateway, auth-service, quản lý người dùng, menu Web Admin, trang chủ App, Docker, giám sát | Bảo | T5 |
| 5.2 | site-service, bản đồ Web và Mobile, nearby | **[Ân]** | T5 |
| 5.3 | content-service, đa ngôn ngữ, media, màn hình chi tiết, tối ưu hiệu năng | **[Xuân Anh]** | T5 |
| 5.4 | beacon-service, quét BLE, chống báo trùng, chồng vùng, FCM, cảnh báo pin | **[Khoa]** | T5 |
| 5.5 | personalization-service, yêu thích, lịch sử, offline, Redis | **[Phú]** | T5 |
| 5.6 | feedback-service, kiểm duyệt, CSDL, Swagger | **[Tường]** | T5 |
| 5.7 | analytics-service, NiFi, Dashboard, xuất báo cáo | **[Tuấn]** | T5 |
| 5.8 | Sự kiện RabbitMQ dùng chung (module `common/events`) | Bảo (tổng hợp từ 5.3–5.7) | T6 |

### 5.1 Phần của Bảo (nháp)

| Chức năng | API / màn hình | Mã | Bằng chứng |
|---|---|---|---|
| API Gateway: route tới 7 service + Backend cũ, JWT, RBAC 12 luật, CORS, Swagger chung | `GatewayRoutesConfig`, `AccessRules`, `/swagger-ui.html` | FR-AUTH-04 | `AccessRulesTest`, `GatewayRoutesConfigTest`; E2E thư mục 2_Security |
| Đăng ký, đăng nhập, refresh dùng 1 lần, đăng xuất | `/api/auth/**` | FR-AUTH-01 → 03 | E2E 1_Auth |
| Quản lý người dùng: danh sách, tìm, đổi vai trò, khoá/mở | `/api/admin/users/**`, `users.html` | FR-AUTH-06 | PR … (patch W7 01, 02) – điền sau merge |
| Menu Web Admin chung, trang Tổng quan số liệu thật | `admin-nav.js` | FR-ADM-01 | PR … |
| Trang chủ App bỏ số gõ cứng, chế độ demo | `main.dart`, `DEMO_MODE` | NFR-03 | PR … |
| Docker profile `all` (7 service, healthcheck), `start-local.ps1` 9 cửa sổ | `Infra/docker-compose.yml` | NFR-12 | PR …; biên bản máy sạch (Tuấn) |
| Giám sát: 8 cảnh báo, 2 dashboard, smoke test | `Infra/monitoring` | NFR-10 | promtool test; cảnh báo ServiceDown Pending → Firing (13/11) |
| Kiểm thử tải k6 | `Infra/loadtest/k6-gateway.js` | NFR-02, 05 | Lần 1: 500 VU, p95 1,09 s, 0% lỗi; lần 2: [load-test-w7](../testing/load-test-w7.md) |

## 6. Kiểm thử

| Mục | Nội dung | Nguồn | Người |
|---|---|---|---|
| 6.1 Kế hoạch kiểm thử | Phạm vi, môi trường, tiêu chí | `Docs/testing/test-plan.md` | **[Ân, Tường]** |
| 6.2 Unit test | Số test mỗi service, kết quả `mvn test` | Test Report | **[Tường – T6]** |
| 6.3 Integration test | E2E Newman qua Gateway (77 request/vòng); smoke test giám sát | Lần chốt W6: 3 vòng, 231 request, 369 assertion, 0 lỗi | Bảo |
| 6.4 System test | Lần 1 (tuần 6), hồi quy lần 2 (tuần 7): tổng case, đạt/lỗi | `09_Test_Plan_Report` | **[Ân, Tường – T6]** |
| 6.5 Kiểm thử tải | k6 500 VU, so sánh trước/sau | [load-test-w7](../testing/load-test-w7.md) | Bảo – T5 |
| 6.6 Kiểm thử trên thiết bị thật | iBeacon (chồng vùng, mất sóng, tắt Bluetooth), FCM, rớt mạng | Bảng kết quả từng tình huống | **[Khoa, Phú, Xuân Anh – T5]** |

## 7. Triển khai và vận hành

Tóm tắt từ [Deployment Guide](../deployment/deployment-guide.md): 2 cách chạy, sơ đồ triển khai, giám sát, kế hoạch đưa lên server (tuần 8). Bảo viết; Tuấn bổ sung kết quả chạy thử máy sạch.

## 8. Quản lý dự án

| Mục | Nội dung | Người |
|---|---|---|
| 8.1 Phương pháp | Scrum 8 sprint (1 tuần/sprint); Jira dự án XDPMHDT (Epic theo sprint, Story theo người) | Bảo |
| 8.2 Phân công | Bảng 7 người × module (Task_main) và phần việc thực tế | Bảo |
| 8.3 Quy trình Git | Nhánh theo người/tuần; PR + squash merge; tiêu đề `XDPMHDT-<số>`; ruleset `protect-main`, check `history-guard` | Bảo |
| 8.4 Sự cố và bài học | 31/10–01/11: lịch sử bị nhân đôi khi merge nhánh chứa lịch sử cũ → khôi phục `main`, dựng lại 4 PR, thêm ruleset và check tự động | Bảo |
| 8.5 Tiến độ | Kế hoạch và thực tế theo tuần; việc trễ và cách xử lý | Bảo |

## 9. Đánh giá kết quả

Bảng đối chiếu mọi yêu cầu Đề tài (theo [SRS Phụ lục A](../requirements/SRS.md#phụ-lục-a-ma-trận-truy-vết)) với trạng thái cuối: Đạt / Một phần / Chưa đạt, kèm bằng chứng. Điền sau tag `v1.0` (T6 20/11).

| Nhóm | Số yêu cầu | Đạt | Một phần | Chưa đạt | Ghi chú |
|---|---|---|---|---|---|
| Chức năng (FR) | … | … | … | … | … |
| Phi chức năng (NFR) | 12 | … | … | … | HTTPS: tuần 8 |
| 7 công nghệ môn học | 7 | … | … | … | Swagger, Redis, Firebase, Microservice + Gateway, NiFi, RabbitMQ, Prometheus + Grafana |

## 10. Kết luận và hướng phát triển

- Kết quả đạt được: …
- Hạn chế: … (lấy từ SRS mục 9 và Test Report)
- Hướng phát triển: triển khai server và HTTPS; marker hiện vật trên bản đồ; gợi ý lộ trình bằng AI; AR; định vị trong nhà chính xác hơn.

## Tài liệu tham khảo

1. Đề tài Smart Heritage (đề xuất của nhóm).
2. Apple Inc. *Getting Started with iBeacon*. …
3. Spring Cloud Gateway Reference Documentation. …
4. RabbitMQ Documentation. …
5. Apache NiFi User Guide. …
6. Prometheus Documentation; Grafana Documentation. …
7. Firebase Cloud Messaging Documentation. …
8. IEEE Std 830-1998.

(Mỗi người bổ sung tài liệu đã dùng cho phần của mình; ghi đủ tên, nơi phát hành, link, ngày truy cập.)

## Phụ lục

- A. Danh mục sản phẩm nộp (thư mục `Nop_bai`): 01_URS, 02_SRS, 03_Architecture, 04_UML, 05_ERD_Database, 06_API_Docs (+ 06_OpenAPI), 07_UI_Wireframes, 08_Source_Code, 09_Test_Plan_Report, 10_Deployment_Guide, 11_User_Manual, 12_Final_Report, 12_Slide.
- B. Bảng phân công chi tiết theo tuần (task_w1 → task_w7).
- C. Danh sách PR đã merge (số PR, người, nội dung).
