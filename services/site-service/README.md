# site-service (Ân, cổng 8082, schema `site_db`)

Quản lý di tích và API tìm di tích gần (`/api/heritage-sites/nearby`, Haversine).
Dựng từ `services/service-template` theo `services/README.md`; code chuyển từ `Backend/` (nhánh `feature/an-w3-nearby`).

## Khác Backend cũ

| Backend cũ | site-service |
|---|---|
| `server.port=8080`, bảng trong `smart_heritage_db` | Cổng 8082, schema `site_db` (`Infra/db/02_site.sql`) |
| Controller tự `try/catch RuntimeException` trả `{message}` | `ApiException` → `GlobalExceptionHandler` (module common) |
| Trả thẳng mảng/object | `ApiResponse`: `{success, message, data}` |
| CORS cấu hình trong service | Gateway lo CORS; không còn `@CrossOrigin`, không Spring Security |

Nghiệp vụ giữ nguyên: `radius` mặc định 5 km, tối đa 50 km; `distanceKm` làm tròn 2 chữ số, sắp gần → xa;
`clearCoordinates` để xóa tọa độ khi sửa. Chưa xóa code cũ trong `Backend/` (để dự phòng đến hết tuần 5).

## Chạy và test

```powershell
mvn -q -DskipTests install                        # ở thư mục gốc, cài module common
mvn -pl services/site-service test                # test chạy trên H2, không cần MySQL
mvn -pl services/site-service spring-boot:run     # cần MySQL 3307
```

- Swagger: http://localhost:8082/swagger-ui.html
- Prometheus: http://localhost:8082/actuator/prometheus

Gateway đã mặc định trỏ `/api/heritage-sites/**` sang `http://localhost:8082`
(`SITE_SERVICE_URL`). Client vẫn gọi `http://localhost:8080`.

Docker: `docker compose -f Infra/docker-compose.yml --profile core up -d --build` (đã khai báo sẵn site-service).

| Biến môi trường | Mặc định |
|---|---|
| `SERVER_PORT` | 8082 |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | `jdbc:mysql://localhost:3307/site_db`, root, mật khẩu dev |
| `SITE_SERVICE_URL` (phía Gateway) | http://localhost:8082 |

Tài liệu API: [Docs/api/site-service.md](../../Docs/api/site-service.md).
