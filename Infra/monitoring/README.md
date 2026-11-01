# Monitoring – Prometheus & Grafana (Bảo - Sprint 5)

## Chạy

```powershell
# Ở thư mục gốc repo
docker compose -f Infra/docker-compose.yml --profile monitoring up -d
# (tuỳ chọn) chạy cả service trong Docker
docker compose -f Infra/docker-compose.yml --profile core --profile monitoring up -d --build

# Kiểm tra nhanh (chỉ đọc)
powershell -ExecutionPolicy Bypass -File Infra/monitoring/smoke-test.ps1
```

Profile `monitoring` bật 4 container: Prometheus, Grafana, mysql-exporter, redis-exporter.

| Thành phần | Địa chỉ | Ghi chú |
|---|---|---|
| Prometheus | http://localhost:9090 | Status → Targets: xem service nào UP/DOWN |
| Grafana | http://localhost:3000 | Tài khoản `admin`, mật khẩu `GRAFANA_ADMIN_PASSWORD` trong `Infra/.env` (mặc định `change-me-grafana`) |
| mysql-exporter | http://localhost:9104/metrics | Dùng `MYSQL_ROOT_PASSWORD`; `mysql_up 1` là kết nối được |
| redis-exporter | http://localhost:9121/metrics | `redis_up 1` là kết nối được |

## Dashboard (tự nạp, thư mục "Smart Heritage")

| Dashboard | Link | Nội dung |
|---|---|---|
| Smart Heritage – Microservices | http://localhost:3000/d/sh-services | Ô **Service** chọn 1/nhiều service. Trạng thái UP/DOWN, request/giây, tỷ lệ 5xx, p95, top endpoint nhiều/chậm, heap, CPU, thread, GC, HikariCP, log ERROR |
| Smart Heritage – MySQL & Redis | http://localhost:3000/d/sh-datastores | Kết nối, query/giây theo lệnh, slow query, buffer pool; Redis client, RAM, số key, cache hit ratio |

- Biểu đồ không tính request `/actuator/**` (Prometheus tự gọi mỗi 15 giây).
- p95 cần `management.metrics.distribution.percentiles-histogram.http.server.requests: true` trong `application.yml`.
  Đã bật: api-gateway, auth-service, content-service, feedback-service. Service mới tách thì thêm 4 dòng này.
- Sửa dashboard trên giao diện **không** lưu vào file. Muốn giữ: Share → Export → Save to file, chép đè vào
  `grafana/provisioning/dashboards/json/` rồi commit.

## Thêm service mới vào giám sát

1. `pom.xml` của service có `spring-boot-starter-actuator` và `micrometer-registry-prometheus` (service-template có sẵn).
2. `application.yml` mở `management.endpoints.web.exposure.include: health,info,prometheus` và bật histogram như trên.
3. `prometheus.yml`, job `smart-heritage`: thêm target `host.docker.internal:<cổng>` kèm `labels: { service: <tên> }`.
4. Khởi động lại Prometheus: `docker compose -f Infra/docker-compose.yml --profile monitoring restart prometheus`.

## Lỗi thường gặp

| Hiện tượng | Cách xử lý |
|---|---|
| Target service DOWN | Service chưa chạy, hoặc chạy sai cổng. Bình thường nếu chưa bật service đó |
| `mysql_up 0` | Sai `MYSQL_ROOT_PASSWORD` hoặc MySQL chưa sẵn sàng: `docker logs smart_heritage_mysql_exporter` |
| Dashboard trống p95 | Service chưa bật histogram hoặc chưa có request nào: gọi thử 1 API rồi đợi 30 giây |
| Grafana không có dashboard | `docker logs smart_heritage_grafana`, tìm dòng `provisioning` |
