# PHÂN TÍCH THIẾT KẾ UML (TH1 - TH2) & BÁO CÁO TỔNG HỢP CUỐI BUỔI THỰC HÀNH 2
**Học phần:** Phát triển phần mềm hướng dịch vụ (PTPMHDV)  
**Hệ thống áp dụng:** Hệ thống Quản lý Thể thao (Sports Management System)  
**Mô hình triển khai:** Ứng dụng tích hợp duy nhất (`localhost:8080`) — Đa module dịch vụ  

---

# PHẦN 1: HỆ THỐNG BIỂU ĐỒ UML TOÀN DIỆN (TH1 & TH2)

Trong mô hình kiến trúc theo định hướng của nhóm, toàn bộ hệ thống chạy trên một tiến trình Spring Boot duy nhất (`SportsManagementSystemApplication` — Port 8080). Các chức năng nghiệp vụ được phân chia thành các package/component độc lập (`tournament`, `match`, `messaging`), đảm bảo nguyên tắc phân tách trách nhiệm (Separation of Concerns) và sẵn sàng tách thành các microservice độc lập khi cần.

---

## 1. Biểu đồ Ca sử dụng (Use Case Diagram)

Biểu đồ mô tả sự tương tác giữa các tác nhân (Actors) và các chức năng của hệ thống trong TH1 (Quản lý Giải đấu) và TH2 (Quản lý Trận đấu & Giao tiếp dịch vụ):

```mermaid
flowchart LR
    %% Actors
    Admin["Quản trị viên (Admin)"]
    Organizer["Ban tổ chức (Organizer)"]
    Referee["Trọng tài (Referee)"]
    Viewer["Người dùng / Khán giả (Viewer)"]

    %% Subsystem
    subgraph SportsManagementSystem["Hệ thống Quản lý Thể thao (Sports Management System)"]
        %% TH1 Use Cases
        UC1(["UC01: Xem danh sách giải đấu"])
        UC2(["UC02: Xem chi tiết giải đấu"])
        UC3(["UC03: Tạo giải đấu mới"])
        UC4(["UC04: Cập nhật thông tin giải đấu"])
        UC5(["UC05: Xóa giải đấu"])

        %% TH2 Use Cases
        UC6(["UC06: Xem lịch thi đấu & kết quả"])
        UC7(["UC07: Tạo trận đấu mới (Kiểm tra giải đấu)"])
        UC8(["UC08: Cập nhật thông tin/kết quả trận đấu"])
        UC9(["UC09: Xóa trận đấu"])
        UC10(["UC10: Nhận thông báo sự kiện trận đấu (Async)"])
    end

    %% Viewer connections (Public)
    Viewer --> UC1
    Viewer --> UC2
    Viewer --> UC6

    %% Organizer connections
    Organizer --> UC1
    Organizer --> UC2
    Organizer --> UC3
    Organizer --> UC4
    Organizer --> UC5
    Organizer --> UC6
    Organizer --> UC7
    Organizer --> UC8

    %% Referee connections
    Referee --> UC6
    Referee --> UC8

    %% Admin connections (All permissions)
    Admin --> UC3
    Admin --> UC4
    Admin --> UC5
    Admin --> UC7
    Admin --> UC8
    Admin --> UC9

    %% System Actor for Async
    Consumer["Message Consumer (RabbitMQ)"]
    UC10 --> Consumer
```

---

## 2. Biểu đồ Lớp (Class Diagram)

Biểu đồ lớp phân tích thiết kế chi tiết kiến trúc phân tầng (Controller → Service → Repository → Model/Entity) của hai module cốt lõi `tournament` và `match`, cùng thành phần giao tiếp bất đồng bộ `messaging`:

