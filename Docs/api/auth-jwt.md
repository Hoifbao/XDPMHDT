# Xác thực JWT & phân quyền RBAC (Bảo – Sprint 3)

## 1. Tóm tắt cho cả nhóm

| Loại API | Ai gọi được | Cần token? |
|---|---|---|
| `GET` mọi API (`/api/artifacts`, `/api/heritage-sites`, `/api/ibeacons`...) | Ai cũng gọi được | Không |
| `POST/PUT/PATCH/DELETE` (thêm/sửa/xóa) | Chỉ **ADMIN** | Có |
| `POST /api/artifacts/{id}/feedbacks` (gửi đánh giá – Tường) | VISITOR hoặc ADMIN | Có |
| `/api/users/{userId}/**` (yêu thích, lịch sử – Phú) | Chính chủ `userId` hoặc ADMIN | Có |
| `/api/me/**` (dữ liệu của người đang đăng nhập – Phú) | VISITOR hoặc ADMIN đã đăng nhập | Có |
| `/api/admin/**` | Chỉ **ADMIN** | Có |
| `/api/statistics/**` (Dashboard – Tuấn) | Chỉ **ADMIN** | Có |
| `/api/auth/me`, `/api/auth/users/{id}` | Chính chủ hoặc ADMIN | Có |
| `/api/auth/register`, `login`, `refresh`, `logout` | Ai cũng gọi được | Không |
| `/api/demo/**` (demo Redis) | Ai cũng gọi được | Không |

Mã lỗi: **401** = chưa đăng nhập / token sai / token hết hạn → client gọi `/api/auth/refresh`.
**403** = đã đăng nhập nhưng không đủ quyền (vd: VISITOR gọi API xóa).

Quy tắc nằm ở `Backend/.../config/SecurityConfig.java`. API mới mà không khớp quy tắc riêng
thì tự áp dụng: GET công khai, thêm/sửa/xóa chỉ ADMIN. Cần quy tắc khác thì báo Bảo.

## 2. Lấy token và dán vào Swagger

1. Chạy Backend: `cd Backend && mvn spring-boot:run`, mở http://localhost:8080/swagger-ui.html
2. Mở `auth-controller` → `POST /api/auth/login` → **Try it out**, nhập:
   ```json
   { "username": "admin", "password": "Test@1234" }
   ```
3. Bấm **Execute**, copy giá trị `accessToken` trong kết quả (chuỗi dài bắt đầu bằng `eyJ...`).
4. Bấm nút **Authorize** (ổ khóa, góc trên bên phải) → dán token (KHÔNG gõ chữ `Bearer`) → **Authorize** → **Close**.
5. Gọi các API thêm/sửa/xóa như bình thường. Access token sống **15 phút**; hết hạn thì login lại
   hoặc gọi `POST /api/auth/refresh` với `refreshToken`.

Muốn test quyền VISITOR: đăng ký tài khoản mới qua `POST /api/auth/register` rồi login bằng tài khoản đó.

## 3. Các API xác thực

| API | Body | Kết quả |
|---|---|---|
| `POST /api/auth/login` | `{username, password}` | Hồ sơ + `accessToken`, `refreshToken`, `expiresIn` (giây) |
| `POST /api/auth/refresh` | `{refreshToken}` | Cặp token **mới**. Token cũ bị vô hiệu ngay (mỗi refresh token dùng 1 lần) |
| `POST /api/auth/logout` | `{refreshToken}` | Thu hồi refresh token. Access token còn hiệu lực tối đa 15 phút rồi tự hết |
| `GET /api/auth/me` | – (cần Bearer) | Hồ sơ người đang đăng nhập |

Ví dụ kết quả login:
```json
{
  "userId": 1, "username": "admin", "email": "admin@example.com",
  "fullName": "Quản trị viên mẫu", "role": "ADMIN", "preferredLanguage": "vi",
  "tokenType": "Bearer",
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "expiresIn": 900,
  "refreshToken": "q3Zx...",
  "refreshExpiresIn": 604800,
  "message": "Đăng nhập thành công!"
}
```

