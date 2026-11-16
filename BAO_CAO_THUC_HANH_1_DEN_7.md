# BÁO CÁO TỔNG HỢP & KIỂM TRA ĐỐI CHIẾU THỰC HÀNH 1 – THỰC HÀNH 7
## Đề tài: Xây dựng và Phát triển Hệ thống Quản lý Thi đấu Thể thao theo Kiến trúc Hướng Dịch vụ (SOA / Microservices)

---

## 1. Tổng quan Kiến trúc Hệ thống (10 Microservices độc lập)

Hệ thống được thiết kế theo mô hình **Microservices & Database-per-Service** gồm **10 dịch vụ độc lập**:

| STT | Tên Microservice | Service ID trên Eureka | Cổng (Port) | Cơ sở dữ liệu (PostgreSQL / H2) | Vai trò trong hệ thống |
| :-: | :--- | :--- | :-: | :--- | :--- |
| 1 | **Service Registry** | `SERVICE-REGISTRY` | `8761` | In-Memory Registry | Máy chủ Netflix Eureka Server quản lý danh bạ dịch vụ tập trung (TH7). |
| 2 | **API Gateway** | `API-GATEWAY` | `8080` | Stateless | Cổng giao tiếp duy nhất định tuyến động (`lb://<service-id>`) tới toàn bộ các service (TH7). |
| 3 | **Auth Service** | `AUTH-SERVICE` | `8083` | `auth_db` | Quản lý định danh, mã hóa mật khẩu BCrypt, cấp phát & xác thực JWT Token (TH5). |
| 4 | **Tournament Service** | `TOURNAMENT-SERVICE` | `8081`, `8091` | `tournament_db` | Quản lý Giải đấu (`Tournament`), Môn thể thao (`Sport`), Nội dung & Luật thi đấu (`Competition`, `CompetitionRule`, `CompetitionFormat`) và Composite Dashboard; chạy **2 instance song song** phục vụ Load Balancing & Failover (TH1–TH7). |
| 5 | **Team Service** | `TEAM-SERVICE` | `8085` | `team_db` | Quản lý Đội thi đấu (`Team`) và Thành viên đội (`TeamMember`); gọi `http://athlete-service` qua Eureka để xác thực vận động viên (TH1–TH7). |
| 6 | **Athlete Service** | `ATHLETE-SERVICE` | `8086` | `athlete_db` | Quản lý Hồ sơ Vận động viên (`Athlete`), trạng thái sức khỏe, tìm kiếm/lọc/phân trang (TH1–TH7). |
| 7 | **Match Service** | `MATCH-SERVICE` | `8082` | `match_db` | Quản lý Lịch thi đấu (`Match`), Đăng ký thi đấu (`Registration`), Danh sách tham dự (`Participant`); gọi `http://tournament-service`, `http://team-service`, `http://athlete-service` qua Eureka; phát sự kiện `MatchFinishedEvent` lên RabbitMQ (TH1–TH7). |
| 8 | **Result Service** | `RESULT-SERVICE` | `8087` | `result_db` | Quản lý Kết quả (`Result`) và Chi tiết trận đấu (`ResultDetail`); gọi `http://match-service` (xác thực & cập nhật `FINISHED`) và `http://ranking-service` (tự động tính lại BXH) qua Eureka (TH1–TH7). |
| 9 | **Ranking Service** | `RANKING-SERVICE` | `8088` | `ranking_db` | Quản lý Bảng xếp hạng (`Ranking`, `RankingItem`); gọi `http://tournament-service`, `http://match-service`, `http://result-service` qua Eureka để tự động tính điểm, hiệu số và xếp hạng (TH1–TH7). |
| 10 | **Notification Service** | `NOTIFICATION-SERVICE` | `8084` | `notification_db` | Lắng nghe sự kiện `MatchFinishedEvent` từ RabbitMQ (Event-Driven) và cung cấp REST API quản lý thông báo (TH1–TH7). |

---

## 2. Ma trận Kiểm tra Đối chiếu từ TH1 đến TH7 trên TẤT CẢ các Service

