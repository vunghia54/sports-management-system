# Hệ thống Quản lý Thi đấu Thể thao (Sports Management System)

Hệ thống Quản lý Thi đấu Thể thao đa môn được xây dựng theo **kiến trúc Hướng dịch vụ (SOA / Microservices)** trên nền **Spring Boot 4.1.1 (Java 17+)**, áp dụng mô hình **Database-per-Service**, giao tiếp đồng bộ qua **Spring Cloud LoadBalancer + Netflix Eureka**, giao tiếp bất đồng bộ hướng sự kiện qua **RabbitMQ**, xác thực **JWT HMAC-SHA256** kết hợp phân quyền **RBAC + Resource Ownership**, và giao diện web quản trị phục vụ trực tiếp qua **API Gateway**.

---

## 1. Mô tả nghiệp vụ & Luồng xử lý chính

Hệ thống số hóa toàn bộ vòng đời tổ chức một giải đấu thể thao gồm nhiều nội dung và môn thi khác nhau (Bóng đá, Cầu lông, Bóng chuyền, Chạy bộ, v.v.):

```text
[1. Đăng ký] ──▶ [2. Duyệt hồ sơ] ──▶ [3. Xếp lịch] ──▶ [4. Kết quả] ──▶ [5. Xếp hạng] ──▶ [6. Thông báo]
(TEAM_MANAGER)     (ORGANIZER)         (ORGANIZER)     (REFEREE -> ORG)    (RANKING-SVC)    (RABBITMQ/NOTIF)
```

1. **Quản lý danh mục & thiết lập giải (`tournament-service`):**
   - Admin/Organizer khởi tạo môn thể thao (`Sport`), giải đấu (`Tournament`), các nội dung thi đấu (`Competition`), quy định số lượng/hạn chót (`CompetitionRule`) và **thể thức thi đấu duy nhất** (`CompetitionFormat`: `KNOCKOUT`, `ROUND_ROBIN`, `GROUP_STAGE`, `HYBRID`).
   - Sau khi nội dung đã phát sinh trận đấu, việc sửa `format` / `rule` bị khóa (`409 Conflict`) để giữ tính nhất quán.
2. **Quản lý Vận động viên & Đội tuyển (`athlete-service`, `team-service`):**
   - `ROLE_TEAM_MANAGER` quản lý hồ sơ VĐV (CCCD duy nhất, trạng thái sức khỏe `QUALIFIED` / `INJURED` / `SUSPENDED`) và đội hình; PII nhạy cảm (`identityCardNumber`, `phone`, `email`, `dateOfBirth`, `ownerUsername`) được ẩn với người không có quyền.
   - Khi thêm thành viên vào đội, `TEAM_MANAGER` chỉ được thêm VĐV thuộc quyền sở hữu của chính mình (`ownerUsername` khớp; `ADMIN` có thể làm thay).
3. **Đăng ký & Duyệt tham dự (`match-service`):**
   - `TEAM_MANAGER` gửi đơn đăng ký (`POST /api/v1/registrations`). Hệ thống kiểm tra quyền sở hữu đội/VĐV, thời hạn đăng ký của giải và `CompetitionRule.registrationDeadline`, loại môn `Sport.sportType`, toàn bộ thành viên đội phải `QUALIFIED`, không trùng VĐV giữa các đội `PENDING`/`APPROVED` cùng nội dung, và khóa đăng ký (`409`) sau khi đã có lịch thi đấu.
   - `ORGANIZER` sở hữu giải duyệt đơn (`PUT /api/v1/registrations/{id}/approve`) -> tự động sinh `Participant`.
4. **Lập lịch thi đấu (`match-service`):**
   - `POST /api/v1/matches/generate-schedule` đọc cấu hình trực tiếp từ `CompetitionFormat` của `tournament-service` (hỗ trợ `KNOCKOUT` kèm xử lý miễn vòng, `ROUND_ROBIN` theo thuật toán xoay vòng Circle Method, `GROUP_STAGE` chia đúng `numberOfGroups` bảng); phát hiện trùng sân/trọng tài/đội trong khung 120 phút (`409 Conflict`).
