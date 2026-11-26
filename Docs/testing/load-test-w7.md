# Báo cáo kiểm thử tải lần 2 (k6) – so sánh trước/sau tối ưu

Sprint 7 · XDPMHDT-95 · Người thực hiện: Bảo · Kịch bản: [`Infra/loadtest/k6-gateway.js`](../../Infra/loadtest/k6-gateway.js) (giữ nguyên tham số tuần 6) · Lần 1: [load-test.md](load-test.md)

> Trạng thái: **đã chạy lần 2** lúc 19:59 ngày 21/11/2026 (Bảo, commit `2a6e9e7` = tag `v1.0`). Số liệu lấy từ `D:\CNTT\XD_PMHDT\k6_ket_qua\20261121_1959\` (`summary.json`, `k6_log.txt`, `moi_truong.txt`, `grafana_hikari_k6_lan2.png`); điền vào file này ngày 27/11/2026 (XDPMHDT-116). Ô nào không có số đo thì ghi rõ, không ước lượng.
>
> Lưu ý nguồn số lần 1: file `load-test.md` trên `main` vẫn là bản mẫu (các ô `…`). Số lần 1 dưới đây lấy từ biên bản chốt code tuần 6 (bàn giao v15, chạy 00:44–00:53 ngày 13/11/2026 trên nhánh `feature/XDPMHDT-40-bao-w6-alerts-k6`, ngay trước khi merge #67) và bình luận Jira XDPMHDT-41. Khi chạy lần 2, nên điền lại `load-test.md` cho khớp.

## 1. Mục tiêu

Đo lại sau tuần 7 để: (1) xác nhận hệ thống vẫn đạt yêu cầu khi đã thêm thay đổi tuần 7; (2) kiểm tra lỗi Major hết pool kết nối DB của content-service (issue W7 #4) đã hết.

| Yêu cầu | Ngưỡng | Nguồn |
|---|---|---|
| Số người dùng đồng thời | 500 | Đề tài – Scalability |
| p95 toàn bộ và từng API chính | ≤ 2 giây | Đề tài – Performance |
| Tỉ lệ lỗi | < 1% | Ngưỡng nhóm đặt trong kịch bản |
| Không còn request chờ kết nối DB kéo dài | Hikari pending ≈ 0 khi giữ 500 VU | Tiêu chí xong issue W7 #4 |

## 2. Kịch bản (giống lần 1)

| Thao tác | API | Tỉ lệ |
|---|---|---|
| Xem chi tiết hiện vật | `GET /api/artifacts/{id}` (id 1001–1025) | 50% |
| Tìm di tích gần | `GET /api/heritage-sites/nearby` | 25% |
| Tải registry beacon | `GET /api/ibeacons/registry` | 20% |
| Đăng nhập | `POST /api/auth/login` | 5% |

Giai đoạn: 0 → 50 VU (1 phút) → 200 (2 phút) → 500 (2 phút) → giữ 500 (3 phút) → 0 (1 phút). Chỉ đọc và đăng nhập.

## 3. Cách chạy

Chuẩn bị: hệ thống chạy đủ 7 service (cùng cách chạy với lần 1 để so sánh công bằng); có dữ liệu hiện vật id 1001–1025 (`Seed_SmartHeritage_v1.sql` hoặc seed bản cuối); bật profile `monitoring`; tắt chương trình nặng khác.

```powershell
powershell -ExecutionPolicy Bypass -File D:\CNTT\XD_PMHDT\Chay_k6_lan2_v1.ps1
```

Script kiểm 4 API trả 200, ghi môi trường (commit, CPU, RAM, Windows), chạy smoke 30 giây, chạy đủ 500 VU, rồi tạo `bang_ket_qua.md` trong `D:\CNTT\XD_PMHDT\k6_ket_qua\<ngày_giờ>\` để dán vào mục 5. Trong lúc giữ 500 VU, chụp Grafana panel HikariCP của content-service.

## 4. Môi trường đo

| Mục | Lần 1 (13/11) | Lần 2 |
|---|---|---|
| Commit | Nhánh XDPMHDT-40 trước khi merge #67 (code W6, chưa có #53) | `2a6e9e7` (nhánh `main`, tag `v1.0`) |
| Máy | Laptop của Bảo; RAM chưa ghi | Laptop của Bảo: Intel Core i7-13650HX (20 luồng), RAM 23,7 GB, Windows 11 Home |
| Cách chạy service | mvn spring-boot:run (start-local + bật thêm beacon, analytics) | Docker `--profile all` (16 container) |
| k6 | `grafana/k6:0.54.0`, chạy cùng máy | Như lần 1 |

## 5. Kết quả trước/sau

| Chỉ số | Lần 1 (tuần 6) | Lần 2 (tuần 7) | Yêu cầu | Đạt? |
|---|---|---|---|---|
| VU tối đa | 500 | 500 | 500 | Đạt |
| Tổng request | 73.397 | 81.479 | – | – |
| Thông lượng trung bình | 135 request/giây | 150 request/giây | – | – |
| p95 toàn bộ | 1,09 s | 31 ms | ≤ 2 s | Đạt |
| p95 chi tiết hiện vật | 1,55 s | 10 ms | ≤ 2 s | Đạt |
| p95 di tích gần | 109 ms | 10 ms | ≤ 2 s | Đạt |
| p95 registry | 123 ms | 12 ms | ≤ 2 s | Đạt |
| p95 đăng nhập | 646 ms | 87 ms | ≤ 2 s | Đạt |
| Tỉ lệ lỗi | 0% (0/73.397) | 0% (0/81.479) | < 1% | Đạt |
| Checks đạt | 100% | 100% (81.475/81.475) | > 99% | Đạt |
| p95 theo phút cao nhất (Grafana) | 2,29 s | Không chụp panel p95 lần 2; p99 toàn bộ (k6) 74 ms, request chậm nhất 143 ms | ≤ 2 s | Đạt (theo số k6) |

Quan sát Grafana lúc giữ 500 VU:

| Quan sát | Lần 1 | Lần 2 |
|---|---|---|
| HikariCP content-service: kết nối bận / tối đa | 10/10 | Tối đa 1 kết nối bận (trung bình 0,06) |
| Request chờ kết nối tối đa | 124 | 0 (mọi service) |
| Thời gian chờ kết nối tối đa | 4,4 s | Không có request phải chờ |
| Cảnh báo bật trên `/alerts` | … (chưa ghi) | Chưa ghi (không chụp trang `/alerts` lúc chạy) |

## 6. Thay đổi giữa 2 lần đo

| Thay đổi | Người | PR / issue |
|---|---|---|
| content-service: cache chi tiết hiện vật, kiểm N+1, chỉnh `maximum-pool-size` | Xuân Anh | PR #84 (XDPMHDT-100, issue W7 #4) |
| site-service bật `percentiles-histogram` (có p95 phía server) | Ân | PR #81 (XDPMHDT-102, issue W7 #7) |
| Docker đủ 7 service, `start-local.ps1` 9 cửa sổ | Bảo | PR #72 (XDPMHDT-94, patch W7 04) |

## 7. Kết luận và giới hạn

- Kết luận: **đạt** p95 ≤ 2 giây ở **500 VU** (p95 toàn bộ 31 ms, API chậm nhất là đăng nhập 87 ms), tỉ lệ lỗi 0%. Pool kết nối DB **hết nghẽn**: content-service tối đa 1 kết nối bận, không có request chờ (lần 1: 10/10 bận, 124 request chờ). Lỗi Major W7 #4 đóng được. TC-123 ghi **Đạt**.
- Điểm nghẽn còn lại: không thấy ở 500 VU. Đăng nhập chậm nhất (p95 87 ms) do băm mật khẩu, vẫn xa ngưỡng.
- Lưu ý khi so sánh: lần 1 chạy service bằng `mvn spring-boot:run`, lần 2 chạy Docker `--profile all`; thời gian phản hồi giảm mạnh (1,09 s → 31 ms) là kết quả chung của tối ưu tuần 7 (#84; bản đo là `v1.0`, chưa có #98) và cách chạy khác nhau, không tách riêng được phần nào do code.
- Giới hạn: hệ thống và k6 chạy chung 1 máy cá nhân, nên số đo thấp hơn môi trường triển khai thật; 2 lần đo phải cùng máy, cùng cách chạy mới so sánh được. Nếu máy không chạy được 500 VU, chỉ ghi số ở mức đạt được, không suy ra kết quả 500 VU.
