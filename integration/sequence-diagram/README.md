# BIỂU ĐỒ TUẦN TỰ MÔ TẢ QUY TRÌNH TÍCH HỢP LIÊN DỊCH VỤ (TH6)
**Đề tài:** Hệ thống Quản lý thi đấu Thể thao  

---

## 1. Sequence Diagram: Tích hợp Đồng bộ REST (Match Service ➔ Tournament Service)

Biểu đồ mô tả quy trình Ban tổ chức tạo lịch thi đấu. `Match Service` đóng vai trò **Consumer** gọi sang `Tournament Service` (**Provider**) với cơ chế **Token Propagation** và xử lý lỗi:

```mermaid
sequenceDiagram
    autonumber
    actor Organizer as Ban tổ chức (Client / Postman)
    participant Auth as Auth Service (8083)
    participant Match as Match Service (8082)<br/>[Consumer]
    participant Tour as Tournament Service (8081)<br/>[Provider]
    participant DB as match_db (PostgreSQL)

    %% 1. Đăng nhập
    Organizer->>Auth: POST /api/v1/auth/login
    Auth-->>Organizer: 200 OK (JWT Token - ROLE_ORGANIZER)

    %% 2. Gọi tạo trận đấu
    Organizer->>Match: POST /api/v1/matches (Bearer Token, tournamentId, competitionId, participant1Id, participant2Id, matchTime)
    
    activate Match
    Match->>Match: JwtAuthenticationFilter kiểm tra token hợp lệ
    
    %% 3. Tích hợp REST với Provider
    Note over Match,Tour: Token Propagation: Forward Bearer JWT qua RestClient (TournamentClient)
    Match->>Tour: GET /api/v1/tournaments/{tournamentId} (Authorization: Bearer JWT)
    activate Tour
    
    alt Trường hợp 1: Tournament hợp lệ
        Tour-->>Match: 200 OK (Tournament Data + X-Instance-Id)
        Match->>Tour: GET /api/v1/competitions/{competitionId} (Authorization: Bearer JWT)
        alt Nội dung thi đấu hợp lệ thuộc giải đấu
            Tour-->>Match: 200 OK (Competition Data)
            Match->>Match: Kiểm tra participant1Id/2Id và xung đột lịch
            Match->>DB: INSERT INTO matches (Lưu thông tin trận đấu)
            DB-->>Match: match record saved
            Match-->>Organizer: 201 Created (X-Tournament-Provider-Instance)
        else Competition không tồn tại (404) hoặc không thuộc giải đấu
            Tour-->>Match: 404 Not Found
            Match-->>Organizer: 400 Bad Request ("Nội dung thi đấu không tồn tại...")
        end
    else Trường hợp 2: Tournament không tồn tại (Lỗi nghiệp vụ)
        Tour-->>Match: 404 Not Found
        Match-->>Organizer: 400 Bad Request ("Invalid tournament ID")
    else Trường hợp 3: Tournament Service bị sập / timeout (Lỗi hệ thống)
        Tour--xMatch: Connection Refused / Timeout / No Eureka Instance
        Match-->>Organizer: 503 Service Unavailable ("Tournament Service is unavailable...")
    end
    deactivate Tour
    deactivate Match
```

---

## 2. Sequence Diagram: Tích hợp Bất đồng bộ Event-Driven qua RabbitMQ

Biểu đồ mô tả sự kiện kết thúc trận đấu và thông báo tự động:

```mermaid
sequenceDiagram
    autonumber
    actor Organizer as Ban tổ chức (Client)
    participant Result as Result Service (8087)
    participant Match as Match Service (8082)<br/>[Producer]
    participant Rabbit as RabbitMQ Broker (5672)<br/>[Exchange: sports.exchange]
    participant Notif as Notification Service (8084)<br/>[Consumer]
    participant DB as notification_db

    Note over Organizer,Result: Trước đó trọng tài đã PATCH trận sang ONGOING và ghi kết quả (POST /api/v1/results)
    Organizer->>Result: PUT /api/v1/results/{resultId}/confirm
    Result->>Match: PATCH /api/v1/matches/{matchId} (status="FINISHED", token nội bộ ROLE_INTERNAL_RESULT_SYNC)
    activate Match
    Match->>Match: Cập nhật trạng thái trận đấu = FINISHED (transaction)
    
    Note over Match,Rabbit: Xuất bản sự kiện bất đồng bộ sau commit (AFTER_COMMIT, Non-blocking)
    Match->>Rabbit: Publish MatchFinishedEvent (RoutingKey: match.finished)
    Match-->>Result: 200 OK
    Result-->>Organizer: 200 OK (kết quả CONFIRMED)
    deactivate Match

    activate Notif
    Rabbit->>Notif: Deliver MatchFinishedEvent (Queue: sports.notification.queue)
    Notif->>Notif: NotificationConsumer xử lý message
    Notif->>DB: INSERT INTO notifications (Lưu thông báo kết quả)
    DB-->>Notif: notification saved
    deactivate Notif
```
