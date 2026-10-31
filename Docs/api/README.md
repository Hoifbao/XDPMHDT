# API Documentation

Store the OpenAPI/Swagger specification and API usage notes here. Keep the specification aligned with the REST API implemented in `Backend/`.

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