```mermaid
classDiagram
    %% Tournament Module
    class TournamentController {
        -TournamentService tournamentService
        +getAllTournaments(name, status, page, size, sortBy, direction) ResponseEntity
        +getTournamentById(id: Long) ResponseEntity
        +createTournament(tournament: Tournament) ResponseEntity
        +updateTournament(id: Long, tournament: Tournament) ResponseEntity
        +patchTournament(id: Long, updates: Map) ResponseEntity
        +deleteTournament(id: Long) ResponseEntity
    }

    class TournamentService {
        -TournamentRepository repository
        +getAllTournaments() List~Tournament~
        +getTournamentById(id: Long) Tournament
        +createTournament(tournament: Tournament) Tournament
        +updateTournament(id: Long, updated: Tournament) Tournament
        +patchTournament(id: Long, updates: Map) Tournament
        +deleteTournament(id: Long) boolean
        +searchTournaments(name, status, page, size, sortBy, direction) TournamentPage
    }

    class TournamentRepository {
        <<interface>>
        +findById(id: Long) Optional~Tournament~
        +save(tournament: Tournament) Tournament
        +deleteById(id: Long) void
        +existsById(id: Long) boolean
    }

    class Tournament {
        -Long id
        -String name
        -String description
        -LocalDate startDate
        -LocalDate endDate
        -String location
        -LocalDate registrationStartDate
        -LocalDate registrationEndDate
        -String status
        +getId() Long
        +getName() String
        +getStatus() String
    }

    %% Match Module
    class MatchController {
        -MatchService matchService
        +getAllMatches() ResponseEntity
        +getMatchById(id: Long) ResponseEntity
        +getMatchesByTournament(tournamentId: Long) ResponseEntity
        +createMatch(match: Match) ResponseEntity
        +updateMatch(id: Long, match: Match) ResponseEntity
        +deleteMatch(id: Long) ResponseEntity
    }

    class MatchService {
        -MatchRepository matchRepository
        -TournamentClient tournamentClient
        -MatchEventProducer matchEventProducer
        +getAllMatches() List~Match~
        +getMatchById(id: Long) Match
        +tournamentExists(tournamentId: Long) boolean
        +createMatch(match: Match) Match
        +updateMatch(id: Long, updatedMatch: Match) Match
        +deleteMatch(id: Long) boolean
    }

    class MatchRepository {
        <<interface>>
        +findById(id: Long) Optional~Match~
        +findByTournamentId(tournamentId: Long) List~Match~
        +save(match: Match) Match
        +deleteById(id: Long) void
        +existsById(id: Long) boolean
    }

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
        +getStatus() String
    }

    class TournamentClient {
        -RestClient restClient
        +tournamentExists(tournamentId: Long) boolean
    }

    %% Messaging Module
    class MatchEventProducer {
        -RabbitTemplate rabbitTemplate
        +publishMatchFinished(event: MatchFinishedEvent) void
    }

    class MatchFinishedEvent {
        -String event
        -Long matchId
        -Long tournamentId
        -String participant1
        -String participant2
    }

    class MatchEventConsumer {
        +handleMatchFinished(event: MatchFinishedEvent) void
    }

    %% Relationships
    TournamentController --> TournamentService : calls
    TournamentService --> TournamentRepository : uses
    TournamentRepository ..> Tournament : manages

    MatchController --> MatchService : calls
    MatchService --> MatchRepository : uses
    MatchRepository ..> Match : manages

    MatchService --> TournamentClient : verifies tournament via
    TournamentClient ..> TournamentController : HTTP / Internal Call

    MatchService --> MatchEventProducer : triggers async event
    MatchEventProducer ..> MatchFinishedEvent : sends
    MatchEventConsumer ..> MatchFinishedEvent : listens to

    Tournament "1" -- "*" Match : business reference
```

---

## 3. Biểu đồ Tuần tự (Sequence Diagrams)

### 3.1. Sơ đồ 1 (TH1): CRUD Giải đấu (Tournament Management)
Minh họa luồng request/response cơ bản từ Client tới tầng Controller, Service và Repository:

