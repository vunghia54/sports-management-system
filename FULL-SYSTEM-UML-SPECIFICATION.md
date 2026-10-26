# TỔNG HỢP HỆ THỐNG BIỂU ĐỒ UML TOÀN DIỆN (TH1 → TH10)
**Học phần:** Phát triển phần mềm hướng dịch vụ (PTPMHDV)  
**Đề tài Bài tập lớn:** Hệ thống Quản lý Thể thao (Sports Management Multi-Service System)  
**Kiến trúc:** Microservices Phân tán kết hợp Event-Driven Architecture & Containerization  

---

## 1. Sơ đồ Kiến trúc Tổng thể Hệ thống (Master Microservices Architecture)

Sơ đồ mô tả bức tranh toàn cảnh của hệ thống Sports Management System từ Client tới Gateway, các Microservices nghiệp vụ, Message Broker, Cơ sở dữ liệu độc lập và Hệ thống Giám sát:

```mermaid
graph TD
    subgraph Clients["Tầng Client & Người dùng"]
        WebAdmin["Web Admin / Ban tổ chức"]
        MobileUser["Mobile App / Vận động viên / Khán giả"]
        PostmanClient["Postman / Automated Test Suite"]
    end

    subgraph GatewayLayer["Tầng Cổng giao tiếp & Bảo mật (TH7)"]
        APIGateway["API Gateway (Port 8080)<br/>- Reverse Proxy & Routing<br/>- Centralized JWT Validation<br/>- Rate Limiting & SSL"]
    end

    subgraph ServiceRegistryLayer["Tầng Quản lý Dịch vụ (TH7)"]
        Discovery["Service Registry (Eureka Server)<br/>Port 8761<br/>- Dynamic Discovery<br/>- Heartbeat & Health Status"]
    end

    subgraph BusinessServices["Tầng Dịch vụ Nghiệp vụ (Core Microservices)"]
        AuthSvc["Auth Service (Port 8083)<br/>- User Management<br/>- BCrypt Password Hash<br/>- Issue JWT Token"]
        TournamentSvc["Tournament Service (Port 8081)<br/>- Tournament Lifecycle<br/>- Resource Ownership Check<br/>- Rules & Registration"]
        MatchSvc["Match Service (Port 8082)<br/>- Match Scheduling<br/>- Score & Result Updates<br/>- Token Propagation"]
        NotificationSvc["Notification Service (Port 8084)<br/>- Event Consumer<br/>- Notification Storage<br/>- Mock Push/Email"]
    end

    subgraph MessagingLayer["Tầng Giao tiếp Bất đồng bộ (TH2 & TH6)"]
        RabbitMQ{{"RabbitMQ Broker (Port 5672)<br/>Exchange: sports.exchange<br/>Queue: sports.notification.queue"}}
    end

    subgraph DataLayer["Tầng Cơ sở Dữ liệu Độc lập (Database per Service - TH4)"]
        DB_Auth[("auth_db<br/>(PostgreSQL)")]
        DB_Tour[("tournament_db<br/>(PostgreSQL)")]
        DB_Match[("match_db<br/>(PostgreSQL)")]
        DB_Notif[("notification_db<br/>(PostgreSQL)")]
    end

    subgraph Observability["Tầng Giám sát & Khả năng phục hồi (TH9 & TH10)"]
        Actuator["Spring Actuator / Metrics"]
        CircuitBreaker["Resilience4j Circuit Breaker"]
    end

    %% Client traffic
    WebAdmin -->|HTTPS| APIGateway
    MobileUser -->|HTTPS| APIGateway
    PostmanClient -->|HTTP| APIGateway

    %% Gateway Routing
    APIGateway -->|/api/v1/auth/**| AuthSvc
    APIGateway -->|/api/v1/tournaments/**| TournamentSvc
    APIGateway -->|/api/v1/matches/**| MatchSvc
    APIGateway -->|/api/v1/notifications/**| NotificationSvc

    %% Service Registry
    AuthSvc -.->|Register & Heartbeat| Discovery
    TournamentSvc -.->|Register & Heartbeat| Discovery
    MatchSvc -.->|Register & Heartbeat| Discovery
    NotificationSvc -.->|Register & Heartbeat| Discovery

    %% Synchronous Service Communication
    MatchSvc -->|REST: GET /tournaments/{id}<br/>(Token Propagation + Circuit Breaker)| TournamentSvc

    %% Asynchronous Event Publishing & Consuming
    MatchSvc -->|Publish: MATCH_FINISHED| RabbitMQ
    RabbitMQ -->|Consume Message| NotificationSvc

    %% Database Connections
    AuthSvc --- DB_Auth
    TournamentSvc --- DB_Tour
    MatchSvc --- DB_Match
    NotificationSvc --- DB_Notif
```

