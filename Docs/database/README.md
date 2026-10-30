# Database Documentation

Store the database schema and design notes here. The ERD source and exported diagrams remain in `Diagram/ERD/`.

## Từ Sprint 4: mỗi service một schema (Tường)

Script nằm trong [`Infra/db/`](../../Infra/db/README.md) và chạy theo thứ tự tên file khi tạo volume MySQL.

| Schema | Service | Cổng | Bảng | User MySQL |
|---|---|---|---|---|
| `auth_db` | auth-service | 8081 | users, refresh_tokens | `auth_user` |
| `site_db` | site-service | 8082 | heritage_sites | `site_user` |
| `content_db` | content-service | 8083 | artifacts, artifact_contents, media_resources | `content_user` |
| `beacon_db` | beacon-service | 8084 | ibeacons | `beacon_user` |
| `personalization_db` | personalization-service | 8085 | visits, likes | `personalization_user` |
| `feedback_db` | feedback-service | 8086 | feedbacks | `feedback_user` |
| `analytics_db` | analytics-service / NiFi | 8087 | fact_visits, fact_feedbacks, dim_artifact, agg_daily_views | `analytics_user` |
| `smart_heritage_db` | Backend cũ (monolith) | 8090 | toàn bộ bảng cũ | `root` |

Khác với ERD gốc (một database):

- Bỏ khóa ngoại chéo schema. Ví dụ `artifacts.site_id` và `feedbacks.user_id` giờ chỉ là cột `BIGINT`.
- Thêm `feedbacks.reviewer_name` trong `feedback_db`: tên người đánh giá được chụp lại lúc gửi.
- Bảng `visits` có thêm `client_event_id`, `source` và khóa duy nhất `(user_id, client_event_id)` để đồng bộ offline không bị ghi trùng.
  `duration_seconds` mặc định là `NULL`.
