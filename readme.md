# Đề Tài Dự Án: Smart Heritage

## 1. Tên Dự Án / Project Names

* **Tiếng Anh (English Name):** Smart Heritage – An iBeacon-Based Smart Tourism System for Interactive Experiences at Historical and Cultural Heritage Sites[cite: 1]
* **Tiếng Việt (Vietnamese Name):** Smart Heritage – Hệ thống du lịch thông minh ứng dụng công nghệ iBeacon hỗ trợ tương tác hiện vật tại các di tích lịch sử và văn hóa[cite: 1]

---

## 2. Nội Dung Đề Xuất Chính / Main Proposal Content

### a) Bối Cảnh & Đặt Vấn Đề / Context & Problem Statement

#### Tiếng Việt
* **Bảo tồn văn hóa & Du lịch thông minh:** Việt Nam sở hữu hàng nghìn di tích lịch sử và văn hóa, lưu giữ bản sắc dân tộc và thu hút hàng triệu lượt khách du lịch trong và ngoài nước mỗi năm[cite: 1]. Cùng với sự phát triển mạnh mẽ của du lịch thông minh và chuyển đổi số, việc nâng cao trải nghiệm du khách song song với bảo tồn giá trị văn hóa là mục tiêu quan trọng của các địa phương và cơ quan quản lý du lịch[cite: 1].
* **Hạn chế của phương thức truyền thống:** Nhiều khu di tích vẫn phụ thuộc vào các phương thức truyền tải thông tin truyền thống như bảng thông tin cố định, tờ rơi hoặc hướng dẫn viên[cite: 1]. Du khách phải chủ động tìm kiếm thông tin hoặc đi theo đoàn mới hiểu được câu chuyện đằng sau các hiện vật[cite: 1]. Điều này dẫn đến trải nghiệm thụ động, hạn chế tương tác, đặc biệt là với thế hệ trẻ và khách du lịch nước ngoài[cite: 1].
* **Thách thức cho du khách quốc tế & Công cụ hiện tại:** Hầu hết thông tin chỉ trình bày bằng tiếng Việt, gây khó khăn cho người nước ngoài[cite: 1]. Các giải pháp kỹ thuật số hiện có như mã QR đòi hỏi người dùng phải quét thủ công từng hiện vật, làm ngắt quãng dòng trải nghiệm tự nhiên và ít hỗ trợ cá nhân hóa hoặc nhận diện theo vị trí[cite: 1].
* **Thiếu hụt công cụ quản lý & Phân tích:** Ban quản lý di tích gặp khó khăn trong việc quản lý nội dung số và đánh giá mức độ tương tác của du khách[cite: 1]. Hiện chưa có nền tảng tích hợp cho phép quản lý nội dung, cấu hình thiết bị, theo dõi tương tác hay phân tích số liệu (lượng du khách, hiện vật phổ biến, phản hồi)[cite: 1]. Việc ra quyết định và quảng bá di sản phần lớn vẫn dựa trên quan sát thủ công[cite: 1].
* **Giải pháp đề xuất:** Nhằm đáp ứng Chương trình Chuyển đổi số Quốc gia và nhu cầu du lịch thông minh, việc xây dựng một nền tảng kết nối di sản thực tế với nội dung số là vô cùng cần thiết[cite: 1].
* **Nền tảng Smart Heritage:** Chúng tôi đề xuất hệ thống **Smart Heritage** ứng dụng công nghệ Bluetooth Low Energy (BLE) iBeacon để tự động phát hiện vị trí gần hiện vật và cung cấp nội dung đa phương tiện (mô tả, hình ảnh, video, thuyết minh audio, đa ngôn ngữ) trực tiếp trên thiết bị di động của du khách[cite: 1]. Nền tảng cũng cung cấp hệ thống quản trị Web để quản lý địa điểm, nội dung số, thiết bị beacon, thống kê du khách và báo cáo vận hành[cite: 1].

