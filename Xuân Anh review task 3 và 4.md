# Xuân Anh review task 3 và 4

## Cập nhật sau góp ý leader — 29/10/2026

**Phần cập nhật này thay thế các kết luận trạng thái bên dưới. Phần cũ được giữ làm lịch sử nhánh XA-task3v4; các đường dẫn content-service/Infra ở phần cũ không thuộc nhánh W3 hiện tại.**

### Đã sửa trong nhánh feature/xa-w3-localization

- Tạo nhánh từ origin/main sau khi fetch, không tiếp tục phát triển trên nhánh cũ chậm main.
- Chỉ lấy CMS, ba JSON mẫu, tài liệu localization và file review từ nhánh cũ.
- Chuyển logic localization/fallback vào Backend chính: GET theo ID và beacon nhận lang mặc định vi; response có requestedLanguage, languageCode, fallback, content. Ngôn ngữ không hỗ trợ trả 400; hiện vật không tồn tại trả 404.
- Bổ sung DTO và xử lý siteId/locationId, media và thay bản dịch khi PUT để CMS hoạt động với backend chính. Giữ transaction cho hiện vật, media và bản dịch; bắt buộc bản Việt, không cho xóa riêng bản Việt.
- Thêm auth.js với data-guard="admin" ngay trước artifacts.js. CMS lấy base từ window.SHAuth.apiBase để request đi qua cơ chế gắn JWT/refresh có sẵn trên main.
- Thêm liên kết localization vào Docs/api/README.md; sửa tài liệu để hướng dẫn backend chính thay vì stack W4 cũ.
- Không đưa Backend/content-service, Infra, file tên git hoặc Interface/pubspec.lock vào phạm vi thay đổi W3. Workspace có thay đổi pubspec.lock ngoài phạm vi; không đưa file này vào commit/PR.

### Kiểm tra và giới hạn

- Đã bổ sung ArtifactLocalizationTest cho backend chính: chọn bản Anh; fallback Việt trên cả ID/beacon; mặc định vi; lang không hỗ trợ; hiện vật không tồn tại; bắt buộc và bảo vệ bản Việt.
- Test dùng MockMvc standalone và repository giả lập, không xác nhận persistence MySQL hoặc bảo mật toàn stack.
- Kết quả: `mvn -f Backend/pom.xml test -q` thành công, 8 test (4 JwtServiceTest + 4 ArtifactLocalizationTest), không lỗi hoặc bỏ qua. `node --check WebAdmin/src/pages/artifacts.js` thành công; cả ba JSON mẫu parse hợp lệ.
- Chưa nghiệm thu Swagger trên database demo hoặc thêm/sửa/xóa bằng trình duyệt đăng nhập thật. Cần chạy checklist trong Docs/api/artifacts-localization.md trước khi báo hoàn tất nghiệm thu.
- Theo yêu cầu tiếp theo: đưa bản sửa sang `XA-task3v4`, đồng bộ `origin/main` để có JWT mới. Commit code `7129ec5`; file review được commit riêng trên `REVIEW`. Chưa tạo PR.
- Khi đồng bộ main, xung đột Infra/docker-compose.yml và Interface/pubspec.lock được giải quyết theo bản main; không mang cấu hình W4 cũ hoặc lockfile phát sinh tại workspace vào commit. Nhánh XA vẫn giữ lịch sử và code W4 cũ, không phải PR W3 sạch từ main.

### Phối hợp và W4 còn lại

- Gửi Khoa [JSON chi tiết](Docs/api/artifact-detail.sample.json) để xác nhận envelope, imageUrls, fallback và UUID/major/minor. Chưa gửi hoặc nhận xác nhận trong phiên này.
- Thống nhất với Phú API cache Redis: đề xuất khóa gồm ID/UUID + lang yêu cầu và vô hiệu cache vi/en khi hiện vật/bản dịch/media đổi; chi tiết trong tài liệu localization. Chưa có xác nhận của Phú.
- W4 **chưa hoàn thành theo khung chung**. Chờ PR W4 của Bảo merge, đọc services/README.md và dựng lại services/content-service theo 5 bước. Main hiện chưa có thư mục services; không tự dựng khung thay Bảo.
- Code W4 cũ được giữ nguyên trên nhánh XA-task3v4 để tái sử dụng sau; không kéo cấu hình Infra cũ sang PR W3.

---

## Lịch sử review nhánh XA-task3v4 (trước góp ý leader)

Ngày cập nhật: 28/10/2026.

## Phạm vi và quy ước