5. **Điều hành, Kết quả & Bảng xếp hạng (`match-service`, `result-service`, `ranking-service`):**
   - Trọng tài được phân công (`refereeUsername`) chỉ được chuyển trạng thái trận đấu từ `SCHEDULED`/`POSTPONED` sang `ONGOING` và nhập/sửa nháp kết quả (`PENDING_CONFIRMATION`).
   - Chỉ Ban tổ chức sở hữu giải hoặc Admin mới xác nhận kết quả (`PUT /api/v1/results/{id}/confirm`). Với thể thức `KNOCKOUT`, tỉ số hòa bắt buộc có điểm luân lưu phân định thắng thua.
   - Sau khi `CONFIRMED`, `result-service` sử dụng token nội bộ `ROLE_INTERNAL_RESULT_SYNC` chuyển trận sang `FINISHED` và kích hoạt `ranking-service` tính lại BXH (`ROUND_ROBIN` / `GROUP_STAGE`); mọi lỗi kết nối tạm thời đều được lưu tác vụ đồng bộ (`RankingSyncTask` & `ResultSyncRecoveryWorker`) để tự động phục hồi.
6. **Thông báo sự kiện (`match-service` -> `RabbitMQ` -> `notification-service`):**
   - Sau khi DB transaction xác nhận kết thúc trận hoàn tất (`@TransactionalEventListener(AFTER_COMMIT)`), `match-service` phát `MatchFinishedEvent` (gồm tỉ số chính thức và luân lưu) qua exchange `sports.exchange` (routing key `match.finished`) tới queue `sports.notification.queue`.
   - `notification-service` chống trùng theo `matchId` và lưu trạng thái đã đọc riêng cho từng người dùng (`notification_reads`).

---

## 2. Phân quyền 5 Vai trò (RBAC + Resource Ownership)

| Vai trò | Cách cấp quyền | Phạm vi nghiệp vụ chính |
|---|---|---|
| `ROLE_ADMIN` | Seed hệ thống / Admin gán | Toàn quyền CRUD, cấp/đổi 5 vai trò hợp lệ qua `PUT /api/v1/auth/users/{id}/role`, đặt lại mật khẩu user, sửa/xóa danh mục môn thể thao (`PUT`/`DELETE /api/v1/sports/{id}`), xóa kết quả/đội. |
| `ROLE_ORGANIZER` | Do Admin cấp | Tạo & quản lý giải đấu do mình sở hữu (`organizerUsername`), cấu hình nội dung/luật/thể thức, tạo môn thể thao mới (`POST`), duyệt đăng ký, xếp lịch, xác nhận/mở lại kết quả thuộc giải mình quản lý. |
| `ROLE_REFEREE` | Do Admin cấp | Bắt đầu trận đấu được phân công (`SCHEDULED`/`POSTPONED` -> `ONGOING`), nhập/sửa nháp kết quả khi chưa xác nhận; không được đặt `FINISHED` trực tiếp hoặc tính BXH. |
| `ROLE_TEAM_MANAGER` | Đăng ký công khai / Admin cấp | Tạo và quản lý đội tuyển & VĐV của mình, thêm VĐV thuộc quyền sở hữu vào đội, gửi hoặc rút đơn đăng ký tham dự giải đấu. |
| `ROLE_VIEWER` | Mặc định khi đăng ký công khai | Tra cứu công khai giải đấu, lịch thi đấu, kết quả, bảng xếp hạng; xem & đánh dấu đã đọc thông báo cá nhân. |

*(Lưu ý: vai trò kỹ thuật nội bộ `ROLE_INTERNAL_RESULT_SYNC` chỉ do `result-service` ký token ngắn hạn để đồng bộ trạng thái `FINISHED` / tính BXH, không thể gán cho tài khoản người dùng.)*

---

## 3. Kiến trúc 10 Microservices & Database-per-Service