| Bài Thực Hành | Nội dung yêu cầu | Trạng thái trên toàn bộ 8 Business Services + Eureka + Gateway |
| :--- | :--- | :--- |
| **TH1** | Xây dựng Web Service cơ bản, CRUD RESTful API (`GET`, `GET /{id}`, `POST`, `PUT /{id}`, `DELETE /{id}`), API `/hello`, `/info`, kiểm thử Postman/Swagger | **ĐẠT 100%** trên cả 8 business services (`tournament`, `team`, `athlete`, `match`, `result`, `ranking`, `notification`, `auth`). |
| **TH2** | Phân tích thiết kế SOA, tách dịch vụ theo miền nghiệp vụ, kết nối CSDL quan hệ (JPA/Hibernate) theo mô hình **Database-per-Service** | **ĐẠT 100%**: 8 CSDL độc lập (`tournament_db`, `team_db`, `athlete_db`, `match_db`, `result_db`, `ranking_db`, `notification_db`, `auth_db`), không dùng khóa ngoại cứng liên CSDL. |
| **TH3** | RESTful nâng cao: Tìm kiếm, Lọc nhiều tiêu chí, Sắp xếp, Phân trang (`/paged`), Thống kê (`/statistics`), Cập nhật một phần (`PATCH /{id}`), Bean Validation (`@Valid`), Xử lý lỗi tập trung (`@RestControllerAdvice`), API Versioning (`/api/v1/...`), OpenAPI/Swagger | **ĐẠT 100%** trên tất cả các service. |
| **TH4** | Giao tiếp đồng bộ (REST Client) và bất đồng bộ (RabbitMQ Message Broker - Event-Driven Architecture) giữa các dịch vụ | **ĐẠT 100%**: Giao tiếp REST liên thông giữa `Team -> Athlete`, `Match -> Tournament/Team/Athlete`, `Result -> Match/Ranking`, `Ranking -> Tournament/Match/Result`, cùng luồng bất đồng bộ `MatchEventProducer -> RabbitMQ -> NotificationConsumer`. |
| **TH5** | Bảo mật Web Service & Microservices: Đăng ký/Đăng nhập BCrypt, cấp phát JWT (`HMAC-SHA256`), phân quyền RBAC (`ROLE_ADMIN`, `ROLE_ORGANIZER`, `ROLE_REFEREE`, `ROLE_USER`), kiểm soát quyền sở hữu tài nguyên (`ownerUsername` Resource Ownership) và lan truyền Token (Token Propagation) | **ĐẠT 100%**: Tất cả 8 services đều tích hợp `JwtTokenProvider`, `JwtAuthenticationFilter` và `SecurityConfig`. |
| **TH6** | Đóng gói Containerization (`Dockerfile` cho từng service) và điều phối toàn hệ thống bằng `docker-compose.yml` (PostgreSQL, RabbitMQ, Eureka, Gateway, các Microservices) | **ĐẠT 100%**. |
| **TH7** | **Service Registry (Netflix Eureka Server `:8761`)**, đăng ký & khám phá dịch vụ động, gọi liên dịch vụ qua tên logic (`@LoadBalanced RestClient`), chạy đa phiên bản (**Multi-Instance `8081` & `8091`**), **Client-Side Load Balancing (Round-Robin)**, **Failover** và tự động phục hồi | **ĐẠT 100%** (Đã kiểm thử tự động `TC01 -> TC06` với mã thoát `0`). |

---

## 3. Chi tiết Giao tiếp Liên Dịch vụ qua Eureka Service Discovery (`@LoadBalanced RestClient`)

Không còn bất kỳ lời gọi trực tiếp qua URL cứng (`localhost:808x`) nào giữa các service; toàn bộ đều sử dụng tên dịch vụ đăng ký trên Eureka:

1. **`TEAM-SERVICE` $\rightarrow$ `ATHLETE-SERVICE`**:
   - Class: `team-service/.../client/AthleteClient.java`
   - Khi thêm thành viên vào đội (`POST /api/v1/teams/{id}/members`), `team-service` gọi `GET http://athlete-service/api/v1/athletes/{athleteId}` qua Eureka để xác thực vận động viên tồn tại và tự động điền `athleteName`.
