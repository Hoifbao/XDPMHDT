# Dựng Apache NiFi và luồng đồng bộ sang `analytics_db`

> Người viết: Nguyễn Thanh Tuấn (mảng Dashboard Thống kê & Xuất báo cáo).
> Sprint 4: luồng `visits` → `fact_visits`. Sprint 5: 5 luồng, mỗi DB của service một nguồn riêng.
> Đã chạy thử trên máy Windows 11 + Docker Desktop ngày 01/11/2026.
> Tài liệu nền: [api-gateway-nifi.md](api-gateway-nifi.md) (tìm hiểu NiFi ở Sprint 2).

## 1. Luồng làm gì

NiFi chép dữ liệu từ DB của từng service sang `analytics_db`. analytics-service (cổng 8087) chỉ đọc `analytics_db`,
không JOIN sang DB của service khác.

| Nguồn | Đích (`analytics_db`) | Chu kỳ | Chống trùng |
|---|---|---|---|
| `personalization_db.visits` | `fact_visits` | 1 phút | Đọc `visit_id` lớn hơn lần trước; khóa chính `visit_id` + UPSERT |
| `feedback_db.feedbacks` | `fact_feedbacks` | 1 phút | Đọc `updated_at` lớn hơn lần trước (bắt cả lúc sửa đánh giá, đổi trạng thái); UPSERT theo `feedback_id` |
| `content_db.artifacts` | `dim_artifact` | 5 phút | Đọc lại toàn bộ, UPSERT theo `artifact_id` (ghi đè) |
| `site_db.heritage_sites` | `dim_site` | 5 phút | Đọc lại toàn bộ, UPSERT theo `site_id` |
| `users` (chỉ `id`, `preferred_language`) | `dim_user_lang` | 5 phút | Đọc lại toàn bộ, UPSERT theo `user_id`; không chép email, mật khẩu |

```
personalization_db.visits ──[Doc visits]────────> [Ghi fact_visits]    ─┐
feedback_db.feedbacks     ──[Doc feedbacks]─────> [Ghi fact_feedbacks] ─┤
content_db.artifacts      ──[Doc artifacts]─────> [Ghi dim_artifact]   ─┼─ failure/retry ─> Funnel (xem lỗi)
site_db.heritage_sites    ──[Doc heritage_sites]> [Ghi dim_site]       ─┤
users                     ──[Doc users]─────────> [Ghi dim_user_lang]  ─┘
  QueryDatabaseTableRecord                          PutDatabaseRecord (UPSERT)
```

`site_id` và ngôn ngữ của lượt tham quan không chép vào `fact_visits` nữa (cột để NULL):
analytics-service lấy qua `dim_artifact.site_id` và `dim_user_lang` khi tính số liệu.

## 2. Các file liên quan

| File | Nội dung |
|---|---|
| `Infra/docker-compose.yml` | Service `nifi` trong profile `nifi` (Bảo dựng): `apache/nifi:1.28.1`, cổng 8443 |
| `Infra/.env.example` | `NIFI_USERNAME`, `NIFI_PASSWORD`, `MYSQL_ROOT_PASSWORD` |
| `Infra/nifi/drivers/` | Chỗ đặt MySQL JDBC driver (file `.jar` không commit) |
| `Infra/db/07_analytics.sql` | Tạo `analytics_db` và các bảng `fact_*`, `dim_*` (Tường quản lý) |
| `Infra/nifi/flows/analytics-sync.json` | Flow NiFi đã xuất, dùng để import |

## 3. Yêu cầu máy

- Docker Desktop đang chạy. NiFi dùng khoảng **1,2 GB RAM** (heap tối đa 1 GB, cấu hình sẵn trong compose).
- Máy nên có từ 8 GB RAM. Chỉ bật profile `nifi` khi cần, làm xong thì tắt.
- Lần đầu kéo image `apache/nifi:1.28.1` khoảng 1,5 GB.

## 4. Các bước dựng lại trên máy khác (PowerShell, ở thư mục gốc repo)

### Bước 1: Lấy code và (tùy chọn) tạo `.env`

```powershell
git pull
Copy-Item Infra/.env.example Infra/.env   # tùy chọn; không có .env thì dùng mật khẩu mặc định
```

### Bước 2: Tải MySQL JDBC driver

```powershell
Invoke-WebRequest -Uri https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/8.4.0/mysql-connector-j-8.4.0.jar -OutFile Infra/nifi/drivers/mysql-connector-j-8.4.0.jar
```

Flow đang trỏ đúng tên file `mysql-connector-j-8.4.0.jar`. Dùng bản khác thì sửa tham số `driver_location` ở Bước 5.

### Bước 3: Bật MySQL, Redis và NiFi

```powershell
docker compose -f Infra/docker-compose.yml --profile nifi up -d
```

