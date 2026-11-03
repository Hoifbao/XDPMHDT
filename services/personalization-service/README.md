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
