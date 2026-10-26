# GIÁO TRÌNH & HƯỚNG DẪN TRIỂN KHAI THỰC HÀNH TỪ BUỔI 6 ĐẾN BUỔI 10 (TH6 → TH10)
**Học phần:** Phát triển phần mềm hướng dịch vụ (PTPMHDV)  
**Tài liệu tham chiếu:** Khung chương trình gợi ý từ trang 16 của `TH2.pdf`  
**Hệ thống áp dụng:** Hệ thống Quản lý Thể thao (Sports Management System)  

---

## TỔNG QUAN LỘ TRÌNH 10 BUỔI THỰC HÀNH CỦA HỌC PHẦN

Theo lộ trình sư phạm phát triển tăng dần trên một bài toán duy nhất được định hình từ Buổi 2:
* **Buổi 1:** Làm quen với dịch vụ và xây dựng RESTful Service đầu tiên.
* **Buổi 2:** Giao tiếp giữa các dịch vụ (Synchronous & Asynchronous Communication).
* **Buổi 3:** Thiết kế và xây dựng RESTful API nâng cao (Validation, Search, Pagination, Swagger).
* **Buổi 4:** Quản lý dữ liệu và kết nối dịch vụ (Database per Service, JPA, Transaction).
* **Buổi 5:** Dịch vụ xác thực và bảo mật (Spring Security, JWT, BCrypt, RBAC & Resource Ownership).
* ➡️ **Buổi 6:** Tiêu thụ và Tích hợp dịch vụ chuyên sâu (Service Consumption & Integration).
* ➡️ **Buổi 7:** Đăng ký dịch vụ và Cổng giao tiếp API (Service Registry & API Gateway).
* ➡️ **Buổi 8:** Đóng gói Container và Vận hành (Deployment & DevOps với Docker & Compose).
* ➡️ **Buổi 9:** Kiểm thử tự động và Giám sát hệ thống (Testing, Health Check & Distributed Tracing).
* ➡️ **Buổi 10:** Quản trị giao tiếp, Khả năng phục hồi và Tối ưu hóa (Resilience4j, Circuit Breaker, Caching & BTL Defense).

Dưới đây là chi tiết đề bài, mục tiêu, cấu trúc 150 phút, kịch bản nghiệp vụ và tiêu chí đánh giá từ **Buổi 6 đến Buổi 10**.

---

# BUỔI THỰC HÀNH 6 — TIÊU THỤ VÀ TÍCH HỢP DỊCH VỤ (SERVICE CONSUMPTION & INTEGRATION)

### Thời lượng: 150 phút
### Trọng tâm: Service Aggregation, WebClient / RestClient nâng cao, Saga Pattern, Tích hợp Notification Service