#### English
* **Cultural Preservation & Smart Tourism:** Vietnam is home to thousands of historical and cultural heritage sites that preserve the nation's identity and attract millions of domestic and international visitors each year[cite: 1]. Along with the rapid development of smart tourism and digital transformation initiatives, enhancing visitors' experiences while preserving cultural values has become an important objective for many local governments and tourism authorities[cite: 1].
* **Limitations of Traditional Information Delivery:** Despite their historical significance, many heritage sites still rely on traditional methods of presenting information, such as static information boards, brochures, or tour guides[cite: 1]. Visitors are required to actively search for information or join guided tours to understand the stories behind historical artifacts[cite: 1]. This often results in a passive tourism experience, limited interaction, and reduced engagement, particularly among younger generations and foreign visitors[cite: 1].
* **Challenges for Foreign Tourists & Existing Digital Tools:** The limitations become even more apparent for international tourists[cite: 1]. Most information is provided only in Vietnamese, making it difficult for non-Vietnamese speakers to access historical knowledge[cite: 1]. Existing digital solutions, such as QR codes, require users to manually scan every artifact, interrupting the natural flow of exploration and reducing convenience[cite: 1]. Furthermore, these solutions provide little support for personalized or location-aware experiences[cite: 1].
* **Management & Analytics Deficits:** From the management perspective, heritage site administrators also face challenges in managing digital content and evaluating visitor engagement[cite: 1]. There is currently no integrated platform that allows administrators to manage historical content, configure smart devices, monitor visitor interactions, or analyze statistics such as the number of visitors, the most popular artifacts, or user feedback[cite: 1]. Consequently, decision-making and heritage promotion are largely based on manual observation rather than data-driven insights[cite: 1].
* **Proposed Solution Overview:** In response to Vietnam's National Digital Transformation Program and the growing demand for smart tourism, there is an increasing need for an intelligent platform capable of connecting physical heritage with digital content while improving both visitor experience and heritage management efficiency[cite: 1].
* **Smart Heritage Platform:** To address these challenges, we propose Smart Heritage, an iBeacon-based Smart Tourism System that enables interactive experiences at historical and cultural heritage sites[cite: 1]. By leveraging Bluetooth Low Energy (BLE) iBeacon technology, the system automatically detects visitors' proximity to historical artifacts and delivers context-aware multimedia content, including historical descriptions, images, videos, audio guides, and multilingual information directly to visitors' mobile devices[cite: 1]. In addition, the platform provides a web-based administration system for managing heritage locations, digital content, beacon devices, visitor statistics, and operational reports, supporting the digital transformation of cultural heritage tourism while creating a more engaging, accessible, and intelligent visitor experience[cite: 1].

---

### b) Giải Pháp Đề Xuất / Proposed Solutions

Hệ thống bao gồm 6 module cốt lõi[cite: 1]:

1. **Smart Visitor Mobile Application (Ứng dụng di động cho du khách):** Đóng vai trò là người bạn đồng hành kỹ thuật số[cite: 1]. Tự động phát hiện hiện vật qua iBeacon và hiển thị nội dung đa phương tiện[cite: 1]. Cho phép định vị bằng bản đồ tương tác, lưu hiện vật yêu thích, xem lịch sử tham quan và tiếp tục xem nội dung khi ra khỏi vùng phủ sóng beacon[cite: 1].
2. **Interactive Heritage Experience Engine (Động cơ trải nghiệm di sản tương tác):** Xử lý tín hiệu BLE beacon liên tục để xác định vị trí và tự động kích hoạt nội dung mà không cần quét QR[cite: 1]. Ngăn chặn thông báo trùng lặp để đảm bảo trải nghiệm mượt mà[cite: 1].
3. **Heritage Content Management System - CMS (Hệ thống quản lý nội dung di sản):** Nền tảng Web tập trung cho quản trị viên quản lý thông tin di tích, hiện vật, tài nguyên đa phương tiện và đa ngôn ngữ một cách linh hoạt[cite: 1].
4. **iBeacon Infrastructure Management Module (Module quản lý hạ tầng iBeacon):** Quản lý, cấu hình thiết bị beacon, gán với hiện vật, quản lý vị trí lắp đặt trên bản đồ và điều chỉnh thông số định vị[cite: 1].
5. **Visitor Analytics and Reporting Dashboard (Bảng điều khiển phân tích & báo cáo):** Trực quan hóa dữ liệu tương tác của du khách (lượng khách, hiện vật nổi bật, tần suất tương tác, thời gian dừng chân, ngôn ngữ ưa thích, phản hồi) hỗ trợ ra quyết định[cite: 1].
6. **Digital Heritage Administration Platform (Nền tảng quản trị di sản số):** Cổng quản trị hợp nhất quản lý người dùng, di tích, beacon, ngôn ngữ, phản hồi và báo cáo vận hành dài hạn[cite: 1].

