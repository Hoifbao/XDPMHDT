# content-service (Xuân Anh, cổng 8083, schema content_db)

Hiện vật, nội dung đa ngôn ngữ (`?lang=vi|en` có fallback tiếng Việt) và media.
Dựng từ `services/service-template` theo `services/README.md`. Code nghiệp vụ, `SiteClient` và test gốc
của Xuân Anh (nhánh `XA-task3v4`), chuyển sang đúng cấu trúc module.

## Khác Backend cũ

| Backend cũ | content-service |
|---|---|
| `@ManyToOne HeritageSite site` | Cột `Long siteId`; kiểm tra và lấy tên di tích qua `SiteClient` (GET `/api/heritage-sites/{id}`) |
| Bảng `Artifacts`, `ArtifactContents` | Bảng `artifacts`, `artifact_contents`, `media_resources` (khớp `Infra/db/03_content.sql`) |
| try/catch trong controller | `ApiException` / `IllegalArgumentException` → `GlobalExceptionHandler` (module common) |
| | Mới: `GET /api/artifacts/batch?ids=3,1,2` (Phú dùng cho Yêu thích/Lịch sử) |

`SiteClient`: thêm/sửa hiện vật mà siteId không tồn tại → 400; site-service tắt → 503 và **không ghi**.
API đọc vẫn chạy khi site-service tắt (chỉ thiếu `locationName`).
Chưa có site-service (Ân) thì mặc định gọi Backend cũ 8090 (`SITE_SERVICE_URL` để đổi).

## Chạy và test

```powershell
mvn -q -DskipTests install                       # ở thư mục gốc, cài module common
mvn -pl services/content-service test            # test chạy trên H2, không cần MySQL
mvn -pl services/content-service spring-boot:run # cần MySQL 3307 + Backend cũ 8090 (để kiểm tra siteId)
```

- Swagger: http://localhost:8083/swagger-ui.html
- Prometheus: http://localhost:8083/actuator/prometheus

Cho Gateway chuyển `/api/artifacts/**`, `/api/media/**` sang service này (mặc định Gateway vẫn trỏ Backend cũ):

```powershell
$env:CONTENT_SERVICE_URL="http://localhost:8083"; mvn -pl services/api-gateway spring-boot:run
```

Docker: `docker compose -f Infra/docker-compose.yml --profile core up -d --build` (đã khai báo sẵn content-service).

| Biến môi trường | Mặc định |
|---|---|
| `SERVER_PORT` | 8083 |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | `jdbc:mysql://localhost:3307/content_db`, root, mật khẩu dev |
| `SITE_SERVICE_URL` | `LEGACY_BACKEND_URL` hoặc http://localhost:8090 |
| `CLIENT_TIMEOUT` | 2s |
