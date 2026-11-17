# BÁO CÁO TỔNG HỢP & KIỂM TRA ĐỐI CHIẾU THỰC HÀNH 1 – THỰC HÀNH 7
## Đề tài: Xây dựng và Phát triển Hệ thống Quản lý Thi đấu Thể thao theo Kiến trúc Hướng Dịch vụ (SOA / Microservices)

> **Ghi chú cập nhật (Ngày lập báo cáo đối chiếu học phần ban đầu: 09/11/2026 — Đã rà soát đồng bộ ngày 16/11/2026):**  
> Các kết quả `ĐẠT` dưới đây phản ánh lần đối chiếu học phần TH1–TH7 tại thời điểm ban đầu. Mã nguồn sau đó đã được rà soát, chuẩn hóa 5 vai trò RBAC, siết chặt bảo mật và sửa các lỗi logic qua hai vòng (**A1–B6** và **C1–C5+**); kết quả kiểm thử tự động và E2E mới nhất xem chi tiết tại [`KET_QUA_SUA_LOI.md`](KET_QUA_SUA_LOI.md).

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

## 2. Ma trận Kiểm tra Đối chiếu từ TH1 đến TH8 trên TẤT CẢ các Service

| Bài Thực Hành | Nội dung yêu cầu | Trạng thái trên toàn bộ 8 Business Services + Eureka + Gateway |
| :--- | :--- | :--- |
| **TH1** | Xây dựng Web Service cơ bản, CRUD RESTful API (`GET`, `GET /{id}`, `POST`, `PUT /{id}`, `DELETE /{id}`), API `/hello`, `/info`, kiểm thử Postman/Swagger | **ĐẠT** (09/11/2026) trên cả 8 business services (`tournament`, `team`, `athlete`, `match`, `result`, `ranking`, `notification`, `auth`; kiểm thử hiện tại xem `KET_QUA_SUA_LOI.md`). |
| **TH2** | Giao tiếp giữa các service (Provider/Consumer qua REST), đa framework (Spring Boot + **Node.js/Express**), xử lý lỗi khi gọi service (404, 503 khi Provider tắt, 504 khi quá thời gian, retry có giới hạn, fallback), cấu hình URL bằng biến môi trường | **ĐẠT** (17/11/2026): Consumer Spring Boot (`match-service` → `tournament-service`) và Consumer Express `framework-comparison/express-tournament-consumer` (timeout 3s, retry tối đa 3 lần, fallback `TEMPORARILY_UNAVAILABLE`); bảng so sánh `framework-comparison/so-sanh-framework.md`. |
| **TH3** | Thiết kế REST (`docs/rest-api-design.md`): tài nguyên, mã trạng thái, PUT/PATCH, idempotent, versioning; RESTful nâng cao: Tìm kiếm, Lọc nhiều tiêu chí, Sắp xếp, Phân trang (`/paged`), Thống kê (`/statistics`), Cập nhật một phần (`PATCH /{id}`), Bean Validation (`@Valid`), Xử lý lỗi tập trung (`@RestControllerAdvice`), API Versioning (`/api/v1/...`), OpenAPI/Swagger | **ĐẠT** (09/11/2026; đã bổ sung `GlobalExceptionHandler` & allowlist `sortBy` trên các service còn thiếu ở các vòng sửa lỗi — xem `KET_QUA_SUA_LOI.md`). |
| **TH4** | Thiết kế dữ liệu và kết nối service: JPA, **Database-per-Service**, quyền sở hữu dữ liệu, gọi service-to-service, transaction/rollback, sơ đồ dữ liệu và sequence diagram | **ĐẠT** (17/11/2026): 8 CSDL độc lập và tài khoản DB riêng, không khóa ngoại liên CSDL, `@Transactional` + sự kiện AFTER_COMMIT; tài liệu `docs/data-and-service-design.md`. |
| **TH5** | Bảo mật Web Service & Microservices: Đăng ký/Đăng nhập BCrypt, cấp phát JWT (`HMAC-SHA256`), phân quyền RBAC **5 vai trò** (`ROLE_ADMIN`, `ROLE_ORGANIZER`, `ROLE_REFEREE`, `ROLE_TEAM_MANAGER`, `ROLE_VIEWER`), kiểm soát quyền sở hữu tài nguyên (`ownerUsername` Resource Ownership) và lan truyền Token | **ĐẠT** (09/11/2026; đã bổ sung `JWT_SECRET` bắt buộc, `jwt.expiration=3600s` và kiểm soát sở hữu chặt chẽ ở các vòng sửa lỗi sau đó — xem `KET_QUA_SUA_LOI.md`). |
| **TH6** | Tích hợp và tiêu thụ dịch vụ: hợp đồng API, Consumer gọi Provider qua Gateway, tích hợp bất đồng bộ RabbitMQ (`sports.exchange`, `match.finished`), xử lý lỗi tích hợp, kiểm thử tích hợp Postman | **ĐẠT** (17/11/2026): `integration/api-contract.md`, `integration/integration-design.md`, `integration/sequence-diagram/README.md`, `integration/postman/integration-tests.json`, kết quả `integration/test-results.md`. |
| **TH7** | **Service Registry (Netflix Eureka Server `:8761`)**, đăng ký & khám phá dịch vụ động, gọi liên dịch vụ qua tên logic (`@LoadBalanced RestClient`), chạy đa phiên bản (**Multi-Instance `8081` & `8091`**), **Client-Side Load Balancing (Round-Robin)**, **Failover** và tự động phục hồi | **ĐẠT** (17/11/2026): `test_th7_service_discovery.ps1` (`TC01–TC03` qua thật: `8081=10`, `8091=10`, RabbitMQ `MATCH_FINISHED` `referenceId=28`) + `tests/service-discovery/postman-collection.json` (`27` requests / `32` assertions pass qua Newman `service-discovery-th7-newman.json`). Ánh xạ Docker failover/503/recovery sang `test_th8_deployment.ps1`: `TC01→DEP01`, `TC02→SPORT02`, `TC03/TC04→SPORT01`, `TC05→DEP03`, `TC06→DEP04` (tất cả `PASS`). |
| **TH8** | Đóng gói Docker từng service, Docker Compose toàn hệ thống, healthcheck, biến môi trường, volume, giám sát | **ĐẠT** (16/11/2026): `docker-compose.yml`, `docs/deployment/`, `test_th8_deployment.ps1` (DEP/SPORT PASS). |

