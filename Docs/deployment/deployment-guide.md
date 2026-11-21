# Hướng dẫn triển khai (Deployment Guide) – Smart Heritage

| Mục | Nội dung |
|---|---|
| Phiên bản | Bản cuối – Sprint 7 (XDPMHDT-94, XDPMHDT-96) |
| Áp dụng cho | `main` sau khi merge patch W7 04 (Docker đủ 7 service, profile `all`, `start-local.ps1` mở 9 cửa sổ). Trước khi merge, cách 1 chưa chạy được đủ 7 service |
| Môi trường đã kiểm | Windows 11 + Docker Desktop + PowerShell 5.1 (máy Bảo). Chạy thử máy sạch: Tuấn (T3 17/11) – kết quả ghi ở mục 9 |
| Tài liệu liên quan | [Kiến trúc](../architecture/architecture.md), [README gốc](../../readme.md), [NiFi](../research/nifi-setup.md), [Giám sát](../../Infra/monitoring/README.md) |

Có 2 cách chạy:

| Cách | Dùng khi | Máy cần |
|---|---|---|
| **1. Toàn bộ trong Docker (1 lệnh)** | Demo, bảo vệ, máy chưa cài gì, triển khai server | Docker |
| **2. Chạy service bằng Maven** | Phát triển, sửa code, xem log từng service | Docker, JDK 17+, Maven |

App Mobile và Web Admin dùng chung cho cả 2 cách (mục 5, 6).

## 1. Yêu cầu máy

| Thành phần | Phiên bản | Ghi chú |
|---|---|---|
| Docker Desktop | Bản mới, có Compose v2 (`docker compose`) | Cấp ≥ 8 GB RAM cho Docker khi chạy profile `all` |
| Git | Bất kỳ | Lấy mã nguồn |
| RAM / ổ đĩa | ≥ 16 GB RAM; ≥ 15 GB trống | 11 container Java + MySQL + RabbitMQ + giám sát |
| JDK, Maven | JDK 17+ (đã chạy JDK 25), Maven 3.9 | Chỉ cần cho cách 2 |
| Flutter | 3.x | Chạy App |
| Trình duyệt | Chrome hoặc Edge | Web Admin, Swagger, Grafana |

Cổng phải trống: 8080–8087, 8090, 3307, 6379, 5672, 15672, 15692, 9090, 3000, 9104, 9121 (thêm 8443 nếu bật NiFi).

Lưu ý PowerShell 5.1: không dùng `&&`; gõ từng lệnh một dòng.

## 2. Lấy mã nguồn và cấu hình

```powershell
git clone https://github.com/Hoifbao/XDPMHDT.git
cd XDPMHDT
git checkout v1.0          # bản nộp; bỏ dòng này nếu muốn main mới nhất
Copy-Item Infra\.env.example Infra\.env
```

Mở `Infra\.env` và đổi các giá trị `change-me…`. Biến chính:

| Biến | Dùng cho | Ghi chú |
|---|---|---|
| `MYSQL_ROOT_PASSWORD` | MySQL và các service | Chỉ có tác dụng ở lần tạo volume đầu tiên |
| `JWT_SECRET` | Gateway, auth-service, Backend cũ | ≥ 32 ký tự; 3 nơi phải giống nhau (compose tự truyền) |
| `RABBITMQ_USER`, `RABBITMQ_PASSWORD` | RabbitMQ và service gửi/nhận sự kiện | UI http://localhost:15672 |
| `GRAFANA_ADMIN_PASSWORD` | Grafana | Tài khoản `admin` |
| `FIREBASE_SERVICE_ACCOUNT` | beacon-service gửi FCM | Đường dẫn file khoá trong container; để trống thì không gửi thông báo thật, chức năng khác vẫn chạy |
| `NIFI_USERNAME`, `NIFI_PASSWORD` | NiFi | Mật khẩu ≥ 12 ký tự |

Không commit `Infra\.env` và file khoá Firebase.

## 3. Cách 1 – Chạy toàn bộ bằng Docker

```powershell
docker compose -f Infra/docker-compose.yml --profile all up -d --build
docker compose -f Infra/docker-compose.yml ps
```

- Lần đầu build 5–10 phút (tải image, build 8 ứng dụng Java). Các lần sau vài chục giây.
- **Đúng khi:** `ps` liệt kê MySQL, Redis, RabbitMQ, Backend cũ, 7 service, Gateway, Prometheus, Grafana, 2 exporter; cột STATUS các container có healthcheck là `healthy`. Gateway bật sau cùng (chờ các service *healthy*).
- Profile `all` = `core` + `mq` + `monitoring`; **không gồm NiFi** (mục 7).
- Đang chạy service bằng `mvn spring-boot:run` thì tắt trước (trùng cổng).