---

### c) Yêu Cầu Chức Năng / Functional Requirements

#### 3.2.1. Khách Tham Quan (Visitor)
* **US01:** As a Visitor, I want to register and log in to the mobile application, so that I can personalize my tourism experience and synchronize my visit history[cite: 1].
  *(Là Du khách, tôi muốn đăng ký và đăng nhập ứng dụng di động để cá nhân hóa trải nghiệm và đồng bộ lịch sử tham quan[cite: 1].)*
* **US02:** As a Visitor, I want to update my personal profile and select my preferred language, so that I can receive heritage information in a language I understand[cite: 1].
  *(Là Du khách, tôi muốn cập nhật hồ sơ và chọn ngôn ngữ ưu tiên để nhận thông tin phù hợp[cite: 1].)*
* **US03:** As a Visitor, I want to view an interactive map of heritage sites, so that I can easily navigate to historical artifacts and nearby attractions[cite: 1].
  *(Là Du khách, tôi muốn xem bản đồ tương tác để dễ dàng di chuyển tới các hiện vật và điểm tham quan[cite: 1].)*
* **US04:** As a Visitor, I want the system to automatically detect nearby historical artifacts using iBeacon technology, so that I can receive relevant information without manually scanning QR codes[cite: 1].
  *(Là Du khách, tôi muốn hệ thống tự động nhận diện hiện vật lân cận qua iBeacon để nhận thông tin mà không cần quét mã QR[cite: 1].)*
* **US05:** As a Visitor, I want to receive multimedia information including historical descriptions, images, videos, and audio guides when approaching an artifact, so that I can better understand its historical and cultural significance[cite: 1].
  *(Là Du khách, tôi muốn nhận nội dung đa phương tiện (bài viết, ảnh, video, audio) khi đến gần hiện vật để hiểu rõ giá trị lịch sử[cite: 1].)*
* **US06:** As a Visitor, I want to continue viewing artifact information even after leaving the beacon coverage area, so that I can finish reading or watching the content without interruption[cite: 1].
  *(Là Du khách, tôi muốn tiếp tục xem thông tin hiện vật kể cả khi đã ra khỏi vùng sóng beacon[cite: 1].)*
* **US07:** As a Visitor, I want to save my favorite artifacts and review my visit history, so that I can revisit interesting places in the future[cite: 1].
  *(Là Du khách, tôi muốn lưu hiện vật yêu thích và xem lại lịch sử chuyến đi[cite: 1].)*
* **US08:** As a Visitor, I want to rate artifacts and submit feedback about my experience, so that I can contribute to improving heritage tourism services[cite: 1].
  *(Là Du khách, tôi muốn đánh giá hiện vật và gửi phản hồi để đóng góp nâng cao chất lượng dịch vụ[cite: 1].)*

#### 3.2.2. Ban Quản Lý Di Tích (Heritage Site Administrator)
* **US09:** As an Administrator, I want to securely log in to the administration platform, so that I can manage the heritage tourism system[cite: 1].
  *(Là Quản trị viên, tôi muốn đăng nhập bảo mật để quản lý hệ thống[cite: 1].)*
