# site-service – Di tích và tìm di tích gần (Ân, cổng 8082, schema `site_db`)

Service tách từ Backend cũ theo `services/README.md` (5 bước). Code: `services/site-service/`.
Client (Web Admin, Mobile) **không đổi địa chỉ API**: vẫn gọi `http://localhost:8080`, Gateway chuyển
`/api/heritage-sites/**` xuống `http://localhost:8082` (`SITE_SERVICE_URL`).

| Thành phần | Địa chỉ |
|---|---|
| Qua Gateway (client dùng) | http://localhost:8080/api/heritage-sites |
| Chạy riêng (Swagger) | http://localhost:8082/swagger-ui.html |
| Prometheus | http://localhost:8082/actuator/prometheus |
| Bảng dữ liệu | `site_db.heritage_sites` (`Infra/db/02_site.sql`) |

## Định dạng phản hồi

Mọi phản hồi dùng khung chuẩn của module `common`:

```json
{ "success": true, "message": "Thành công", "data": { } }
```

Lỗi trả `{ "success": false, "message": "..." }` với mã HTTP đúng (400/404).
`WebAdmin/src/pages/heritage-sites.js` đọc `body.data` nên chạy được với cả service mới và Backend cũ.

## Endpoint

| Method | Đường dẫn | Quyền (Gateway kiểm tra) | Mô tả |
|---|---|---|---|
| GET | `/api/heritage-sites` | công khai | Danh sách di tích |
| GET | `/api/heritage-sites/nearby?lat=&lng=&radius=` | công khai | Di tích gần vị trí, sắp gần → xa |
| GET | `/api/heritage-sites/{id}` | công khai | Chi tiết; không tồn tại → 404 (content/beacon-service dùng để kiểm tra `siteId`) |
| POST | `/api/heritage-sites` | ADMIN | Thêm di tích, trả 201 |
| PUT | `/api/heritage-sites/{id}` | ADMIN | Sửa (cập nhật một phần); `clearCoordinates: true` để xóa tọa độ |
| DELETE | `/api/heritage-sites/{id}` | ADMIN | Xóa; không tồn tại → 404 |

### GET /api/heritage-sites/nearby

| Tham số | Bắt buộc | Ràng buộc |
|---|---|---|
| `lat` | Có | -90 đến 90 (WGS-84) |
| `lng` | Có | -180 đến 180 (WGS-84) |
| `radius` | Không | km, > 0 và ≤ 50, mặc định **5** |

- Khoảng cách tính bằng công thức Haversine, bán kính Trái Đất 6371.0088 km.
- Chỉ trả di tích **có tọa độ** và cách điểm tìm kiếm ≤ `radius`; sắp xếp gần → xa.
- `distanceKm` làm tròn 2 chữ số thập phân.
- `lat`/`lng` ngoài miền, `radius` ≤ 0 hoặc > 50 → HTTP 400.

```http
GET /api/heritage-sites/nearby?lat=21.0277644&lng=105.8354028&radius=5
```

```json
{
  "success": true,
  "message": "Thành công",
  "data": [
    {
      "id": 1,
      "name": "Văn Miếu - Quốc Tử Giám",
      "location": "58 Quốc Tử Giám, Đống Đa, Hà Nội",
      "mapImageUrl": "https://example.com/maps/van-mieu.png",
      "latitude": 21.0277644,
      "longitude": 105.8354028,
      "distanceKm": 0.0
    },
    {
      "id": 2,
      "name": "Hoàng thành Thăng Long",
      "location": "19C Hoàng Diệu, Ba Đình, Hà Nội",
      "mapImageUrl": "https://example.com/maps/hoang-thanh.png",
      "latitude": 21.0354430,
      "longitude": 105.8349380,
      "distanceKm": 0.86
    }
  ]
}
```

#### Đối chiếu tay 3 điểm (dán vào PR)

Điểm tìm kiếm: Văn Miếu `21.0277644, 105.8354028`.

| Di tích | Tọa độ | Khoảng cách tính tay (Haversine) | `distanceKm` API trả | Trong bán kính 5 km |
|---|---|---|---|---|
| Văn Miếu - Quốc Tử Giám | 21.0277644, 105.8354028 | 0 km (cùng điểm) | 0.0 | Có |
| Hoàng thành Thăng Long | 21.0354430, 105.8349380 | ≈ 0.855 km | 0.86 | Có |
| Điểm xa (Cầu Giấy) | 21.4000000, 105.8354028 | ≈ 41.391 km | 41.39 | Không (chỉ có khi `radius=50`) |

Ba điểm này được kiểm tự động trong `services/site-service/src/test/java/com/smartheritage/site/HeritageSiteApiTest.java`
(`nearby_sortsNearestFirstAndSkipsSitesOutsideRadius`, `nearby_uses5kmDefaultWhenRadiusIsMissing`).

### POST / PUT thân request

```json
{
  "name": "Chùa Một Cột",
  "location": "Ba Đình, Hà Nội",
  "mapImageUrl": "https://example.com/maps/chua-mot-cot.png",
  "latitude": 21.0358,
  "longitude": 105.8335,
  "clearCoordinates": false
}
```

- `name` bắt buộc, không được trùng di tích khác.
- `latitude` và `longitude` phải gửi cùng nhau; gửi một trường → 400.
- PUT là cập nhật một phần: bỏ trống trường nào thì giữ nguyên. Muốn xóa tọa độ (di tích rời khỏi
  bản đồ và `/nearby`) thì gửi `clearCoordinates: true` — CMS tự gửi khi người dùng xóa trống cả 2 ô.

## Chạy và test

```powershell
mvn -q -DskipTests install                        # ở thư mục gốc, cài module common
mvn -pl services/site-service test                # test chạy trên H2, không cần MySQL
mvn -pl services/site-service spring-boot:run     # cần MySQL 3307
```

Cho Gateway trỏ sang service này (mặc định đã trỏ `http://localhost:8082`):

```powershell
$env:SITE_SERVICE_URL="http://localhost:8082"; mvn -pl services/api-gateway spring-boot:run
```

Test qua Gateway `http://localhost:8080`: GET không token → 200; POST/PUT/DELETE không token → 401;
tài khoản VISITOR → 403; `admin / Test@1234` → 200.

| Biến môi trường | Mặc định |
|---|---|
| `SERVER_PORT` | 8082 |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | `jdbc:mysql://localhost:3307/site_db`, root, mật khẩu dev |
| `SITE_SERVICE_URL` (phía Gateway) | http://localhost:8082 |