```mermaid
sequenceDiagram
    autonumber
    actor Client as Client / Postman
    participant TC as TournamentController
    participant TS as TournamentService
    participant TR as TournamentRepository
    participant DB as Database / In-Memory

    %% Create Tournament Flow
    Note over Client, DB: Luồng Tạo mới Giải đấu (TH1)
    Client->>TC: POST /api/v1/tournaments (JSON data)
    activate TC
    TC->>TS: createTournament(tournament)
    activate TS
    TS->>TR: save(tournament)
    activate TR
    TR->>DB: INSERT INTO tournaments (...)
    DB-->>TR: Saved entity with Generated ID
    TR-->>TS: Tournament entity
    deactivate TR
    TS-->>TC: Created Tournament
    deactivate TS
    TC-->>Client: 201 Created (JSON Response)
    deactivate TC

    %% Get Tournament by ID Flow
    Note over Client, DB: Luồng Tra cứu Giải đấu theo ID
    Client->>TC: GET /api/v1/tournaments/1
    activate TC
    TC->>TS: getTournamentById(1)
    activate TS
    TS->>TR: findById(1)
    activate TR
    TR->>DB: SELECT * FROM tournaments WHERE id = 1
    DB-->>TR: Tournament record
    TR-->>TS: Optional[Tournament]
    deactivate TR
    TS-->>TC: Tournament
    deactivate TS
    TC-->>Client: 200 OK (JSON Data)
    deactivate TC
```

---

### 3.2. Sơ đồ 2 (TH2): Giao tiếp Đồng bộ (Synchronous) — Tạo Trận đấu có kiểm tra Giải đấu
Minh họa điểm mấu chốt của TH2: **Một service/component kiểm tra dữ liệu ở service khác trước khi thực hiện nghiệp vụ**.

```mermaid
sequenceDiagram
    autonumber
    actor Client as Client / Postman
    participant MC as MatchController
    participant MS as MatchService
    participant TC as TournamentService / TournamentClient
    participant MR as MatchRepository
    participant DB as Database

    Client->>MC: POST /api/matches {"tournamentId": 1, ...}
    activate MC
    MC->>MS: createMatch(match)
    activate MS

    %% Step 1: Service-to-Service Check
    rect rgb(235, 245, 255)
        Note over MS, TC: Bước giao tiếp đồng bộ (Synchronous Call)
        MS->>TC: tournamentExists(1) / getTournamentById(1)
        activate TC
        TC-->>MS: Kết quả: Tournament (Tồn tại - 200 OK)
        deactivate TC
    end

    %% Case 1: Tồn tại -> Tạo Match
    alt Tournament tồn tại
        MS->>MR: save(match)
        activate MR
        MR->>DB: INSERT INTO matches (...)
        DB-->>MR: Saved Match
        MR-->>MS: Match entity (ID = 1)
        deactivate MR
        MS-->>MC: Match vừa tạo
        MC-->>Client: 201 Created (JSON Match)
    else Tournament không tồn tại (Ví dụ tournamentId = 999)
        MS->>TC: tournamentExists(999)
        activate TC
        TC-->>MS: Kết quả: null / 404 Not Found
        deactivate TC
        MS-->>MC: return null
        MC-->>Client: 400 Bad Request ("Invalid tournament ID")
    end
    deactivate MS
    deactivate MC
```

---

### 3.3. Sơ đồ 3 (TH2 - Nâng cao): Xử lý Timeout, Retry và Fallback khi dịch vụ gián đoạn
Minh họa tình huống **Bài 4 & Bài 5 của TH2** khi dịch vụ đối tác phản hồi chậm hoặc tạm dừng hoạt động:

```mermaid
sequenceDiagram
    autonumber
    actor Client as Client / Postman
    participant MS as MatchService
    participant TC as TournamentClient
    participant TS as TournamentService (Remote/Mock)

    Client->>MS: POST /api/matches (tournamentId = 1)
    activate MS
    MS->>TC: tournamentExists(1)
    activate TC

    %% Retry Loop
    rect rgb(255, 240, 240)
        Note over TC, TS: Thử nghiệm Retry tối đa 3 lần khi lỗi mạng
        TC->>TS: Lần 1: GET /api/v1/tournaments/1
        TS--x TC: Timeout (> 3s) / Connection Refused
        Note over TC: Đợi 1 giây (Backoff)
        TC->>TS: Lần 2: GET /api/v1/tournaments/1
        TS--x TC: Connection Refused
        Note over TC: Đợi 2 giây
        TC->>TS: Lần 3: GET /api/v1/tournaments/1
        TS--x TC: Connection Refused
    end

    %% Fallback Trigger
    rect rgb(255, 255, 230)
        Note over TC, MS: Kích hoạt cơ chế Fallback (Mức 3 - Bài 5 TH2)
        TC-->>MS: Ném lỗi có kiểm soát: 503 SERVICE_UNAVAILABLE
        deactivate TC
        MS-->>Client: 503 Service Unavailable<br/>{"status": 503, "message": "Tournament Service is temporarily unavailable"}
    end
    deactivate MS
```

