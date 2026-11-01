# feedback-service (Tường – chuẩn bị ở Sprint 4, tách hẳn ở tuần 5)

Đánh giá (số sao + bình luận) của du khách cho hiện vật, kiểm duyệt của ADMIN, điểm trung bình.
Cổng **8086**, schema **feedback_db** (`Infra/db/06_feedback.sql`).

Tuần 5 route qua Gateway (8080): `/api/artifacts/*/feedbacks`, `/api/artifacts/*/rating-summary`, `/api/admin/feedbacks/**`
(phần Gateway và docker-compose do Bảo cập nhật).

## API

| Method | Đường dẫn | Quyền (Gateway kiểm tra) | Ghi chú |
|---|---|---|---|
| POST | `/api/artifacts/{id}/feedbacks` | VISITOR hoặc ADMIN | Người gửi lấy từ `X-User-Id`; trạng thái PENDING. Lần đầu 201; đã có đánh giá thì ghi đè, trả 200 |
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

## Chặn spam và đồng bộ NiFi (tuần 5)

- Mỗi người chỉ có **1 đánh giá cho 1 hiện vật** (khóa duy nhất `uq_feedbacks_user_artifact`).
  Gửi lại thì ghi đè số sao, bình luận, tên hiển thị; giữ `id`, `createdAt`; đưa về **PENDING** để ADMIN duyệt lại.
  Đánh giá đã duyệt mà bị sửa sẽ biến khỏi danh sách công khai và điểm trung bình cho tới khi được duyệt lại.
- Hai lần gửi đầu tiên cùng lúc va vào khóa duy nhất → **409**; gửi lại sẽ thành sửa đánh giá.
- Cột `updated_at` đổi mỗi khi sửa đánh giá hoặc ADMIN đổi trạng thái. Luồng NiFi `feedbacks → fact_feedbacks`
  dùng `updated_at` làm Maximum-value Column (không dùng `id`, vì `id` không đổi khi status đổi).
  Giá trị do service gán, cùng đồng hồ với `created_at`. Khi demo nên sửa dữ liệu qua API/Web, không sửa tay bằng SQL
  (MySQL trong container chạy giờ UTC nên `ON UPDATE` có thể cho mốc thời gian lệch).

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
| `CONTENT_SERVICE_URL` | `http://localhost:8083` (content-service; trong docker-compose: `http://content-service:8083`) |
| `AUTH_SERVICE_URL` | `http://localhost:8081` |

Gọi thẳng cổng 8086 thì phải tự gắn header mà Gateway sẽ gắn. Ví dụ:

```bash
curl -X POST localhost:8086/api/artifacts/2/feedbacks -H "X-User-Id: 3" -H "X-User-Role: VISITOR" \
     -H "Content-Type: application/json" -d '{"rating":4,"comment":"Rất hay"}'
```

## Việc tuần 5 còn lại

1. (Bảo) Gateway, route `feedback-service`: thêm `/api/artifacts/*/rating-summary` và `/api/admin/feedbacks/**`
   (2 đường dẫn này hiện rơi vào content-service và Backend cũ). Đặt `FEEDBACK_SERVICE_URL=http://localhost:8086`.
2. (Bảo) docker-compose (profile core): thêm container feedback-service, dùng `feedback_user`,
   `CONTENT_SERVICE_URL=http://content-service:8083`.
3. (Tuấn) Luồng NiFi feedbacks dùng `updated_at` làm cột mốc (xem mục trên).
4. Web Admin `feedback.js`: thay dữ liệu giả bằng `GET /api/admin/feedbacks` và `PUT .../status`.