```text
                    ┌────────────────────────────────────────┐
                    │       Web UI / Client (Browser)        │
                    └───────────────────┬────────────────────┘
                                        │ HTTP (CORS restricted)
                                        ▼
┌───────────────────┐       ┌────────────────────────┐
│ SERVICE-REGISTRY  │◄──────│      API-GATEWAY       │  :8080
│  (Eureka :8761)   │       │  Static UI + Routing   │
└─────────┬─────────┘       └───────────┬────────────┘
          │ Service Discovery           │ lb://<service-id>
          ▼                             ▼
┌──────────────────────────────────────────────────────────────────────┐
│  AUTH-SERVICE (:8083)            [auth_db]        JWT & Users        │
│  TOURNAMENT-SERVICE (:8081/8091) [tournament_db]  Sports/Tours/Rules │
│  TEAM-SERVICE (:8085)            [team_db]        Teams & Members    │
│  ATHLETE-SERVICE (:8086)         [athlete_db]     Athletes & PII     │
│  MATCH-SERVICE (:8082)           [match_db]       Regs/Matches       │
│  RESULT-SERVICE (:8087)          [result_db]      Results & Recovery │
│  RANKING-SERVICE (:8088)         [ranking_db]     Standings          │
│  NOTIFICATION-SERVICE (:8084)    [notification_db] Per-user Notifs   │
└───────────────────────────────┬──────────────────────▲───────────────┘
                                │                      │
                    Publish AFTER_COMMIT        Consume Queue
                                ▼                      │
                       ┌────────────────────────────────┐
                       │   RabbitMQ Broker (:5672)      │
                       │ sports.exchange / match.finished│
                       │  -> sports.notification.queue  │
                       └────────────────────────────────┘
```

| STT | Module Maven | Service ID trên Eureka | Cổng mặc định | CSDL độc lập | Chức năng chính |
| :-: |---|---| :-: |---|---|
| 1 | `service-registry` | `SERVICE-REGISTRY` | `8761` | — | Máy chủ Netflix Eureka Server đăng ký & phát hiện dịch vụ |
| 2 | `api-gateway` | `API-GATEWAY` | `8080` | Stateless | Định tuyến động API, giới hạn CORS & phục vụ giao diện web SPA |
| 3 | `auth-service` | `AUTH-SERVICE` | `8083` | `auth_db` | Đăng ký, đăng nhập, đổi mật khẩu, quản trị người dùng, phát JWT |
| 4 | `tournament-service` | `TOURNAMENT-SERVICE` | `8081`, `8091` | `tournament_db` | Giải đấu, Môn thể thao, Nội dung, Luật & Thể thức (hỗ trợ 2 instance) |
| 5 | `team-service` | `TEAM-SERVICE` | `8085` | `team_db` | Đội tuyển và thành viên đội (xác thực qua `athlete-service`, `match-service`) |
| 6 | `athlete-service` | `ATHLETE-SERVICE` | `8086` | `athlete_db` | Hồ sơ VĐV, bảo vệ PII/CCCD, kiểm tra ràng buộc xóa |
| 7 | `match-service` | `MATCH-SERVICE` | `8082` | `match_db` | Đăng ký thi đấu, `Participant`, xếp lịch & phát sự kiện RabbitMQ |
| 8 | `result-service` | `RESULT-SERVICE` | `8087` | `result_db` | Ghi nhận, xác nhận kết quả & hàng đợi `RankingSyncTask` bền vững |
| 9 | `ranking-service` | `RANKING-SERVICE` | `8088` | `ranking_db` | Tính điểm, hiệu số, thứ hạng BXH từ kết quả `CONFIRMED` |
| 10 | `notification-service` | `NOTIFICATION-SERVICE` | `8084` | `notification_db` | Nhận sự kiện RabbitMQ và quản lý thông báo + trạng thái đã đọc theo user |

---

## 4. Biến môi trường bắt buộc & Tài khoản Demo

### Biến môi trường

Xem mẫu tại [`.env.example`](.env.example):

| Biến môi trường | Bắt buộc | Mô tả |
|---| :-: |---|
| `JWT_SECRET` | Có | Chuỗi bí mật ký/xác thực JWT (`>= 32` bytes). Không còn giá trị mặc định hardcoded trong mã nguồn Java; thiếu hoặc ngắn hơn 32 bytes service sẽ dừng khởi động. |
| `CORS_ALLOWED_ORIGINS` | Không (mặc định `http://localhost:8080`) | Danh sách origin được phép gọi API Gateway (phân tách bằng dấu phẩy, cấm wildcard `*`). |
| `DB_PASSWORD` | Khi chạy Docker/PostgreSQL | Mật khẩu người dùng PostgreSQL `sports` (`docker-compose.yml` dùng fallback `sports123` nếu không đặt). |
| `APP_DEMO_SEED_USERS` | Không (mặc định `false`) | Đặt `true` trên `auth-service` (hoặc kích hoạt profile `demo`) nếu muốn tự động khởi tạo 6 tài khoản demo dưới đây. |

