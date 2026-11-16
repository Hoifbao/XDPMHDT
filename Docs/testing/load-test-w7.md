# Báo cáo kiểm thử tải lần 2 (k6) – so sánh trước/sau tối ưu

Sprint 7 · XDPMHDT-95 · Người thực hiện: Bảo · Kịch bản: [`Infra/loadtest/k6-gateway.js`](../../Infra/loadtest/k6-gateway.js) (giữ nguyên tham số tuần 6) · Lần 1: [load-test.md](load-test.md)

> Trạng thái: **chưa chạy lần 2** (dự kiến sáng T5 19/11/2026, sau khi content-service tối ưu xong). Các ô `…` điền từ kết quả chạy thật; không ước lượng.
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
| Commit | Nhánh XDPMHDT-40 trước khi merge #67 (code W6, chưa có #53) | … |
| Máy | Laptop của Bảo; RAM chưa ghi | … (script tự ghi CPU, RAM) |
| Cách chạy service | mvn spring-boot:run (start-local + bật thêm beacon, analytics) | … |
| k6 | `grafana/k6:0.54.0`, chạy cùng máy | Như lần 1 |

## 5. Kết quả trước/sau

| Chỉ số | Lần 1 (tuần 6) | Lần 2 (tuần 7) | Yêu cầu | Đạt? |
|---|---|---|---|---|
| VU tối đa | 500 | … | 500 | … |
| Tổng request | 73.397 | … | – | – |
| Thông lượng trung bình | 135 request/giây | … | – | – |
| p95 toàn bộ | 1,09 s | … | ≤ 2 s | … |
| p95 chi tiết hiện vật | 1,55 s | … | ≤ 2 s | … |
| p95 di tích gần | 109 ms | … | ≤ 2 s | … |
| p95 registry | 123 ms | … | ≤ 2 s | … |
| p95 đăng nhập | 646 ms | … | ≤ 2 s | … |
| Tỉ lệ lỗi | 0% (0/73.397) | … | < 1% | … |
| Checks đạt | 100% | … | > 99% | … |
| p95 theo phút cao nhất (Grafana) | 2,29 s | … | ≤ 2 s | … |

Quan sát Grafana lúc giữ 500 VU:

| Quan sát | Lần 1 | Lần 2 |
|---|---|---|
| HikariCP content-service: kết nối bận / tối đa | 10/10 | … |
| Request chờ kết nối tối đa | 124 | … |
| Thời gian chờ kết nối tối đa | 4,4 s | … |
| Cảnh báo bật trên `/alerts` | … (chưa ghi) | … |

## 6. Thay đổi giữa 2 lần đo

| Thay đổi | Người | PR / issue |
|---|---|---|
| content-service: cache chi tiết hiện vật, kiểm N+1, chỉnh `maximum-pool-size` | Xuân Anh | … (issue W7 #4) |
| site-service bật `percentiles-histogram` (có p95 phía server) | Ân | … (issue W7 #7) |
| Docker đủ 7 service, `start-local.ps1` 9 cửa sổ | Bảo | … (patch W7 04) |

## 7. Kết luận và giới hạn

- Kết luận: … (đạt / chưa đạt p95 ≤ 2 giây ở … VU; pool kết nối DB còn / hết nghẽn)
- Điểm nghẽn còn lại: …
- Giới hạn: hệ thống và k6 chạy chung 1 máy cá nhân, nên số đo thấp hơn môi trường triển khai thật; 2 lần đo phải cùng máy, cùng cách chạy mới so sánh được. Nếu máy không chạy được 500 VU, chỉ ghi số ở mức đạt được, không suy ra kết quả 500 VU.