---

## 2. Biểu đồ Ca sử dụng Tổng thể (System Use Case Diagram)

Bao quát toàn bộ chức năng từ TH1 đến TH10 với 4 nhóm người dùng chính:

```mermaid
flowchart LR
    %% Actors
    Admin["Quản trị viên (Admin)"]
    Organizer["Ban tổ chức (Organizer)"]
    Referee["Trọng tài (Referee)"]
    Viewer["Vận động viên / Khán giả (Viewer)"]

    subgraph SecurityModule["Module Xác thực & Danh tính (Auth)"]
        UC_Login(["Đăng nhập & Nhận JWT"])
        UC_Register(["Đăng ký tài khoản"])
        UC_Profile(["Xem thông tin cá nhân"])
    end

    subgraph TournamentModule["Module Quản lý Giải đấu (Tournament)"]
        UC_ViewTour(["Xem danh sách & Chi tiết giải đấu"])
        UC_CreateTour(["Tạo mới giải đấu"])
        UC_UpdateTour(["Cập nhật giải đấu (Ownership)"])
        UC_DeleteTour(["Xóa giải đấu (Ownership)"])
    end

    subgraph MatchModule["Module Quản lý Trận đấu (Match)"]
        UC_ViewMatch(["Xem lịch thi đấu & kết quả"])
        UC_CreateMatch(["Tạo trận đấu (Check Tournament)"])
        UC_UpdateResult(["Cập nhật tỉ số & kết quả"])
        UC_DeleteMatch(["Hủy / Xóa trận đấu"])
    end

    subgraph NotificationModule["Module Thông báo (Notification)"]
        UC_ReceiveNotif(["Xem thông báo tự động (Async)"])
        UC_MarkRead(["Đánh dấu đã đọc thông báo"])
    end

    %% Viewer
    Viewer --> UC_Register
    Viewer --> UC_Login
    Viewer --> UC_ViewTour
    Viewer --> UC_ViewMatch
    Viewer --> UC_ReceiveNotif

    %% Referee
    Referee --> UC_Login
    Referee --> UC_ViewMatch
    Referee --> UC_UpdateResult
    Referee --> UC_ReceiveNotif

    %% Organizer
    Organizer --> UC_Login
    Organizer --> UC_CreateTour
    Organizer --> UC_UpdateTour
    Organizer --> UC_DeleteTour
    Organizer --> UC_CreateMatch
    Organizer --> UC_ReceiveNotif

    %% Admin (Full Power)
    Admin --> UC_Login
    Admin --> UC_CreateTour
    Admin --> UC_UpdateTour
    Admin --> UC_DeleteTour
    Admin --> UC_CreateMatch
    Admin --> UC_UpdateResult
    Admin --> UC_DeleteMatch
    Admin --> UC_ReceiveNotif
    Admin --> UC_MarkRead
```

---

## 3. Biểu đồ Lớp Chi tiết Đa dịch vụ (System Class Diagram)

Thể hiện mô hình hướng đối tượng, các Entity, Service, Controller, DTO, Repository và Messaging qua các dịch vụ:

```mermaid
classDiagram
    %% Auth Service
    class User {
        -Long id
        -String username
        -String password
        -String fullName
        -String email
        -String role
        -boolean active
        +getId() Long
        +getUsername() String
        +getRole() String
    }
    class AuthService {
        +login(LoginRequest) LoginResponse
        +register(RegisterRequest) User
        +getUserByUsername(username) User
    }
    class AuthController {
        +login(LoginRequest) ResponseEntity
        +register(RegisterRequest) ResponseEntity
        +getCurrentUser(authHeader) ResponseEntity
    }

    %% Tournament Service
    class Tournament {
        -Long id
        -String name
        -String description
        -LocalDate startDate
        -LocalDate endDate
        -String location
        -String status
        -String organizerUsername
        +getId() Long
        +getOrganizerUsername() String
    }
    class TournamentService {
        +createTournament(Tournament, currentUser) Tournament
        +updateTournament(id, Tournament, authentication) Tournament
        +deleteTournament(id, authentication) boolean
        +getTournamentById(id) Tournament
        +searchTournaments(name, status, page, size...) TournamentPage
    }
    class TournamentController {
        +getAllTournaments() ResponseEntity
        +getTournamentById(id) ResponseEntity
        +createTournament(Tournament, auth) ResponseEntity
        +updateTournament(id, Tournament, auth) ResponseEntity
        +deleteTournament(id, auth) ResponseEntity
    }

    %% Match Service
    class Match {
        -Long id
        -Long tournamentId
        -String round
        -LocalDateTime matchTime
        -String location
        -String participant1
        -String participant2
        -String status
        +getId() Long
        +getTournamentId() Long
    }
    class MatchService {
        +getAllMatches() List~Match~
        +getMatchById(id) Match
        +createMatch(Match) Match
        +updateMatch(id, Match) Match
        +deleteMatch(id) boolean
    }
    class MatchController {
        +getAllMatches() ResponseEntity
        +createMatch(Match) ResponseEntity
        +updateMatch(id, Match) ResponseEntity
        +deleteMatch(id) ResponseEntity
    }
    class TournamentClient {
        -RestClient restClient
        +tournamentExists(tournamentId) boolean
    }

    %% Notification Service
    class Notification {
        -Long id
        -String recipient
        -String title
        -String message
        -String type
        -Long referenceId
        -LocalDateTime createdAt
        -boolean isRead
        +getId() Long
    }
    class NotificationConsumer {
        +handleMatchFinished(MatchFinishedEvent) void
    }
    class NotificationService {
        +getAllNotifications() List~Notification~
        +markAsRead(id) Notification
    }
    class NotificationController {
        +getAllNotifications(recipient) ResponseEntity
        +markAsRead(id) ResponseEntity
    }

    %% Shared Event
    class MatchFinishedEvent {
        -String event
        -Long matchId
        -Long tournamentId
        -String participant1
        -String participant2
    }

    %% Relationships
    AuthController --> AuthService
    TournamentController --> TournamentService
    MatchController --> MatchService
    NotificationController --> NotificationService

    MatchService --> TournamentClient : verifies tournament
    TournamentClient ..> TournamentController : HTTP REST (Port 8081)

    MatchService ..> MatchFinishedEvent : publishes
    NotificationConsumer ..> MatchFinishedEvent : consumes
    NotificationConsumer --> NotificationService : creates notification
```

---

## 4. Biểu đồ Tuần tự Nghiệp vụ End-to-End Hoàn chỉnh (End-to-End Sequence Diagram - TH6)

Mô tả một nghiệp vụ hoàn chỉnh đi xuyên qua toàn bộ các dịch vụ: Đăng nhập → Tạo giải đấu → Tạo trận đấu (giao tiếp đồng bộ) → Cập nhật kết quả → Bắn sự kiện RabbitMQ → Nhận thông báo (giao tiếp bất đồng bộ):

