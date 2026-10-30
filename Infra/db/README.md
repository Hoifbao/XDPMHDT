# Script database theo từng schema (Tường – Sprint 4)

Từ tuần 4, mỗi microservice có **schema riêng** và **user MySQL riêng**. User chỉ được cấp quyền trên schema của mình.
`Infra/docker-compose.yml` mount cả thư mục này vào `/docker-entrypoint-initdb.d`, nên MySQL chạy các file
**theo thứ tự tên file**, và **chỉ chạy khi volume MySQL còn trống**.

| File | Schema | Service (cổng) | Phụ trách | Bảng |
|---|---|---|---|---|
| `00_create_schemas.sql` | 7 schema bên dưới | — | Tường | Tạo schema, user, GRANT |
| `01_auth.sql` | `auth_db` | auth-service (8081) | Bảo | users, refresh_tokens |
| `02_site.sql` | `site_db` | site-service (8082) | Ân | heritage_sites |
| `03_content.sql` | `content_db` | content-service (8083) | Xuân Anh | artifacts, artifact_contents, media_resources |
| `04_beacon.sql` | `beacon_db` | beacon-service (8084) | Khoa | ibeacons |
| `05_personalization.sql` | `personalization_db` | personalization-service (8085, tuần 5) | Phú | visits, likes |
| `06_feedback.sql` | `feedback_db` | feedback-service (8086, tuần 5) | Tường | feedbacks |
| `07_analytics.sql` | `analytics_db` | analytics-service (8087, tuần 5) / NiFi | Tuấn | fact_visits, fact_feedbacks, dim_artifact, agg_daily_views |
| `99_init_db_legacy.sql` | `smart_heritage_db` | Backend cũ (8090) | — | Toàn bộ bảng cũ (giữ đến hết tuần 5) |

## Quy ước

- **Không có khóa ngoại chéo schema.** Cột tham chiếu sang service khác là `BIGINT` thường
  (vd `artifacts.site_id`, `feedbacks.user_id`). Cần kiểm tra thì service gọi REST sang service sở hữu dữ liệu.
  Khóa ngoại trong cùng schema vẫn giữ (vd `artifact_contents.artifact_id -> artifacts.id`).
- **Id dữ liệu mẫu giữ nguyên giữa các schema:** user 1..4 (`admin`, `visitor_an`, `visitor_mai`, `visitor_john`),
  di tích 1..2, hiện vật 1..3, beacon 1..4. Ví dụ hiện vật id 1 là "Khuê Văn Các" ở `content_db`, `beacon_db`,
  `personalization_db`, `feedback_db`, `smart_heritage_db`.
- Mật khẩu mọi tài khoản mẫu: `Test@1234`.
- **Chỉ Tường sửa các file trong thư mục này.** Cần đổi bảng/cột thì nhắn Tường (giữ quy định từ tuần 3).

## User MySQL (chỉ dùng cho máy dev)

| Schema | User | Mật khẩu |
|---|---|---|
| `auth_db` | `auth_user` | `auth_pass_dev` |
| `site_db` | `site_user` | `site_pass_dev` |
| `content_db` | `content_user` | `content_pass_dev` |
| `beacon_db` | `beacon_user` | `beacon_pass_dev` |
| `personalization_db` | `personalization_user` | `personalization_pass_dev` |
| `feedback_db` | `feedback_user` | `feedback_pass_dev` |
| `analytics_db` | `analytics_user` | `analytics_pass_dev` |

Backend cũ và NiFi vẫn dùng `root` trong tuần 4.

### Cho service dùng schema và user riêng

Template (`services/service-template/.../application.yml`) đọc `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`.
Ví dụ với site-service:

```
DB_URL=jdbc:mysql://localhost:3307/site_db?useSSL=false&allowPublicKeyRetrieval=true
DB_USERNAME=site_user
DB_PASSWORD=site_pass_dev
```

Trong docker-compose, đổi `localhost:3307` thành `mysql-db:3306`.
auth-service dùng `AUTH_DB_URL`, `AUTH_DB_USERNAME`, `AUTH_DB_PASSWORD`. Tuần 4 auth-service vẫn trỏ `smart_heritage_db`
(xem `Docs/architecture/microservices.md` mục 5); khi chuyển sang `auth_db` thì Bảo chỉ cần đặt 3 biến trên.

## Chạy lại database từ đầu

Mỗi lần pull code có thay đổi trong `Infra/db` (lệnh này **xóa toàn bộ dữ liệu cũ**):

```bash
docker compose -f Infra/docker-compose.yml down -v
docker compose -f Infra/docker-compose.yml up -d
```

## Kiểm tra sau khi chạy

```bash
# Có đủ 7 schema mới + smart_heritage_db
docker exec smart_heritage_mysql mysql -uroot -prootpassword123 -e "SHOW DATABASES"

# Script có lỗi không (MySQL dừng nếu 1 file lỗi)
docker logs smart_heritage_mysql 2>&1 | grep -i error

# User chỉ đọc được schema của mình: lệnh 1 chạy được, lệnh 2 báo ERROR 1142 (denied)
docker exec smart_heritage_mysql mysql -usite_user -psite_pass_dev -e "SELECT COUNT(*) FROM site_db.heritage_sites"
docker exec smart_heritage_mysql mysql -usite_user -psite_pass_dev -e "SELECT COUNT(*) FROM content_db.artifacts"
```

Database đang chạy từ trước bản này mà không muốn `down -v`: xem `Infra/personalization_migration.sql`
(chỉ thêm cột cho bảng `visits` của `smart_heritage_db`).