### Tài khoản Demo (chỉ nạp khi bật `APP_DEMO_SEED_USERS=true`)

| Username | Mật khẩu | Vai trò | Ghi chú |
|---|---|---|---|
| `admin` | `admin123` | `ROLE_ADMIN` | Quản trị hệ thống |
| `organizer1` | `organizer123` | `ROLE_ORGANIZER` | Ban tổ chức giải đấu 1 |
| `organizer2` | `organizer123` | `ROLE_ORGANIZER` | Ban tổ chức giải đấu 2 |
| `referee1` | `referee123` | `ROLE_REFEREE` | Trọng tài |
| `manager1` | `manager123` | `ROLE_TEAM_MANAGER` | Lãnh đội |
| `user1` | `user123` | `ROLE_VIEWER` | Khán giả / người xem |

> **Khuyến cáo bảo mật:** Không bật `APP_DEMO_SEED_USERS=true` và không dùng khóa mẫu trong `.run/*.run.xml` hoặc `.env.example` cho môi trường triển khai thật.

---

## 5. Hướng dẫn Biên dịch, Chạy & Kiểm thử

### Cách 1: Chạy trực tiếp trên IntelliJ IDEA (`.run/`)

1. Đảm bảo PostgreSQL (`localhost:5432`) và RabbitMQ (`localhost:5672`) đã sẵn sàng (hoặc bật container hạ tầng qua `docker compose up -d postgres-db rabbitmq`).
2. Trong IntelliJ IDEA, chọn cấu hình Run **`00_Run_All_10_Microservices`** (hoặc chạy tuần tự `01_ServiceRegistry_8761` trước rồi `02..11`). Các cấu hình `.run/02..11.run.xml` đã khai báo sẵn biến môi trường cục bộ cần thiết.

### Cách 2: Chạy toàn bộ bằng Docker Compose

1. Sao chép [`.env.example`](.env.example) thành `.env` và đặt giá trị `JWT_SECRET` (tối thiểu 32 ký tự):
   ```powershell
   Copy-Item .env.example .env
   ```
2. Khởi chạy hạ tầng và toàn bộ các microservice:
   ```powershell
   docker compose up --build -d
   ```
3. (Tùy chọn) Nạp bộ dữ liệu mẫu giải đấu/đội/VĐV/trận đấu qua API Gateway:
   ```powershell
   .\khoi_dong_du_lieu.bat
   # hoặc: python seed_all_soa_data.py
   ```

### Cách 3: Biên dịch & Chạy bộ kiểm thử tự động bằng Maven

```powershell
# Chạy toàn bộ test tự động trên tất cả 10 module
.\mvnw.cmd test

# Đóng gói toàn bộ file JAR
.\mvnw.cmd clean package -DskipTests
```

### Địa chỉ truy cập nhanh

- **Giao diện Web (API Gateway):** `http://localhost:8080`
- **Eureka Server Dashboard:** `http://localhost:8761`
- **API Gateway Routes:** `http://localhost:8080/api/v1/gateway/routes`
- **RabbitMQ Management UI:** `http://localhost:15672` (mặc định `guest`/`guest`)

---

## 6. Kiểm thử Service Registry & Load Balancing (Thực hành 7 — mục con)

Kịch bản kiểm thử tự động các kịch bản Service Discovery, Multi-Instance (`8081` & `8091`), Load Balancing, Failover và phục hồi được lưu trong `test_th7_service_discovery.ps1`:

```powershell
# Chạy tự động TC01 -> TC06 rồi tự động dọn tiến trình
powershell -ExecutionPolicy Bypass -File .\test_th7_service_discovery.ps1

# Hoặc giữ lại các service sau khi test để kiểm tra thủ công trên Eureka/Web/Postman
powershell -ExecutionPolicy Bypass -File .\test_th7_service_discovery.ps1 -KeepRunning
```

- Tham khảo tài liệu chi tiết kiểm thử và sửa lỗi mới nhất tại [`KET_QUA_SUA_LOI.md`](KET_QUA_SUA_LOI.md) và [`BAO_CAO_THUC_HANH_1_DEN_7.md`](BAO_CAO_THUC_HANH_1_DEN_7.md).