Docker tự chạy các script trong `Infra/db/` khi volume MySQL trống, nên `analytics_db` có sẵn.
NiFi cần khoảng 30–60 giây để khởi động.

### Bước 4: Đăng nhập NiFi và import flow

1. Mở <https://localhost:8443/nifi>. Trình duyệt báo chứng chỉ không tin cậy (NiFi tự ký): chọn **Advanced → Proceed**.
2. Đăng nhập `admin` / `change-me-nifi-123` (hoặc giá trị `NIFI_USERNAME`, `NIFI_PASSWORD` trong `Infra/.env`).
3. Kéo biểu tượng **Process Group** ở thanh công cụ trên cùng thả vào màn hình.
4. Bấm biểu tượng **Browse** bên phải ô tên, chọn `Infra/nifi/flows/analytics-sync.json`, bấm **Add**.

Máy đã import flow tuần 4 (**Tuan - visits to analytics_db**): chuột phải vào group cũ → **Stop**, rồi xóa group đó,
để 2 luồng không cùng ghi vào `fact_visits`.

### Bước 5: Nhập mật khẩu DB

File flow **không chứa mật khẩu** (NiFi bỏ giá trị nhạy cảm khi xuất).

1. Chuột phải vào process group **Tuan - dong bo analytics_db** → **Parameters**.
2. Sửa `db_password` = mật khẩu MySQL (`rootpassword123` hoặc `MYSQL_ROOT_PASSWORD` trong `Infra/.env`) → **Apply**.

| Tham số | Giá trị mặc định | Ghi chú |
|---|---|---|
| `personalization_db_url` | `jdbc:mysql://mysql-db:3306/personalization_db?...` | Nguồn `visits` (Phú) |
| `feedback_db_url` | `jdbc:mysql://mysql-db:3306/feedback_db?...` | Nguồn `feedbacks` (Tường) |
| `content_db_url` | `jdbc:mysql://mysql-db:3306/content_db?...` | Nguồn `artifacts` (Xuân Anh) |
| `site_db_url` | `jdbc:mysql://mysql-db:3306/site_db?...` | Nguồn `heritage_sites` (Ân) |
| `users_db_url` | `jdbc:mysql://mysql-db:3306/smart_heritage_db?...` | Tuần 5 auth-service vẫn dùng `smart_heritage_db`. Khi chuyển sang `auth_db` thì chỉ đổi tên DB ở đây |
| `analytics_db_url` | `jdbc:mysql://mysql-db:3306/analytics_db?...` | Đích |
| `db_user` | `root` | Đọc được mọi DB nguồn; user `analytics_user` chỉ có quyền trên `analytics_db` |
| `db_password` | (trống, phải nhập) | Nhạy cảm |
| `driver_location` | `/opt/nifi/nifi-current/drivers/mysql-connector-j-8.4.0.jar` | Đường dẫn bên trong container |

`mysql-db` là tên service trong compose, không dùng `localhost`.

### Bước 6: Bật và chạy

1. Chuột phải vào process group → **Enable all controller services**.
2. Chuột phải lần nữa → **Start**.

### Bước 7: Kiểm tra kết quả

```powershell
docker exec smart_heritage_mysql mysql -uroot -prootpassword123 -e "SELECT 'fact_visits', COUNT(*) FROM analytics_db.fact_visits UNION ALL SELECT 'fact_feedbacks', COUNT(*) FROM analytics_db.fact_feedbacks UNION ALL SELECT 'dim_artifact', COUNT(*) FROM analytics_db.dim_artifact UNION ALL SELECT 'dim_site', COUNT(*) FROM analytics_db.dim_site UNION ALL SELECT 'dim_user_lang', COUNT(*) FROM analytics_db.dim_user_lang"
```

Với dữ liệu mẫu trong `Infra/db/` phải thấy: 4, 6, 3, 2, 4.

### Tắt NiFi khi xong

```powershell
docker compose -f Infra/docker-compose.yml --profile nifi stop nifi
```

Flow được lưu trong volume `nifi_conf`, bật lại không phải import lại. Chỉ mất khi chạy `down -v`.

## 5. Cấu hình chính của flow

