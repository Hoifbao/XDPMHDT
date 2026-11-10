# feedback-service (Tường – chuẩn bị ở Sprint 4, tách hẳn ở tuần 5)

Đánh giá (số sao + bình luận) của du khách cho hiện vật, kiểm duyệt của ADMIN, điểm trung bình.
Cổng **8086**, schema **feedback_db** (`Infra/db/06_feedback.sql`).

Tuần 5 route qua Gateway (8080): `/api/artifacts/*/feedbacks`, `/api/artifacts/*/rating-summary`, `/api/admin/feedbacks/**`.
Cả 3 nhóm đường dẫn đã có trong route `feedback-service` của `GatewayRoutesConfig` (order 3, trước content-service);
mặc định `FEEDBACK_SERVICE_URL` của Gateway là `http://localhost:8086`. Test: `GatewayRoutesConfigTest`.

## API

| Method | Đường dẫn | Quyền (Gateway kiểm tra) | Ghi chú |
|---|---|---|---|
| POST | `/api/artifacts/{id}/feedbacks` | VISITOR hoặc ADMIN | Người gửi lấy từ `X-User-Id`; trạng thái PENDING. Lần đầu 201; đã có đánh giá thì ghi đè, trả 200 |
| GET | `/api/artifacts/{id}/feedbacks` | Công khai | Chỉ đánh giá APPROVED |
| GET | `/api/artifacts/{id}/rating-summary` | Công khai | Mới: điểm trung bình, chỉ tính APPROVED |
| GET | `/api/admin/feedbacks?status=&artifactId=` | ADMIN | Lọc PENDING/APPROVED/REJECTED và/hoặc theo hiện vật, bỏ trống = tất cả |
| PUT, PATCH | `/api/admin/feedbacks/{id}/status` | ADMIN | `{"status":"APPROVED"}` hoặc `REJECTED` |
| GET | `/api/admin/feedbacks/pending-count` | ADMIN | Tuần 6: số chờ duyệt trên menu Web, `{"pending": n}` |
| GET | `/api/feedbacks/me` | Đăng nhập | Tuần 6: đánh giá của tôi, mọi trạng thái. Thiếu `X-User-Id` trả 401 |

## Sự kiện RabbitMQ (tuần 6)

Gửi lên exchange topic `sh.events`, chỉ sau khi giao dịch DB đã commit. RabbitMQ lỗi thì ghi log kèm `eventId`, API vẫn thành công.

| Sự kiện | Routing key | Khi nào | `data` |
|---|---|---|---|
| FeedbackCreated | `feedback.created` | Du khách tạo đánh giá mới (gửi lại để ghi đè thì không gửi, tránh đếm trùng) | `feedbackId, userId, artifactId, rating, createdAt` |
| FeedbackApproved | `feedback.approved` | Đánh giá chuyển sang APPROVED (duyệt lại đánh giá đã duyệt thì không gửi) | `feedbackId, userId, artifactId, artifactName` |

- Không lấy được tên hiện vật từ content-service thì `artifactName` là `Hiện vật #<id>`.
- Queue và dead-letter queue do bên nhận tự tạo. Bên nhận bỏ qua sự kiện trùng theo `eventId`.
- Gửi bằng thư viện chung `common/events`: `EventPublisher.publishAfterCommit`, khung `EventEnvelope`.
  Cấu hình nạp ở `config/EventsImportConfig` (không đặt `@Import` trên lớp Application, vì `@WebMvcTest` nạp lớp đó mà không có RabbitMQ).
- Message không có header `__TypeId__`, bên nhận đọc bằng record của service mình.
- `artifactName` của FeedbackApproved được lấy **sau khi commit**, giao dịch DB không phải chờ content-service.
- Metric Prometheus (thư viện đếm): `sh_events_published_total{type, result}` với `result` = `success` | `failed`. feedback-service tạo sẵn 4 tổ hợp của `FeedbackCreated`, `FeedbackApproved` bằng 0 lúc khởi động, nên alert dùng `increase()` bắt được cả lần lỗi đầu tiên.

**Hướng dẫn cho bên nhận (Khoa, Tuấn)**