* **US10:** As an Administrator, I want to create, edit, and manage heritage sites and historical artifacts, so that visitors always receive accurate and up-to-date information[cite: 1].
  *(Là Quản trị viên, tôi muốn quản lý địa điểm di sản và hiện vật để đảm bảo thông tin luôn chính xác[cite: 1].)*
* **US11:** As an Administrator, I want to upload and manage multimedia resources including images, videos, audio guides, and multilingual descriptions, so that digital heritage content remains rich and accessible[cite: 1].
  *(Là Quản trị viên, tôi muốn tải lên và quản lý nội dung đa phương tiện cũng như bản dịch đa ngôn ngữ[cite: 1].)*
* **US12:** As an Administrator, I want to register, configure, and assign iBeacon devices to specific artifacts or locations, so that the system can accurately deliver location-aware content[cite: 1].
  *(Là Quản trị viên, tôi muốn đăng ký, cấu hình và gán thiết bị iBeacon cho hiện vật/vị trí cụ thể[cite: 1].)*
* **US13:** As an Administrator, I want to manage the deployment locations of beacon devices on an interactive map, so that I can monitor and maintain the beacon infrastructure efficiently[cite: 1].
  *(Là Quản trị viên, tôi muốn quản lý vị trí lắp đặt beacon trên bản đồ tương tác để theo dõi và bảo trì hạ tầng[cite: 1].)*
* **US14:** As an Administrator, I want to manage supported languages and digital content translations, so that international visitors can access heritage information more conveniently[cite: 1].
  *(Là Quản trị viên, tôi muốn quản lý ngôn ngữ và bản dịch hỗ trợ khách quốc tế[cite: 1].)*
* **US15:** As an Administrator, I want to moderate visitor feedback and reviews, so that I can improve the quality of heritage services[cite: 1].
  *(Là Quản trị viên, tôi muốn kiểm duyệt phản hồi, đánh giá của du khách[cite: 1].)*
* **US16:** As an Administrator, I want to view dashboards showing visitor statistics, artifact popularity, interaction frequency, language preferences, and user feedback, so that I can evaluate tourism performance and support decision-making[cite: 1].
  *(Là Quản trị viên, tôi muốn xem Dashboard thống kê du khách, hiện vật nổi bật, tần suất tương tác, ngôn ngữ và phản hồi để ra quyết định[cite: 1].)*
* **US17:** As an Administrator, I want to export statistical reports in PDF or Excel format, so that I can support management activities and tourism promotion[cite: 1].
  *(Là Quản trị viên, tôi muốn xuất báo cáo thống kê định dạng PDF hoặc Excel[cite: 1].)*

#### 3.2.3. Hệ Thống (System)
* **US18:** As the System, I want to continuously detect visitors' proximity to nearby iBeacon devices, so that artifact information can be delivered automatically[cite: 1].
  *(Hệ thống liên tục quét khoảng cách tới các iBeacon gần kề để gửi thông tin tự động[cite: 1].)*
* **US19:** As the System, I want to prevent duplicate notifications while visitors remain within the same beacon coverage area, so that users are not repeatedly interrupted[cite: 1].
  *(Hệ thống ngăn chặn thông báo trùng lặp khi du khách vẫn ở trong cùng vùng sóng beacon[cite: 1].)*
* **US20:** As the System, I want to automatically trigger notifications when visitors enter the coverage area of a different beacon, so that they can discover new historical artifacts seamlessly[cite: 1].
  *(Hệ thống tự động kích hoạt thông báo khi du khách đi vào vùng sóng của beacon mới[cite: 1].)*
* **US21:** As the System, I want to synchronize visitor interactions, beacon information, and digital content between the mobile application and the administration platform, so that all information remains consistent[cite: 1].
  *(Hệ thống đồng bộ dữ liệu giữa ứng dụng di động và nền tảng quản trị Web[cite: 1].)*