Kiểm tra nhanh:

```powershell
Invoke-RestMethod http://localhost:8080/api/heritage-sites
Invoke-RestMethod http://localhost:8080/api/ibeacons/registry
$gp = ((Get-Content Infra\.env | Select-String '^GRAFANA_ADMIN_PASSWORD=') -split '=',2)[1].Trim()
powershell -ExecutionPolicy Bypass -File Infra\monitoring\smoke-test.ps1 -GrafanaPassword $gp
```

`smoke-test.ps1` kết thúc bằng ĐẠT là được; dòng `[CANH BAO]` là lưu ý, `[LOI]` mới là chưa đạt.

`-GrafanaPassword` lấy từ `GRAFANA_ADMIN_PASSWORD` trong `Infra\.env` (đã đổi ở mục 2). Bỏ tham số này thì script dùng mặc định `change-me-grafana` và báo 3 `[LOI]` Grafana dù hệ thống vẫn chạy đúng.

## 4. Cách 2 – Chạy service bằng Maven (phát triển)

```powershell
docker compose -f Infra/docker-compose.yml --profile mq --profile monitoring up -d
powershell -ExecutionPolicy Bypass -File Infra\dev\start-local.ps1
```

- Script chạy `mvn install` module dùng chung, rồi mở **9 cửa sổ**: Backend cũ 8090, 7 service 8081–8087, Gateway 8080 (đã đặt sẵn địa chỉ beacon, analytics). Lần sau thêm `-SkipInstall` để bỏ bước cài.
- **Đúng khi:** cửa sổ chính in các dòng mã 200 (gồm `/api/ibeacons/registry`) và dòng "San sang…". Không gõ lệnh vào các cửa sổ service.
- Không bật `--profile core` ở cách này.

## 5. Mở Web Admin

1. Mở `WebAdmin/src/pages/login.html` bằng Chrome (kéo thả file) hoặc VS Code Live Server.
2. Đăng nhập `admin` / `Test@1234` → vào trang Tổng quan; dùng menu bên trái để vào Di tích, Hiện vật, Beacon, Phản hồi, Người dùng, Tài khoản.
3. Web Admin ở máy khác cùng mạng: thêm `?api=http://<IP máy chạy>:8080` vào cuối địa chỉ một lần (trình duyệt nhớ).

## 6. Chạy App du khách

```powershell
cd Interface
flutter pub get
flutter devices
```

| Thiết bị | Lệnh | Địa chỉ API |
|---|---|---|
| Chrome (không có iBeacon thật) | `flutter run -d chrome` | `http://localhost:8080` (mặc định) |
| Máy ảo Android | `flutter run -d emulator-5554` | `http://10.0.2.2:8080` (mặc định) |
| Điện thoại thật (cùng Wi-Fi) | `flutter run -d <id> --dart-define=API_BASE_URL=http://<IP>:8080` | IP LAN của máy chạy hệ thống |
| File APK gửi người khác | `flutter build apk --dart-define=API_BASE_URL=http://<IP>:8080` | APK ở `build\app\outputs\flutter-apk\app-release.apk`; đổi IP thì build lại |

- Điện thoại thật: mở cổng 8080 trên tường lửa Windows một lần (PowerShell quyền Administrator): `New-NetFirewallRule -DisplayName "SmartHeritage 8080" -Direction Inbound -Protocol TCP -LocalPort 8080 -Action Allow`. Kiểm tra: trình duyệt điện thoại mở `http://<IP>:8080/api/heritage-sites` thấy JSON.
- Chế độ demo (hiện nút "Mô phỏng beacon" khi không có beacon thật): thêm `--dart-define=DEMO_MODE=true`.
- iBeacon thật cần Android thật, bật Bluetooth và Vị trí, cấp quyền khi app hỏi. UUID hệ thống: `E2C56DB5-DFFB-48D2-B060-D0F5A71096E0`.

## 7. Thành phần tuỳ chọn

