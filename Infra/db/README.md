# Script database theo từng schema (Tường – Sprint 4)

Từ tuần 4, mỗi microservice có **schema riêng** và **user MySQL riêng**. User chỉ được cấp quyền trên schema của mình.
`Infra/docker-compose.yml` mount cả thư mục này vào `/docker-entrypoint-initdb.d`, nên MySQL chạy các file
**theo thứ tự tên file**, và **chỉ chạy khi volume MySQL còn trống**.

| File | Schema | Service (cổng) | Phụ trách | Bảng |
|---|---|---|---|---|
| `00_create_schemas.sql` | 7 schema bên dưới | — | Tường | Tạo schema, user, GRANT |
| `01_auth.sql` | `auth_db` | auth-service (8081) | Bảo | users, refresh_tokens |
| `02_site.sql` | `site_db` | site-service (8082) | Ân | heritage_sites |
| `03_content.sql` | `content_db` | content-service (8083) | Xuân Anh | artifacts, artifact_contents, media_resources, artifact_event_outbox |
| `04_beacon.sql` | `beacon_db` | beacon-service (8084) | Khoa | ibeacons |
| `05_personalization.sql` | `personalization_db` | personalization-service (8085, tuần 5) | Phú | visits, likes |
| `06_feedback.sql` | `feedback_db` | feedback-service (8086, tuần 5) | Tường | feedbacks |
| `07_analytics.sql` | `analytics_db` | analytics-service (8087, tuần 5) / NiFi | Tuấn | fact_visits, fact_feedbacks, dim_artifact, agg_daily_views |
| `09_demo_seed.sql` | `content_db` | content-service (8083) | Tường (dữ liệu: Xuân Anh) | Dữ liệu demo bản cuối: thay hiện vật mẫu bằng 6 hiện vật; media ở `Infra/seed/media/` (xem [`Infra/seed/README.md`](../seed/README.md)) |
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

## Tuần 7: cột `users.locked` (trạng thái tài khoản)

`users.locked TINYINT(1) NOT NULL DEFAULT 0`: 0 = đang hoạt động, 1 = bị khoá. ADMIN khoá/mở trên trang quản lý
người dùng (`PATCH /api/admin/users/{id}/status`); tài khoản bị khoá không đăng nhập, không làm mới token được.
Cột có ở cả `01_auth.sql` (`auth_db`) và `99_init_db_legacy.sql` (`smart_heritage_db`, auth-service đang dùng).

Database đã có dữ liệu mà không muốn `down -v` thì thêm cột bằng tay
(báo `Duplicate column name 'locked'` nghĩa là cột đã có, bỏ qua):

```bash
docker exec smart_heritage_mysql mysql -uroot -prootpassword123 -e "ALTER TABLE smart_heritage_db.users ADD COLUMN locked TINYINT(1) NOT NULL DEFAULT 0 AFTER role"
docker exec smart_heritage_mysql mysql -uroot -prootpassword123 -e "ALTER TABLE auth_db.users ADD COLUMN locked TINYINT(1) NOT NULL DEFAULT 0 AFTER role"
```

## Tuần 7: bảng `content_db.artifact_event_outbox` đưa vào script

Bảng outbox của sự kiện ArtifactUpdated (Xuân Anh, tuần 6) trước đây chỉ do content-service tự tạo (`ddl-auto: update`).
Nay có trong `03_content.sql`, giống hệt `services/content-service/db/artifact_event_outbox.sql`, nên máy mới tạo DB là có
bảng ngay. DB đang chạy thì không cần làm gì (bảng đã được service tạo); nếu thiếu, chạy:

```bash
docker exec -i smart_heritage_mysql mysql -uroot -prootpassword123 < services/content-service/db/artifact_event_outbox.sql
```

## Sửa lỗi 22/11: tên ngôn ngữ "Tiếng Việt" bị lỗi dấu

`08_languages.sql` thiếu `SET NAMES utf8mb4` nên khi Docker tạo DB, tên `vi` bị lưu thành "Tiáº¿ng Viá»‡t"
(thấy ở mục Ngôn ngữ hỗ trợ trên trang Hiện vật). Đã thêm dòng này. DB tạo mới (`down -v`) tự đúng; DB đang chạy
(ví dụ máy demo đã nạp dữ liệu) sửa bằng 1 lệnh, không mất dữ liệu (giá trị viết dạng hex = "Tiếng Việt" để PowerShell không làm hỏng dấu):

```bash
docker exec smart_heritage_mysql mysql -uroot -prootpassword123 --default-character-set=utf8mb4 -e "UPDATE content_db.languages SET name = CONVERT(X'5469E1BABF6E67205669E1BB8774' USING utf8mb4) WHERE code = 'vi'"
```