- **Task 3** tương ứng với nhiệm vụ 1 đã giao: Hiện vật, Đa phương tiện & Đa ngôn ngữ (Artifacts & Localization).
- **Task 4** tương ứng với nhiệm vụ 2 đã giao: Tách content-service (8083, content_db), kiểm tra siteId qua site-service và CMS chạy qua Gateway.
- Báo cáo phản ánh phần code đã triển khai và kiểm tra trong phiên làm việc; không đồng nghĩa với đã triển khai thành công toàn bộ hệ thống thực tế.
- Phần code chính đã có. Chạy toàn stack Docker, kiểm thử trình duyệt, migration thực tế và xác nhận hợp đồng với Khoa vẫn chưa hoàn tất.

## Task 3 — Hiện vật, Đa phương tiện & Đa ngôn ngữ

### 1. Backend — API đa ngôn ngữ

| Hạng mục | Trạng thái | Chi tiết |
| --- | --- | --- |
| Lấy chi tiết theo `?lang=vi/en` | Đã làm | `GET /api/artifacts/{id}?lang=vi` hoặc `lang=en`; mặc định tiếng Việt. |
| Fallback tiếng Việt | Đã làm | Thiếu bản tiếng Anh thì trả bản tiếng Việt. |
| Thông tin ngôn ngữ thực tế | Đã làm | Response có `requestedLanguage`, `languageCode`, `fallback`. |
| Nội dung theo ngôn ngữ | Đã làm | `content` là bản được chọn; `name` và `description` trên response chi tiết được localize. |
| Tất cả bản dịch để CMS biên tập | Đã làm | Response giữ `contents[]`. |
| Localization khi tra cứu beacon | Đã làm | `GET /api/artifacts/beacon/{uuid}?lang=en` dùng cùng cấu trúc chi tiết và fallback. |
| Bắt buộc bản Việt | Đã làm | Tạo hiện vật hoặc thay danh sách bản dịch phải giữ vi; không cho xóa riêng bản Việt. |
| Kiểm tra bản dịch | Đã làm | Kiểm tra ngôn ngữ, tiêu đề, mô tả và ngôn ngữ trùng; hỗ trợ vi/en. |
| Localization danh sách | Chưa làm | API danh sách trả tên gốc; localization áp dụng ở API chi tiết và beacon. |
| Fallback từng trường | Không triển khai | Fallback theo cả bản dịch; không tự ghép audio Việt vào bản Anh khi thiếu audio. |

#### Các endpoint hiện có

```text
GET    /api/artifacts
GET    /api/artifacts?siteId=1
GET    /api/artifacts?search=keyword
GET    /api/artifacts/{id}?lang=vi
GET    /api/artifacts/beacon/{uuid}?lang=en

POST   /api/artifacts
PUT    /api/artifacts/{id}
DELETE /api/artifacts/{id}

GET    /api/artifacts/{id}/contents
GET    /api/artifacts/{id}/contents/{lang}
POST   /api/artifacts/{id}/contents
DELETE /api/artifacts/contents/{contentId}
```

### 2. Backend — Media và lưu dữ liệu

| Hạng mục | Trạng thái | Chi tiết |
| --- | --- | --- |
| Media dùng chung | Đã làm | Lưu `media[]`, gồm IMAGE/AUDIO/VIDEO, URL và chú thích. |
| Media theo ngôn ngữ | Đã làm | Bản dịch hỗ trợ `audioUrl`, `videoUrl`, `imageUrls`, `transcript`. |
| Kiểm tra URL | Đã làm | Kiểm tra HTTP/HTTPS và một số giới hạn độ dài. |
| Lưu nguyên tử | Đã làm | Hiện vật, bản dịch và media được lưu trong cùng giao dịch. |
| Thay danh sách bản dịch | Đã làm | PUT có contents sẽ thay danh sách, vẫn phải giữ vi; không gửi contents thì giữ nguyên. |
| Thay hoặc xóa media | Đã làm | `media: []` xóa danh sách; không gửi media thì giữ nguyên. |
| Xóa dữ liệu liên quan | Đã làm | Xóa hiện vật sẽ xóa cả bản dịch và media. |
| Xác minh file media truy cập được | Chưa làm | Chưa kiểm tra URL có file thật, MIME hoặc khả năng phát audio/video. |
| Upload file | Chưa triển khai; ngoài yêu cầu URL hiện tại | Chỉ quản lý link media, chưa upload lên máy chủ hoặc kho lưu trữ. |

#### Điểm cần thống nhất về JSON

`imageUrls` giữ kiểu **chuỗi JSON chứa mảng URL** theo DTO cũ, không phải mảng JSON trực tiếp:

```json
{
  "imageUrls": "[\"https://example.com/image.jpg\"]"
}
```

- `locationId` được giữ làm alias tương thích của `siteId`; không gửi hai ID khác nhau.
- Khi tạo, trường unique tùy chọn có thể để null.
- Khi cập nhật, gửi chuỗi rỗng để bỏ gán artifactCode/beaconUuid; bỏ field hoặc gửi null thì giữ nguyên.
- URL example.com trong các file mẫu là minh họa, cần thay bằng link media thật.