---

### 3.4. Sơ đồ 4 (TH2): Giao tiếp Bất đồng bộ (Asynchronous Communication) qua RabbitMQ
Minh họa Mục 4.3 của TH2: **Client cập nhật kết quả trận đấu, hệ thống gửi message bất đồng bộ qua Message Broker, không làm nghẽn client**:

```mermaid
sequenceDiagram
    autonumber
    actor Client as Trọng tài (Referee)
    participant MC as MatchController
    participant MS as MatchService
    participant MR as MatchRepository
    participant Producer as MatchEventProducer
    participant RabbitMQ as RabbitMQ Exchange / Queue
    participant Consumer as MatchEventConsumer

    Client->>MC: PUT /api/matches/1 (status = "FINISHED", scores...)
    activate MC
    MC->>MS: updateMatch(1, matchData)
    activate MS

    MS->>MR: save(updatedMatch)
    activate MR
    MR-->>MS: Match saved
    deactivate MR

    %% Asynchronous Event Publishing
    rect rgb(230, 255, 240)
        Note over MS, RabbitMQ: Bắn sự kiện bất đồng bộ (Fire-and-Forget)
        MS->>Producer: publishMatchFinished(event)
        activate Producer
        Producer->>RabbitMQ: Push message "MATCH_FINISHED" vào Queue
        deactivate Producer
    end

    %% Return response immediately
    MS-->>MC: Match đã cập nhật
    deactivate MS
    MC-->>Client: 200 OK (Phản hồi tức thì, không cần đợi xử lý sự kiện)
    deactivate MC

    %% Consumer handles event independently
    rect rgb(240, 240, 255)
        Note over RabbitMQ, Consumer: Xử lý nền bất đồng bộ (Non-blocking)
        RabbitMQ->>Consumer: Gửi message từ Queue tới Listener
        activate Consumer
        Consumer->>Consumer: Ghi nhật ký trận đấu hoàn tất & tính điểm BXH
        deactivate Consumer
    end
```

---

## 4. Biểu đồ Hoạt động (Activity Diagram)

Mô tả thuật toán kiểm tra tính hợp lệ của Giải đấu trước khi khởi tạo Trận đấu:

```mermaid
flowchart TD
    Start([Bắt đầu: Nhận Request POST /api/matches]) --> ValidateInput{"Dữ liệu Match hợp lệ?<br/>(Có tournamentId?)"}
    
    ValidateInput -- Không --> Err400_1["Trả về 400 Bad Request<br/>(Thiếu tournamentId)"] --> End([Kết thúc])
    
    ValidateInput -- Có --> CheckService{"Gọi kiểm tra Tournament<br/>(Đồng bộ / Client)"}
    
    CheckService -- "Lỗi kết nối / Timeout (>3s)" --> Fallback["Kích hoạt Fallback / Retry"] --> Err503["Trả về 503 Service Unavailable"] --> End
    
    CheckService -- "Tournament không tồn tại (404)" --> Err400_2["Từ chối tạo Match<br/>Trả về 400 Bad Request"] --> End
    
    CheckService -- "Tournament hợp lệ (200 OK)" --> SaveDB["Lưu Match vào CSDL / Bộ nhớ"]
    
    SaveDB --> Response201["Trả về 201 Created<br/>(Kèm thông tin Match vừa tạo)"] --> End
```

---

