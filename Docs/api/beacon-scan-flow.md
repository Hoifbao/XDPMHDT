# Luồng quét iBeacon → chi tiết hiện vật (chốt W3, 30/10/2026)

Hợp đồng dữ liệu giữa **Khoa** (iBeacon, Mobile) và **Xuân Anh** (Hiện vật, đa ngôn ngữ), dùng để làm W4.
Bảo chốt dựa trên code đang có trên `main`.

## 1. Các bước

| # | App làm gì | API | Ghi chú |
|---|---|---|---|
| 1 | Mở màn quét: tải registry | `GET /api/ibeacons/registry` | Công khai. Trả các beacon `ACTIVE`: `id, uuid, major, minor, siteId, siteName, artifactId, artifactName, ...` |
| 2 | Quét BLE, tách gói iBeacon | — | Khóa so khớp: `UUID-major-minor` (UUID chữ hoa), xem `beaconKey()` trong `beacon_service.dart` |
| 3 | Chọn beacon gần nhất | — | Trong các beacon khớp registry và có `artifactId != null`: chọn `accuracy` (mét) nhỏ nhất, tức RSSI mạnh nhất. W4 nên lọc thêm ngưỡng (ví dụ ≤ 3 m) và giữ ổn định vài giây để tránh nhảy qua lại |
| 4 | Mở chi tiết hiện vật | `GET /api/artifacts/{artifactId}?lang=vi\|en` | Công khai. JSON mẫu: [artifact-detail.sample.json](artifact-detail.sample.json), [fallback](artifact-detail-fallback.sample.json) |
| 5 | Ghi lượt xem (nếu đã đăng nhập) | `POST /api/me/visits` | `{ artifactId, clientEventId (UUID v4), source: "BEACON", detectedAt (ISO-8601), durationSeconds? }`. Không đo được thời gian thì bỏ `durationSeconds` (lưu NULL) |

## 2. Trường app dùng ở bước 4

Envelope chung: `{ success, message, data }`.

| Trường trong `data` | Ý nghĩa |
|---|---|
| `id`, `name`, `thumbnailUrl`, `locationName` | Thông tin chính. `name`/`description` đã theo ngôn ngữ được trả về |
| `content.title`, `content.description`, `content.audioUrl`, `content.videoUrl`, `content.transcript` | Nội dung thuyết minh đã chọn ngôn ngữ |
| `content.imageUrls` | **Chuỗi JSON** (ví dụ `"[\"https://...\"]"`), app phải `jsonDecode` lần nữa |
| `languageCode`, `requestedLanguage`, `fallback` | `fallback = true` khi thiếu bản dịch và đang trả bản tiếng Việt: app hiện nhãn "Chưa có bản dịch" |
| `media[]` | `mediaType` (IMAGE, AUDIO, VIDEO), `fileUrl`, `caption` |

## 3. Quyết định đã chốt

- **Không dùng `GET /api/artifacts/beacon/{uuid}` cho luồng quét.** Mọi beacon trong `init_db.sql` dùng chung UUID `E2C56DB5-DFFB-48D2-B060-D0F5A71096E0` và chỉ khác `major/minor`, nên tra theo UUID không xác định được hiện vật. Cột `artifacts.beacon_uuid` là cột tạm (NULL trong dữ liệu mẫu). Liên kết chính thức là `ibeacons.artifact_id`. Endpoint cũ giữ nguyên để không vỡ code, Swagger đã ghi "[Cũ]". Khi tách beacon-service ở W4, cân nhắc bỏ hẳn.
- `?lang` chỉ nhận `vi` hoặc `en`, mặc định `vi`. Giá trị khác trả 400. App lấy ngôn ngữ theo `preferredLanguage` của người dùng, chưa đăng nhập thì dùng ngôn ngữ máy.
- Mobile đã có `RegistryBeacon.artifactId` (đọc từ registry) để gọi bước 4.

## 4. Dữ liệu mẫu để thử

| Beacon (major, minor) | Hiện vật | Trạng thái |
|---|---|---|
| (1, 1) | 1 – Khuê Văn Các | ACTIVE |
| (1, 2) | 2 – Bia Tiến sĩ | ACTIVE |
| (2, 1) | 3 – Đoan Môn | ACTIVE |
| (1, 3) | chưa gán | INACTIVE, không có trong registry |