### 3. Web — CMS Hiện vật

File chính:

- [artifacts.html](WebAdmin/src/pages/artifacts.html)
- [artifacts.js](WebAdmin/src/pages/artifacts.js)

| Hạng mục | Trạng thái | Chi tiết |
| --- | --- | --- |
| Form gọi API thật | Đã làm | Thay hành vi chỉ hiển thị thông báo mẫu bằng request API. |
| Tải di tích thật | Đã làm | Lấy `/api/heritage-sites`, bỏ danh sách di tích cố định. |
| Thông tin hiện vật | Đã làm | Tên, mã, mô tả, thumbnail, niên đại, chất liệu, kích thước, beacon UUID và trạng thái. |
| Bản dịch Việt/Anh | Đã làm | Thêm bản dịch, kiểm tra trùng ngôn ngữ và bắt buộc bản Việt. |
| Media URL | Đã làm | Media dùng chung và ảnh/audio/video theo bản dịch. |
| Tạo hiện vật | Đã làm | POST, thông báo kết quả, ghi ID vào URL. |
| Tải hiện vật để sửa | Đã làm | Dùng `?id=...`, tải dữ liệu và điền form. |
| Cập nhật | Đã làm | PUT hiện vật đang mở. |
| Danh sách và xóa | Đã làm | Danh sách lấy từ API; có xác nhận khi xóa. |
| Trạng thái gửi request | Đã làm | Khóa nút lưu khi đang gửi; hiện kết quả hoặc lỗi. |
| Gọi qua Gateway | Đã làm trong code | CMS phục vụ qua Gateway dùng cùng origin cho `/api/...`. |
| Preview media | Chưa làm | Chưa có xem ảnh, nghe audio hoặc phát video ngay trong form. |
| Biên tập transcript | Chưa làm | Backend có transcript nhưng CMS chưa có ô nhập riêng. |
| Bộ lọc và tìm kiếm trên CMS | Chưa làm | Backend có API, giao diện chưa có điều khiển tương ứng. |
| Kiểm thử trên trình duyệt | Chưa làm | Đã kiểm tra cú pháp JavaScript; chưa chạy kiểm thử UI đầu cuối. |

### 4. Phối hợp Khoa và JSON mẫu cho tuần 4

#### Đã làm

- Viết tài liệu hợp đồng API cho luồng quét beacon.
- Lưu các file trong `Docs/api/`:
  - [artifact-create.sample.json](Docs/api/artifact-create.sample.json): request tạo hiện vật.
  - [artifact-detail.sample.json](Docs/api/artifact-detail.sample.json): response chi tiết tiếng Anh.
  - [artifact-detail-fallback.sample.json](Docs/api/artifact-detail-fallback.sample.json): response fallback tiếng Việt.
- Mô tả envelope `success/message/data`, nội dung được chọn, media và thông tin fallback.
- Kiểm tra cả 3 file JSON parse hợp lệ.

#### Chưa làm / chưa chốt

- Chưa liên hệ hoặc nhận xác nhận trực tiếp từ Khoa; chưa gửi tin nhắn cho Khoa trong phiên làm việc.
- JSON hiện là đề xuất bàn giao, chưa phải hợp đồng được hai bên xác nhận.
- Chưa xác nhận thiết bị dùng UUID duy nhất hay UUID + major + minor.
- API hiện tra cứu bằng UUID duy nhất. Nếu beacon dùng chung UUID, phải bổ sung hợp đồng tra cứu theo bộ ba.
- Chưa tích hợp và thử luồng quét trên ứng dụng/thiết bị của Khoa.

#### Nội dung cần Khoa xác nhận

1. Cách parse envelope `success/message/data`.
2. `imageUrls` là chuỗi JSON chứa mảng URL.
3. Cách dùng `requestedLanguage`, `languageCode`, `fallback` và `content`.
4. Cách định danh beacon thực tế: UUID hay UUID/major/minor.
5. Cách xử lý không tìm thấy hiện vật và lỗi service trên ứng dụng.

## Task 4 — Tách content-service và CMS qua Gateway

### 1. Phần đã triển khai