1. Không import class của feedback-service: mỗi service tự khai báo record, chỉ cần các trường mình dùng.
2. Nạp `EventsConfig` của common (có sẵn `MessageConverter` JSON); kiểu dữ liệu lấy từ tham số của `@RabbitListener`.
3. Tự tạo queue, dead-letter queue và binding vào `sh.events` theo routing key cần nhận.
4. Lưu `eventId` đã xử lý, gặp lại thì bỏ qua, vì RabbitMQ có thể giao một tin nhiều lần.

```java
record FeedbackApprovedData(Long feedbackId, Long userId, Long artifactId, String artifactName) {}

@RabbitListener(queues = "notification.feedback-approved")
void onFeedbackApproved(EventEnvelope<FeedbackApprovedData> event) {
    if (!processedEvents.markIfNew(event.eventId())) {
        return; // tin đã xử lý
    }
    // gửi FCM cho event.data().userId()
}
```

**Rủi ro đã biết**

- Không có publisher confirm hay outbox: RabbitMQ chết đúng lúc gửi thì sự kiện bị mất (đánh giá vẫn lưu). Xem số lần lỗi qua metric `result="failed"`.
- `management.health.rabbit.enabled: false`: `/actuator/health` không báo RabbitMQ DOWN. Trạng thái RabbitMQ cần theo dõi riêng trên Prometheus/Grafana.
- `result="success"` chỉ có nghĩa là đã đưa tin vào kết nối, chưa phải RabbitMQ xác nhận đã nhận. Chưa có queue bind vào `sh.events` thì tin bị bỏ mà vẫn tính `success`.
- Không chạy `--profile mq` thì mỗi lần gửi/duyệt có thể chậm thêm tối đa ~2 giây (connection-timeout).

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

Chạy cùng cả hệ thống:

- Local: `powershell -ExecutionPolicy Bypass -File Infra\dev\start-local.ps1` mở thêm cửa sổ feedback-service 8086.
  Script này chưa bật content-service nên đặt `CONTENT_SERVICE_URL=http://localhost:8090` (Backend cũ) cho feedback-service.
- Docker: `docker compose -f Infra/docker-compose.yml --profile core up -d --build` (container `smart_heritage_feedback`,
  user MySQL `feedback_user`, gọi `content-service:8083` và `auth-service:8081`).

| Biến môi trường | Mặc định |
|---|---|
| `DB_URL` | `jdbc:mysql://localhost:3307/feedback_db?...` |
| `DB_USERNAME` / `DB_PASSWORD` | `root` / `rootpassword123` (user riêng: `feedback_user` / `feedback_pass_dev`) |
| `SPRING_RABBITMQ_HOST` / `SPRING_RABBITMQ_PORT` | `localhost` / `5672` |
| `SPRING_RABBITMQ_USERNAME` / `SPRING_RABBITMQ_PASSWORD` | `smartheritage` / `change-me-rabbit` |
| `CONTENT_SERVICE_URL` | `http://localhost:8083` (content-service; trong docker-compose: `http://content-service:8083`) |
| `AUTH_SERVICE_URL` | `http://localhost:8081` |

Gọi thẳng cổng 8086 thì phải tự gắn header mà Gateway sẽ gắn. Ví dụ:

```bash
curl -X POST localhost:8086/api/artifacts/2/feedbacks -H "X-User-Id: 3" -H "X-User-Role: VISITOR" \
     -H "Content-Type: application/json" -d '{"rating":4,"comment":"Rất hay"}'
```

## Việc tuần 5 còn lại

1. ~~Gateway: route `/api/artifacts/*/rating-summary`, `/api/admin/feedbacks/**`, mặc định 8086~~: đã xong (Tường), nhờ Bảo review.
2. ~~docker-compose + `start-local.ps1`~~: đã xong (Tường).
3. (Tuấn) Luồng NiFi feedbacks dùng `updated_at` làm cột mốc (xem mục trên).
4. ~~Web Admin `feedback.js`: gọi `GET /api/admin/feedbacks` và `PATCH .../status`~~: đã xong (Tường).
