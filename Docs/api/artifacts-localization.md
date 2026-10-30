# Hiện vật và đa ngôn ngữ — W3

API chạy trong backend chính, profile mặc định tại http://localhost:8080/api/artifacts.
W3 không thay đổi Infra. W4 chờ PR khung của Bảo merge, sau đó triển khai trong services/content-service theo 5 bước services/README.md khi tài liệu có trên main.

## Hợp đồng

- GET /api/artifacts/{id}?lang=vi và GET /api/artifacts/beacon/{uuid}?lang=en: mặc định vi, hỗ trợ vi/en không phân biệt hoa thường; ngôn ngữ khác trả 400.
- Thiếu bản Anh chọn cả bản Việt, fallback:true; có bản yêu cầu thì fallback:false. Không ghép từng trường giữa hai bản.
- Response success/message/data; data có requestedLanguage, languageCode, fallback, content, contents[], media[]. name/description được localize ở detail/beacon; danh sách giữ tên gốc.
- Danh sách hỗ trợ siteId hoặc alias locationId, và search.
- POST hiện vật bắt buộc bản Việt; hiện vật, media và bản dịch lưu trong một transaction.
- PUT có contents thì thay danh sách, vẫn phải giữ vi; bỏ contents thì giữ nguyên. media:[] xóa media, bỏ media thì giữ nguyên.
- DELETE hiện vật xóa cả media và bản dịch.
- GET /{id}/contents, GET /{id}/contents/{lang}, POST /{id}/contents để đọc/upsert bản dịch; DELETE /contents/{contentId} không cho xóa riêng bản Việt.
- siteId và locationId là alias, không gửi hai giá trị khác nhau. Backend kiểm tra di tích bằng repository cùng database.
- Mỗi bản dịch cần title/description. imageUrls là chuỗi JSON chứa mảng URL. Media dùng HTTP/HTTPS; URL example.com chỉ minh họa.
- Mã/UUID tùy chọn để null khi tạo; PUT chuỗi rỗng bỏ gán, null hoặc bỏ field giữ nguyên.
- Tạo thành công 201; đọc/sửa/xóa 200; hiện vật không tồn tại 404; dữ liệu không hợp lệ 400. Dữ liệu cũ thiếu cả bản yêu cầu lẫn vi sẽ báo lỗi.

## CMS và xác thực

Phục vụ thư mục WebAdmin, mở src/pages/artifacts.html, đăng nhập ADMIN.
Trang nạp ../api/auth.js với data-guard="admin" trước artifacts.js; auth.js gắn JWT và xử lý refresh.
CMS dùng window.SHAuth.apiBase, cấu hình máy chủ bằng ?api=http://localhost:8080.

## Bàn giao Khoa

- [Request tạo](artifact-create.sample.json).
- [Chi tiết tiếng Anh](artifact-detail.sample.json).
- [Fallback Việt](artifact-detail-fallback.sample.json).

Luồng mobile (đã chốt 30/10, xem [beacon-scan-flow.md](beacon-scan-flow.md)): registry → (uuid, major, minor) → artifactId → GET /api/artifacts/{artifactId}?lang=vi|en → đọc data.content, data.media, data.languageCode, data.fallback.
Không dùng GET /api/artifacts/beacon/{uuid} cho luồng quét: mọi beacon dùng chung 1 UUID và chỉ khác major/minor.

## Cache Redis — đề xuất cho Phú, chưa xác nhận

Khóa cache gồm ID/UUID và ngôn ngữ yêu cầu đã chuẩn hóa; không chỉ languageCode vì response có requestedLanguage/fallback.
Sửa/xóa hiện vật, bản dịch hoặc media phải vô hiệu cache vi/en và beacon cũ/mới.
Không cache thao tác ghi hay phản hồi lỗi. TTL cần thống nhất với Phú.

## Chạy và nghiệm thu

Chạy `mvn -f Backend/pom.xml test`, sau đó `mvn -f Backend/pom.xml spring-boot:run` với MySQL theo application.properties.
Swagger: http://localhost:8080/swagger-ui/index.html.
Tạo di tích trước, dùng ADMIN tạo một hiện vật Việt/Anh và một hiện vật chỉ Việt.
Kiểm tra GET ID/beacon lang=en chọn đúng bản và fallback; CMS thêm/sửa/xóa sau đăng nhập; xóa phiên vào lại phải về login.
Kiểm thử MySQL demo, Swagger và trình duyệt thật cần ghi nhận riêng với test tự động.