| Hạng mục | Trạng thái | Chi tiết |
| --- | --- | --- |
| Module content-service riêng | Đã làm | `Backend/content-service`, có Maven project và application riêng. |
| Cổng 8083 | Đã cấu hình | Có thể ghi đè bằng biến môi trường. |
| Database content_db | Đã cấu hình | Datasource riêng cho content. |
| Quản lý hiện vật | Đã có trong service mới | Controller, service, repository, entity và DTO. |
| Nội dung đa ngôn ngữ | Đã có trong service mới | Lưu và xử lý bản dịch tại content-service. |
| Media | Đã có trong service mới | Lưu và xử lý cùng thao tác hiện vật. |
| Bỏ quan hệ JPA tới di tích | Đã làm trong content-service | Dùng siteId dạng giá trị; không phụ thuộc entity HeritageSite của backend cũ. |
| Kiểm tra siteId qua HTTP | Đã làm | Gọi site-service `/api/heritage-sites/{id}` khi tạo hoặc PUT có gửi siteId. |
| Timeout và lỗi site-service | Đã làm | Có timeout; site không tồn tại trả 400, service không sẵn sàng trả 503. |
| Backend đóng vai trò site-service | Đã cấu hình | Profile site-service, cổng 8082, database site_db. |
| Tắt API hiện vật cũ trong profile site-service | Đã làm | Tắt ArtifactController và ArtifactService cũ ở profile này. |
| Gateway Nginx | Đã cấu hình | Cổng 8080; `/api/artifacts...` tới content-service, các `/api/...` khác tới backend chạy profile site-service. |
| Phục vụ CMS qua Gateway | Đã cấu hình | Phục vụ WebAdmin và chuyển tiếp request API. |
| Dockerfile và Compose | Đã thêm | Có cấu hình build/chạy hai service và Gateway. |
| Script chuyển dữ liệu | Đã viết | `Infra/migrate-content.sql`, dành cho database đích trống. |

### 2. Kiến trúc hiện tại trong cấu hình

```text
Trình duyệt / CMS
       |
       v
Gateway Nginx :8080
       |
       +-- /api/artifacts... --> content-service :8083 --> content_db
       |                              |
       |                              +-- HTTP kiểm tra siteId --+
       |                                                        |
       +-- /api/... ----------> backend profile site-service :8082
                                            |
                                            v
                                         site_db
```

Sơ đồ mô tả cấu hình đã viết, chưa phải kết quả xác minh stack Docker đang chạy.

### 3. Phần chưa hoàn tất và giới hạn

#### 3.1. Chưa chạy toàn bộ stack Docker

- Môi trường làm việc không có Docker CLI.
- Chưa xác nhận khởi động đồng thời Gateway, content-service, site-service và MySQL.
- Chưa kiểm thử request thực tế từ trình duyệt qua Gateway tới các service và database.

#### 3.2. Chưa chạy migration

- Script đã có nhưng chưa áp dụng lên database thực tế.
- Dữ liệu `smart_heritage_db` chưa tự động được chuyển sang site_db/content_db.
- Script chỉ chuyển di tích và content; không chuyển tài khoản hay cấu hình beacon cũ.
- Cần sao lưu, dừng ghi và dùng database đích trống; kiểm tra ID, mã và UUID trùng trước khi chạy.
- Cần chạy trong client dừng khi lỗi và ROLLBACK, không dùng chế độ bỏ qua lỗi.
- Không cần xóa volume MySQL hiện có.

#### 3.3. site-service chưa là module độc lập

- Hiện dùng profile site-service của backend cũ.
- Chưa tạo Maven module site-service độc lập.
- Backend còn giữ model và nghiệp vụ monolith cũ.

#### 3.4. Chưa loại bỏ mọi phụ thuộc content khỏi monolith

- Entity/repository hiện vật cũ còn giữ vì module beacon cũ đang phụ thuộc.
- Đã tách service phục vụ content nhưng chưa hoàn tất việc gỡ mọi phụ thuộc content trong backend cũ.
- Vì vẫn giữ các model cũ, profile site-service chưa có phạm vi persistence chỉ giới hạn ở dữ liệu di tích.

#### 3.5. CRUD gán beacon cũ chưa chuyển đổi

- API đọc nội dung hiện vật theo beacon tại content-service đã có.
- Phần quản lý/gán beacon cũ chưa chuyển sang gọi content-service.
- Cần thống nhất nguồn hiện vật để tránh phần gán beacon cũ truy cập dữ liệu hiện vật trong database cũ hoặc bảng cũ của backend.

#### 3.6. Tên di tích chưa tự đồng bộ

- Tên di tích trong content được chụp lúc xác minh siteId.
- Khi site-service đổi tên di tích, locationName trong content chưa tự cập nhật ngay.

#### 3.7. Chưa kiểm thử MySQL thực tế

- Kiểm thử persistence hiện dùng H2 chế độ MySQL.
- Chưa xác nhận schema, migration và lưu dữ liệu trên MySQL Docker thực tế.
- H2 không thay thế hoàn toàn kiểm thử tích hợp MySQL.

### 4. File triển khai và tài liệu chính