* **US22:** As the System, I want to record visitor interactions, visited artifacts, and user preferences, so that statistical reports and tourism analytics can be generated[cite: 1].
  *(Hệ thống ghi nhận tương tác, lịch sử và sở thích du khách để phục vụ phân tích báo cáo[cite: 1].)*
* **US23:** As the System, I want to support multilingual content delivery based on each visitor's language preference, so that visitors from different countries can enjoy a personalized tourism experience[cite: 1].
  *(Hệ thống truyền tải nội dung theo ngôn ngữ ưu tiên của từng du khách[cite: 1].)*
* **US24:** As the System, I want to securely store all operational data and user information, so that the platform remains reliable and protects user privacy[cite: 1].
  *(Hệ thống bảo mật toàn bộ dữ liệu vận hành và thông tin du khách[cite: 1].)*

---

### d) Yêu Cầu Phi Chức Năng / Non-Functional Requirements

| Tiêu Chí (Attribute) | Yêu Cầu Chi Tiết (English) | Chi Tiết Tiếng Việt |
| :--- | :--- | :--- |
| **Performance** | • App detects iBeacon & shows info within **< 2 seconds**[cite: 1].<br>• Web platform responds within **< 2 seconds** for 95% of API requests[cite: 1].<br>• Dashboard reports generate within **< 5 seconds**[cite: 1]. | • App nhận diện beacon & hiển thị nội dung **< 2 giây**[cite: 1].<br>• Web phản hồi API **< 2 giây** cho 95% yêu cầu[cite: 1].<br>• Xuất báo cáo Dashboard **< 5 giây**[cite: 1]. |
| **Usability** | • Intuitive UI with minimal learning curve[cite: 1].<br>• Complete core tasks within **3–5 interactions**[cite: 1].<br>• Responsive web design & mobile device support[cite: 1]. | • Giao diện trực quan, dễ sử dụng[cite: 1].<br>• Thao tác chính hoàn thành trong **3–5 lần chạm/click**[cite: 1].<br>• Giao diện Web tương thích đa màn hình[cite: 1]. |
| **Security** | • HTTPS/TLS 1.2+ encryption for all communication[cite: 1].<br>• Authorization via Role-Based Access Control (RBAC)[cite: 1].<br>• Secure data storage for personal info[cite: 1]. | • Mã hóa kết nối **HTTPS/TLS 1.2+**[cite: 1].<br>• Phân quyền theo vai trò (**RBAC**)[cite: 1].<br>• Bảo mật thông tin cá nhân và dữ liệu hệ thống[cite: 1]. |
| **Scalability** | • Supports at least **500 concurrent users**[cite: 1].<br>• Manages multiple sites, thousands of artifacts, hundreds of beacons[cite: 1].<br>• Modular backend for horizontal scaling[cite: 1]. | • Hỗ trợ tối thiểu **500 người dùng đồng thời**[cite: 1].<br>• Quản lý nhiều di tích, hàng ngàn hiện vật, hàng trăm beacon[cite: 1].<br>• Kiến trúc hỗ trợ mở rộng hàng ngang[cite: 1]. |
| **Availability** | • At least **99% system availability**[cite: 1].<br>• Offline/Fallback: Visitors can manually browse loaded info if beacon signals fail[cite: 1]. | • Độ khả dụng đạt **99%**[cite: 1].<br>• Hỗ trợ chế độ xem lại dữ liệu đã tải khi gặp sự cố sóng beacon[cite: 1]. |
| **Reliability** | • Accurate proximity detection & duplicate prevention[cite: 1].<br>• Reliable data synchronization[cite: 1].<br>• Graceful recovery from temporary network drops[cite: 1]. | • Nhận diện vị trí chính xác, không lặp thông báo[cite: 1].<br>• Đồng bộ dữ liệu tin cậy[cite: 1].<br>• Tự phục hồi sau khi bị ngắt mạng tạm thời[cite: 1]. |
| **Maintainability** | • Modular architecture (Mobile, Backend API, Beacon Engine, Web Admin)[cite: 1].<br>• RESTful APIs documented using OpenAPI/Swagger[cite: 1].<br>• Future-proof for AI recommendations, AR experiences, Smart City integration[cite: 1]. | • Kiến trúc phân module rõ ràng[cite: 1].<br>• Tài liệu API chuẩn OpenAPI/Swagger[cite: 1].<br>• Sẵn sàng tích hợp AI, AR, Smart City trong tương lai[cite: 1]. |

