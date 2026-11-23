# XDPMHDT-99 — Biên bản nghiệm thu Android Emulator (Phú, Sprint 7)

> Trạng thái: **CHƯA NGHIỆM THU TRÊN EMULATOR**. Đây là kịch bản và mẫu ghi bằng chứng, không phải ảnh hoặc kết quả thực thi. Không đánh dấu ĐẠT khi chưa thao tác thật. Không merge PR #88 trước khi nhóm trưởng review.

## 1. Nền mã và CI tham khảo

- Nhánh PR: `phuht0462-a11y:fix/XDPMHDT-99-phu-w7-personalization`; PR [#88](https://github.com/Hoifbao/XDPMHDT/pull/88).
- Ngày 22/11/2026, upstream `main` tại `31853e64d4cdbf6725fb7de4b7dfb88b8267d779` (mới hơn mốc `2a6e9e7` trong tin nhắn nhóm trưởng). Khi đã kiểm tra, nhánh PR phía trên đang **ahead 2 / behind 0** so với main trước khi thêm các commit CI và sửa demo. Không force-push nếu không cần thiết.
- [CI đã chạy trên commit e29ff04](https://github.com/phuht0462-a11y/XDPMHDT/actions/runs/44991030284): **14/14 Flutter tests đạt**, gồm **10 test** trong `personalization_offline_test.dart`; **14 kiểm tra tích hợp** đạt với Auth, Content, Gateway, MySQL, Redis, RabbitMQ, Analytics thật trên runner dùng stack tạm.
- `flutter analyze` trả về **39 vấn đề** trong nhánh và **39 vấn đề** trong bản main đối chứng (đa số deprecation/info, có warning chưa dùng field ở map screen). Lệnh trả exit code 1; job CI cho phép tiếp tục để báo cáo baseline, không được ghi `flutter analyze clean`.
- Sau các commit mới (sửa nguồn `BEACON`, chạy toàn bộ Flutter suite), **phải xem kết quả CI tại head SHA mới nhất**; kết quả cũ không chứng minh build của head mới.

## 2. Chuẩn bị Windows / Android Emulator

1. Mở **Android Studio → Device Manager → Create Virtual Device** → chọn máy Pixel, system image có **Google Play**, tải SDK image nếu được yêu cầu, rồi bấm nút ▶ khởi động AVD.
2. Dùng PowerShell **trong thư mục gốc của repository**. Đảm bảo `git status --short` không có thay đổi chưa lưu; `git remote -v` để xác định upstream/fork trước khi cập nhật. Checkout đúng nhánh PR đã được đẩy lên GitHub, không chạy thử trên main:
   ```powershell
   git status --short
   git branch --show-current
   git remote -v
   git fetch origin
   # Nếu đã có nhánh tuần 7 trên máy:
   git switch fix/XDPMHDT-99-phu-w7-personalization
   # Fetch từ fork và chỉ cập nhật khi fast-forward an toàn:
   git fetch https://github.com/phuht0462-a11y/XDPMHDT.git fix/XDPMHDT-99-phu-w7-personalization
   git merge --ff-only FETCH_HEAD
   git log -1 --oneline
   ```
   Nếu chưa có nhánh local: sau khi fetch nhánh fork, dùng `git switch -c fix/XDPMHDT-99-phu-w7-personalization FETCH_HEAD`. Nếu báo branch conflict/dirty hoặc không fast-forward: dừng, không `reset --hard` và không `push --force`.
3. Khởi động Docker Desktop. Tại gốc repo:
   ```powershell
   docker compose -f Infra/docker-compose.yml --profile all up -d --build
   docker compose -f Infra/docker-compose.yml --profile all ps
   ```
   Không chạy `docker compose down -v`; không xoá database hoặc volume. Nếu máy đang chạy service bằng `mvn spring-boot:run` chiếm cổng 8080–8087 thì dừng chúng trước.
4. Chạy Flutter trên AVD:
   ```powershell
   cd Interface
   flutter devices
   flutter pub get
   flutter analyze
   flutter test
   flutter test test/features/personalization/personalization_offline_test.dart
   flutter run --dart-define=DEMO_MODE=true --dart-define=API_BASE_URL=http://10.0.2.2:8080
   ```
   `10.0.2.2` là địa chỉ từ Android Emulator tới localhost trên máy host; không đặt `localhost:8080` trong app AVD.

## 3. Kịch bản offline bắt buộc

**Chuẩn bị**: Đăng nhập tài khoản thử A; khi có mạng mở trước nội dung 2 hiện vật cần yêu thích. Đảm bảo app truy cập được Gateway, có dữ liệu Content. Mở ứng dụng ở chế độ debug với `DEMO_MODE=true`. Để sẵn màn hình terminal khác ở thư mục gốc repo để điều khiển Docker.

| Mã | Bước thao tác | Kỳ vọng | Thực tế | Ảnh/log |
|---|---|---|---|---|
| EM-01 | Có mạng, đăng nhập A và xem lịch sử/yêu thích | Có dữ liệu thật hoặc empty state hợp lệ; không crash | **CHƯA CHẠY** | `01-online.png` |
| EM-02 | Ngắt gateway trước khi thao tác: `docker stop smart_heritage_gateway` | App không mất dữ liệu cache; có thể ghi hàng chờ | **CHƯA CHẠY** | `02-offline.png` |
| EM-03 | Trên Home, **bấm Mô phỏng beacon → Xem hiện vật** đủ 3 lần; mỗi lần quay lại Home | Có 3 sự kiện tham quan local và UUID riêng; source `BEACON` (sau commit sửa demo) | **CHƯA CHẠY** | `03-beacon.png` |
| EM-04 | Vào **chi tiết của 2 hiện vật khác nhau**, bấm nút trái tim khi offline | Có 2 favorite local, trạng thái chờ đồng bộ; không mất sau điều hướng | **CHƯA CHẠY** | `04-favorites.png` |
| EM-05 | Bật gateway: `docker start smart_heritage_gateway`; chờ healthy, để app foreground/resume | Hàng chờ được thử lại; DB có 3 UUID riêng và 2 yêu thích; không trùng lần retry | **CHƯA CHẠY** | `05-sync.png` |
| EM-06 | Thử mất phản hồi: ngắt gateway khi thao tác đang gửi, bật lại, retry | Không sinh UUID mới cho lượt cũ; không nhân đôi bản ghi | **CHƯA CHẠY** | `06-retry.png` |
| EM-07 | Đổi sang tài khoản B, xem lịch sử/yêu thích; đổi lại A | B không nhìn thấy hàng chờ/cache của A; A còn dữ liệu đúng | **CHƯA CHẠY** | `07-switch.png` |
| EM-08 | Đóng/mở ứng dụng khi đang có hàng chờ | Outbox bền vững, được thử lại khi mở/đăng nhập | **CHƯA CHẠY** | `08-reopen.png` |
| EM-09 | Tắt Redis trong môi trường thử, thao tác đọc/ghi có Content | Không trả lỗi 500 chỉ vì Redis ngừng | **CHƯA CHẠY** | `09-redis.png` |

**Cách giả lập beacon sát luồng BLE hơn**: vào **Quét iBeacon thật** → biểu tượng con bọ (chỉ ở debug) → với từng beacon đã được gán hiện vật, bấm **Vào vùng**, chờ ít nhất 2 giây, sau đó **Ra vùng**. Chính bước **Ra vùng** mới ghi `source=BEACON`; mỗi beacon có cooldown 60 giây nên hãy chọn 3 beacon khác nhau hoặc chờ. Nếu AVD không hỗ trợ BLE, vẫn có thể dùng nút Home theo hướng dẫn của nhóm trưởng. Nút Home cần bấm tiếp **Xem hiện vật** trong hộp thoại; chỉ mở hộp thoại chưa tạo lượt tham quan.

**Lưu ý**: mục ngôi sao trên một thẻ Home có thể chưa xử lý sự kiện; hãy dùng nút trái tim trong **màn chi tiết hiện vật**. Không tuyên bố ba beacon vật lý đã được quét nếu chỉ chạy giả lập.

**Phương án Airplane mode**: trên Android Emulator kéo thanh thông báo xuống và bật Airplane mode; nếu app vẫn truy cập được `10.0.2.2`, dùng lệnh dừng Gateway như EM-02 để tạo lỗi HTTP có thể tái lập. Tránh làm gián đoạn server nhóm đang dùng; chỉ thao tác trên môi trường test riêng.

## 4. Thu thập bằng chứng thật trên Windows

Trong PowerShell tại gốc repo:

```powershell
New-Item -ItemType Directory -Force Docs/testing/evidence/phu-w7-emulator | Out-Null
adb devices
adb shell screencap -p /sdcard/phu-w7-proof.png
adb pull /sdcard/phu-w7-proof.png Docs/testing/evidence/phu-w7-emulator/01-online.png
adb logcat -d -s flutter:I AndroidRuntime:E > Docs/testing/evidence/phu-w7-emulator/device-log.txt
```

Đổi tên ảnh sau mỗi tình huống theo EM-01...EM-09. Có thể quay màn hình Android Emulator bằng nút Record Screen (trong Extended Controls) rồi lưu MP4. Với `flutter analyze` và `flutter test`, lưu nguyên output terminal/capture màn hình, ghi số test, lỗi và commit SHA hiện tại.

**Không commit** token JWT, mật khẩu, email/tài khoản thật, log có secrets, hoặc dữ liệu riêng của nhóm. Chỉ gửi ảnh/log đã rà soát.

## 5. Xóa nhánh cũ — việc cần quyền maintainer

- Chỉ xoá `Hoifbao/XDPMHDT:feature/personalization-history-favorites-redis` sau khi đối chiếu PR #63/#64 đã merge. Tại 22/11 nhánh cũ vẫn tồn tại và tip `18397a3fa3520a84bbdee29cf9e06013da755b0b`.
- Maintainer có thể mở GitHub → repo → **Branches** → tìm chính xác tên nhánh cũ → **Delete branch**. Nếu có quyền push trên upstream, thay thế bằng `git push <upstream-remote> --delete feature/personalization-history-favorites-redis`; phải xác nhận remote thật trước khi chạy.
- **Không** xoá nhánh `fix/XDPMHDT-99-phu-w7-personalization` đang phục vụ PR #88. Không đụng nhánh `main`.

## 6. Kết luận nghiệm thu

Ngày, tên người thử, AVD (tên máy / Android version), commit SHA thử, link CI, kết quả `flutter analyze`, `flutter test`, số event/favorite trong server, các ảnh/video, lỗi và bước khắc phục: **chưa được nhập từ phiên chạy thực tế**.

- [ ] 10+ test trong `personalization_offline_test.dart` đạt ở phiên CI head mới nhất
- [ ] Full `flutter test` được thực thi và ghi kết quả
- [ ] `flutter analyze` được thực thi, ghi số warnings/info và exit code, so sánh với baseline
- [ ] 3 sự kiện BEACON / 2 favorites offline → retry và sync không trùng
- [ ] Đổi tài khoản không lẫn dữ liệu, mở lại app giữ queue
- [ ] Ảnh/log thật đủ cho các tình huống
- [ ] Trưởng nhóm review mã nguồn và bằng chứng
- [ ] Nhánh cũ đã được maintainer xóa

Chỉ sau khi các mục được kiểm chứng và nhóm trưởng đồng ý mới chuyển PR #88 sang **Ready for review** và merge theo quy trình nhóm.
