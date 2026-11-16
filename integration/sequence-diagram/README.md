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
    Organizer->>Match: POST /api/v1/matches (Bearer Token, tournamentId=1)
    
    activate Match
    Match->>Match: JwtAuthenticationFilter kiểm tra token hợp lệ
    
    %% 3. Tích hợp REST với Provider
    Note over Match,Tour: Token Propagation: Forward Bearer JWT qua RestClient
    Match->>Tour: GET /api/v1/tournaments/1 (Authorization: Bearer JWT)
    activate Tour
    
    alt Trường hợp 1: Tournament hợp lệ (Thành công)
        Tour-->>Match: 200 OK (Tournament Data)
        Match->>DB: INSERT INTO matches (Lưu thông tin trận đấu)
        DB-->>Match: match record saved
        Match-->>Organizer: 201 Created (Match ID = 15)
    else Trường hợp 2: Tournament không tồn tại (Lỗi nghiệp vụ)
        Tour-->>Match: 404 Not Found
        Match-->>Organizer: 400 Bad Request ("Tournament does not exist")
    else Trường hợp 3: Tournament Service bị sập (Lỗi hệ thống)
        Tour--xMatch: Connection Refused / Timeout
        Match-->>Organizer: 503 Service Unavailable ("Tournament Service unavailable")
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
    actor Referee as Trọng tài (Client)
    participant Match as Match Service (8082)<br/>[Producer]
    participant Rabbit as RabbitMQ Broker (5672)<br/>[Exchange: sports.exchange]
    participant Notif as Notification Service (8084)<br/>[Consumer]
    participant DB as notification_db

    Referee->>Match: PUT /api/v1/matches/15 (status="FINISHED")
    activate Match
    Match->>Match: Cập nhật trạng thái trận đấu = FINISHED
    
    Note over Match,Rabbit: Xuất bản sự kiện bất đồng bộ sau commit (AFTER_COMMIT, Non-blocking)
    Match->>Rabbit: Publish MatchFinishedEvent (RoutingKey: match.finished)
    Match-->>Referee: 200 OK (Match Finished)
    deactivate Match

    activate Notif
    Rabbit->>Notif: Deliver MatchFinishedEvent (Queue: sports.notification.queue)
    Notif->>Notif: NotificationConsumer xử lý message
    Notif->>DB: INSERT INTO notifications (Lưu thông báo kết quả)
    DB-->>Notif: notification saved
    deactivate Notif
```