## 5. Sơ đồ Kiến trúc Thành phần (Component Architecture Diagram)

Mô hình triển khai thống nhất trong một ứng dụng duy nhất (`localhost:8080`):

```mermaid
graph TD
    subgraph ClientLayer["Client Layer"]
        Postman["Postman / Web App"]
    end

    subgraph SportsManagementApp["Sports Management System (Port 8080)"]
        subgraph TournamentModule["Tournament Component"]
            TC["TournamentController"]
            TS["TournamentService"]
            TR["TournamentRepository"]
        end

        subgraph MatchModule["Match Component"]
            MC["MatchController"]
            MS["MatchService"]
            MR["MatchRepository"]
            TClient["TournamentClient"]
        end

        subgraph MessagingModule["Messaging Component"]
            Producer["MatchEventProducer"]
            Consumer["MatchEventConsumer"]
        end
    end

    subgraph InfrastructureLayer["Hạ tầng Dịch vụ"]
        DB[("PostgreSQL / In-Memory DB")]
        RabbitMQ{{"RabbitMQ Message Broker"}}
    end

    %% Connections
    Postman -->|HTTP REST| TC
    Postman -->|HTTP REST| MC

    TC --> TS --> TR --> DB
    MC --> MS --> MR --> DB

    MS -->|Giao tiếp đồng bộ| TClient
    TClient -.->|HTTP / Internal Call| TC

    MS -->|Giao tiếp bất đồng bộ| Producer
    Producer -->|AMQP Push| RabbitMQ
    RabbitMQ -->|AMQP Consume| Consumer
```

---

# PHẦN 2: NỘI DUNG TỔNG KẾT & CÂU HỎI CUỐI TH2 (THEO TH2.PDF)

---

## 1. Exit Ticket cuối buổi (5 câu hỏi cốt lõi kèm lời giải chi tiết)

### Câu 1: REST API và Spring Boot có phải là một khái niệm không? Giải thích.
* **Trả lời:** **KHÔNG.** Đây là hai khái niệm hoàn toàn khác biệt ở hai tầng bản chất:
  1. **REST API (Representational State Transfer):** Là một **kiểu kiến trúc phần mềm (Architectural Style)** và chuẩn giao tiếp dựa trên nền tảng giao thức HTTP. REST định nghĩa các nguyên tắc thiết kế như: Stateless, Resource-oriented (tài nguyên định danh bằng URI danh từ), thao tác chuẩn qua các HTTP Method (`GET`, `POST`, `PUT`, `DELETE`), trao đổi dữ liệu định dạng JSON/XML và sử dụng chuẩn mã trạng thái HTTP Status Code.
  2. **Spring Boot:** Là một **framework mã nguồn mở cụ thể của ngôn ngữ Java**, cung cấp môi trường, thư viện và công cụ để lập trình viên hiện thực hóa các ứng dụng Web/Microservices một cách nhanh chóng.
