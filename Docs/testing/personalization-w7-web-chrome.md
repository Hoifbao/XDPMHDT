# XDPMHDT-99 — Kiểm thử Flutter Web trên Chrome (23/11/2026)

## Tình trạng và nguồn bằng chứng

- Pull request: [#88](https://github.com/Hoifbao/XDPMHDT/pull/88), chưa merge; nhánh `fix/XDPMHDT-99-phu-w7-personalization`.
- Đã rebase 6 commit lên upstream `main` **`e2ff2fc764c766f7fdc42abf2e34faf44bce44bb`** ngày 23/11; dùng cập nhật ref có expected SHA tương đương `--force-with-lease`. Kiểm tra ngay sau rebase: behind 0, ahead 6.
- **Runner:** GitHub Actions `ubuntu-latest`, Flutter 3.50.0, Google Chrome **157.0.8103.78** (headless).
- **CI Web:** [run #45156218632](https://github.com/phuht0462-a11y/XDPMHDT/actions/runs/45156218632) (commit `5d84dc8`).
- **CI Flutter và tích hợp sau rebase:** [run #45156218562](https://github.com/phuht0462-a11y/XDPMHDT/actions/runs/45156218562). Chạy trên nền upstream mới, kết quả: 14/14 test chọn lọc, **87/87 full Flutter tests**, và kiểm tra tích hợp với các dịch vụ thật trên CI đạt.
- **Analyzer baseline đã điều chỉnh theo upstream mới:** [run #45156703177](https://github.com/phuht0462-a11y/XDPMHDT/actions/runs/45156703177) ghi nhận **34 vấn đề** trên PR và **34 vấn đề** trên `main` tương ứng. `flutter analyze` trả exit code 1 ở cả hai phía; không tuyên bố analyzer sạch.

## Kết quả Flutter Web/Chrome

| Bước | Kết quả đã thấy | Phạm vi |
|---|---|---|
| Khởi tạo `web/` tạm thời trong CI bằng `flutter create --platforms=web` | **PASS** | Folder `Interface/web` của repo chỉ có `.gitkeep`, CI tạo scaffold tạm thời; chưa thêm scaffold vào mã nguồn chính |
| `flutter pub get` | **PASS** | Tải và giải quyết dependencies |
| `flutter build web` | **PASS** | Có `build/web`, build không có backend gắn sẵn |
| `flutter test --platform chrome` | **12/12 PASS** | 10 bài kiểm thử `personalization_offline_test.dart` và 2 bài `personalization_screens_test.dart` chạy trên Chrome headless |
| Mở app Web đã build trong Chrome headless + chụp screenshot | **PASS** — [run #45156819980](https://github.com/phuht0462-a11y/XDPMHDT/actions/runs/45156819980) | Chrome đã mở URL từ HTTP server cục bộ, tạo ảnh PNG, xuất DOM và xác nhận có `flutter-view`/`flt-glass-pane`; artifact `smart-heritage-flutter-web-chrome-proof`. Chưa kiểm thử E2E bằng thao tác người dùng thực |
| Luồng đăng nhập → quét beacon BLE → offline → đồng bộ với backend qua Chrome | **CHƯA KIỂM THỬ E2E** | Chrome test và Web build không thay thế kiểm thử Android Emulator 9/9 mà nhóm trưởng báo cáo |

**Kết luận:** Flutter Web biên dịch được; 12 test chạy qua Chrome headless thành công, và bản Web đã build được mở qua Chrome headless với artifact ảnh/DOM. Chưa khẳng định ảnh đã được reviewer xem thủ công hoặc toàn bộ luồng chức năng UI hoạt động. Đây là smoke test và kiểm thử logic, **không phải** nghiệm thu trình duyệt có thao tác thủ công với toàn bộ backend.

## Cách tái lập

Chỉ chạy sau khi có đủ Flutter SDK/Chrome và dung lượng trống phù hợp (không bắt buộc Android Studio):

```powershell
cd Interface
flutter create --platforms=web --project-name smartheritage --no-pub .
flutter pub get
flutter test --platform chrome test/personalization_screens_test.dart test/features/personalization/personalization_offline_test.dart
flutter build web --dart-define=DEMO_MODE=true --dart-define=API_BASE_URL=http://localhost:8080
# Cần Gateway trên cùng máy để kiểm thử tương tác thật:
flutter run -d chrome --dart-define=DEMO_MODE=true --dart-define=API_BASE_URL=http://localhost:8080
```

Các lệnh trên có thể tạo file scaffold ở `Interface/web`; kiểm tra `git status` và không tự commit file phát sinh vào PR nếu nhóm chưa duyệt triển khai Web. CI tạo các file đó ở workspace tạm và không đẩy vào main. Không nhập mật khẩu/token demo vào kho mã.

## Tình trạng nghiệm thu do nhóm trưởng cung cấp

Theo tin nhắn ngày 23/11/2026, **nhóm trưởng đã kiểm thử Android Emulator và thông báo 9/9 tình huống đạt**. Kết quả này là báo cáo từ nhóm trưởng, không phải kết quả đo từ CI Chrome. Ở thời điểm tra cứu, chưa tìm thấy chi tiết per-case trong reviews/comments của PR #88 qua kết nối GitHub hiện tại; xem [biên bản emulator](personalization-w7-emulator-acceptance.md). Chỉ nhóm trưởng quyết định chuyển Draft và merge sau khi xem đầy đủ kết quả.
