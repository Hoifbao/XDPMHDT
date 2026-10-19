# Tìm hiểu API Gateway và Apache NiFi

> Người viết: Nguyễn Thanh Tuấn (mảng Dashboard Thống kê & Xuất báo cáo), Sprint 2.
> Mục đích: chuẩn bị kiến thức cho các tuần sau. Tài liệu chỉ để tìm hiểu, **chưa phải quyết định kiến trúc của nhóm**.
> Các link đã được mở kiểm tra ngày 19/10/2026.

---

## 1. API Gateway

### 1.1 Là gì?

API Gateway là **một cửa vào duy nhất** đứng trước các dịch vụ Backend. Client (App Flutter, Web Admin) chỉ gọi tới Gateway. Gateway nhận request, xử lý các việc chung rồi chuyển tiếp (route) tới đúng dịch vụ phía sau.

```
App Flutter ─┐                 ┌─> Backend Spring Boot
             ├─> API Gateway ──┤
Web Admin  ──┘                 └─> (dịch vụ khác sau này, vd dịch vụ thống kê)
```

### 1.2 Vai trò chính

| Vai trò | Ý nghĩa |
|---|---|
| Định tuyến (routing) | Chọn dịch vụ đích theo đường dẫn, header, method... |
| Xác thực tập trung | Kiểm tra token một lần ở cửa vào thay vì lặp lại ở từng dịch vụ |
| Giới hạn tần suất (rate limit) | Chặn client gọi quá nhiều, trả về `429 Too Many Requests` |
| CORS | Cấu hình một chỗ cho phép Web Admin (khác origin) gọi API |
| Ghi log / theo dõi | Một điểm chung để ghi nhận mọi request |

### 1.3 Ví dụ: Spring Cloud Gateway

Nhóm đang dùng Spring Boot, nên Spring Cloud Gateway là lựa chọn gần nhất.

- **Route** gồm: id, URI đích, các **predicate** (điều kiện khớp request) và các **filter** (sửa request/response).
  Danh sách predicate: <https://docs.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway-server-webflux/request-predicates-factories.html>
- **Rate limit**: filter `RequestRateLimiter`. Bản cài đặt có sẵn dùng **Redis** theo thuật toán *token bucket*. Khi bị chặn, gateway trả `429 Too Many Requests`.
  <https://docs.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway-server-webflux/gatewayfilter-factories/requestratelimiter-factory.html>
- **CORS** cấu hình chung cho toàn gateway:
  <https://docs.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway-server-webflux/cors-configuration.html>
- Có 2 biến thể là **WebFlux** (reactive) và **Server WebMVC** (servlet):
  <https://docs.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway-server-webmvc.html>
  Biến thể nào hợp với Backend của nhóm (đang dùng Spring MVC hay WebFlux): **cần kiểm chứng**.
- Trang dự án: <https://spring.io/projects/spring-cloud-gateway>. Tài liệu tham chiếu: <https://docs.spring.io/spring-cloud-gateway/reference/>

### 1.4 Đặt ở đâu trong hệ thống Smart Heritage

Hiện hệ thống chỉ có **một** Backend Spring Boot. Gateway, nếu dùng, sẽ là một ứng dụng riêng đứng giữa client và Backend:

```
Client ──HTTPS──> Gateway (vd cổng 8080) ──> Backend Spring Boot (cổng nội bộ)
```

**Nhận xét:** với một Backend duy nhất, Gateway chủ yếu mang lại rate limit và CORS tập trung. Lợi ích rõ hơn khi tách thêm dịch vụ (vd dịch vụ thống kê riêng). Đây là **đề xuất để thảo luận**, chưa phải yêu cầu.

---

## 2. Apache NiFi

### 2.1 Là gì?

Apache NiFi là công cụ **tự động hóa luồng dữ liệu**: lấy dữ liệu từ nguồn, biến đổi, rồi đẩy tới đích. Luồng được kéo thả trên giao diện web, không cần viết nhiều code.
Trang chủ: <https://nifi.apache.org/>

### 2.2 Khái niệm cốt lõi

Nguồn: NiFi Overview, <https://nifi.apache.org/docs/nifi-docs/html/overview.html>

| Khái niệm | Ý nghĩa ngắn |
|---|---|
| **FlowFile** | Một "gói" dữ liệu đi trong luồng, gồm nội dung (content) và thuộc tính (attributes) |
| **FlowFile Processor** | Khối xử lý: đọc, biến đổi, ghi, định tuyến dữ liệu |
| **Connection** | Hàng đợi nối các processor, có **back pressure** khi đầy |
| **Flow Controller** | Bộ điều phối, cấp tài nguyên cho các processor chạy |
| **Process Group** | Nhóm nhiều processor thành một khối con |
| **Data Provenance** | Lưu vết từng FlowFile đi qua đâu, phục vụ kiểm tra |

Hướng dẫn sử dụng: <https://nifi.apache.org/docs/nifi-docs/html/user-guide.html>
Danh sách processor: <https://nifi.apache.org/components/>

### 2.3 Dùng NiFi để gom dữ liệu thống kê ra sao (đề xuất)

Dữ liệu tương tác của hệ thống nằm trong MySQL, ở các bảng `visits` (`detected_at`, `duration_seconds`) và `feedbacks` (`rating`, `status`, `created_at`) theo `Infra/init_db.sql` hiện tại. Một luồng NiFi có thể làm như sau:

```
QueryDatabaseTable (đọc dòng mới của visits / feedbacks)
      │   theo "Maximum-value Columns", vd id hoặc detected_at
      ▼
Biến đổi / gộp theo ngày, theo hiện vật   (processor cụ thể: cần kiểm chứng)
      ▼
PutDatabaseRecord (ghi vào bảng tổng hợp phục vụ Dashboard)
```

- `QueryDatabaseTable` hỗ trợ **lấy tăng dần** (chỉ lấy dòng mới) nhờ thuộc tính *Maximum-value Columns*:
  <https://nifi.apache.org/components/org.apache.nifi.processors.standard.QueryDatabaseTable/>
- `PutDatabaseRecord` ghi bản ghi vào cơ sở dữ liệu:
  <https://nifi.apache.org/components/org.apache.nifi.processors.standard.PutDatabaseRecord/>
- Bảng tổng hợp (vd thống kê theo ngày) **chưa có** trong `init_db.sql`. Nếu dùng hướng này thì phải thiết kế thêm.

**Lợi ích mong đợi:** Dashboard đọc bảng đã tổng hợp sẵn thay vì đếm lại toàn bộ `visits` mỗi lần mở. Điều này giúp đạt yêu cầu "Dashboard reports < 5 giây" trong `readme.md`. Mức cải thiện thực tế: **cần đo kiểm chứng**.

**Cân nhắc:** NiFi là một hệ thống chạy riêng (thêm container, thêm cấu hình). Với quy mô đồ án, cách đơn giản hơn là dùng truy vấn `GROUP BY` trực tiếp kèm cache. Nên chọn sau khi có số liệu đo thực tế. Đây là **đề xuất**.

---

## 3. Liên hệ với Dashboard của nhóm (đề xuất)

1. **Tuần hiện tại:** Dashboard (`WebAdmin/src/pages/dashboard.html`) dùng dữ liệu giả.
2. **Bước tiếp:** Backend có API thống kê, Dashboard gọi qua API (qua Gateway nếu nhóm dùng).
3. **Khi dữ liệu lớn:** cân nhắc NiFi hoặc job định kỳ để tổng hợp trước, giúp Dashboard nhanh.

Các mục trên đều là **đề xuất**, cần nhóm thống nhất trước khi làm.
