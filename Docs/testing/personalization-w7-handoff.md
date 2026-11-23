# XDPMHDT-99 — các bước chốt phần tuần 7 của Phú

PR: [#88](https://github.com/Hoifbao/XDPMHDT/pull/88). Nhánh: `fix/XDPMHDT-99-phu-w7-personalization` trong fork `phuht0462-a11y/XDPMHDT`.
Kết quả thực tế và những mục chưa đạt nằm ở [báo cáo kiểm thử](personalization-w7.md). Không đánh dấu Jira Done chỉ vì build thành công.

## 1. Lấy đúng nhánh và cập nhật theo nhóm trưởng

Trong PowerShell tại thư mục gốc dự án, chạy `git status --short`. Nếu có thay đổi của bạn, lưu/commit chúng trước khi chuyển nhánh; không reset hoặc ghi đè.

Nếu máy **chưa có** nhánh tuần 7 này:

```powershell
git fetch https://github.com/phuht0462-a11y/XDPMHDT.git fix/XDPMHDT-99-phu-w7-personalization
git switch -c fix/XDPMHDT-99-phu-w7-personalization FETCH_HEAD
```

Nếu nhánh **đã có**, chuyển sang nhánh rồi cập nhật từ fork:

```powershell
git switch fix/XDPMHDT-99-phu-w7-personalization
git fetch https://github.com/phuht0462-a11y/XDPMHDT.git fix/XDPMHDT-99-phu-w7-personalization
git merge --ff-only FETCH_HEAD
```

Lệnh trên chỉ fast-forward đúng nhánh tuần 7, không tạo merge commit. Nếu báo không fast-forward được, dừng để đối chiếu commit trên máy; không force/reset.

Sau đó cập nhật nền mã nhóm theo lời nhắc ngày 20/11:

```powershell
git fetch origin
git rebase origin/main
```

Ở đây `origin` phải là `https://github.com/Hoifbao/XDPMHDT.git` như máy Phú đã kiểm tra. Không dùng `git merge origin/main`. Nền mã đã kiểm chứng là `c6ec6c8`, có cả PR #86 và #87; nhánh không bị tụt sau main tại thời điểm kiểm tra.

## 2. Bật hệ thống và WebAdmin

Giữ Docker Desktop chạy. Dừng các cửa sổ `mvn spring-boot:run` đang chiếm cổng 8080–8087/8090 trước khi dùng toàn bộ Compose. Giữ nguyên `Infra/.env` và volume đang có.

Dashboard cần NiFi; chuẩn bị driver nếu chưa có:

```powershell
if (-not (Test-Path Infra/nifi/drivers/mysql-connector-j-8.4.0.jar)) {
    New-Item -ItemType Directory -Force Infra/nifi/drivers | Out-Null
    Invoke-WebRequest -Uri "https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/8.4.0/mysql-connector-j-8.4.0.jar" -OutFile "Infra/nifi/drivers/mysql-connector-j-8.4.0.jar"
}
docker compose -f Infra/docker-compose.yml --profile all --profile nifi up -d --build
docker compose -f Infra/docker-compose.yml --profile all --profile nifi ps
```

Mở **https://localhost:8443/nifi** bằng tài khoản NiFi của môi trường. Nếu chưa có flow, import `Infra/nifi/flows/analytics-sync.json`; nếu đã có thì dùng group hiện hữu, không tạo hai bản chạy song song. Đặt parameter `db_password` đúng mật khẩu MySQL của môi trường, Enable controller services rồi Start. Đối chiếu cấu hình trong [hướng dẫn NiFi](../research/nifi-setup.md). Không reset database để làm số liệu xuất hiện; nếu thiếu bảng, phối hợp Tường chạy migration phù hợp.

Trong **tab PowerShell riêng**, từ thư mục gốc:

```powershell
cd WebAdmin
python -m http.server 5500
```

Mở **http://localhost:5500/src/pages/login.html**. Không kéo file HTML vào Chrome hoặc dùng `file://`, vì bản đồ OSM bị chặn theo lời nhóm trưởng. Sau khi flow chạy, kiểm tra Dashboard; các dim chạy chu kỳ 5 phút nên không mặc định phải có đủ số liệu ngay khi vừa Start.

## 3. Kiểm tra bản tuần 7

Tại thư mục gốc:

```powershell
mvn -pl services/personalization-service -am test
cd Interface
flutter pub get
flutter test test/features/personalization/personalization_offline_test.dart test/visit_history_grouping_test.dart test/personalization_screens_test.dart
flutter analyze
```

Không đồng nhất lint toàn ứng dụng với kết quả test; xem chi tiết CI trong báo cáo. Không sửa module của thành viên khác chỉ để ẩn warning.

Kiểm tra API tích hợp trên môi trường thử đang chạy:

```powershell
python services/personalization-service/scripts/smoke_live.py --output phu-w7-live-smoke.json
```

Lệnh này chạy ở **gốc repo**, tạo hai tài khoản thử ngẫu nhiên, hai yêu thích và ba lượt tham quan; cần dữ liệu Content có ít nhất ba hiện vật ACTIVE và tài khoản admin demo. Có thể đặt `SH_SMOKE_ADMIN_USER` / `SH_SMOKE_ADMIN_PASSWORD` cho môi trường riêng. Không đưa token/mật khẩu/file log nhạy cảm lên Git. Chỉ thêm `--chaos` trong môi trường thử được phép tạm dừng Redis/Content; script sẽ bật lại hai service khi kết thúc.

DB có dữ liệu cần Tường review/chạy `Infra/personalization_w7_indexes.sql` trên schema hiện hữu. Không dùng `down -v` hoặc seed lại dữ liệu demo. Migration chạy lại được và chỉ bổ sung index của Phú.

## 4. Phần phải nghiệm thu trên điện thoại và cùng nhóm

| Thao tác | Bằng chứng cần lưu |
|---|---|
| Có mạng, đăng nhập và mở dữ liệu hiện vật | App dùng đúng Gateway, đúng tài khoản thử |
| Ngắt mạng, ghi 3 lượt beacon và 2 yêu thích | Video hiển thị đủ dữ liệu local và trạng thái chờ |
| Bật mạng, để app foreground/resume | Server có đúng 3 UUID và 2 yêu thích; hàng chờ hết |
| Mất phản hồi giữa lúc gửi rồi thử lại | UUID không đổi; DB và Analytics không tăng trùng |
| Đóng/mở app khi còn hàng chờ | Dữ liệu chờ vẫn còn, đồng bộ tiếp sau đăng nhập |
| Đổi tài khoản | Không đọc/gửi hàng chờ của tài khoản cũ dưới tài khoản mới |
| Dừng Redis trên môi trường thử | Đọc/ghi qua Content vẫn hoạt động, không 500 |

Điện thoại thật phải trỏ `API_BASE_URL` tới IP LAN của máy chạy Gateway (cấu hình trong `AuthService`); `localhost` trên điện thoại là chính điện thoại. Giữ cùng mạng và quyền BLE theo hướng dẫn của Khoa.

Bảo review phần tích hợp `PersonalizationSync` trong `Interface/lib/main.dart`; Tường review migration; Ân nhận kết quả vào Test Report. Phú tập phần hỏi đáp trong [tài liệu SRS/báo cáo](../requirements/personalization-w7.md). Sau khi đạt những mục này và được review, chuyển PR sang Ready for review; nhóm trưởng quyết định squash merge rồi mới chốt Jira Done.

## 5. Nhánh cũ

File phân công yêu cầu dọn `Hoifbao/XDPMHDT:feature/personalization-history-favorites-redis`, đã được thay bởi PR #63/#64 (cả hai đã merge). Tip kiểm tra: `18397a3fa3520a84bbdee29cf9e06013da755b0b`, ngày 27/10/2026. Đã lưu tham chiếu local `archive/phu-pre-w7-personalization` để đối chiếu; chưa xóa nhánh từ xa. Maintainer cần xác nhận đúng nhánh cũ trước khi xóa, không xóa nhánh `fix/XDPMHDT-99-phu-w7-personalization` đang review. Không đưa lịch sử nhánh cũ vào main/nhánh tuần 7.
