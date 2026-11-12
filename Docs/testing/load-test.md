# Báo cáo kiểm thử tải (k6) – lần 1

Sprint 6 · XDPMHDT-41 · Người thực hiện: Bảo · Kịch bản: [`Infra/loadtest/k6-gateway.js`](../../Infra/loadtest/k6-gateway.js)

> Trạng thái: **chưa có số đo**. Các ô `…` điền từ kết quả chạy thật; không ước lượng, không lấy số máy khác.

## 1. Mục tiêu

Đo độ trễ và tỉ lệ lỗi của hệ thống khi có nhiều du khách dùng đồng thời, đối chiếu yêu cầu phi chức năng:

| Yêu cầu | Ngưỡng |
|---|---|
| Số người dùng đồng thời | 500 |
| Độ trễ p95 | ≤ 2 giây |
| Tỉ lệ lỗi | < 1% |

## 2. Kịch bản

Mọi request đi qua **API Gateway (cổng 8080)**, giống app Mobile/Web thật. Mỗi người dùng ảo (VU) lặp lại 1 thao tác rồi nghỉ 1–3 giây:

| Thao tác | API | Tỉ lệ | Service phía sau |
|---|---|---|---|
| Xem chi tiết hiện vật | `GET /api/artifacts/{id}` (id 1001–1025) | 50% | content-service |
| Tìm di tích gần | `GET /api/heritage-sites/nearby?lat=&lng=&radius=5` | 25% | site-service |
| Tải registry beacon | `GET /api/ibeacons/registry` | 20% | beacon-service |
| Đăng nhập | `POST /api/auth/login` | 5% | auth-service |

Các giai đoạn tải (khoảng 9 phút):

| Giai đoạn | Thời gian | Số VU |
|---|---|---|
| Khởi động | 1 phút | 0 → 50 |
| Tăng dần | 2 phút | 50 → 200 |
| Tăng tới tối đa | 2 phút | 200 → 500 |
| **Giữ tải tối đa** | 3 phút | 500 |
| Hạ tải | 1 phút | 500 → 0 |

Chỉ gọi API đọc và đăng nhập, không làm thay đổi dữ liệu.

## 3. Cách chạy

Chuẩn bị: bật đủ MySQL, Redis, Gateway và auth, site, content, beacon-service; nạp dữ liệu `Seed_SmartHeritage_v1.sql` (hiện vật id 1001–1025); bật profile `monitoring` để xem Grafana trong lúc chạy.

```powershell
# Ở thư mục gốc repo. Thử nhanh 30 giây, 5 VU:
docker run --rm -i -e PROFILE=smoke -v ${PWD}/Infra/loadtest:/scripts grafana/k6:0.54.0 run /scripts/k6-gateway.js
# Chạy đủ tới 500 VU:
docker run --rm -i -v ${PWD}/Infra/loadtest:/scripts grafana/k6:0.54.0 run /scripts/k6-gateway.js
# Máy không chịu nổi 500 VU: hạ mức tối đa, ghi rõ vào mục 6
docker run --rm -i -e MAX_VUS=200 -v ${PWD}/Infra/loadtest:/scripts grafana/k6:0.54.0 run /scripts/k6-gateway.js
```

Kịch bản tự kiểm tra 4 API trả 200 trước khi tạo tải; sai thì dừng và báo API nào lỗi.

## 4. Môi trường đo

| Mục | Giá trị |
|---|---|
| Ngày chạy | … |
| Máy | CPU …, RAM …, Windows … |
| Cách chạy service | `mvn spring-boot:run` / Docker profile core … |
| Phiên bản | commit `…` trên `main` |
| Ghi chú | k6 chạy trên cùng máy với các service (k6 cũng dùng CPU) |

## 5. Kết quả

Số VU tối đa đạt được: **…**   ·   Tổng request: **…**   ·   Thông lượng: **… request/giây**

| Chỉ số | Toàn bộ | Chi tiết hiện vật | Di tích gần | Registry | Đăng nhập | Yêu cầu | Đạt? |
|---|---|---|---|---|---|---|---|
| p95 | … | … | … | … | … | ≤ 2 giây | … |
| p99 | … | … | … | … | … | – | – |
| Trung bình | … | … | … | … | … | – | – |
| Tỉ lệ lỗi (`http_req_failed`) | … | | | | | < 1% | … |
| Checks đạt | … | | | | | > 99% | … |

Quan sát trên Grafana (dashboard *Smart Heritage – Microservices*) lúc giữ 500 VU:

| Quan sát | Ghi nhận |
|---|---|
| Service có p95 cao nhất | … |
| CPU / heap cao nhất | … |
| HikariCP (kết nối DB) có lúc chờ không | … |
| Cảnh báo nào bật trên http://localhost:9090/alerts | … |

## 6. Kết luận và giới hạn

- Kết luận: … (đạt / chưa đạt yêu cầu p95 ≤ 2 giây ở … VU)
- Điểm nghẽn chính: …
- Giới hạn của lần đo: toàn bộ hệ thống và k6 chạy chung 1 máy cá nhân, nên số đo thấp hơn môi trường triển khai thật. Nếu máy không chạy được 500 VU, chỉ ghi số đo ở mức đạt được, không suy ra kết quả 500 VU.
- Việc tiếp theo: …