### 1. Mục tiêu buổi thực hành
Sau 150 phút, sinh viên có khả năng:
1. Nắm vững kỹ thuật **Service Aggregation (Gom tụ dịch vụ)**: Tạo một Composite API tổng hợp dữ liệu từ nhiều dịch vụ độc lập.
2. Sử dụng `RestClient` hoặc `WebClient` với cấu hình Interceptor để tự động chuyển tiếp Bearer Token (`Token Propagation`).
3. Hiểu bài toán **Distributed Transaction (Giao dịch phân tán)** và cách áp dụng mô hình **Saga Choreography** thông qua Message Broker.
4. Xây dựng dịch vụ [`notification-service`](file:///c:/Users/vutru/Downloads/sports-management-system-main/notification-service) tiêu thụ sự kiện bất đồng bộ từ RabbitMQ.

### 2. Kịch bản thực hành 150 phút
* **Giai đoạn 1 (30 phút) — Composite API / Data Aggregator:**
  * Xây dựng API tổng hợp: `GET /api/v1/tournaments/{id}/dashboard`
  * API này sẽ gọi song song:
    1. Lấy thông tin chi tiết giải đấu từ `tournament-service`.
    2. Lấy danh sách tất cả các trận đấu của giải đó từ `match-service` (`GET /api/v1/matches/tournament/{id}`).
    3. Đóng gói thành một DTO duy nhất `TournamentDashboardDTO` trả về cho Client.
* **Giai đoạn 2 (40 phút) — Tích hợp Notification Service qua RabbitMQ:**
  * Khi trận đấu kết thúc (`PUT /api/v1/matches/{id}` với `status = "FINISHED"`), `match-service` xuất bản sự kiện `MatchFinishedEvent`.
  * `notification-service` lắng nghe queue `sports.notification.queue`, tự động tạo một thông báo lưu vào bảng `notifications` trong `notification_db`.
* **Giai đoạn 3 (50 phút) — Xử lý lỗi phân tán và tính nhất quán dữ liệu (Data Consistency):**
  * Mô phỏng kịch bản giải đấu bị hủy (`DELETE /api/v1/tournaments/{id}`).
  * `tournament-service` phát sự kiện `TournamentCancelledEvent`.
  * `match-service` lắng nghe và tự động chuyển trạng thái tất cả các trận đấu liên quan sang `CANCELLED` (Saga Choreography).
* **Giai đoạn 4 (30 phút) — Kiểm thử tích hợp bằng Postman:**
  * Kiểm thử luồng khép kín từ Dashboard API đến Event-driven notification.

### 3. Sản phẩm nộp cuối Buổi 6
1. Mã nguồn module [`notification-service`](file:///c:/Users/vutru/Downloads/sports-management-system-main/notification-service) chạy thành công trên cổng 8084.
2. Endpoint `GET /api/v1/tournaments/{id}/dashboard` trả về dữ liệu tổng hợp hợp lệ.
3. Postman Collection chứng minh sự kiện từ `match-service` tạo thành công bản ghi thông báo trong `notification-service`.

---

# BUỔI THỰC HÀNH 7 — ĐĂNG KÝ DỊCH VỤ VÀ CỔNG GIAO TIẾP API (SERVICE REGISTRY & API GATEWAY)

### Thời lượng: 150 phút
### Trọng tâm: Eureka Server / Consul, Spring Cloud Gateway, Dynamic Routing, Centralized Security

### 1. Mục tiêu buổi thực hành
Sau 150 phút, sinh viên có khả năng:
1. Giải thích được tại sao **KHÔNG NÊN** hard-code IP/Port của các dịch vụ trong môi trường cloud/container.
2. Khởi tạo và cấu hình một **Service Registry** (Spring Cloud Netflix Eureka Server) để quản lý danh bạ dịch vụ.
3. Đăng ký các dịch vụ (`auth-service`, `tournament-service`, `match-service`, `notification-service`) làm Eureka Client.
4. Xây dựng **API Gateway** làm điểm tiếp nhận request duy nhất của toàn hệ thống (Single Entry Point).
5. Chuyển cơ chế xác thực JWT lên tầng Gateway (Centralized Authentication Filter).

### 2. Kịch bản thực hành 150 phút
* **Giai đoạn 1 (30 phút) — Khởi tạo Eureka Server:**
  * Tạo module `discovery-service` (Port 8761).
  * Kích hoạt `@EnableEurekaServer`.
  * Cấu hình các dịch vụ con đăng ký với Eureka qua `eureka.client.serviceUrl.defaultZone=http://localhost:8761/eureka/`.
* **Giai đoạn 2 (40 phút) — Cấu hình Dynamic Routing tại API Gateway:**
  * Tạo module `gateway-service` (Port 8080).
  * Cấu hình định tuyến động dựa trên tên dịch vụ (Service ID) thay vì URL cố định:
    ```yaml
    spring:
      cloud:
        gateway:
          routes:
            - id: auth-service
              uri: lb://auth-service
              predicates:
                - Path=/api/v1/auth/**
            - id: tournament-service
              uri: lb://tournament-service
              predicates:
                - Path=/api/v1/tournaments/**
            - id: match-service
              uri: lb://match-service
              predicates:
                - Path=/api/v1/matches/**
    ```
* **Giai đoạn 3 (50 phút) — Centralized JWT Authentication Filter:**
  * Gateway chặn mọi request gửi tới các route bảo vệ.
  * Kiểm tra header `Authorization: Bearer <token>`.
  * Nếu không có token hoặc token sai -> Gateway trả về ngay lập tức `401 Unauthorized` mà không cần forward request vào các dịch vụ phía trong (giảm tải cho core services).
* **Giai đoạn 4 (30 phút) — Thử nghiệm Client-side Load Balancing:**
  * Chạy 2 instance của `tournament-service` (Port 8081 và 8085).
  * Gửi nhiều request tới `http://localhost:8080/api/v1/tournaments` và quan sát Gateway phân phối tải vòng tròn (Round Robin).

### 3. Sản phẩm nộp cuối Buổi 7
1. Ảnh chụp giao diện Eureka Dashboard (`http://localhost:8761`) hiển thị đầy đủ danh sách các dịch vụ đăng ký thành công.
2. API Gateway định tuyến chính xác tất cả các endpoint qua duy nhất cổng 8080.

---

# BUỔI THỰC HÀNH 8 — ĐÓNG GÓI CONTAINER VÀ VẬN HÀNH (DEPLOYMENT & DEVOPS)

### Thời lượng: 150 phút
### Trọng tâm: Docker, Multi-stage Build, Docker Compose, Network Isolation, Persistent Volumes

### 1. Mục tiêu buổi thực hành
Sau 150 phút, sinh viên có khả năng:
1. Viết `Dockerfile` tối ưu hóa kích thước image bằng Alpine JRE và Multi-stage build.
2. Viết file [`docker-compose.yml`](file:///c:/Users/vutru/Downloads/sports-management-system-main/docker-compose.yml) để khởi động toàn bộ hệ sinh thái (App + Database + Message Broker) chỉ bằng một câu lệnh duy nhất.
3. Thiết lập mạng phân lập (`bridge network`) và ổ đĩa lưu trữ bền vững (`persistent volumes`).
4. Quản lý cấu hình bằng biến môi trường (Environment Variables).

### 2. Kịch bản thực hành 150 phút
* **Giai đoạn 1 (30 phút) — Viết Dockerfile cho từng Microservice:**
  * Sử dụng base image nhẹ: `eclipse-temurin:17-jre-alpine`.
  * Thiết lập non-root user và tối ưu layer caching.
* **Giai đoạn 2 (50 phút) — Thiết kế Docker Compose Orchestration:**
  * Khởi tạo container PostgreSQL tạo sẵn 4 database độc lập: `auth_db`, `tournament_db`, `match_db`, `notification_db`.
  * Khởi tạo container RabbitMQ Management (`rabbitmq:3-management`).
  * Khởi tạo 4 application containers (`auth-service`, `tournament-service`, `match-service`, `notification-service`).
  * Sử dụng chỉ thị `depends_on` để quản lý thứ tự khởi động (DB, Broker khởi động trước App).
* **Giai đoạn 3 (40 phút) — Kiểm tra Network & Persistent Storage:**
  * Kiểm tra các container giao tiếp với nhau bằng tên service (DNS nội bộ của Docker): `postgres-db`, `rabbitmq`, `tournament-service`.
  * Dừng container database (`docker compose down`), khởi động lại (`docker compose up -d`) và chứng minh dữ liệu không bị mất nhờ gắn volume `postgres_data`.
* **Giai đoạn 4 (30 phút) — Kiểm thử toàn diện:**
  * Chạy toàn bộ Postman test suite qua cổng ánh xạ của container.

### 3. Sản phẩm nộp cuối Buổi 8
1. File [`docker-compose.yml`](file:///c:/Users/vutru/Downloads/sports-management-system-main/docker-compose.yml) hoàn chỉnh có thể chạy được trên máy giảng viên.
2. Lệnh kiểm tra trạng thái: `docker compose ps` cho thấy toàn bộ các container ở trạng thái `Up / Healthy`.

---

# BUỔI THỰC HÀNH 9 — KIỂM THỬ TỰ ĐỘNG VÀ GIÁM SÁT HỆ THỐNG (TESTING & OBSERVABILITY)

### Thời lượng: 150 phút
### Trọng tâm: Automated Unit/Integration Test, Spring Actuator, Distributed Tracing với Correlation ID

### 1. Mục tiêu buổi thực hành
Sau 150 phút, sinh viên có khả năng:
1. Viết Unit Test và Integration Test kiểm thử tự động tầng Service và Controller với Mockito và `@SpringBootTest`.
2. Giám sát trạng thái hoạt động (Health status, Metrics, Liveness, Readiness probe) qua Spring Boot Actuator.
3. Triển khai cơ chế **Distributed Tracing** bằng cách tự động sinh mã vết yêu cầu `TraceId` và `SpanId` (Correlation ID) gắn vào HTTP Header.
4. Định dạng log có cấu trúc (Structured Logging) để phục vụ tra cứu sự cố trên môi trường phân tán.

### 2. Kịch bản thực hành 150 phút
* **Giai đoạn 1 (45 phút) — Tự động hóa kiểm thử (Automated Tests):**
  * Viết Test Case kiểm tra Resource Ownership trong `TournamentControllerTest`:
    * Mock authenticated user = `organizer2`.
    * Thử update giải đấu của `organizer1` -> Assert trả về `403 Forbidden`.
  * Viết Test Case kiểm tra Token Propagation trong `MatchServiceTest`.
* **Giai đoạn 2 (35 phút) — Cấu hình Spring Boot Actuator:**
  * Bổ sung endpoint kiểm tra sức khỏe: `GET /actuator/health`.
  * Cấu hình chi tiết hiển thị trạng thái kết nối tới Database và RabbitMQ:
    ```properties
    management.endpoints.web.exposure.include=health,info,metrics
    management.endpoint.health.show-details=always
    ```
* **Giai đoạn 3 (40 phút) — Triển khai Correlation ID (Distributed Tracing):**
  * Tạo một Servlet Filter `CorrelationIdFilter`:
    * Kiểm tra xem incoming request có header `X-Correlation-Id` không.
    * Nếu chưa có -> Tự động sinh `UUID.randomUUID().toString()`.
    * Đưa Correlation ID vào `MDC (Mapped Diagnostic Context)` để log của mọi thread đều in kèm ID này.
    * Khi gọi dịch vụ khác qua `RestClient`, tự động đính kèm header `X-Correlation-Id`.
* **Giai đoạn 4 (30 phút) — Kiểm chứng trên log hệ thống:**
  * Gửi một request tạo Match và quan sát log của cả `gateway`, `match-service`, `tournament-service`, `notification-service` đều có chung một mã `TraceId`.

### 3. Sản phẩm nộp cuối Buổi 9
1. Bộ kiểm thử tự động chạy pass 100% khi thực hiện lệnh `mvn test`.
2. Minh chứng log hệ thống thể hiện một luồng request mang cùng mã `TraceId` xuyên suốt qua các microservice.

---

# BUỔI THỰC HÀNH 10 — QUẢN TRỊ GIAO TIẾP, PHỤC HỒI VÀ TỐI ƯU HÓA (RESILIENCE, COMMUNICATION & DEFENSE)

### Thời lượng: 150 phút
### Trọng tâm: Resilience4j, Circuit Breaker, Rate Limiter, In-memory Cache, Hoàn thiện BTL

### 1. Mục tiêu buổi thực hành
Sau 150 phút, sinh viên có khả năng:
1. Triển khai mẫu thiết kế **Circuit Breaker** (Ngắt mạch tự động) ngăn chặn lỗi dây chuyền (Cascading Failure).
2. Thiết lập cơ chế **Rate Limiting** để chống tấn công từ chối dịch vụ (DoS/Brute Force).
3. Triển khai **In-memory Caching** (`@Cacheable`) để tăng tốc độ phản hồi cho các API tra cứu công khai có tần suất đọc lớn (như danh sách giải đấu).
4. Hoàn thiện toàn bộ hồ sơ kỹ thuật, biểu đồ UML và kịch bản demo bảo vệ Bài tập lớn cuối kỳ.

### 2. Kịch bản thực hành 150 phút
* **Giai đoạn 1 (45 phút) — Triển khai Circuit Breaker với Resilience4j:**
  * Cấu hình Circuit Breaker cho lời gọi `TournamentClient` trong `MatchService`:
    * Tỉ lệ lỗi mở mạch (`failureRateThreshold`): 50%.
    * Thời gian chờ ở trạng thái OPEN (`waitDurationInOpenState`): 10 giây.
    * Số request thăm dò ở HALF_OPEN (`permittedNumberOfCallsInHalfOpenState`): 3.
  * Phương thức Fallback:
    ```java
    public boolean tournamentExistsFallback(Long id, Throwable ex) {
        System.out.println(">>> Circuit Breaker kích hoạt Fallback cho Tournament ID: " + id);
        return false;
    }
    ```
  * Thử nghiệm tắt `tournament-service` và quan sát Circuit Breaker ngắt mạch ngay lập tức (Fast-Fail) mà không bị treo request.
* **Giai đoạn 2 (30 phút) — Tối ưu hóa hiệu năng bằng Cache:**
  * Áp dụng `@Cacheable(value = "tournaments")` cho API tra cứu giải đấu.
  * Đo lường thời gian phản hồi: Lần đầu truy vấn DB (~25ms), các lần sau lấy từ Cache (~1ms).
  * Tự động xóa cache (`@CacheEvict`) khi giải đấu được cập nhật.
* **Giai đoạn 3 (45 phút) — Hoàn thiện hồ sơ Bài tập lớn:**
  * Rà soát bảng phân quyền ma trận, sơ đồ cơ sở dữ liệu và toàn bộ 5 tài liệu thiết kế.
  * Chuẩn bị Postman Runner để tự động chạy toàn bộ kịch bản kiểm thử trong 2 phút trước hội đồng.
* **Giai đoạn 4 (30 phút) — Tổng kết và Đánh giá học phần:**
  * Giáo viên đánh giá mức độ đạt được của sinh viên so với chuẩn đầu ra môn học.

---

## BẢNG TIÊU CHÍ ĐÁNH GIÁ TỔNG KẾT BÀI TẬP LỚN CUỐI HỌC PHẦN (THANG ĐIỂM 10)

| STT | Hạng mục đánh giá | Yêu cầu kỹ thuật bắt buộc | Điểm |
|:---:|---|---|:---:|
| 1 | **Kiến trúc Hướng dịch vụ (SOA / Microservices)** | Tách biệt tối thiểu 3-4 service nghiệp vụ độc lập, mỗi service có cơ sở dữ liệu riêng (Database per Service), không truy cập chéo CSDL. | **2.0** |
| 2 | **Giao tiếp Dịch vụ (Service Communication)** | Hiện thực hóa cả 2 cơ chế: Đồng bộ (HTTP/REST qua RestClient có xử lý lỗi) và Bất đồng bộ (RabbitMQ Message Broker với Publisher/Subscriber). | **2.0** |
| 3 | **Bảo mật & Phân quyền (Security & JWT)** | Cấp phát JWT token, băm mật khẩu BCrypt, phân quyền theo vai trò (RBAC) kết hợp **kiểm tra quyền sở hữu tài nguyên (Resource Ownership)**. | **2.0** |
| 4 | **Đóng gói & Vận hành (Docker & DevOps)** | Có `Dockerfile` chuẩn cho từng service và `docker-compose.yml` khởi động toàn bộ hệ thống bằng 1 lệnh duy nhất. | **1.5** |
| 5 | **Khả năng phục hồi & Giám sát (Resilience & Observability)** | Cấu hình Circuit Breaker / Fallback ngăn chặn lỗi dây chuyền, log có mã vết Correlation ID. | **1.0** |
| 6 | **Hồ sơ thiết kế & Kiểm thử (UML & Postman)** | Đầy đủ sơ đồ UML (Use Case, Class, Sequence, Deployment) và Postman Collection kiểm thử đầy đủ các tình huống lỗi và thành công. | **1.5** |
| **TỔNG CỘNG** | | **ĐÁP ỨNG TRỌN VẸN CHUẨN ĐẦU RA MÔN HỌC PTPMHDV** | **10.0** |