| File / thư mục | Vai trò |
| --- | --- |
| [Backend/content-service/](Backend/content-service/README.md) | Module content-service và hướng dẫn |
| [application-site-service.properties](Backend/src/main/resources/application-site-service.properties) | Profile site-service của backend cũ |
| [Infra/docker-compose.yml](Infra/docker-compose.yml) | Cấu hình stack |
| [Infra/Dockerfile.backend](Infra/Dockerfile.backend) | Build backend cho vai trò site-service |
| [Backend/content-service/Dockerfile](Backend/content-service/Dockerfile) | Build content-service |
| [Infra/gateway.conf](Infra/gateway.conf) | Gateway và phục vụ WebAdmin |
| [Infra/services-databases.sql](Infra/services-databases.sql) | Tạo database cho service |
| [Infra/migrate-content.sql](Infra/migrate-content.sql) | Chuyển dữ liệu cũ theo phạm vi đã nêu |
| [Docs/api/artifacts-localization.md](Docs/api/artifacts-localization.md) | Hợp đồng API, JSON, cách chạy và bàn giao Khoa |

## Kiểm thử đã thực hiện

### 1. Năm test đã pass

| Test | Nội dung xác minh |
| --- | --- |
| Fallback và beacon | Thiếu tiếng Anh trả tiếng Việt; response beacon cùng hợp đồng; từ chối lang không hỗ trợ; beacon không tồn tại; bỏ gán beacon bằng chuỗi rỗng. |
| Bản dịch, media và xóa | Thêm bản Anh; lấy nội dung Anh; thay bản dịch/media; xóa hiện vật và dữ liệu liên quan. |
| Payload không hợp lệ | Từ chối tạo thiếu bản Việt hoặc URL media không hợp lệ. |
| Site không tồn tại / lỗi | Không ghi hiện vật khi siteId không hợp lệ hoặc site-service không sẵn sàng. |
| SiteClient HTTP | Gọi HTTP thật tới server giả lập; kiểm tra site hợp lệ và ánh xạ phản hồi lỗi. |

File test:

- [ArtifactApiTest.java](Backend/content-service/src/test/java/com/smartheritage/content/ArtifactApiTest.java)
- [SiteClientTest.java](Backend/content-service/src/test/java/com/smartheritage/content/SiteClientTest.java)

### 2. Kiểm tra bổ sung đã đạt

- Maven kiểm tra backend hiện có thành công; backend hiện có chưa có bộ test nghiệp vụ mới trong phạm vi này.
- Content-service compile và chạy 5 test thành công.
- JavaScript qua `node --check`.
- Ba file JSON mẫu parse thành công.
- `git diff --check` trong phạm vi thay đổi không báo lỗi khoảng trắng; có cảnh báo chuyển LF/CRLF.

### 3. Chưa kiểm thử

- Trình duyệt và thao tác form thực tế.
- Toàn stack Docker/Gateway.
- Database MySQL thực tế.
- Chạy migration dữ liệu.
- Luồng quét beacon trên mobile và thiết bị thực tế.
- Tích hợp với code của Khoa.

## Checklist còn lại để nghiệm thu

### Các việc cần ưu tiên

- [ ] Chạy `docker compose -f Infra/docker-compose.yml up -d --build` trên máy có Docker.
- [ ] Kiểm tra khởi động Gateway 8080, site-service 8082, content-service 8083 và MySQL.
- [ ] Tạo di tích qua API/CMS di tích trước khi tạo hiện vật.
- [ ] Mở `http://localhost:8080/src/pages/artifacts.html` và thử thêm/sửa/xóa hiện vật.
- [ ] Kiểm tra dữ liệu Việt/Anh, fallback và media qua Gateway.
- [ ] Kiểm tra lỗi siteId không tồn tại và site-service bị dừng trong stack thực tế.
- [ ] Chạy migration trên bản sao dữ liệu cũ, xác minh số bản ghi và liên kết.
- [ ] Khoa xác nhận JSON và định danh UUID hay UUID/major/minor.
- [ ] Tích hợp và thử quét beacon thực tế.
- [ ] Chuyển CRUD gán beacon cũ sang tham chiếu content-service.
- [ ] Gỡ các phụ thuộc content còn lại trong backend cũ nếu tiêu chí nghiệm thu yêu cầu tách microservice hoàn toàn.

### Các phần hoàn thiện giao diện và vận hành còn thiếu

- [ ] Preview ảnh/audio/video trên CMS nếu cần cho nghiệm thu.
- [ ] Thêm trường transcript trên CMS nếu cần biên tập từ web.
- [ ] Thêm tìm kiếm/lọc danh sách trên giao diện nếu cần.
- [ ] Thống nhất nhu cầu localization danh sách hiện vật.
- [ ] Thống nhất cơ chế cập nhật tên di tích khi site-service đổi tên.

## Lưu ý về workspace và Git

