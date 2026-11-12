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

Smoke test mục 5 kiểm Swagger chung tại Gateway: mỗi mục tải được tài liệu và `servers` trỏ về Gateway. Service chưa chạy chỉ cảnh báo; thêm tên vào `-RequireServices` (vd `-RequireServices api-gateway,auth-service`) để bắt buộc.

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

## Cảnh báo (alert rule)

File `alerts.yml`, Prometheus nạp qua `rule_files` trong `prometheus.yml`. Xem tại http://localhost:9090/alerts
(trạng thái **Inactive → Pending → Firing**). Sửa `alerts.yml` xong: `docker compose -f Infra/docker-compose.yml --profile monitoring restart prometheus`.

Ngưỡng cập nhật Sprint 6 (XDPMHDT-40).

| Cảnh báo | Điều kiện | Chờ (`for`) | Mức |
|---|---|---|---|
| ServiceDown | Service **đang chạy** (UP trong 10 phút gần nhất) rồi không đọc được metric. Service chưa bật từ đầu không báo | 1 phút | critical |
| MySQLDown / RedisDown | `mysql_up == 0` / `redis_up == 0` | 30 giây | critical |
| HighErrorRate5xx | Lỗi 5xx > 5% tính trên 5 phút, khi có > 0.2 request/giây | 1 phút | warning |
| HighLatencyP95 | p95 > 2 giây tính trên 5 phút (yêu cầu p95 ≤ 2 giây) | 1 phút | warning |
| RabbitMQDown | RabbitMQ đang chạy (UP trong 10 phút) rồi không đọc được số liệu | 1 phút | critical |
| RabbitMQQueueBacklog | Một hàng đợi thường (không phải DLQ) có > 100 tin chờ xử lý | 2 phút | warning |
| RabbitMQDeadLetter | Một hàng đợi DLQ (tên chứa `dlq`) có tin | 30 giây | warning |

Tắt 1 service → `ServiceDown` chuyển Firing sau khoảng 1 phút 15 – 1 phút 30 giây (scrape 15 giây + chờ 1 phút); bật lại → tắt ở lần scrape kế tiếp.
`RabbitMQDeadLetter` giữ Firing tới khi xoá tin trong DLQ: RabbitMQ UI (http://localhost:15672) → Queues → `<tên>.dlq` → **Purge Messages**.

Kiểm tra quy tắc (không cần chạy Prometheus):

```powershell
docker run --rm -v ${PWD}/Infra/monitoring:/m -w /m --entrypoint promtool prom/prometheus:v2.54.1 test rules alerts_test.yml
```

Hiện chỉ hiển thị trên Prometheus (chưa gửi email/Telegram – cần Alertmanager).

## RabbitMQ (Sprint 6)

Image `rabbitmq:3.13-management` đã bật sẵn plugin `rabbitmq_prometheus` (cổng 15692). Prometheus đọc
`http://rabbitmq:15692/metrics/per-object` (job `rabbitmq`) để có số tin theo **từng hàng đợi**.

```powershell
docker compose -f Infra/docker-compose.yml --profile mq --profile monitoring up -d
```

- Kiểm tra: http://localhost:9090/targets → job `rabbitmq` là **UP**; hoặc mở http://localhost:15692/metrics/per-object, tìm `rabbitmq_queue_messages_ready`.
- Truy vấn thử trên Prometheus: `rabbitmq_queue_messages_ready` (tin chờ theo hàng đợi), `rabbitmq_queue_messages{queue=~".*dlq.*"}` (tin trong DLQ).
- Không bật profile `mq` thì target `rabbitmq` DOWN – bình thường, không bật cảnh báo.

## Kiểm thử tải k6 (Sprint 6)

Kịch bản `Infra/loadtest/k6-gateway.js`, tăng dần tới 500 người dùng ảo qua Gateway. Cách chạy và kết quả: [Docs/testing/load-test.md](../../Docs/testing/load-test.md).

## Giả lập traffic cho demo

```powershell
powershell -ExecutionPolicy Bypass -File Infra/monitoring/traffic-sim.ps1                 # normal ~3 request/giây, Ctrl+C để dừng
powershell -ExecutionPolicy Bypass -File Infra/monitoring/traffic-sim.ps1 -Mode spike     # tăng tải 60 giây (8 luồng)
powershell -ExecutionPolicy Bypass -File Infra/monitoring/traffic-sim.ps1 -Mode error     # trộn lỗi 5xx
powershell -ExecutionPolicy Bypass -File Infra/monitoring/traffic-sim.ps1 -Mode scenario  # 5 phút: normal → spike → normal → error → normal
```

- Gọi qua gateway 8080: `/api/artifacts`, `/api/artifacts/{1..3}`, `/api/auth/me` không token (401), `/api/auth/login` admin.
- Chế độ `error` gọi `/api/khong-ton-tai`: gateway chuyển về Backend cũ 8090. Backend cũ **tắt** → 500 (bật `HighErrorRate5xx`);
  Backend cũ **bật** → 404 (chỉ tăng 4xx). Script tự cảnh báo nếu thấy cổng 8090 đang mở.
- Chỉ gọi API đọc và đăng nhập, không sửa dữ liệu.

## Lỗi thường gặp

| Hiện tượng | Cách xử lý |
|---|---|
| Target service DOWN | Service chưa chạy, hoặc chạy sai cổng. Bình thường nếu chưa bật service đó |
| `mysql_up 0` | Sai `MYSQL_ROOT_PASSWORD` hoặc MySQL chưa sẵn sàng: `docker logs smart_heritage_mysql_exporter` |
| Dashboard trống p95 | Service chưa bật histogram hoặc chưa có request nào: gọi thử 1 API rồi đợi 30 giây |
| Tab Alerts trống | Prometheus chưa nạp `alerts.yml`: `docker logs smart_heritage_prometheus`, tìm dòng `rule`. Nhớ `restart prometheus` sau khi sửa |
| Grafana không có dashboard | `docker logs smart_heritage_grafana`, tìm dòng `provisioning` |
