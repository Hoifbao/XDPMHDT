# Demo Redis Cache (Phú – Tuần 2, Chapter 3)

## Chuẩn bị
1. Chạy MySQL và Redis: `docker compose -f Infra/docker-compose.yml up -d` (Redis ở cổng 6379).
2. Chạy Backend: Run `BackendApplication` trong VS Code (hoặc `cd Backend` rồi `mvn spring-boot:run`).
3. Mở Swagger: http://localhost:8080/swagger-ui.html → nhóm **redis-demo-controller**.

## Kịch bản demo
| Bước | API | Kết quả mong đợi |
|---|---|---|
| 1 | `POST /api/demo/redis?key=hello&value=SmartHeritage&ttlSeconds=30` | Lưu chuỗi vào Redis, hết hạn sau 30 giây |
| 2 | `GET /api/demo/redis/hello` | Trả về `value` và `ttlSecondsRemaining` giảm dần |
| 3 | Đợi quá 30 giây rồi gọi lại bước 2 | 404 – key đã hết hạn |
| 4 | `GET /api/demo/redis/artifacts/1` (lần 1) | `source = MYSQL`: đọc DB rồi lưu cache 60 giây |
| 5 | Gọi lại bước 4 | `source = REDIS`, `elapsedMs` nhỏ hơn lần 1 |

Kiểm tra trực tiếp trong Redis:
```
docker exec -it smart_heritage_redis redis-cli
KEYS *
GET artifact:1
TTL artifact:1
```

## Mẫu thiết kế: cache-aside
1. Đọc Redis theo key `artifact:{id}`.
2. Có dữ liệu (cache hit) → trả về ngay.
3. Không có (cache miss) → đọc MySQL, ghi vào Redis kèm TTL, rồi trả về.

TTL giúp dữ liệu cache tự hết hạn, tránh hiển thị nội dung cũ quá lâu sau khi Admin sửa hiện vật.

## Lưu ý
- Redis chưa chạy → API trả 503 sau tối đa 2 giây (`spring.data.redis.timeout=2s`); các API khác vẫn hoạt động.
- Tuần sau có thể áp dụng cho API thật, ví dụ cache `GET /api/artifacts/{id}` và xoá cache khi `PUT/DELETE`.