- Workspace có nhiều thay đổi và xung đột Git từ trước phiên làm việc.
- Các xung đột ngoài phạm vi hai task chưa được giải quyết.
- Không coi việc code và test thành công là xác nhận repository đã sẵn sàng merge hoặc deploy.
- Backend mặc định cũ và Gateway đều dùng cổng 8080; cần dừng backend mặc định trước khi chạy Gateway, hoặc đổi cổng.

## Kết luận trạng thái

- **Task 3:** đã triển khai API localization/fallback, media, CMS gọi API và JSON mẫu; chưa hoàn tất xác nhận với Khoa, kiểm thử trình duyệt và tích hợp beacon thực tế.
- **Task 4:** đã triển khai module content-service, cấu hình database/cổng, kiểm tra siteId qua HTTP và Gateway; chưa chạy stack Docker, migration thực tế và chưa gỡ toàn bộ phụ thuộc content khỏi monolith/beacon cũ.

## Giải trình của Xuân Anh về các hạng mục chưa hoàn thành

Phần dưới đây là giải trình của mình dựa trên trạng thái triển khai và kiểm thử đã ghi nhận. Mình phân biệt giữa phần đã viết code, phần đã kiểm tra bằng test và phần đã chạy thành công trong hệ thống thực tế. Những hạng mục chưa có đủ kết quả xác minh vẫn được mình để ở trạng thái chưa hoàn thành.

### 1. Chưa chạy kiểm thử toàn bộ hệ thống bằng Docker và MySQL thực tế

Trong môi trường thực hiện task chưa có Docker CLI, nên mình chưa chạy được stack gồm Gateway, site-service, content-service và MySQL theo Docker Compose. Mình đã chuẩn bị Dockerfile, cấu hình kết nối, routing và database, nhưng chưa có kết quả thực tế để xác nhận các container khởi động và giao tiếp đúng với nhau.

Các test hiện tại dùng H2 ở chế độ MySQL và site-service giả lập. Những test này xác minh được logic lưu dữ liệu, fallback, kiểm tra siteId và xử lý lỗi ở phạm vi kiểm thử, nhưng chưa chứng minh được kết nối mạng giữa container, thời điểm service sẵn sàng, cấu hình MySQL hoặc cách Gateway chuyển tiếp request trong môi trường chạy thật.

Vì vậy, mình chưa đánh dấu phần triển khai toàn stack là hoàn thành. Việc còn lại của mình là chạy trên máy có Docker, kiểm tra log từng service và thử các request từ Gateway đến database, bao gồm cả tình huống site-service ngừng hoạt động.

### 2. Chưa kiểm thử CMS trực tiếp trên trình duyệt

Mình đã nối form với API thật và kiểm tra cú pháp JavaScript. Tuy nhiên, trong phiên thực hiện chưa chạy một phiên trình duyệt để thao tác đầy đủ từ tải danh sách di tích, nhập bản dịch, thêm media, lưu, tải lại, sửa đến xóa hiện vật.

Đây là phần kiểm chứng còn thiếu của mình. Kiểm tra cú pháp không xác nhận được toàn bộ hành vi DOM, validation của trình duyệt, trạng thái nút bấm, thông báo lỗi hay khả năng giữ đúng dữ liệu sau khi tải lại trang. Việc chưa chạy toàn stack cũng khiến mình chưa xác minh được luồng CMS gọi API qua Gateway trong môi trường dự kiến sử dụng.

Mình cần thực hiện một lượt kiểm thử trình duyệt với dữ liệu thật và ghi lại kết quả trước khi kết luận CMS đã được nghiệm thu đầy đủ.

### 3. Chưa chốt JSON và tích hợp luồng beacon với Khoa

Mình đã chuẩn bị cấu trúc response, tài liệu và JSON mẫu để bàn giao. Tuy nhiên, trong phiên làm việc chưa có trao đổi trực tiếp hoặc phản hồi xác nhận của Khoa. Vì vậy, mình mới hoàn thành phần đề xuất hợp đồng API, chưa hoàn thành yêu cầu phối hợp chốt hợp đồng giữa hai bên.

Điểm cần xác nhận trước tiên là cách nhận diện beacon. API hiện tại tra cứu theo UUID duy nhất, trong khi thiết bị có thể dùng chung UUID và phân biệt bằng major/minor. Mình chưa có thông tin xác nhận cấu hình thiết bị thực tế để quyết định có cần bổ sung endpoint theo bộ ba hay không.

Ngoài ra, mình giữ `imageUrls` dưới dạng chuỗi JSON để tương thích DTO cũ. Khoa cần xác nhận cách parse trường này, envelope `success/message/data` và các trường fallback trên mobile. Khi chưa có xác nhận và chưa thử trên luồng quét thực tế, mình không thể coi file JSON mẫu là hợp đồng đã chốt hoặc coi tích hợp beacon là hoàn thành.

