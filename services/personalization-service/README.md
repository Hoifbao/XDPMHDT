# personalization-service — Phú W5

Microservice **Cá nhân hóa & Lưu trữ ngoại tuyến (History, Favorites & Offline Cache)**, cổng **8085**, schema **personalization_db**.

## Nguyên tắc tách service

- Chỉ sở hữu bảng `likes` và `visits` trong `personalization_db`.
- `user_id` và `artifact_id` chỉ là `Long`, **không** `@ManyToOne` sang `users`/`artifacts` vì hai bảng đó thuộc service khác.
- User hiện tại lấy bằng `CurrentUser.requireUserId()` từ `services/common`; client không tự gửi `X-User-Id` qua Gateway.
- Tên/trạng thái/site/thumbnail hiện vật lấy qua REST từ content-service. `CONTENT_SERVICE_URL` có thể trỏ `http://localhost:8083`; khi chạy cùng Backend cũ có thể trỏ `http://localhost:8090`.
- Cache/offline phía app Flutter tiếp tục dùng `Interface/lib/features/personalization/personalization_service.dart`; backend chống ghi trùng bằng `clientEventId`.

## Chạy

```powershell
mvn -q -DskipTests install
mvn -pl services/personalization-service spring-boot:run
```

Swagger: `http://localhost:8085/swagger-ui.html`
Health: `http://localhost:8085/actuator/health`

Khi test tích hợp qua Gateway 8080, đặt:

```powershell
$env:PERSONALIZATION_SERVICE_URL="http://localhost:8085"
```

## API

- `GET /api/me/favorites?page=0&size=20`
- `POST|PUT /api/me/favorites/{artifactId}`
- `DELETE /api/me/favorites/{artifactId}`
- `GET /api/me/favorites/{artifactId}/exists`
- `GET /api/me/visits?page=0&size=20`
- `POST /api/me/visits`
- `POST /api/me/visits/batch`
- `GET /api/admin/visits?from=2026-10-01&to=2026-10-31&siteId=1`
- Redis demo trực tiếp cổng 8085: `/api/cache/demo` (chỉ bật khi `REDIS_DEMO_ENABLED=true`).

## Kiểm tra build

```powershell
mvn -pl services/personalization-service -am test
mvn -pl services/personalization-service -am -DskipTests package
```

## Sprint 6 — RabbitMQ events / cache invalidation

- Exchange chung `sh.events`: sau khi commit DB, phát `VisitRecorded` theo routing key `visit.recorded`.
- `POST /api/me/visits` hoặc `POST /api/me/visits/batch`: chỉ lượt mới mới phát sự kiện; retry `clientEventId` cũ không gửi trùng.
- Envelope: `eventId` (UUID), `type`, `version`, `occurredAt`, `data`. Dữ liệu visit: `visitId,userId,artifactId,siteId,detectedAt,durationSeconds,source`.
- Queue nhận `artifact.updated`: `personalization.artifact-updated`; xóa Redis key `artifact:summary:{id}`, chống xử lý trùng `eventId`. Lỗi listener thử lại tối đa 3 lần rồi chuyển `personalization.artifact-updated.dlq`.
- `ArtifactClient` sử dụng Redis cache-aside TTL 10 phút, fallback REST nếu Redis không sẵn sàng. Sau sự kiện ArtifactUpdated, lần đọc tên/thumbnail mới lấy từ content-service.
- Bật RabbitMQ: `docker compose -f Infra/docker-compose.yml --profile mq up -d`. UI `http://localhost:15672`, xem exchange và queue; host/port/account qua `SPRING_RABBITMQ_*`.
- Giám sát `/actuator/prometheus`: `sh_events_published_total{type="VisitRecorded",result="success|failed"}`.

### Kiểm thử W6

```powershell
mvn -pl services/personalization-service -am test
mvn -pl services/personalization-service -am -DskipTests package
```

1. Gửi `POST /api/me/visits` một lần (UUID mới): xem `visit.recorded` được nhận bởi consumer hoặc bind queue kiểm tra trong RabbitMQ UI.
2. Gửi lại cùng `clientEventId`: `data=false`, không có sự kiện mới.
3. Đồng bộ `/api/me/visits/batch`: chỉ các UUID mới sinh sự kiện.
4. Lấy `GET /api/me/favorites` để nạp cache `artifact:summary:ID`; sửa tên hiện vật trên CMS để phát `artifact.updated`, xem log listener xóa cache; gọi lại Favorites sẽ thấy tên mới.

**Giới hạn:** `EventPublisher.publishAfterCommit` của `common` log lỗi khi RabbitMQ không sẵn sàng và không tự phát lại nếu thiếu outbox bền vững. DLQ chỉ xử lý lỗi bên nhận. Cần phối hợp Bảo nếu nhóm yêu cầu bảo đảm không mất sự kiện khi broker down.

## XDPMHDT-48 — Sprint 6 (nhánh từ main mới nhất)

- Lượt tham quan mới (`POST /api/me/visits`) hoặc lượt mới trong batch (`POST /api/me/visits/batch`) được lưu DB trước; sau commit gọi `EventPublisher.publishAfterCommit("visit.recorded", "VisitRecorded", data)`.
- Data đúng hợp đồng với analytics-service của Tuấn: `visitId`, `artifactId`, `detectedAt` (`LocalDateTime`, múi giờ `Asia/Ho_Chi_Minh`).
- Shared `EventsConfig` được import bằng `@Configuration` riêng (`EventsImportConfig`), không sửa thư viện common.
- Subscriber `artifact.updated` xóa `artifact:summary:{id}` (Redis), chống lặp `eventId`, retry 3 lần rồi dead-letter.
- Cache-aside trong `ArtifactClient`: TTL 10 phút, Redis lỗi sẽ gọi content-service.
- Không copy/cherry-pick nhánh cũ bị conflict; PR squash merge, không merge main vào feature branch.

### Kiểm thử và cấu hình

```powershell
mvn -pl services/personalization-service -am test
mvn -pl services/personalization-service -am -DskipTests package
docker compose -f Infra/docker-compose.yml --profile mq up -d
$env:ANALYTICS_SERVICE_URL = "http://localhost:8087"
$env:BEACON_SERVICE_URL = "http://localhost:8084"
```

RabbitMQ: http://localhost:15672. Cần có analytics-service nhận `visit.recorded` để kiểm tra đếm realtime.
Trong RabbitMQ UI, xem queue của analytics, không chỉ exchange (exchange topic không tự giữ tin).
Lưu ý: `publishAfterCommit` không lưu producer outbox để gửi lại khi broker mất kết nối; cần phối hợp Bảo nếu muốn bảo đảm không mất tin.