Gọi bằng curl:
```bash
curl -X DELETE http://localhost:8080/api/artifacts/5 -H "Authorization: Bearer <accessToken>"
```

## 4. Dùng trong code của từng phần

**Backend – lấy người đang đăng nhập** (thay cho việc nhận `userId` từ URL/body):
```java
import com.smartheritage.backend.security.SecurityUtils;
Long userId = SecurityUtils.currentUserId()
        .orElseThrow(() -> new IllegalStateException("Chưa đăng nhập"));
```
hoặc trong Controller: `@AuthenticationPrincipal AuthPrincipal me` → `me.userId()`, `me.role()`, `me.isAdmin()`.

**Web Admin** – mỗi trang quản trị chỉ cần 1 dòng trong `<head>`, đặt TRƯỚC file JS của trang:
```html
<script src="../api/auth.js" data-guard="admin"></script>
```
Sau đó mọi `fetch(API_BASE + '/api/...')` tự gắn token và tự refresh khi hết hạn, **không cần sửa code fetch**.
Chưa đăng nhập hoặc không phải ADMIN → tự chuyển về `login.html` (đăng nhập xong quay lại đúng trang cũ).

**Mobile (Flutter)** – mọi lệnh `http.get/post/put/delete` trong app tự gắn token (cấu hình ở `main.dart`
bằng `http.runWithClient` + `lib/core/network/auth_http_client.dart`). Token lưu bằng `shared_preferences`,
tắt app mở lại vẫn còn đăng nhập. Sau khi pull nhớ chạy `flutter pub get`.

## 5. Database (Tường cập nhật vào `Infra/init_db.sql`)

Hibernate (`ddl-auto=update`) tự tạo bảng này khi chạy Backend, nhưng để `init_db.sql` là nguồn chuẩn:
```sql
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    token_hash  CHAR(64)    NOT NULL UNIQUE,      -- SHA-256 của refresh token, không lưu token gốc
    user_id     BIGINT      NOT NULL,
    expires_at  DATETIME(6) NOT NULL,
    revoked     BIT(1)      NOT NULL DEFAULT 0,
    created_at  DATETIME(6) NOT NULL,
    INDEX idx_refresh_tokens_user (user_id),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
```

## 6. Cấu hình

`application.properties`: `app.jwt.secret` (≥ 32 ký tự; deploy thật thì đặt biến môi trường `JWT_SECRET`),
`app.jwt.access-token-ttl-seconds` (mặc định 900), `app.jwt.refresh-token-ttl-seconds` (mặc định 604800 = 7 ngày).

## 6. Từ tuần 4: đăng nhập đi qua API Gateway

- Client **không đổi gì**: vẫn gọi `http://localhost:8080` với cùng đường dẫn và cùng JSON.
  Cổng 8080 giờ là **api-gateway**; `/api/auth/**` được chuyển tới **auth-service (8081)**; Backend cũ chuyển sang **8090**.
- Luật phân quyền ở mục 1 giờ được kiểm tra tại Gateway (`services/api-gateway/.../security/AccessRules.java`).
  Backend cũ vẫn tự kiểm tra thêm một lần như cũ.
- Swagger: login thử ở `http://localhost:8081/swagger-ui.html`. API `/api/auth/me` chỉ chạy khi gọi qua Gateway
  (vì đọc header `X-User-*` do Gateway gắn).
- Service mới lấy người đang đăng nhập bằng:
  ```java
  import com.smartheritage.common.web.CurrentUser;
  Long userId = CurrentUser.requireUserId();   // chưa đăng nhập -> 401
  boolean admin = CurrentUser.isAdmin();
  ```
  Không dùng `SecurityUtils` / `@AuthenticationPrincipal` trong service mới (chỉ Backend cũ còn dùng).
- Gateway xóa mọi header `X-User-*` client tự gửi, nên app **không được** tự gắn `X-User-Id`.
- Kiến trúc chi tiết: `Docs/architecture/microservices.md`.