Phần tiếp theo mình cần làm là trao đổi với Khoa về các điểm này, điều chỉnh hợp đồng nếu cần và cùng thử một lượt quét beacon đến màn hình chi tiết hiện vật.

### 4. Chưa chuyển dữ liệu cũ sang site_db và content_db

Mình đã viết script migration nhưng chưa chạy trên database thực tế. Script hiện dành cho database đích trống và giữ lại ID từ hệ thống cũ. Nếu chạy trên database đã có dữ liệu mà chưa đối chiếu, có thể gặp trùng ID, mã hiện vật hoặc UUID, dẫn đến lỗi hoặc liên kết dữ liệu không đúng.

Mình chưa có kết quả kiểm tra trạng thái database đích và chưa thực hiện bước sao lưu, chạy thử trên bản sao, đối chiếu số bản ghi. Vì vậy, mình chưa chạy migration trực tiếp và chưa xác nhận dữ liệu cũ đã được chuyển thành công.

Script hiện chỉ chuyển di tích, hiện vật, bản dịch và media; chưa chuyển tài khoản hoặc cấu hình beacon cũ. Mình cần xác minh phạm vi dữ liệu cần giữ, chạy thử migration có kiểm soát và kiểm tra các liên kết trước khi dùng database mới cho hệ thống thực tế.

### 5. Chưa tách hoàn toàn site-service và loại bỏ phụ thuộc content khỏi backend cũ

Mình đã tạo content-service thành module riêng, dùng database riêng và kiểm tra siteId qua HTTP. Tuy nhiên, site-service hiện được chạy bằng profile của backend cũ, chưa phải một module độc lập chỉ chứa nghiệp vụ di tích.

Lý do phần tách này chưa hoàn tất là module beacon cũ vẫn sử dụng entity/repository hiện vật trong monolith. Nếu chỉ xóa các lớp đó thì phần quản lý và gán beacon sẽ mất phụ thuộc cần thiết. Trong triển khai hiện tại, mình giữ lại các lớp cũ và tắt controller/service hiện vật cũ trong profile site-service để service mới tiếp nhận API content.

Đây vẫn là một giới hạn kiến trúc, không có nghĩa là việc tách hoàn toàn không làm được. Mình cần chuyển các nơi đang truy cập hiện vật nội bộ sang hợp đồng với content-service, xác minh luồng gán beacon rồi mới gỡ entity/repository và thu hẹp phạm vi persistence của site-service. Phần này cần được làm tiếp nếu tiêu chí nghiệm thu yêu cầu tách microservice hoàn toàn.

### 6. Chưa chuyển CRUD gán beacon cũ sang content-service

API đọc nội dung theo beacon ở content-service đã có, nhưng luồng quản lý/gán beacon cũ vẫn kiểm tra hiện vật qua repository của backend cũ. Hai luồng này chưa dùng chung một nguồn dữ liệu hiện vật sau khi tách database.

Để hoàn tất, mình phải thay phần tra cứu và kiểm tra hiện vật trong nghiệp vụ gán beacon bằng lời gọi sang content-service, đồng thời xử lý các trường hợp hiện vật không tồn tại, không thuộc đúng di tích hoặc service không sẵn sàng. Phần này cũng cần khớp với cách định danh beacon mà Khoa xác nhận.

Mình chưa triển khai chuyển đổi đó trong đợt này. Vì vậy, API đọc hiện vật theo beacon đã có nhưng chưa thể kết luận toàn bộ vòng đời quản lý và quét beacon đã hoạt động thống nhất với content-service.

### 7. Các phần giao diện còn thiếu: preview media, transcript, tìm kiếm và bộ lọc

Phần CMS mình đã triển khai tập trung vào nhập thông tin hiện vật, URL media, bản Việt/Anh và lưu dữ liệu qua API. Preview ảnh/audio/video chưa được bổ sung; yêu cầu ban đầu về media URL cũng chưa nêu rõ cần trình phát hoặc xem trước trong form.

Riêng transcript là trường backend đã hỗ trợ nhưng mình chưa đưa vào giao diện biên tập. Đây là phần chưa đồng bộ hết khả năng của backend lên CMS. Nếu quản trị viên cần nhập lời thuyết minh hoặc nội dung hỗ trợ tiếp cận từ web, mình cần bổ sung trường này và kiểm tra khả năng lưu, tải lại, cập nhật.

Backend đã có tìm kiếm/lọc, nhưng mình chưa xây dựng ô tìm kiếm và bộ lọc trên danh sách CMS. Mình không coi các phần này là đã có chỉ vì API đã hỗ trợ. Khi nghiệm thu, cần xác định phần nào là bắt buộc và mình sẽ bổ sung giao diện tương ứng.