---

## 3. Chi tiết Giao tiếp Liên Dịch vụ qua Eureka Service Discovery (`@LoadBalanced RestClient`)

Các lời gọi liên dịch vụ sử dụng tên logic đăng ký trên Eureka thông qua `@LoadBalanced RestClient` (địa chỉ mặc định `http://<service-id>`, cho phép cấu hình qua biến `services.*.url` khi chạy độc lập/kiểm thử):

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

### 4.2. Kết quả Luồng Nghiệp vụ Liên thông 8 Services (`TC02` — theo lần chạy đối chiếu & đã đồng bộ với luồng phân quyền 5 vai trò mới)
- **[1. AUTH-SERVICE]**: Đăng nhập tài khoản Organizer hợp lệ (tài khoản `organizer1` được Admin cấp quyền; đăng ký công khai chỉ nhận `ROLE_VIEWER` hoặc `ROLE_TEAM_MANAGER`) và đăng nhập `ROLE_TEAM_MANAGER` (`manager1`) / `ROLE_REFEREE` (`referee1`).
- **[2. TOURNAMENT-SERVICE]**: Organizer tạo `Sport`, `Tournament`, `Competition` và cấu hình `ROUND_ROBIN` qua API Gateway.
- **[3. ATHLETE-SERVICE]**: Team Manager tạo hồ sơ vận động viên `Nguyen Quang Hai` thành công.
- **[4. TEAM-SERVICE $\rightarrow$ ATHLETE-SERVICE]**: Team Manager tạo 2 đội (`CLB Ha Noi`, `CLB Viettel`) và thêm VĐV thuộc quyền sở hữu qua Eureka thành công.
- **[5. MATCH-SERVICE $\rightarrow$ TOURNAMENT & TEAM-SERVICE]**: Team Manager đăng ký tham gia, Organizer duyệt đăng ký 2 đội (tạo `Participant`), tạo/xếp lịch `Match` và gán trọng tài `referee1` (Instance xử lý: `tournament-service:8091`).
- **[6. RESULT-SERVICE $\rightarrow$ MATCH & RANKING-SERVICE]**: Trọng tài chuyển trận sang `ONGOING` & ghi nhận tỉ số `3-1`, Organizer xác nhận (`Status: CONFIRMED`), `result-service` dùng quyền nội bộ `ROLE_INTERNAL_RESULT_SYNC` chuyển trận sang `FINISHED`.
- **[7. RANKING-SERVICE]**: Tự động tính lại BXH: Hạng #1 = `CLB Ha Noi` (`Điểm: 3`, `Hiệu số: +2`).
- **[8. NOTIFICATION-SERVICE]**: Nhận sự kiện `MatchFinishedEvent` từ `sports.exchange` / `sports.notification.queue` và tạo thông báo kết quả trận đấu (kèm tỉ số, theo dõi trạng thái đọc theo từng user).

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