* **Minh chứng:** REST API có thể được hiện thực hóa bằng rất nhiều framework/ngôn ngữ khác nhau như Quarkus (Java), Express (Node.js), FastAPI (Python), ASP.NET Core (C#), Golang... Việc triển khai REST API không đồng nghĩa và không phụ thuộc duy nhất vào Spring Boot.

---

### Câu 2: Trong mô hình Match → Tournament (hoặc Appointment → Patient), service nào là Consumer, service nào là Provider?
* **Trả lời:**
  * **`Tournament Service` (hoặc `Patient Service`) là PROVIDER (Service cung cấp dịch vụ):** Vì nó nắm giữ dữ liệu giải đấu/bệnh nhân và cung cấp các endpoint API (ví dụ: `GET /api/v1/tournaments/{id}`) cho bên ngoài gọi vào.
  * **`Match Service` (hoặc `Appointment Service`) là CONSUMER (Service tiêu thụ dịch vụ):** Vì khi có yêu cầu tạo trận đấu mới, nó phải đóng vai trò là một client chủ động gửi HTTP Request đến `Tournament Service` để tra cứu và tiêu thụ dữ liệu nhằm xác nhận giải đấu có tồn tại hay không.

---

### Câu 3: Điều gì xảy ra nếu Tournament Service (Patient Service) bị ngừng hoạt động?
* **Trả lời:**
  * Nếu không có cơ chế phòng vệ, khi `Tournament Service` dừng hoạt động (down/sập):
    1. `Match Service` gửi HTTP request tới `Tournament Service` sẽ nhận lỗi **Connection Refused** hoặc bị treo vô thời hạn dẫn tới **Read Timeout**.
    2. Các thread trong `Match Service` bị chặn (blocked), làm cạn kiệt tài nguyên máy chủ (thread exhaustion), dẫn tới việc `Match Service` cũng bị quá tải và sập theo. Đây chính là hiện tượng **Lỗi dây chuyền (Cascading Failure)** đặc trưng trong hệ thống phân tán.
    3. Trải nghiệm người dùng bị hỏng nghiêm trọng khi nhận về lỗi HTTP 500 kèm chi tiết stack trace không an toàn.
  * **Giải pháp khắc phục:** Phải triển khai cấu hình Timeout hợp lý (ví dụ 3 giây), cơ chế **Retry** (thử lại tối đa 3 lần với backoff), và bộ ngắt mạch **Circuit Breaker / Fallback** để trả về phản hồi có kiểm soát (HTTP 503 hoặc dữ liệu dự phòng) bảo vệ hệ thống.

---

### Câu 4: Phân biệt Synchronous Communication và Asynchronous Communication
* **Bảng so sánh chi tiết:**

| Tiêu chí | Giao tiếp Đồng bộ (Synchronous) | Giao tiếp Bất đồng bộ (Asynchronous) |
|---|---|---|
| **Mô hình** | **Request / Response** | **Message / Event (Publish / Subscribe)** |
| **Cơ chế xử lý** | **Blocking:** Service A gửi request sang Service B và **bắt buộc phải chờ** B xử lý xong để nhận phản hồi. | **Non-blocking (Fire-and-forget):** Service A gửi message vào Message Broker rồi lập tức tiếp tục công việc của mình mà **không cần chờ** Service B. |
| **Độ phụ thuộc (Coupling)** | **Temporal Coupling cao:** Cả hai service A và B đều phải đang hoạt động đồng thời (Available) tại thời điểm gọi. | **Decoupled:** Service B có thể đang bảo trì hoặc tắt; message vẫn nằm an toàn trong Queue và sẽ được xử lý khi B khởi động lại. |
| **Giao thức phổ biến** | HTTP/REST, gRPC | AMQP (RabbitMQ), Kafka, MQTT |
| **Trường hợp áp dụng** | Khi Service A **bắt buộc cần dữ liệu ngay lập tức** để quyết định nghiệp vụ (VD: Kiểm tra giải đấu có tồn tại không trước khi tạo trận). | Khi xử lý tác vụ hậu kỳ, tốn thời gian, hoặc gửi thông báo (VD: Gửi thông báo trận đấu kết thúc, cập nhật bảng xếp hạng, gửi email). |

---

### Câu 5: Khi nào nên cân nhắc giao tiếp bất đồng bộ thay vì gọi REST trực tiếp?
Nên ưu tiên giao tiếp bất đồng bộ (Message-driven) trong 5 trường hợp sau:
1. **Tác vụ xử lý tốn nhiều thời gian (Long-running Tasks):** Ví dụ gửi email xác nhận cho hàng nghìn vận động viên, xuất báo cáo PDF, tính toán lại toàn bộ bảng xếp hạng sau giải đấu.
2. **Cần tách rời mức độ phụ thuộc giữa các dịch vụ (Decoupling):** Khi Service A không cần biết chi tiết Service nào sẽ tiêu thụ kết quả của mình; A chỉ cần phát đi sự kiện `MatchFinishedEvent`.
3. **Mô hình 1-Nhiều (One-to-Many / Fan-out):** Khi một sự kiện xảy ra cần kích hoạt nhiều dịch vụ cùng lúc (VD: Trận đấu kết thúc cần đồng thời thông báo cho: Notification Service, Ranking Service, Statistic Service).
4. **Cân bằng tải và làm mịn đỉnh tải (Peak Load Shaving / Buffering):** Khi lượng truy cập tăng đột biến, Message Queue đóng vai trò vùng đệm, giúp các service tiêu thụ xử lý dần dần theo năng lực thực tế mà không bị sập.
5. **Đảm bảo tính sẵn sàng cao (High Resilience):** Khi service đích có thể tạm thời không khả dụng, tin nhắn vẫn được lưu trữ bền vững (Durable) trên Broker và không bị mất mát dữ liệu.

---

## 2. Bảng so sánh Framework đa nền tảng (Spring Boot vs Quarkus vs Node.js + Express)

Đúng theo yêu cầu của Bài 3 (Trang 8–9 trong tài liệu `TH2.pdf`), đây là bảng khảo sát kỹ thuật so sánh cùng một nghiệp vụ trên nhiều nền tảng:

| Tiêu chí | Spring Boot | Quarkus | Node.js + Express |
|---|---|---|---|
| **Ngôn ngữ** | Java (17+) | Java (GraalVM Native / JVM) | JavaScript / TypeScript |
| **Tạo REST API** | `@RestController`, `@GetMapping`, `@PostMapping` (Spring MVC) | `@Path`, `@GET`, `@POST` (JAX-RS / RESTEasy Reactive) | `app.get()`, `app.post()`, Router middleware |
| **HTTP Client** | `RestClient`, `WebClient` | Quarkus REST Client Reactive (`@RegisterRestClient`) | `axios`, `node-fetch`, built-in `fetch` |
| **Xử lý JSON** | Jackson (`ObjectMapper`) tự động | Jackson hoặc JSON-B tự động | `express.json()` middleware built-in |
| **Quản lý Thư viện** | Maven (`pom.xml`) / Gradle | Maven (`pom.xml`) / Gradle | npm / pnpm / yarn (`package.json`) |
| **Cơ chế Cấu hình** | `application.properties` / `application.yml` | `application.properties` (MicroProfile Config) | `.env`, `process.env`, file `config.js` |
| **Thời gian khởi động** | Trung bình: **1.5s – 3.5s** | Cực nhanh: **0.05s** (Native) / **~1.0s** (JVM) | Rất nhanh: **0.3s – 0.8s** |
| **Bộ nhớ chiếm dụng (RAM)** | Khá lớn: **~250MB – 450MB** | Rất thấp: **~30MB – 60MB** (Native) | Thấp: **~40MB – 80MB** |
| **Mức độ phức tạp** | Trung bình; hệ sinh thái phong phú, nhiều tài liệu | Trung bình - Khá; tối ưu hoàn hảo cho Docker/K8s | Đơn giản, linh hoạt, cú pháp ngắn gọn |
| **Đánh giá bản chất** | Phù hợp hệ thống Enterprise lớn, ổn định cao | Lựa chọn số 1 cho Serverless, Cloud-native | Rất nhanh cho I/O bound, prototype và service nhẹ |

> **Nhận thức sư phạm cốt lõi:**  
> Kiến trúc hướng dịch vụ và giao thức giao tiếp HTTP/REST + JSON hoàn toàn **độc lập với framework triển khai**. Dù dùng Spring Boot, Quarkus hay Node.js, bản chất thông điệp gửi qua mạng vẫn là các HTTP Request/Response mang dữ liệu JSON chuẩn mực.

---

## 3. Thử thách mở rộng cuối TH2 (Mức 1 → Mức 4)

Trong bài tập của nhóm, các thử thách nâng cao đã được thiết kế như sau:

* **Mức 1 — Service Discovery giả lập & Cấu hình tập trung:**  
  Địa chỉ của service không bao giờ được hard-code trực tiếp trong mã Java mà luôn được externalize vào `application.properties`:
  ```properties
  services.tournament.url=${TOURNAMENT_SERVICE_URL:http://localhost:8080}
  ```
* **Mức 2 — Cơ chế Retry (Thử lại khi lỗi tạm thời):**  
  Khi gọi dịch vụ đối tác, cấu hình cơ chế thử lại tối đa 3 lần (Max Retry = 3) kèm khoảng chờ tăng dần (exponential backoff) trước khi kết luận thất bại.
* **Mức 3 — Cơ chế Fallback (Dự phòng có kiểm soát):**  
  Nếu dịch vụ đối tác không phản hồi hoặc bị timeout, hệ thống không để văng `500 Internal Server Error` mà chủ động bắt exception và trả về mã lỗi `503 Service Unavailable` kèm cấu trúc JSON thông báo rõ ràng:
  ```json
  {
    "status": 503,
    "message": "Tournament Service is temporarily unavailable"
  }
  ```
* **Mức 4 — Khái niệm API Gateway:**  
  Client bên ngoài chỉ tương tác qua một cổng duy nhất (Port 8080). Gateway sẽ đóng vai trò Reverse Proxy định tuyến request về đúng component (`/api/v1/tournaments/**` tới Tournament Component, `/api/v1/matches/**` tới Match Component).

---

## 4. Hướng dẫn kiểm thử 4 Test Cases chuẩn trên ứng dụng (Port 8080)

Khi khởi chạy ứng dụng duy nhất `SportsManagementSystemApplication` trên cổng `localhost:8080`, tiến hành kiểm thử theo đúng trình tự:

### Test Case 1: Tạo Giải đấu mẫu (TH1)
* **Request:** `POST http://localhost:8080/api/v1/tournaments`
* **Body:**
```json
{
  "name": "Hanoi Open 2026",
  "description": "Giai bong da sinh vien",
  "startDate": "2026-11-01",
  "endDate": "2026-11-15",
  "location": "San van dong My Dinh",
  "registrationStartDate": "2026-10-01",
  "registrationEndDate": "2026-10-25",
  "status": "OPEN"
}
```
* **Kết quả:** `201 Created`, giải đấu được tạo thành công với `id = 1`.

---

### Test Case 2: Tạo Trận đấu hợp lệ (TH2 - Synchronous Success)
* **Request:** `POST http://localhost:8080/api/v1/matches`
* **Body:**
```json
{
  "tournamentId": 1,
  "round": "ROUND_1",
  "matchTime": "2026-11-05T09:00:00",
  "location": "San A",
  "participant1": "Doi CNTT",
  "participant2": "Doi DTVT",
  "status": "SCHEDULED"
}
```
* **Kết quả:** `201 Created`, Match được tạo thành công vì `tournamentId = 1` tồn tại.

---

### Test Case 3: Tạo Trận đấu với Giải đấu KHÔNG tồn tại (TH2 - Synchronous Validation)
* **Request:** `POST http://localhost:8080/api/v1/matches`
* **Body:**
```json
{
  "tournamentId": 9999,
  "round": "FINAL",
  "matchTime": "2026-11-20T19:00:00",
  "location": "San Chinh",
  "participant1": "Doi A",
  "participant2": "Doi B",
  "status": "SCHEDULED"
}
```
* **Kết quả:** `400 Bad Request` (hoặc thông báo lỗi giải đấu không tồn tại), hệ thống **từ chối lưu Match**, dữ liệu được bảo toàn tính toàn vẹn.

---

### Test Case 4: Cập nhật Trận đấu hoàn tất & Kích hoạt Sự kiện RabbitMQ (TH2 - Asynchronous)
* **Request:** `PUT http://localhost:8080/api/v1/matches/1`
* **Body:**
```json
{
  "tournamentId": 1,
  "round": "ROUND_1",
  "matchTime": "2026-11-05T09:00:00",
  "location": "San A",
  "participant1": "Doi CNTT (Score: 3)",
  "participant2": "Doi DTVT (Score: 1)",
  "status": "FINISHED"
}
```
* **Kết quả:** `200 OK`, ứng dụng ghi nhận `MATCH_FINISHED`, `MatchEventProducer` đẩy event vào RabbitMQ và `MatchEventConsumer` nhận message in log xử lý nền thành công.