2. **`MATCH-SERVICE` $\rightarrow$ `TOURNAMENT-SERVICE`, `TEAM-SERVICE`, `ATHLETE-SERVICE`**:
   - Class: `match-service/.../client/TournamentClient.java`
   - Khi đăng ký thi đấu (`POST /api/v1/registrations`), `match-service` gọi `http://tournament-service/api/v1/competitions/{id}` và `http://team-service/api/v1/teams/{id}` (hoặc `http://athlete-service/api/v1/athletes/{id}`) qua Eureka.
   - Khi tạo trận đấu (`POST /api/v1/matches`), `match-service` gọi `http://tournament-service/api/v1/tournaments/{id}` (được cân bằng tải Round-Robin giữa instance `8081` và `8091`) và trả về header `X-Tournament-Provider-Instance`.
3. **`RESULT-SERVICE` $\rightarrow$ `MATCH-SERVICE` & `RANKING-SERVICE`**:
   - Class: `result-service/.../client/MatchAndRankingClient.java`
   - Khi ghi nhận kết quả (`POST /api/v1/results`), `result-service` gọi `GET http://match-service/api/v1/matches/{matchId}` để lấy tên 2 đội và tự động xác định đội thắng (`winner`).
   - Khi xác nhận kết quả (`PUT /api/v1/results/{id}/confirm`), `result-service` gọi `PATCH http://match-service/api/v1/matches/{matchId}` (chuyển trạng thái trận đấu sang `FINISHED`) và gọi `POST http://ranking-service/api/v1/rankings/competition/{competitionId}/recalculate`.
4. **`RANKING-SERVICE` $\rightarrow$ `TOURNAMENT-SERVICE`, `MATCH-SERVICE`, `RESULT-SERVICE`**:
   - Class: `ranking-service/.../client/RankingProviderClient.java`
   - Khi tính toán bảng xếp hạng (`POST /api/v1/rankings/competition/{competitionId}/recalculate`), `ranking-service` gọi cả 3 service qua Eureka để tổng hợp số trận, thắng/hòa/thua, bàn thắng/bàn thua, hiệu số và điểm số.

---

## 4. Kết quả Kiểm thử Thực tế Bài Thực Hành 7 (`test_th7_service_discovery.ps1`)

Chạy lệnh kiểm thử tự động toàn diện:
```powershell
powershell -ExecutionPolicy Bypass -File .\test_th7_service_discovery.ps1
```

### 4.1. Kết quả Đăng ký Eureka (`TC01`)
Tất cả dịch vụ hiển thị trạng thái `UP` trên Eureka Server (`http://localhost:8761`):
- `API-GATEWAY`: `api-gateway:8080 [UP]`
- `AUTH-SERVICE`: `auth-service:8083 [UP]`
- `TOURNAMENT-SERVICE`: `tournament-service:8081 [UP], tournament-service:8091 [UP]` *(2 instances)*
- `TEAM-SERVICE`: `team-service:8085 [UP]`
- `ATHLETE-SERVICE`: `athlete-service:8086 [UP]`
- `MATCH-SERVICE`: `match-service:8082 [UP]`
- `RESULT-SERVICE`: `result-service:8087 [UP]`
- `RANKING-SERVICE`: `ranking-service:8088 [UP]`
- `NOTIFICATION-SERVICE`: `notification-service:8084 [UP]`

