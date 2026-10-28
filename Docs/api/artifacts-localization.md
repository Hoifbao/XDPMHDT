# Hiện vật, localization và hợp đồng cho luồng beacon

Gateway: `http://localhost:8080`. CMS: `/src/pages/artifacts.html`.
Content-service: `8083`, MySQL `content_db`. Backend hiện có chạy profile
`site-service` tại `8082`, dùng `site_db`; đây là bước tách content khỏi monolith,
không phải tách toàn bộ auth/beacon thành các microservice riêng.
Content-service không truy cập repository hoặc khóa ngoại tới database di tích.
`siteId` được xác minh qua `GET site-service/api/heritage-sites/{id}` khi tạo
hoặc khi PUT có gửi siteId. siteName/locationName là tên chụp lúc xác minh,
không tự đồng bộ khi di tích đổi tên.

## API qua Gateway

| Method | Path | Ý nghĩa |
| --- | --- | --- |
| GET | `/api/artifacts?siteId=1` | Danh sách theo di tích (không truyền: tất cả) |
| GET | `/api/artifacts?search=drum` | Tìm tên gốc tiếng Việt; siteId ưu tiên nếu truyền cả hai |
| GET | `/api/artifacts/{id}?lang=vi` | Chi tiết; mặc định vi |
| GET | `/api/artifacts/beacon/{uuid}?lang=en` | Cùng cấu trúc chi tiết, dùng khi quét beacon |
| POST | `/api/artifacts` | Tạo hiện vật, contents và media trong một giao dịch |
| PUT | `/api/artifacts/{id}` | Cập nhật các trường có gửi |
| DELETE | `/api/artifacts/{id}` | Xóa cả hiện vật, bản dịch và media |
| GET | `/api/artifacts/{id}/contents` | Tất cả bản dịch để biên tập |
| GET | `/api/artifacts/{id}/contents/{lang}` | Một bản dịch, có fallback vi |
| POST | `/api/artifacts/{id}/contents` | Upsert một bản dịch |
| DELETE | `/api/artifacts/contents/{contentId}` | Xóa bản Anh; không được xóa bản Việt |

Mẫu request: [artifact-create.sample.json](artifact-create.sample.json).
Mẫu response theo cấu trúc API: [artifact-detail.sample.json](artifact-detail.sample.json)
và [artifact-detail-fallback.sample.json](artifact-detail-fallback.sample.json).
Các URL `example.com` là minh họa; thay bằng media URL thật khi nhập CMS.

`lang` hỗ trợ vi/en (không phân biệt hoa thường); giá trị khác trả 400.
Fallback theo **cả bản dịch**: thiếu bản Anh trả bản Việt, không ghép từng field.
Mỗi bản dịch được lưu cần title và description; audio/video rỗng được phép.
Response có `requestedLanguage`, `languageCode` thực tế và `fallback`.
`name`/`description` trên response detail đã được localize;
`content` là bản được chọn, `contents` chứa tất cả bản dịch để CMS biên tập.
Media dùng chung nằm ở `media[]`, media theo ngôn ngữ nằm trong `content`.

Để tương thích payload cũ, `locationId` là alias của `siteId`; không gửi hai ID
khác nhau. `imageUrls` giữ định dạng **chuỗi JSON chứa mảng URL** như DTO cũ,
không phải mảng trực tiếp. Trong CMS, nhập mỗi ảnh trên một dòng.
PUT có contents sẽ thay danh sách bản dịch (phải giữ vi); không gửi contents
thì giữ nguyên. `media: []` xóa media, không gửi media thì giữ nguyên.
Trường unique tùy chọn để null khi tạo để không chiếm mã/UUID rỗng.
Khi cập nhật, gửi chuỗi rỗng để bỏ gán artifactCode/beaconUuid; null hoặc bỏ field thì giữ nguyên.
List response giữ tên gốc, không localize; dùng detail khi hiển thị thuyết minh.

HTTP: tạo 201; đọc/sửa/xóa 200; ID không tồn tại 404; payload không hợp lệ
hoặc siteId không tồn tại 400; site-service lỗi hoặc timeout 503;
xung đột constraint khi lưu đồng thời 409. Lỗi có envelope success/message/data.

## Bàn giao Khoa — tuần 4

Luồng: nhận beacon UUID → GET `/api/artifacts/beacon/{uuid}?lang=en`
→ đọc `data.content`, `data.media` và `data.languageCode`.
UUID gán cho một hiện vật duy nhất trong content_db.
Đây là hợp đồng theo UUID của API hiện tại; nếu thiết bị dùng chung UUID và phân
biệt major/minor thì cần bổ sung hợp đồng tra cứu bộ ba trước khi tích hợp.
File mẫu là đề xuất bàn giao, **chưa có xác nhận của Khoa** trong phiên làm việc này.
Cần Khoa xác nhận: parse envelope, imageUrls là chuỗi JSON, fallback vi và cơ chế
UUID/major/minor thực tế. Không có tin nhắn nào đã được gửi cho Khoa.

## Chạy và kiểm thử

```powershell
docker compose -f Infra/docker-compose.yml up -d --build
# CMS: http://localhost:8080/src/pages/artifacts.html
# Tạo ít nhất một di tích qua /api/heritage-sites trước khi nhập hiện vật.
mvn -f Backend/content-service/pom.xml test
```

Chạy local ngoài Docker: MySQL 3307, `mvn -f Backend/pom.xml spring-boot:run
"-Dspring-boot.run.profiles=site-service"` và `mvn -f Backend/content-service/pom.xml
spring-boot:run`. CMS phải được phục vụ cùng origin với Gateway để gọi API.

Volume MySQL hiện có được giữ nguyên. Schema site_db/content_db tự tạo qua JDBC;
không tự sao chép dữ liệu từ smart_heritage_db. Với dữ liệu cũ, sao lưu trước,
khởi động service tạo schema, rồi dùng script chuyển dữ liệu trong
`Infra/migrate-content.sql` sau khi kiểm tra ID và UUID trùng. Script dành cho
database đích trống; chạy trong client dừng ngay và ROLLBACK khi lỗi, không dùng
chế độ bỏ qua lỗi. Không cần xóa volume. Script chỉ chuyển di tích và content;
không chuyển tài khoản và cấu hình beacon cũ.
Monolith cũ vẫn chạy được với profile mặc định (8080); dừng nó trước khi chạy
Gateway vì trùng cổng. Profile site-service tắt ArtifactController/ArtifactService;
các model monolith còn giữ để không phá module beacon cũ. Luồng beacon đọc hiện vật
mới đi qua content-service; CRUD gán beacon cũ chưa được chuyển sang gọi content-service.