| Thành phần | Bật | Địa chỉ | Ghi chú |
|---|---|---|---|
| Swagger chung | Có sẵn với Gateway | http://localhost:8080/swagger-ui.html | Ô *Select a definition* chọn service; **Authorize** bằng `accessToken` (không gõ `Bearer`) |
| Grafana | Có trong `all` / `monitoring` | http://localhost:3000 | Dashboard *Smart Heritage – Microservices* |
| Prometheus | Có trong `all` / `monitoring` | http://localhost:9090/alerts | 8 cảnh báo; bình thường đều Inactive |
| RabbitMQ UI | Có trong `all` / `mq` | http://localhost:15672 | Tab Queues: các hàng sự kiện và `.dlq` |
| NiFi (số liệu Dashboard tổng hợp) | `docker compose -f Infra/docker-compose.yml --profile nifi up -d` | https://localhost:8443/nifi | Lần đầu: tải driver JDBC, import flow theo [nifi-setup.md](../research/nifi-setup.md). Không bật NiFi thì Dashboard chỉ có số liệu "Hôm nay" và dữ liệu đã nạp trước |
| Traffic giả cho biểu đồ | `powershell -ExecutionPolicy Bypass -File Infra\monitoring\traffic-sim.ps1` | – | Ctrl+C để dừng |

## 8. Dữ liệu và tài khoản

| Dùng cho | Tài khoản | Mật khẩu |
|---|---|---|
| Web Admin, App (quản trị) | `admin` | `Test@1234` |
| App (du khách) | `visitor_an`, `visitor_mai`, `visitor_john` (tiếng Anh) | `Test@1234` |
| Grafana | `admin` | giá trị `GRAFANA_ADMIN_PASSWORD` |
| RabbitMQ | giá trị `RABBITMQ_USER` | giá trị `RABBITMQ_PASSWORD` |

- Script `Infra/db/*.sql` tự chạy **chỉ khi volume MySQL trống** (lần đầu). Dữ liệu demo bản cuối do Tường bổ sung vào `Infra/db` trong tuần 7.
- File media upload nằm trong volume `content_uploads` (Docker) hoặc thư mục `MEDIA_DIRECTORY` của content-service (cách 2, mặc định `services/content-service/uploads`).

**Nạp media cho dữ liệu demo.** File media mẫu nằm trong `Infra/seed/media/`; trong dữ liệu seed, URL media có dạng
`http://localhost:8080/api/media/files/<tên file>` (content-service phục vụ file trong thư mục upload qua Gateway).
Sau khi content-service đã chạy, chép media vào thư mục upload (chạy lại nhiều lần không sao, file trùng tên bị ghi đè):

```bash
# Cach 1 (Docker), dung o thu muc goc repo
docker cp Infra/seed/media/. smart_heritage_content:/app/uploads/
# Cach 2 (Maven/start-local), dung o thu muc goc repo (PowerShell)
Copy-Item Infra/seed/media/* services/content-service/uploads/ -Force
```

Kiểm tra: mở `http://localhost:8080/api/media/files/<tên một file>` trên trình duyệt phải thấy ảnh/âm thanh. Volume
`content_uploads` giữ media qua các lần khởi động lại; chỉ mất khi chạy `down -v`.
- **Không chạy `down -v` trên máy demo** sau khi đã nạp dữ liệu cuối: lệnh này xoá sạch MySQL và media.

## 9. Dừng, khởi động lại, làm sạch

```powershell
docker compose -f Infra/docker-compose.yml --profile all stop      # dừng, giữ dữ liệu
docker compose -f Infra/docker-compose.yml --profile all start     # chạy lại
docker compose -f Infra/docker-compose.yml --profile all up -d --build   # sau khi pull code mới
docker compose -f Infra/docker-compose.yml --profile all down      # xoá container, giữ volume
docker compose -f Infra/docker-compose.yml --profile all down -v   # XOÁ CẢ DỮ LIỆU – chỉ khi Tường báo đổi Infra/db
```

Cách 2: đóng các cửa sổ service (bấm X) rồi `docker compose -f Infra/docker-compose.yml --profile mq --profile monitoring stop`.

Biên bản chạy thử máy sạch (Tuấn, 21/11, tag `v1.0` 2a6e9e7, Windows 11 + Docker Desktop). Clone mới vào thư mục trống, volume MySQL/Grafana/media mới hoàn toàn. Một số image nền (`maven`, `mysql`, `redis`) đã có sẵn trên máy nên máy hoàn toàn mới sẽ lâu hơn do phải tải image.