| Thành phần | Thuộc tính | Giá trị | Lý do |
|---|---|---|---|
| `Doc visits` (`QueryDatabaseTableRecord`) | Custom Query | `SELECT id AS visit_id, user_id, artifact_id, detected_at, duration_seconds FROM visits` | Chỉ đọc bảng của 1 service, không JOIN |
| | Maximum-value Columns | `visit_id` | Chỉ đọc dòng mới |
| `Doc feedbacks` | Custom Query | `SELECT id AS feedback_id, artifact_id, rating, status, created_at, updated_at FROM feedbacks` | |
| | Maximum-value Columns | `updated_at` | Bắt cả đánh giá bị sửa hoặc ADMIN đổi trạng thái (Tường thêm cột này ở tuần 5) |
| `Doc artifacts`, `Doc heritage_sites`, `Doc users` | Maximum-value Columns | (trống) | Bảng danh mục nhỏ: mỗi 5 phút đọc lại hết rồi ghi đè |
| `Doc users` | Custom Query | `SELECT id AS user_id, COALESCE(preferred_language, 'vi') AS language_code FROM users` | Chỉ lấy id và ngôn ngữ |
| Mọi bộ đọc | Use Avro Logical Types | `true` | Giữ `DATETIME` đúng kiểu thời gian, không lệch giờ |
| `Ghi ...` (`PutDatabaseRecord`) | Statement Type | `UPSERT` | Trùng khóa chính thì cập nhật, không thêm dòng |
| | Unmatched Column Behavior | `Ignore Unmatched Columns` | Cột đích không có ở nguồn (vd `synced_at`) thì bỏ qua |
| Controller services | 5 `MySQL nguon - ...` + `MySQL dich - analytics_db` (`DBCPConnectionPool`), `Avro writer`, `Avro reader` | | Mỗi DB nguồn một kết nối: DB nào đổi chỗ thì chỉ sửa 1 tham số |

### Chống nhân đôi dữ liệu (2 lớp)

1. **Trạng thái NiFi:** `Doc visits` nhớ `visit_id` lớn nhất, `Doc feedbacks` nhớ `updated_at` lớn nhất
   (chuột phải processor → **View state**). Lần sau chỉ đọc dòng lớn hơn.
2. **Khóa chính bảng đích + `UPSERT`:** nếu trạng thái bị xóa và NiFi đọc lại từ đầu, dòng đã có chỉ bị ghi đè.

## 6. Kết quả chạy thử (01/11/2026)

| Kiểm tra | Kết quả |
|---|---|
| Lần chạy đầu sau `down -v` → `up -d` | `fact_visits` 4, `fact_feedbacks` 6, `dim_artifact` 3, `dim_site` 2, `dim_user_lang` 4; hàng đợi lỗi trống |
| So với nguồn (JOIN theo id, so từng cột) | 0 dòng lệch ở `visits`, `feedbacks`; tên tiếng Việt `dim_artifact`, `dim_site` khớp từng byte |
| Thêm 1 dòng `visits` (`duration_seconds` NULL) | Sau khoảng 20 giây: `fact_visits` 5 dòng, dòng mới giữ NULL |
| ADMIN duyệt feedback 3 (`PENDING` → `APPROVED`, `updated_at` đổi) | Sau khoảng 20 giây: `fact_feedbacks.status` = `APPROVED` |
| Xóa trạng thái của cả 5 bộ đọc rồi chạy lại | Vẫn 5 / 6 / 3 / 4 dòng, không trùng khóa |
| Import lại file `analytics-sync.json` vào NiFi | Đủ 10 processor |

## 7. Lỗi thường gặp

| Hiện tượng | Nguyên nhân | Cách xử lý |
|---|---|---|
| Controller service báo không tìm thấy driver | Chưa tải `.jar` hoặc sai tên file | Làm lại Bước 2; kiểm tra `docker exec smart_heritage_nifi ls /opt/nifi/nifi-current/drivers` (Git Bash cần thêm `MSYS_NO_PATHCONV=1` trước lệnh) |
| `Communications link failure` | URL dùng `localhost` hoặc MySQL chưa chạy | URL phải là `mysql-db:3306`; kiểm tra `docker ps` |
| `Access denied for user 'root'` | Chưa nhập hoặc nhập sai `db_password` | Làm lại Bước 5 |
| `Table 'analytics_db.dim_site' doesn't exist` | Volume MySQL tạo trước khi có bảng mới trong `Infra/db/07_analytics.sql` | `docker compose -f Infra/docker-compose.yml down -v` rồi `up -d` (xóa dữ liệu thử) |
| `Unknown column 'updated_at'` ở `Doc feedbacks` | `feedback_db` tạo trước tuần 5 | Như trên |
| Funnel có hàng chờ | Ghi vào `analytics_db` lỗi | Chuột phải hàng đợi → **List queue** xem lỗi; sửa xong chuột phải → **Empty queue** |
| Muốn đồng bộ lại từ đầu | | Dừng processor → chuột phải → **View state** → **Clear state** → Start |
| Không đăng nhập được NiFi | Mật khẩu ngắn hơn 12 ký tự | `NIFI_PASSWORD` trong `.env` phải từ 12 ký tự |

## 8. Việc còn lại

- Khi personalization-service (Phú) ghi lượt tham quan vào `personalization_db`, luồng `visits` tự lấy được, không phải sửa.
- Khi auth-service chuyển sang `auth_db`: đổi `users_db_url` sang `.../auth_db?...`.
