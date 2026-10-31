# Tách service trong 5 bước (Bảo – Sprint 4)

Áp dụng cho: **site-service** (Ân, 8082), **content-service** (Xuân Anh, 8083), **beacon-service** (Khoa, 8084);
tuần 5: personalization (Phú, 8085), feedback (Tường, 8086), analytics (Tuấn, 8087).

Cấu trúc thư mục sau khi tách:

```
SmartHeritage/
├── pom.xml                 <- POM cha (smart-heritage-parent), liệt kê mọi module
├── Backend/                <- Backend cũ (monolith), cổng 8090. API chưa tách vẫn ở đây
├── services/
│   ├── common/             <- dùng chung: ApiResponse, ApiException, GlobalExceptionHandler, CurrentUser, JwtService
│   ├── api-gateway/        <- cổng 8080, client chỉ gọi vào đây
│   ├── auth-service/       <- cổng 8081
│   ├── service-template/   <- MẪU để copy
│   └── site-service/       <- (Ân tạo theo hướng dẫn dưới)
└── Infra/                  <- docker-compose (profile core, mq, monitoring, nifi), prometheus
```

Trước khi bắt đầu: `git checkout main` → `git pull` → `git checkout -b feature/<tên>-w4-<service>`,
rồi trong VS Code bấm **Java: Clean Java Language Server Workspace** (hoặc *Reload Projects*) để nhận pom cha.

---

## Bước 1. Copy mẫu

Copy thư mục `services/service-template` thành `services/<tên>-service`, ví dụ `services/site-service`. Sau đó đổi:

| Chỗ cần đổi | Mẫu | Ví dụ site-service |
|---|---|---|
| `pom.xml`: `artifactId`, `name`, `description` | `service-template` | `site-service` |
| Thư mục package `src/main/java/com/smartheritage/template` | `template` | `site` |
| Dòng `package ...` ở đầu mọi file .java | `com.smartheritage.template...` | `com.smartheritage.site...` |
| Tên class Application | `TemplateServiceApplication` | `SiteServiceApplication` |
| `Dockerfile`: tên module và cổng | `service-template`, `8099` | `site-service`, `8082` |

Mẹo VS Code: mở thư mục mới → Ctrl+Shift+H, tìm `com.smartheritage.template` thay bằng `com.smartheritage.site`.

## Bước 2. Khai báo module trong POM cha

Mở `pom.xml` ở thư mục gốc, thêm 1 dòng vào `<modules>`:

```xml
<module>services/site-service</module>
```

Kiểm tra: `mvn -q -pl services/site-service -am -DskipTests package` chạy xong không báo lỗi.

## Bước 3. Chuyển code từ Backend cũ sang

1. Copy entity, repository, service, controller, dto của phần mình từ `Backend/src/main/java/com/smartheritage/backend/...`
   sang package mới. Xóa các file mẫu `SampleItem*`.
2. Sửa import:
   - `com.smartheritage.backend.dto.ApiResponse` → `com.smartheritage.common.api.ApiResponse`
   - `SecurityUtils.currentUserId()` / `@AuthenticationPrincipal` → `CurrentUser.requireUserId()` (`com.smartheritage.common.web.CurrentUser`)
   - Lỗi: `throw ApiException.notFound("...")`, `ApiException.badRequest("...")` → tự thành JSON đúng mã HTTP
3. Bỏ `@CrossOrigin` trên controller (Gateway đã lo CORS) và không thêm Spring Security (Gateway đã kiểm tra quyền).
4. Gỡ quan hệ sang bảng của service khác: `@ManyToOne HeritageSite site` → `@Column(name = "site_id") Long siteId`.
   Cần kiểm tra dữ liệu ở service khác thì gọi REST (xem bảng phụ thuộc chéo trong `task_w4_chi_tiet.docx` mục III.4).
5. **Chưa xóa** code cũ trong `Backend/` ở tuần 4. Gateway sẽ chuyển route sang service mới ở bước 5, code cũ để dự phòng.

## Bước 4. Cấu hình cổng và schema

Mở `src/main/resources/application.yml`, đổi 3 chỗ có ghi `ĐỔI`:

```yaml
server:
  port: ${SERVER_PORT:8082}
spring:
  application:
    name: site-service
  datasource:
    url: "${DB_URL:jdbc:mysql://localhost:3307/site_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true}"
```

Schema tạo bằng script của Tường (`Infra/db/`). Trong lúc chờ, `ddl-auto: update` + `createDatabaseIfNotExist` tự tạo bảng trống
để chạy thử.

Chạy: `mvn -q -DskipTests install` (ở thư mục gốc) rồi `mvn -pl services/site-service spring-boot:run` → mở `http://localhost:8082/swagger-ui.html`
và `http://localhost:8082/actuator/prometheus` (phải ra số liệu).

## Bước 5. Nối vào Gateway và test

1. Mở `services/api-gateway/src/main/resources/application.yml`, đổi địa chỉ service của mình:
   ```yaml
   site: ${SITE_SERVICE_URL:http://localhost:8082}
   ```
   (Route `/api/heritage-sites/**` đã có sẵn trong `GatewayRoutesConfig.java`, không phải sửa code.)
2. Chạy đủ 4 thứ: MySQL/Redis (`docker compose -f Infra/docker-compose.yml up -d`), Backend cũ (8090), auth-service (8081),
   Gateway (8080), service của mình.
3. Test qua **http://localhost:8080** (không phải cổng riêng):
   - GET không token → 200
   - POST/PUT/DELETE không token → 401; tài khoản VISITOR → 403; admin / Test@1234 → 200
   - Web Admin trang của mình vẫn chạy (không đổi địa chỉ API)
4. Mở PR, ghi rõ đã test gì (ảnh Swagger/Postman), nhờ Bảo review.

---

## Chạy toàn bộ khi dev

| Thành phần | Lệnh (ở thư mục gốc repo) | Cổng |
|---|---|---|
| **Cả hệ thống bằng 1 lệnh** | `powershell -ExecutionPolicy Bypass -File Infra\dev\start-local.ps1` (mở 4 cửa sổ + MySQL/Redis, tự kiểm qua 8080) | 3307, 6379, 8090, 8081, 8080, 8082 |
| MySQL + Redis | `docker compose -f Infra/docker-compose.yml up -d mysql-db redis-cache` | 3307, 6379 |
| Backend cũ | `cd Backend && mvn spring-boot:run` | 8090 |
| auth-service | `mvn -pl services/auth-service spring-boot:run` | 8081 |
| api-gateway | `mvn -pl services/api-gateway spring-boot:run` | 8080 |
| Hoặc cả 3 trong Docker | `docker compose -f Infra/docker-compose.yml --profile core up -d --build` | 8090, 8081, 8080 |

**Lần đầu, và mỗi khi pull code có sửa `common` hoặc `pom.xml` cha:** chạy `mvn -q -DskipTests install` ở thư mục gốc
để cài module `common` vào máy. Sau đó mới chạy từng service bằng `mvn -pl <module> spring-boot:run`
(không thêm `-am`, vì `-am` sẽ chạy cả module `common` và báo lỗi không tìm thấy main class).

## Lỗi hay gặp

| Lỗi | Cách sửa |
|---|---|
| `Could not find artifact com.smartheritage:common` | Chạy `mvn -q -DskipTests install` ở thư mục gốc |
| Gateway báo `Spring MVC found on classpath` | Không thêm `spring-boot-starter-web` vào api-gateway hoặc common |
| `Port 8080 was already in use` | Backend cũ còn chạy cổng cũ: kéo main mới (Backend đã đổi sang 8090) hoặc tắt tiến trình cũ |
| `Port 8090 was already in use` | Backend cũ đang chạy ở cửa sổ khác / IntelliJ / Docker: `netstat -ano \| findstr :8090` lấy PID rồi `Stop-Process -Id <PID> -Force` |
| Gọi API qua Gateway bị 401 dù đã login | Gateway, auth-service, Backend cũ phải cùng `JWT_SECRET` |
| API cần đăng nhập trả 401 khi gọi thẳng cổng riêng | Bình thường: header `X-User-*` chỉ có khi đi qua Gateway. Test qua 8080 |
| Trình duyệt báo lỗi CORS "multiple values" | Xóa `@CrossOrigin` / cấu hình CORS trong service của mình |