| Bước | Thời gian | Kết quả |
|---|---|---|
| Mục 2: `git clone`, `git checkout v1.0`, tạo `Infra\.env`, đổi các giá trị `change-me…` | ~1 phút | Không vướng |
| Mục 3: build image | 5 phút 46 giây | Không lỗi |
| Mục 3: khởi động đến khi đủ *healthy* | 1 phút 43 giây | Đủ 16 container: 10 container có healthcheck đều *healthy*; Grafana, Prometheus, Redis, 2 exporter *Up*. DB khởi tạo từ `Infra/db` (gồm `09_demo_seed.sql`) không lỗi |
| Mục 3: kiểm tra nhanh | – | `/api/heritage-sites`, `/api/ibeacons/registry` trả 200; `smoke-test.ps1 -GrafanaPassword …` **ĐẠT**, không có `[LOI]` |
| Mục 5: Web Admin, Dashboard, xuất báo cáo qua Gateway | – | `admin` đăng nhập được; Dashboard không lọc, lọc di tích, lọc khoảng ngày đều trả số; xuất Excel/PDF có lọc mở được; khoảng ngày ngược và `format` sai trả 400; không token 401, `visitor_an` 403; du khách ghi 1 lượt xem thì số "Hôm nay" tăng ngay (sự kiện RabbitMQ). Dashboard máy sạch không có số tổng hợp vì `all` không gồm NiFi (mục 7) |
| Mục 8: nạp media `docker cp Infra/seed/media/. smart_heritage_content:/app/uploads/` | 1 giây | 18/18 file media mà seed dùng đều mở được qua `/api/media/files/…`. Seed: 2 di tích, 6 hiện vật, 4 beacon, 4 tài khoản |
| **Tổng** (clone → đủ *healthy*) | **~8,5 phút** | Đạt yêu cầu ≤ 15 phút |

Lần chạy trước (18/11, `main` 761248f, 11 phút) vướng 3 chỗ: chưa có tag v1.0, `smoke-test.ps1` thiếu `-GrafanaPassword`, chưa có `Infra/seed/media`. Cả 3 đã sửa trên v1.0.

## 10. Lỗi thường gặp

| Hiện tượng | Nguyên nhân | Cách xử lý |
|---|---|---|
| `dockerDesktopLinuxEngine … cannot find the file` | Docker Desktop chưa chạy | Mở Docker Desktop, đợi biểu tượng hết xoay |
| `port is already allocated` / start-local báo "Cong dang ban" | Tiến trình khác giữ cổng | `Get-NetTCPConnection -LocalPort <cổng> -State Listen` lấy `OwningProcess`, rồi `Stop-Process -Id <PID>`; không tắt hết java |
| Container `unhealthy` hoặc Gateway không bật | Service khởi động chậm hoặc lỗi | `docker compose -f Infra/docker-compose.yml logs <tên service> --tail 100` |
| Build Docker lỗi tải thư viện Maven | Mạng chặn Maven Central | Đổi mạng hoặc dùng cách 2 trên máy đã có thư viện |
| Không thấy dữ liệu mẫu | Volume MySQL đã có dữ liệu cũ | Dữ liệu chỉ nạp khi volume trống (cân nhắc `down -v`, xem cảnh báo mục 8) |
| Cảnh báo ServiceDown beacon/analytics (cách 2) | Bản `start-local.ps1` cũ | Pull `main` mới (sau patch W7 04) |
| content-service in stack trace mỗi 10 giây | RabbitMQ chưa bật | Bật `--profile mq` |
| RabbitMQDeadLetter Firing mãi | Còn tin trong DLQ | RabbitMQ UI → Queues → hàng `.dlq` → Purge |
| HighLatencyP95 bật thoáng qua sau khi khởi động | Request đầu chậm | Bình thường, tự tắt |
| App báo lỗi kết nối | Sai địa chỉ API | Chrome: localhost; máy ảo: 10.0.2.2; máy thật: `API_BASE_URL` + tường lửa + cùng Wi-Fi |
| Web Admin quay về trang đăng nhập | Token hết hạn hoặc không phải ADMIN | Đăng nhập lại bằng `admin` |
| Đăng nhập báo tài khoản bị khoá | Admin đã khoá tài khoản | Admin mở khoá ở trang Người dùng |

## 11. Triển khai lên server (kế hoạch tuần 8)

Chưa thực hiện tại thời điểm viết. Hướng làm dựa trên cách 1:

1. VPS Ubuntu 22.04+, ≥ 4 vCPU, 16 GB RAM; cài Docker Engine và Compose plugin.
2. `git clone`, `git checkout v1.0`, tạo `Infra/.env` với mật khẩu mạnh, `JWT_SECRET` mới.
3. `docker compose -f Infra/docker-compose.yml --profile all up -d --build`.
4. Đặt reverse proxy (Nginx hoặc Caddy) có chứng chỉ TLS phía trước cổng 8080 để đạt yêu cầu HTTPS/TLS 1.2+ (NFR-04); tường lửa chỉ mở 80/443 (và 22). Không mở 8081–8087, 3307, 6379, 5672, 9090, 3000 ra Internet.
5. Đưa Web Admin lên cùng tên miền (thư mục tĩnh của reverse proxy); build App với `API_BASE_URL=https://<tên miền>`.
6. Sao lưu: `mysqldump` hằng ngày và volume `content_uploads`.
