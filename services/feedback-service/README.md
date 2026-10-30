# feedback-service (Tường – chuẩn bị ở Sprint 4, tách hẳn ở tuần 5)

Đánh giá (số sao + bình luận) của du khách cho hiện vật, kiểm duyệt của ADMIN, điểm trung bình.
Cổng **8086**, schema **feedback_db** (`Infra/db/06_feedback.sql`).

Tuần 4 **chưa route qua Gateway**: `/api/artifacts/*/feedbacks` ở cổng 8080 vẫn do Backend cũ (8090) xử lý.

## API

| Method | Đường dẫn | Quyền (Gateway kiểm tra) | Ghi chú |
|---|---|---|---|
| POST | `/api/artifacts/{id}/feedbacks` | VISITOR hoặc ADMIN | Người gửi lấy từ `X-User-Id`; trạng thái PENDING |
| GET | `/api/artifacts/{id}/feedbacks` | Công khai | Chỉ đánh giá APPROVED |
| GET | `/api/artifacts/{id}/rating-summary` | Công khai | Mới: điểm trung bình, chỉ tính APPROVED |
| GET | `/api/admin/feedbacks?status=` | ADMIN | Mới: lọc PENDING/APPROVED/REJECTED, bỏ trống = tất cả |
| PUT, PATCH | `/api/admin/feedbacks/{id}/status` | ADMIN | `{"status":"APPROVED"}` hoặc `REJECTED` |

## Khác Backend cũ

- `Feedback` lưu `Long userId`, `Long artifactId`. Không còn `@ManyToOne User`, không import entity của service khác.
- Kiểm tra hiện vật (tồn tại + ACTIVE) bằng cách gọi `GET {CONTENT_SERVICE_URL}/api/artifacts/{id}`:
  - Timeout 2 giây.
  - 404 → 404.
  - Không kết nối được hoặc lỗi 5xx → **503** kèm thông báo.
- Tên người đánh giá được chụp lại lúc gửi vào cột `reviewer_name`:
  - Lấy bằng `GET {AUTH_SERVICE_URL}/api/auth/me`, chuyển tiếp header `X-User-*`.
  - Lỗi thì để trống, API hiển thị "Khách".

## Chạy và test

```bash
mvn -q -DskipTests install                          # ở thư mục gốc, lần đầu
mvn -pl services/feedback-service spring-boot:run   # http://localhost:8086/swagger-ui.html
mvn -pl services/feedback-service test
```

| Biến môi trường | Mặc định |
|---|---|
| `DB_URL` | `jdbc:mysql://localhost:3307/feedback_db?...` |
| `DB_USERNAME` / `DB_PASSWORD` | `root` / `rootpassword123` (user riêng: `feedback_user` / `feedback_pass_dev`) |
| `CONTENT_SERVICE_URL` | `http://localhost:8090` (Backend cũ; content-service xong thì đổi `http://localhost:8083`) |
| `AUTH_SERVICE_URL` | `http://localhost:8081` |

Gọi thẳng cổng 8086 thì phải tự gắn header mà Gateway sẽ gắn. Ví dụ:

```bash
curl -X POST localhost:8086/api/artifacts/2/feedbacks -H "X-User-Id: 3" -H "X-User-Role: VISITOR" \
     -H "Content-Type: application/json" -d '{"rating":4,"comment":"Rất hay"}'
```

## Việc tuần 5 để tách hẳn

1. Gateway (`GatewayRoutesConfig`, route `feedback-service`): thêm `/api/artifacts/*/rating-summary` và `/api/admin/feedbacks/**`
   (2 đường dẫn này hiện rơi vào content-service và Backend cũ). Đặt `FEEDBACK_SERVICE_URL=http://localhost:8086`.
2. docker-compose (profile core): thêm container feedback-service, dùng `feedback_user`.
3. Web Admin `feedback.js`: thay dữ liệu giả bằng `GET /api/admin/feedbacks` và `PUT .../status`.
4. Khi content-service chạy: đặt `CONTENT_SERVICE_URL=http://content-service:8083`.