---

### e) Cơ Sở Lý Thuyết & Thực Tắc / Theory & Practical Methodologies

* **Quy trình phát triển phần mềm:** Phương pháp Agile / Scrum[cite: 1].
* **Tiêu chuẩn phân tích & thiết kế:** UML 2.0[cite: 1].

#### Danh Mục Sản Phẩm Hồ Sơ Dự Kiến (Expected Deliverables)[cite: 1]
1. User Requirements Specification (URS)[cite: 1]
2. Software Requirements Specification (SRS)[cite: 1]
3. System Architecture Design Document[cite: 1]
4. UML Diagrams (Use Case, Activity, Sequence, Class, Component, Deployment)[cite: 1]
5. Entity Relationship Diagram (ERD) & Database Design Specification[cite: 1]
6. REST API Documentation (Swagger / OpenAPI)[cite: 1]
7. User Interface (UI) Design and Wireframes[cite: 1]
8. Source Code Repository[cite: 1]
9. Test Plan and Test Report (Unit, Integration, System Testing)[cite: 1]
10. Deployment Guide[cite: 1]
11. User Manual (Visitor & Heritage Site Administrator)[cite: 1]
12. Final Capstone Report and Presentation Materials[cite: 1]

---

### f) Sản Phẩm Đầu Ra / Expected Products[cite: 1]

1. **Mobile Smart Tourism Application:** Ứng dụng di động đa nền tảng kết nối iBeacon, hiển thị thông tin đa phương tiện, bản đồ tương tác, lưu hiện vật yêu thích, lịch sử tham quan và gửi phản hồi[cite: 1].
2. **Heritage Content Management Platform:** Nền tảng Web Admin giúp quản lý di tích, hiện vật, đa phương tiện và đa ngôn ngữ[cite: 1].
3. **iBeacon Management System:** Hệ thống quản lý tập trung hỗ trợ đăng ký, cấu hình, lập bản đồ vị trí và theo dõi tình trạng beacon[cite: 1].
4. **Analytics and Reporting Dashboard:** Dashboard trực quan hóa dữ liệu du khách, độ phổ biến hiện vật, ngôn ngữ, thời gian tham quan và xuất báo cáo[cite: 1].
5. **REST API Services:** Hệ thống API RESTful hoàn chỉnh, bảo mật, phân quyền và có tài liệu Swagger đầy đủ[cite: 1].

---

### g) Các Gói Công Việc Đề Xuất / Proposed Tasks[cite: 1]