### 4.2. Kết quả Luồng Nghiệp vụ Liên thông 8 Services (`TC02`)
- **[1. AUTH-SERVICE]**: Đăng ký & đăng nhập JWT thành công (`Role = ROLE_ORGANIZER`).
- **[2. TOURNAMENT-SERVICE]**: Tạo `Sport`, `Tournament`, `Competition` thành công qua API Gateway.
- **[3. ATHLETE-SERVICE]**: Tạo hồ sơ vận động viên `Nguyen Quang Hai` thành công.
- **[4. TEAM-SERVICE $\rightarrow$ ATHLETE-SERVICE]**: Tạo 2 đội (`CLB Ha Noi`, `CLB Viettel`) và xác thực VĐV `Nguyen Quang Hai` qua Eureka thành công.
- **[5. MATCH-SERVICE $\rightarrow$ TOURNAMENT & TEAM-SERVICE]**: Duyệt đăng ký 2 đội, tạo `Participant` và tạo `Match` thành công (Instance xử lý: `tournament-service:8091`).
- **[6. RESULT-SERVICE $\rightarrow$ MATCH & RANKING-SERVICE]**: Ghi nhận & xác nhận tỉ số `3-1` (`Status: CONFIRMED`).
- **[7. RANKING-SERVICE]**: Tự động tính lại BXH: Hạng #1 = `CLB Ha Noi` (`Điểm: 3`, `Hiệu số: +2`).
- **[8. NOTIFICATION-SERVICE]**: Tạo thông báo kết quả trận chung kết thành công.

### 4.3. Bảng Thống kê 20 Requests Cân bằng tải Round-Robin (`TC03`)

| Lần gửi | Instance xử lý | Cổng (Port) | Kết quả |
| :-: | :--- | :-: | :-: |
| 1 | `tournament-service:8081` | `8081` | Thành công (`200 OK`) |
| 2 | `tournament-service:8091` | `8091` | Thành công (`200 OK`) |
| 3 | `tournament-service:8081` | `8081` | Thành công (`200 OK`) |
| 4 | `tournament-service:8091` | `8091` | Thành công (`200 OK`) |
| 5 | `tournament-service:8081` | `8081` | Thành công (`200 OK`) |
| 6 | `tournament-service:8091` | `8091` | Thành công (`200 OK`) |
| 7 | `tournament-service:8081` | `8081` | Thành công (`200 OK`) |
| 8 | `tournament-service:8091` | `8091` | Thành công (`200 OK`) |
| 9 | `tournament-service:8081` | `8081` | Thành công (`200 OK`) |
| 10 | `tournament-service:8091` | `8091` | Thành công (`200 OK`) |
| 11 | `tournament-service:8081` | `8081` | Thành công (`200 OK`) |
| 12 | `tournament-service:8091` | `8091` | Thành công (`200 OK`) |
| 13 | `tournament-service:8081` | `8081` | Thành công (`200 OK`) |
| 14 | `tournament-service:8091` | `8091` | Thành công (`200 OK`) |
| 15 | `tournament-service:8081` | `8081` | Thành công (`200 OK`) |
| 16 | `tournament-service:8091` | `8091` | Thành công (`200 OK`) |
| 17 | `tournament-service:8081` | `8081` | Thành công (`200 OK`) |
| 18 | `tournament-service:8091` | `8091` | Thành công (`200 OK`) |
| 19 | `tournament-service:8081` | `8081` | Thành công (`200 OK`) |
| 20 | `tournament-service:8091` | `8091` | Thành công (`200 OK`) |

$\Rightarrow$ **Tổng kết 20 requests:** Instance `8081` xử lý **10 lần** (50%), Instance `8091` xử lý **10 lần** (50%) — đúng thuật toán **Round-Robin Load Balancing**.

### 4.4. Kết quả Kiểm thử Chịu lỗi & Phục hồi (`TC04`, `TC05`, `TC06`)
- **`TC04` (Dừng Instance 1 - Port `8081`)**: Hệ thống tự động chuyển hướng (Failover) 100% lưu lượng sang Instance 2 (`tournament-service:8091`), không làm gián đoạn dịch vụ.
- **`TC05` (Dừng tiếp Instance 2 - Port `8091`)**: Khi toàn bộ Provider ngừng hoạt động, Consumer (`match-service`) bắt lỗi và trả về mã trạng thái chuẩn `HTTP 503 Service Unavailable`.
- **`TC06` (Khởi động lại Instance 1 - Port `8081`)**: Instance tự động đăng ký lại vào Eureka Server và tiếp tục phục vụ request thành công (`tournament-service:8081`).