```mermaid
sequenceDiagram
    autonumber
    actor Organizer as Ban tổ chức (organizer1)
    actor Referee as Trọng tài (referee1)
    participant Gateway as API Gateway (8080)
    participant Auth as Auth Service (8083)
    participant Tour as Tournament Service (8081)
    participant Match as Match Service (8082)
    participant Broker as RabbitMQ Exchange
    participant Notif as Notification Service (8084)
    actor Users as Khán giả / VĐV

    %% Step 1: Login
    Note over Organizer, Auth: Bước 1: Đăng nhập và lấy Bearer JWT (TH5)
    Organizer->>Gateway: POST /api/v1/auth/login {"username": "organizer1", "password": "..."}
    Gateway->>Auth: Forward request
    Auth-->>Gateway: 200 OK + JWT Token (ROLE_ORGANIZER, sub: organizer1)
    Gateway-->>Organizer: Trả về accessToken (900s)

    %% Step 2: Create Tournament
    Note over Organizer, Tour: Bước 2: Tạo giải đấu mới (Gán quyền sở hữu)
    Organizer->>Gateway: POST /api/v1/tournaments (Header: Bearer Token)
    Gateway->>Tour: Forward request kèm Bearer JWT
    Tour->>Tour: JwtAuthFilter xác thực token
    Tour->>Tour: Lưu DB với organizerUsername = "organizer1"
    Tour-->>Gateway: 201 Created (Tournament ID = 1)
    Gateway-->>Organizer: 201 Created

    %% Step 3: Create Match
    Note over Organizer, Match: Bước 3: Tạo trận đấu thuộc giải đấu ID = 1 (TH2 & TH4)
    Organizer->>Gateway: POST /api/v1/matches {"tournamentId": 1, ...}
    Gateway->>Match: Forward request kèm Bearer JWT
    Match->>Match: JwtAuthFilter xác thực role
    rect rgb(230, 245, 255)
        Note over Match, Tour: Giao tiếp đồng bộ liên dịch vụ (Token Propagation)
        Match->>Tour: GET /api/v1/tournaments/1 (Header: Bearer Token)
        Tour-->>Match: 200 OK (Tournament tồn tại hợp lệ)
    end
    Match->>Match: Lưu Match vào Database
    Match-->>Gateway: 201 Created (Match ID = 101)
    Gateway-->>Organizer: 201 Created

    %% Step 4: Referee updates match score
    Note over Referee, Match: Bước 4: Trọng tài cập nhật kết quả trận đấu hoàn tất
    Referee->>Gateway: PUT /api/v1/matches/101 {"status": "FINISHED", "score": "3-1"}
    Gateway->>Match: Forward request kèm Bearer JWT (ROLE_REFEREE)
    Match->>Match: Cập nhật kết quả vào DB

    %% Step 5: Asynchronous event publishing
    rect rgb(230, 255, 230)
        Note over Match, Broker: Bắn sự kiện bất đồng bộ qua RabbitMQ (TH2 & TH6)
        Match->>Broker: Push MatchFinishedEvent (matchId: 101, tournamentId: 1)
    end
    Match-->>Gateway: 200 OK (Cập nhật thành công)
    Gateway-->>Referee: 200 OK

    %% Step 6: Notification processing
    rect rgb(255, 245, 230)
        Note over Broker, Notif: Xử lý nền bất đồng bộ (Non-blocking)
        Broker->>Notif: Deliver MatchFinishedEvent
        Notif->>Notif: Tạo Notification record cho tất cả người dùng
    end

    %% Step 7: Users read notifications
    Users->>Gateway: GET /api/v1/notifications
    Gateway->>Notif: Forward request
    Notif-->>Users: 200 OK (Danh sách thông báo kết quả trận đấu mới nhất)
```

---

## 5. Biểu đồ Tuần tự Khả năng Phục hồi (Circuit Breaker & Fallback - TH10)

Mô tả cơ chế tự động bảo vệ hệ thống khi dịch vụ đối tác (`Tournament Service`) gặp sự cố quá tải hoặc mất mạng:

```mermaid
sequenceDiagram
    autonumber
    actor Client as Client / Postman
    participant Match as Match Service
    participant CB as Circuit Breaker (Resilience4j)
    participant Tour as Tournament Service (Remote)

    Note over Match, Tour: Trạng thái ban đầu: CLOSED (Bình thường)

    %% Request 1-3 Failures
    loop 3 lần gọi gặp sự cố (Threshold 50%)
        Client->>Match: POST /api/v1/matches
        Match->>CB: Execute call
        CB->>Tour: GET /api/v1/tournaments/{id}
        Tour--x CB: Read Timeout (>3s) / Connection Refused
        CB-->>Match: Record Failure
        Match-->>Client: 503 Service Unavailable (Fallback Response)
    end

    Note over CB: Tỉ lệ lỗi vượt ngưỡng! Chuyển trạng thái sang OPEN

    %% Request while OPEN
    rect rgb(255, 230, 230)
        Note over Client, CB: Ngắt mạch hoàn toàn (Tránh làm nghẽn thread pool)
        Client->>Match: POST /api/v1/matches
        Match->>CB: Execute call
        CB-->>Match: Fast-Fail (CallNotPermittedException - Không gửi ra mạng)
        Match-->>Client: 503 Fallback Response: "Tournament Service is currently offline"
    end

    Note over CB: Hết thời gian chờ (Wait Duration 10s) -> Chuyển sang HALF_OPEN

    %% Request in HALF_OPEN
    rect rgb(255, 255, 230)
        Note over CB, Tour: Thử nghiệm một số request thăm dò
        Client->>Match: POST /api/v1/matches
        Match->>CB: Execute call
        CB->>Tour: GET /api/v1/tournaments/{id}
        Tour-->>CB: 200 OK (Dịch vụ đã hồi phục!)
        CB-->>Match: Call Successful
        Match-->>Client: 201 Created
    end

    Note over CB: Thăm dò thành công! Đóng mạch lại: CLOSED
```

---

## 6. Biểu đồ Triển khai (UML Deployment Diagram - Docker Multi-Container - TH8)