| Task Package | Nội Dung Chi Tiết (English) | Chi Tiết Tiếng Việt |
| :--- | :--- | :--- |
| **Task Package 1** | Database design, authentication module, user management, and REST API development[cite: 1]. | Thiết kế CSDL, module xác thực, quản lý người dùng và phát triển REST API[cite: 1]. |
| **Task Package 2** | Mobile application development including authentication, multilingual support, interactive maps, Bluetooth (BLE) integration, iBeacon detection, artifact information display, favorites, and visit history[cite: 1]. | Phát triển App Mobile: xác thực, đa ngôn ngữ, bản đồ tương tác, quét BLE/iBeacon, hiển thị hiện vật, yêu thích và lịch sử tham quan[cite: 1]. |
| **Task Package 3** | Web Administration Platform development including heritage site management, artifact management, multimedia management, multilingual content management, and beacon configuration[cite: 1]. | Phát triển Web Admin: quản lý khu di tích, hiện vật, đa phương tiện, nội dung đa ngôn ngữ và cấu hình beacon[cite: 1]. |
| **Task Package 4** | Analytics Dashboard development, statistical reporting, visitor feedback management, and system integration[cite: 1]. | Phát triển Dashboard phân tích, xuất báo cáo thống kê, quản lý phản hồi du khách và tích hợp hệ thống[cite: 1]. |
| **Task Package 5** | System testing, performance testing, Docker containerization, deployment, API documentation, and user manuals[cite: 1]. | Kiểm thử hệ thống, kiểm thử hiệu năng, đóng gói Docker, triển khai, viết tài liệu API và hướng dẫn sử dụng[cite: 1]. |

> **Notice:** All team members are required to understand the complete Smart Heritage system architecture, including the interaction between the Mobile Application, Backend Services, Web Administration Platform, Database, and Bluetooth Low Energy (iBeacon) infrastructure[cite: 1]. Each member must clearly explain the overall system architecture as well as their individual module implementation during the capstone defense[cite: 1].

---

## 3. Ý Kiến Khác / Other Comments[cite: 1]

*(Đề xuất bổ sung tính năng mở rộng nếu có)*
* Định hướng phát triển tích hợp công nghệ Thực tế ảo tăng cường (AR) và thuật toán AI gợi ý lộ trình tham quan thông minh trong các phiên bản mở rộng tiếp theo.

---

## Chạy local — quy ước cổng (thống nhất từ Sprint 4)

> **Cổng chính là 8080 (API Gateway).** Web Admin, app Mobile, Postman và mọi test API đều gọi `http://localhost:8080`.
> 8090 là Backend cũ đứng sau Gateway, chỉ gọi thẳng khi debug hoặc xem Swagger.

| Cổng | Thành phần | Ghi chú |
|---|---|---|
| **8080** | api-gateway | Cổng duy nhất client gọi. Kiểm JWT + phân quyền, chuyển tiếp request |
| 8081 | auth-service | Nhận `/api/auth/**` từ Gateway |
| 8090 | Backend cũ (monolith) | Các API chưa tách. Swagger: http://localhost:8090/swagger-ui/index.html |
| 8082–8087 | site / content / beacon / personalization / feedback / analytics | Dần thay Backend cũ từ W4–W5 |
| 3307, 6379 | MySQL, Redis (Docker) | |

Chạy đủ bằng 1 lệnh (thư mục gốc repo): `powershell -ExecutionPolicy Bypass -File Infra\dev\start-local.ps1`.
Chi tiết: [services/README.md](services/README.md).

## 4. Cấu Trúc Repository / Repository Structure

- `Backend/`: Java/Maven REST API; mã nguồn hiện tại dùng package `com.smartheritage.backend`.
- `WebAdmin/`: giao diện quản trị prototype (`admin.html`, `theme.css`); tài liệu và vị trí dự kiến cho API, pages, components nằm trong thư mục này.
- `Interface/`: Flutter app dành cho khách tham quan, gồm `lib/`, `android/`, `test/`, `web/` và các tệp cấu hình Flutter.
- `Infra/`: Docker Compose và SQL khởi tạo cơ sở dữ liệu.
- `Diagram/`: sơ đồ kiến trúc, cơ sở dữ liệu và luồng nghiệp vụ.
- `Docs/`: tài liệu requirements, API, database, testing, deployment và user manuals.
- `.env.example`: mẫu cấu hình môi trường; tạo `.env` cục bộ và thay các giá trị mẫu trước khi chạy Backend.

Các thư mục `core`, `features`, `shared`, `api`, `pages`, `components` và một số package Backend hiện là khung tổ chức mã nguồn; triển khai chức năng sẽ được bổ sung tại đó.
