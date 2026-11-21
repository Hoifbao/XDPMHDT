# Dữ liệu demo bản cuối (Tường – tuần 7)

| Thành phần | Vị trí | Nội dung |
|---|---|---|
| Dữ liệu | [`Infra/db/09_demo_seed.sql`](../db/09_demo_seed.sql) | `content_db`: 6 hiện vật (2 di tích), 12 bản dịch Việt/Anh, 6 ảnh minh hoạ |
| Media | [`media/`](media/) | 18 file: mỗi hiện vật 1 ảnh lớn, 1 ảnh nhỏ (`thumbnailUrl`), 1 audio thuyết minh tiếng Anh (`.wav`) |
| Nguồn gốc ảnh, audio | [`services/content-service/demo-data/media-credits.json`](../../services/content-service/demo-data/media-credits.json) | Tác giả, giấy phép từng ảnh (Xuân Anh) |

Tên file media là UUID do API upload đặt, trùng đúng tên trong các URL `http://localhost:8080/api/media/files/<tên>` của seed.
**Không đổi tên file**: đổi thì URL trong seed không còn khớp.

## Nạp vào hệ thống

1. `Infra/db/09_demo_seed.sql` tự chạy khi volume MySQL còn trống (lần đầu `up`, hoặc sau `down -v`), sau `03_content.sql`.
2. Khi content-service đã chạy, chép media vào thư mục upload (chạy lại nhiều lần không sao):

```bash
# Cách 1 (Docker), ở thư mục gốc repo
docker cp Infra/seed/media/. smart_heritage_content:/app/uploads/
# Cách 2 (Maven/start-local), PowerShell
Copy-Item Infra/seed/media/* services/content-service/uploads/ -Force
```

3. Kiểm tra: `http://localhost:8080/api/artifacts` có 6 hiện vật; mở một URL ảnh trong kết quả trên trình duyệt phải thấy ảnh.

## Tạo lại seed khi Xuân Anh đổi `demo-data`

1. `docker compose -f Infra/docker-compose.yml --profile core --profile mq down -v`, rồi `… up -d --build`, chờ đủ *healthy*.
2. Chạy `services/content-service/scripts/import-demo-data.ps1` (qua Gateway 8080). Lưu ý (19/11): script dừng ở bước
   kiểm tra tên di tích khi chạy bằng Windows PowerShell 5.1, vì API không ghi `charset` nên tiếng Việt bị đọc sai;
   dùng PowerShell 7 (`pwsh`) hoặc bản đã sửa của Xuân Anh.
3. Dump 3 bảng nội dung:
   ```bash
   docker exec smart_heritage_mysql mysqldump -uroot -prootpassword123 --no-create-info --skip-extended-insert \
     --complete-insert --skip-comments --default-character-set=utf8mb4 content_db artifacts artifact_contents media_resources
   ```
   Đưa các câu `INSERT` vào `09_demo_seed.sql` (giữ phần đầu file: `DELETE` 3 bảng, `AUTO_INCREMENT`). Đổi tiền tố
   `http://content-service:8083/api/media/files/` thành `http://localhost:8080/api/media/files/` (upload qua Gateway trong Docker
   trả tên máy nội bộ).
4. Xoá `Infra/seed/media/*` cũ, chép lại: `docker cp smart_heritage_content:/app/uploads/. Infra/seed/media/`.
5. Đối chiếu: mỗi tên file trong URL của seed có đúng 1 file trong `media/`, không có file thừa.
6. Xoá `services/content-service/demo-data/imported-records.json` (file tạm của script import, không commit).

## Ghi chú

- Id 1..3 (Khuê Văn Các, Bia Tiến sĩ, Đoan Môn) giữ nguyên như dữ liệu mẫu, nên `beacon_db`, `feedback_db`,
  `personalization_db` vẫn khớp. Hiện vật mới id 4..6 (Đại Trung Môn, Nền điện Kính Thiên, Cột cờ Hà Nội) chưa gắn beacon.
- Chưa có thuyết minh tiếng Việt dạng audio (manifest của Xuân Anh); bản tiếng Việt chỉ có chữ và ảnh.
- Không chạy `down -v` trên máy demo sau khi đã nạp dữ liệu: lệnh này xoá cả MySQL và media đã chép.