Mô hình triển khai container hóa toàn bộ hệ sinh thái dịch vụ qua Docker Compose:

```mermaid
graph TD
    subgraph HostMachine["Máy chủ Triển khai (Docker Host / Production VM)"]
        subgraph BridgeNetwork["Docker Bridge Network: sports-network"]
            
            subgraph GatewayNode["Node: API Gateway"]
                C_Gateway["Container: sports-gateway<br/>Port: 8080<br/>Image: spring-cloud-gateway / nginx"]
            end

            subgraph AppNodes["Node: Application Services"]
                C_Auth["Container: sports-auth-service<br/>Port: 8083<br/>JRE: eclipse-temurin:17"]
                C_Tour["Container: sports-tournament-service<br/>Port: 8081<br/>JRE: eclipse-temurin:17"]
                C_Match["Container: sports-match-service<br/>Port: 8082<br/>JRE: eclipse-temurin:17"]
                C_Notif["Container: sports-notification-service<br/>Port: 8084<br/>JRE: eclipse-temurin:17"]
            end

            subgraph BrokerNode["Node: Message Broker"]
                C_Rabbit["Container: sports-rabbitmq<br/>Ports: 5672 (AMQP), 15672 (UI)<br/>Image: rabbitmq:3-management"]
            end

            subgraph DatabaseNode["Node: Database Server"]
                C_Postgres["Container: sports-postgres<br/>Port: 5432<br/>Image: postgres:15-alpine<br/>Databases: auth_db, tournament_db,<br/>match_db, notification_db"]
            end
        end

        subgraph PersistentVolumes["Persistent Storage"]
            V_DB[("Volume: postgres_data<br/>/var/lib/postgresql/data")]
        end
    end

    %% Routing
    C_Gateway --> C_Auth
    C_Gateway --> C_Tour
    C_Gateway --> C_Match
    C_Gateway --> C_Notif

    %% App to Broker
    C_Match --> C_Rabbit
    C_Notif --> C_Rabbit

    %% App to DB
    C_Auth --> C_Postgres
    C_Tour --> C_Postgres
    C_Match --> C_Postgres
    C_Notif --> C_Postgres

    C_Postgres --- V_DB
```

---

## 7. Biểu đồ Hoạt động Nghiệp vụ Toàn trình (Activity Diagram)

Mô tả vòng đời khép kín của Giải đấu và Trận đấu trong hệ thống:

```mermaid
stateDiagram-v2
    [*] --> DangNhap: Người dùng đăng nhập (Auth Service)
    
    state DangNhap {
        KiemTraTaiKhoan: Kiểm tra username/password
        BămMatKhau: Đối chiếu BCrypt
        CapJWT: Cấp Token JWT (vai trò, thời hạn 900s)
    }

    DangNhap --> PhanQuyen: Kiểm tra Role qua Bearer Token
    
    state PhanQuyen {
        state if_role <<choice>>
        if_role --> TaoGiaiDau: ROLE_ORGANIZER / ADMIN
        if_role --> XemCongKhai: ROLE_USER / VIEWER
        if_role --> CapNhatTranDau: ROLE_REFEREE
    }

    state TaoGiaiDau {
        NhapThongTinGiai: Nhập tên, thời gian, địa điểm
        GanOwner: Gán organizerUsername = currentUsername
        LuuGiaiDau: Lưu vào tournament_db
    }

    TaoGiaiDau --> LenLichThiDau: Lên lịch các trận đấu

    state LenLichThiDau {
        GoiCheckTournament: Match Service gọi Tournament Service (Đồng bộ)
        state if_tour_valid <<choice>>
        GoiCheckTournament --> if_tour_valid
        if_tour_valid --> LuuTranDau: Giải đấu tồn tại (200 OK)
        if_tour_valid --> BaoLoi: Không tồn tại -> Báo 400 Bad Request
    }

    LuuTranDau --> ThiDau: Trận đấu diễn ra theo lịch
    
    state ThiDau {
        CapNhatTiSo: Trọng tài nhập kết quả
        DoiTrangThai: Chuyển status = "FINISHED"
    }

    ThiDau --> BanSuKien: Match Service phát MatchFinishedEvent
    
    state BanSuKien {
        DayVaoRabbitMQ: Đẩy message vào sports.exchange
        ConsumerNhan: Notification Service bắt sự kiện
        LuuThongBao: Lưu thông báo vào notification_db
    }

    BanSuKien --> [*]: Hoàn tất vòng đời sự kiện
```