### 8. Chưa localization danh sách và chưa fallback theo từng trường

Mình áp dụng localization cho API chi tiết hiện vật và tra cứu beacon, là hai nơi trả nội dung thuyết minh cho người dùng. API danh sách hiện giữ tên gốc, chưa nhận và xử lý lang để đổi tên từng hiện vật trong danh sách. Nếu yêu cầu đa ngôn ngữ áp dụng cả màn hình danh sách thì đây là phần mình còn phải bổ sung.

Cơ chế fallback hiện tại chọn cả bản tiếng Việt khi thiếu bản tiếng Anh. Mình chưa xây dựng quy tắc ghép từng trường giữa hai bản dịch, chẳng hạn nội dung Anh nhưng dùng audio Việt khi thiếu audio Anh. Việc ghép như vậy sẽ tạo nội dung có nhiều ngôn ngữ trong cùng một bản trả về và cần được thống nhất về trải nghiệm sử dụng. Vì thế mình ghi rõ giới hạn hiện tại thay vì tự coi từng trường trống là trường hợp đã có fallback.

### 9. Chưa kiểm tra media có tồn tại và chưa có upload file

Mình đã kiểm tra định dạng URL HTTP/HTTPS, nhưng chưa triển khai bước tải hoặc kiểm tra tài nguyên từ URL để xác minh file tồn tại, MIME đúng hoặc trình duyệt có phát được. Đây là phần xác minh nội dung media còn thiếu; URL hợp lệ về cú pháp vẫn có thể trỏ đến file bị xóa, yêu cầu quyền truy cập hoặc không hỗ trợ phát trên thiết bị.

Upload file chưa được triển khai vì yêu cầu ban đầu sử dụng link media dạng URL. Chức năng upload cần thêm nơi lưu file, giới hạn dung lượng, kiểm tra loại file và cơ chế quyền truy cập. Nếu phạm vi được mở rộng sang upload, mình cần thiết kế và thực hiện phần đó riêng; hiện CMS chỉ nhận các URL đã được cung cấp sẵn.

### 10. Chưa tự đồng bộ tên di tích khi site-service thay đổi

Content-service hiện lưu siteId và tên di tích chụp tại thời điểm kiểm tra. Cách này giúp trả thông tin chi tiết mà không phải gọi site-service mỗi lần đọc, nhưng tên có thể cũ sau khi di tích được đổi tên.

Mình chưa triển khai gọi lại khi đọc, cơ chế làm mới định kỳ hoặc sự kiện cập nhật tên. Mỗi phương án có cách xử lý độ trễ và phụ thuộc service khác nhau, nên mình cần thống nhất mức độ cập nhật mong muốn trước khi bổ sung. Trong trạng thái hiện tại, mình ghi nhận rõ locationName chưa được đảm bảo đồng bộ tức thời.

### 11. Cập nhật về conflict Git sau khi lập báo cáo

Phần lưu ý Git ở trên phản ánh thời điểm lập báo cáo ban đầu. Sau đó, mình đã xử lý hết 36 conflict, giữ lại mã nguồn và loại cache/build khỏi index nhưng vẫn giữ file local. Commit `67ef9ee` đã được push lên nhánh `XA-task3v4`; nhánh local đã đồng bộ với remote tại thời điểm push.

Vì vậy, conflict Git không còn là hạng mục đang chờ xử lý và không nên tiếp tục được dùng làm lý do cho các phần chức năng chưa hoàn thành. Việc commit/push thành công cũng chưa thay thế các bước kiểm thử Docker, MySQL, trình duyệt hoặc tích hợp với Khoa nêu trên. File review này được giữ ở local, ngoài commit, theo yêu cầu.

### 12. Trách nhiệm và bước hoàn thiện tiếp theo của mình

Mình chịu trách nhiệm về các phần triển khai và kiểm chứng còn thiếu trong phạm vi được giao. Một số phần phụ thuộc môi trường chạy hoặc xác nhận tích hợp, còn một số phần như ô nhập transcript và điều khiển tìm kiếm trên CMS là phần giao diện mình chưa bổ sung. Mình ghi riêng từng trường hợp để tránh gộp tất cả thành lý do thiếu môi trường.

Thứ tự hoàn thiện mình đề xuất là: chạy stack thật và kiểm thử CMS; chốt hợp đồng với Khoa; thử migration trên bản sao dữ liệu; chuyển nghiệp vụ gán beacon sang content-service; sau đó hoàn tất việc tách các phụ thuộc cũ và bổ sung những hạng mục giao diện được xác định là bắt buộc. Mình chưa đưa ra ngày hoàn thành cụ thể khi chưa xác nhận môi trường chạy và lịch phối hợp tích hợp.
