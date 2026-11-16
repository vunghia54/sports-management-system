# 02. KIẾN TRÚC HƯỚNG DỊCH VỤ (SOA), ERD & SEQUENCE DIAGRAM

---

## 1. Kiến trúc Tổng thể 10 Microservices (SOA Architecture)

Hệ thống được thiết kế tuân thủ nghiêm ngặt các nguyên tắc **Service-Oriented Architecture (SOA)**:
- **Độc lập tiến trình & Cổng giao tiếp:** 10 dịch vụ chạy thành các tiến trình độc lập, giao tiếp qua RESTful API và Message Broker (RabbitMQ).
- **Database-per-Service:** 8 dịch vụ nghiệp vụ sở hữu 8 schema/database tách biệt (`auth_db`, `tournament_db`, `team_db`, `athlete_db`, `match_db`, `result_db`, `ranking_db`, `notification_db`). Không dịch vụ nào truy cập trực tiếp bảng dữ liệu của dịch vụ khác.
- **Service Registry & Discovery (Eureka `:8761`):** Không hardcode địa chỉ IP/Port; toàn bộ lời gọi liên dịch vụ sử dụng tên logic (`http://tournament-service`, `http://match-service`, `http://athlete-service`, `http://ranking-service`) qua `@LoadBalanced RestClient`.
- **Client-Side Load Balancing & Failover:** `tournament-service` vận hành đồng thời 2 instances (`:8081` và `:8091`), tự động chia tải Round-Robin và tự động chuyển đổi dự phòng (Failover) khi 1 instance gặp sự cố.

```mermaid
flowchart TB
    Client["Web Application (14 Pages Dual-Theme UI)\n& Postman / Mobile Client"]

    subgraph GatewayLayer["Tầng Cổng Giao Tiếp & Khám Phá Dịch Vụ"]
        GW["API Gateway (:8080)\nRouting + LoadBalancer + Static Web UI"]
        Eureka["Service Registry - Eureka Server (:8761)\nService Discovery & Health Check"]
    end

    subgraph CoreServices["Tầng Dịch Vụ Nghiệp Vụ (Core SOA Microservices)"]
        Auth["Auth Service (:8083)\nJWT + BCrypt + RBAC"]
        Tour1["Tournament Service Inst 1 (:8081)\nTournament, Sport, Competition"]
        Tour2["Tournament Service Inst 2 (:8091)\nRound-Robin Replica"]
        Team["Team Service (:8085)\nTeam & TeamMember"]
        Ath["Athlete Service (:8086)\nAthlete Profile & Health"]
        Match["Match Service (:8082)\nMatch, Registration, Conflict Check"]
        Res["Result Service (:8087)\nScore, Details, Referee Confirm"]
        Rank["Ranking Service (:8088)\nAuto Standings & Tie-Breaker"]
        Notif["Notification Service (:8084)\nEvent Consumer & Alerts"]
    end

    subgraph InfraLayer["Tầng Hạ Tầng Dữ Liệu & Message Broker"]
        MQ[("RabbitMQ (:5672)\nmatch.exchange")]
        DB[("8 Independent Databases\n(PostgreSQL 15 / H2 Auto-Server)")]
    end

    Client -->|HTTP /api/v1/**| GW
    GW -.->|Lookup Service ID| Eureka
    GW --> Auth
    GW --> Tour1
    GW --> Tour2
    GW --> Team
    GW --> Ath
    GW --> Match
    GW --> Res
    GW --> Rank
    GW --> Notif

    Team -->|REST via Eureka| Ath
    Match -->|REST LoadBalanced| Tour1
    Match -->|REST LoadBalanced| Tour2
    Res -->|REST via Eureka| Match
    Res -->|REST via Eureka| Rank
    Rank -->|REST via Eureka| Match
    Rank -->|REST via Eureka| Res
    Match -->|Publish Event| MQ
    MQ -->|Consume Event| Notif

    Auth --- DB
    Tour1 --- DB
    Tour2 --- DB
    Team --- DB
    Ath --- DB
    Match --- DB
    Res --- DB
    Rank --- DB
    Notif --- DB
```

---

## 2. Sơ đồ Quan hệ Thực thể Liên Dịch vụ (ERD — Database-per-Service)

