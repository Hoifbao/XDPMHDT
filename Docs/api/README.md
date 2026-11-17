# API Documentation

Store the OpenAPI/Swagger specification and API usage notes here. Keep the specification aligned with the REST API implemented in `services/`.

## Tài liệu OpenAPI của 7 service (tuần 7, Tường)

Mọi client (app, Web Admin) gọi API qua **API Gateway `http://localhost:8080`**. Gateway kiểm tra JWT, phân quyền
rồi chuyển request tới service. Swagger của cả 7 service xem tại một chỗ:
**`http://localhost:8080/swagger-ui.html`** → chọn service ở ô góc phải trên.

File OpenAPI (JSON, xuất qua Gateway nên đường dẫn đúng như client gọi) nằm trong [`openapi/`](openapi/):

| Service | Cổng | Nhóm API chính | Số đường dẫn | File |
|---|---|---|---|---|
| auth-service | 8081 | Đăng ký, đăng nhập, làm mới/thu hồi token, hồ sơ; quản lý người dùng (`/api/admin/users`) | 9 | [auth-service.json](openapi/auth-service.json) |
| site-service | 8082 | Di tích, tìm di tích gần (`/api/heritage-sites/nearby`) | 3 | [site-service.json](openapi/site-service.json) |
| content-service | 8083 | Hiện vật, bản dịch, media, tải tệp, ngôn ngữ, tra hiện vật theo beacon | 14 | [content-service.json](openapi/content-service.json) |
| beacon-service | 8084 | iBeacon, gán hiện vật, vị trí trên sơ đồ, pin, registry cho app; thiết bị nhận FCM (`/api/me/devices`) | 9 | [beacon-service.json](openapi/beacon-service.json) |
| personalization-service | 8085 | Yêu thích, lịch sử tham quan (đồng bộ offline theo lô) | 6 | [personalization-service.json](openapi/personalization-service.json) |
| feedback-service | 8086 | Gửi/sửa đánh giá, đánh giá công khai, điểm trung bình, đánh giá của tôi, kiểm duyệt | 6 | [feedback-service.json](openapi/feedback-service.json) |
| analytics-service | 8087 | Thống kê tổng quan, thời gian thực, xuất PDF/Excel | 3 | [analytics-service.json](openapi/analytics-service.json) |

**Phân quyền tại Gateway** (chi tiết: `services/api-gateway/.../security/AccessRules.java`, [auth-jwt.md](auth-jwt.md)):

| Đường dẫn | Quyền |
|---|---|
| `POST /api/auth/register`, `login`, `refresh`, `logout` | Công khai |
| `/api/auth/me`, `/api/auth/users/**`, `/api/me/**` | Đã đăng nhập |
| `/api/admin/**`, `/api/statistics/**` | Chỉ ADMIN |
| `POST /api/artifacts/{id}/feedbacks` | VISITOR hoặc ADMIN |
| `GET /api/**` còn lại | Công khai |
| `POST`, `PUT`, `PATCH`, `DELETE` `/api/**` còn lại | Chỉ ADMIN |

Thiếu, sai hoặc hết hạn token → `401`; đúng token nhưng không đủ quyền → `403`.

**Xuất lại file OpenAPI** khi API thay đổi (hệ thống đang chạy, ở thư mục gốc repo):

```powershell
powershell -ExecutionPolicy Bypass -File Docs/api/openapi/export-openapi.ps1
```

Script in số đường dẫn của từng service; service nào không chạy thì báo `LỖI` và giữ nguyên file cũ của service đó.

## Nearby heritage sites

`GET /api/heritage-sites/nearby?lat={lat}&lng={lng}&radius={radius}`

| Tham số  | Bắt buộc | Ý nghĩa              | Ràng buộc              |
|----------|----------|----------------------|------------------------|
| `lat`    | Có       | Vĩ độ WGS-84         | -90 đến 90             |
| `lng`    | Có       | Kinh độ WGS-84       | -180 đến 180           |
| `radius` | Không    | Bán kính tìm kiếm km | > 0 và ≤ 50; mặc định 5  |

- Trả về khung `{ success, message, data: [...] }`, `data` là các di tích có tọa độ nằm trong bán kính, **sắp xếp gần → xa**.
- Từ tuần 4 endpoint này chạy ở `site-service` (8082), client vẫn gọi qua Gateway `http://localhost:8080`.
- Mỗi phần tử gồm các trường của `HeritageSiteResponse` + `distanceKm` (làm tròn 2 chữ số).
- Di tích chưa có tọa độ sẽ bị bỏ qua.
- Tham số sai → HTTP 400. Endpoint công khai (không cần token).

Ví dụ:

```http
GET /api/heritage-sites/nearby?lat=21.0277644&lng=105.8354028&radius=5
```

Chi tiết service, bảng đối chiếu khoảng cách 3 điểm và ví dụ request/response: [site-service.md](site-service.md).

## CMS coordinate selection

The heritage-site CMS uses Leaflet with OpenStreetMap tiles. Clicking the map sets the site's latitude and longitude; the existing create/update API persists those values. OpenStreetMap attribution is displayed by Leaflet.

Hiện vật và đa ngôn ngữ (backend chính W3): [Artifacts & Localization](artifacts-localization.md).

Luồng quét iBeacon → chi tiết hiện vật (Khoa ↔ Xuân Anh, chốt W3): [beacon-scan-flow.md](beacon-scan-flow.md).
