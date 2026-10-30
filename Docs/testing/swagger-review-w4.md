# Rà soát Swagger các service đã tách – tuần 4 (Tường)

Tiêu chí (Definition of Done, mục VI file task tuần 4):

1. Đủ endpoint so với Backend cũ.
2. Mỗi endpoint có `@Operation(summary = ...)`.
3. Có ví dụ request/response (`@Schema(example = ...)` trên DTO hoặc `@ExampleObject`).
4. Có `@ApiResponses` cho các mã lỗi chính (400/401/403/404/503).
5. `/swagger-ui.html` và `/actuator/prometheus` mở được ở cổng riêng.

Cách kiểm tra: chạy service, mở `http://localhost:<cổng>/swagger-ui.html`, đối chiếu với bảng endpoint của `Backend/.../controller`.
Đánh dấu: ✅ đạt, ⚠️ thiếu một phần, ❌ chưa có, ⏳ chưa có PR để rà.

| Service | Cổng | Người | Trạng thái nhánh (30/10) | Đủ endpoint | @Operation | Ví dụ request | Mã lỗi | Swagger/Prometheus |
|---|---|---|---|---|---|---|---|---|
| auth-service | 8081 | Bảo | Trên main | ✅ register, login, refresh, logout, me, users/{id} | ✅ 6/6 | ❌ `RegisterRequest`, `LoginRequest`, `RefreshRequest` chưa có `@Schema(example)` | ❌ chưa có `@ApiResponses` | ✅ |
| site-service | 8082 | Ân | ⏳ chưa đẩy nhánh `feature/an-w4-site-service` | ⏳ | ⏳ | ⏳ | ⏳ | ⏳ |
| content-service | 8083 | Xuân Anh | ⏳ chưa đẩy nhánh `feature/xa-w4-content-service` | ⏳ | ⏳ | ⏳ | ⏳ | ⏳ |
| beacon-service | 8084 | Khoa | ⏳ chưa đẩy nhánh `feature/khoa-w4-beacon-service` | ⏳ | ⏳ | ⏳ | ⏳ | ⏳ |
| feedback-service | 8086 | Tường | Nhánh `feature/tuong-w4-feedback-prep` | ✅ 6 endpoint (POST/GET feedbacks, rating-summary, admin list, PUT/PATCH status) | ✅ | ✅ | ✅ | ✅ |

## Danh sách thiếu gửi từng người

- **Bảo (auth-service):**
  - Thêm `@Schema(example = ...)` cho 3 DTO đăng ký/đăng nhập/refresh trong `AuthController`.
  - Thêm `@ApiResponses` (400 sai dữ liệu, 401 sai mật khẩu/token, 403 xem hồ sơ người khác).
- **Ân, Xuân Anh, Khoa:** rà khi PR tách service được mở (mốc Thứ 7 22:00).
  Lưu ý chung khi copy controller từ Backend cũ:
  - Giữ `@Operation` có sẵn.
  - Đổi `@io.swagger.v3.oas.annotations.responses.ApiResponse` cho khớp mã lỗi mới.
  - Lỗi gọi service khác trả 503 thì ghi vào `@ApiResponses`.