```mermaid
erDiagram
    USERS {
        bigint id PK
        string username UK
        string password
        string full_name
        string email
        string role
        boolean active
    }

    SPORTS ||--o{ COMPETITIONS : "has"
    TOURNAMENTS ||--o{ COMPETITIONS : "contains"
    TOURNAMENTS {
        bigint id PK
        string name
        string description
        date start_date
        date end_date
        string location
        string status
        string organizer_username
    }
    COMPETITIONS {
        bigint id PK
        bigint tournament_id FK
        bigint sport_id FK
        string name
        string age_group
        int max_participants
    }

    TEAMS ||--o{ TEAM_MEMBERS : "rosters"
    ATHLETES ||..o{ TEAM_MEMBERS : "referenced_by_athleteId"
    TEAMS {
        bigint id PK
        string name
        string short_name
        string manager_name
        string contact_phone
        string contact_email
    }
    TEAM_MEMBERS {
        bigint id PK
        bigint team_id FK
        bigint athlete_id "Logical FK to Athlete Svc"
        string athlete_name
        int jersey_number
        string position
    }
    ATHLETES {
        bigint id PK
        string full_name
        date date_of_birth
        string identity_card_number
        string health_status
    }

    MATCHES ||..|| RESULTS : "referenced_by_matchId"
    RESULTS ||--o{ RESULT_DETAILS : "includes"
    MATCHES {
        bigint id PK
        bigint tournament_id "Logical FK to Tournament Svc"
        bigint competition_id
        string round
        string group_name
        datetime match_time
        string location
        string referee
        string participant1
        string participant2
        string status
    }
    RESULTS {
        bigint id PK
        bigint match_id "Logical FK to Match Svc"
        int score_participant1
        int score_participant2
        string status
        string confirmed_by
        string notes
    }

    RANKINGS ||--o{ RANKING_ITEMS : "orders"
    RANKINGS {
        bigint id PK
        bigint tournament_id
        bigint competition_id
        datetime last_updated
    }
    RANKING_ITEMS {
        bigint id PK
        bigint ranking_id FK
        string participant_name
        int rank_position
        int matches_played
        int won
        int drawn
        int lost
        int goals_for
        int goals_against
        int goal_difference
        int points
    }
```

---

## 3. Biểu đồ Tuần tự (Sequence Diagram) — Luồng Trọng tài Xác nhận Kết quả & Tự động Cập nhật Bảng Xếp Hạng

```mermaid
sequenceDiagram
    autonumber
    actor Referee as Trọng tài (Web UI / Postman)
    participant GW as API Gateway (:8080)
    participant Eureka as Eureka Server (:8761)
    participant Res as Result Service (:8087)
    participant Match as Match Service (:8082)
    participant MQ as RabbitMQ (:5672)
    participant Notif as Notification Service (:8084)
    participant Rank as Ranking Service (:8088)

    Referee->>GW: PUT /api/v1/results/{id}/confirm?confirmedBy=TT_FIFA
    GW->>Eureka: Resolve "result-service"
    GW->>Res: Forward PUT /api/v1/results/{id}/confirm
    Res->>Res: Lưu trạng thái CONFIRMED (saveAndFlush)
    Res->>Match: PATCH http://match-service/api/v1/matches/{matchId} (status=FINISHED)
    Match->>Match: Cập nhật trạng thái FINISHED
    Match--)MQ: Publish MatchFinishedEvent (Async)
    MQ--)Notif: Consume Event & Lưu Notification
    Match-->>Res: 200 OK (Match Updated)
    Res->>Rank: POST http://ranking-service/api/v1/rankings/competition/{compId}/recalculate
    Rank->>Match: GET http://match-service/api/v1/matches/competition/{compId}
    Rank->>Res: GET http://result-service/api/v1/results/match/{matchId}
    Rank->>Rank: Tính Điểm (3-1-0), Hiệu số (GD), Sắp xếp thứ hạng
    Rank-->>Res: 200 OK (Standings Updated)
    Res-->>GW: 200 OK (Result Confirmed & Standings Synced)
    GW-->>Referee: Hiển thị Toast Thành công & Cập nhật BXH trực tiếp
```

