# HỢP ĐỒNG GIAO TIẾP API LIÊN DỊCH VỤ (API CONTRACT - TH6)
**Hệ thống:** Quản lý thi đấu Thể thao (Sports Management Multi-Service System)  

---

## 1. Provider API: Tournament Service (Port 8081)

### API: Tra cứu thông tin Giải đấu theo ID
- **Endpoint:** `GET /api/v1/tournaments/{id}`
- **Consumer gọi:** `Match Service` (thông qua `TournamentClient`)
- **Headers:**
  - `Authorization`: `Bearer <jwt_token>` (Token Propagation)
  - `Accept`: `application/json`
- **Response thành công (`200 OK`):**
  ```json
  {
    "id": 1,
    "name": "Giải Bóng Đá Sinh Viên Toàn Quốc 2026",
    "description": "Giải bóng đá cúp đại học mùa xuân",
    "startDate": "2026-11-20",
    "endDate": "2026-12-05",
    "location": "Sân Vận Động Mỹ Đình",
    "status": "OPEN_REGISTRATION",
    "organizerUsername": "organizer1"
  }
  ```
- **Response lỗi (`404 Not Found`):**
  ```json
  {
    "status": 404,
    "message": "Tournament not found with id: 9999"
  }
  ```

---

## 2. Consumer API: Match Service (Port 8082)

### API: Tạo Lịch thi đấu Trận đấu (Có tích hợp kiểm tra Provider)
- **Endpoint:** `POST /api/v1/matches`
- **Caller:** Client (Web Admin / Postman)
- **Quyền yêu cầu:** `ROLE_ORGANIZER` hoặc `ROLE_ADMIN`
- **Headers:**
  - `Content-Type`: `application/json`
  - `Authorization`: `Bearer <jwt_token>`
- **Request Body:**
  ```json
  {
    "tournamentId": 1,
    "competitionId": 1,
    "round": "Vòng Bảng - Bảng A",
    "participant1Id": 10,
    "participant2Id": 11,
    "location": "Sân Vận Động Mỹ Đình",
    "scheduledTime": "2026-11-25T19:00:00",
    "status": "SCHEDULED"
  }
  ```
- **Response khi Provider xác nhận hợp lệ (`201 Created`):**
  ```json
  {
    "id": 15,
    "tournamentId": 1,
    "competitionId": 1,
    "round": "Vòng Bảng - Bảng A",
    "participant1Id": 10,
    "participant2Id": 11,
    "participant1": "CLB Bóng Đá Hà Nội",
    "participant2": "CLB Thể Công Viettel",
    "location": "Sân Vận Động Mỹ Đình",
    "status": "SCHEDULED"
  }
  ```
- **Response khi Provider không tìm thấy giải đấu (`400 Bad Request`):**
  ```json
  {
    "status": 400,
    "message": "Tournament does not exist with id: 9999"
  }
  ```
- **Response khi Provider sập (`503 Service Unavailable`):**
  ```json
  {
    "status": 503,
    "message": "Tournament Service is temporarily unavailable"
  }
  ```

---

## 3. Asynchronous Message Contract: RabbitMQ Broker

### Event: `MatchFinishedEvent`
- **Producer:** `Match Service`
- **Consumer:** `Notification Service`
- **Exchange:** `sports.exchange` (Topic Exchange)
- **Queue:** `sports.notification.queue`
- **Routing Key:** `match.finished`
- **Message Payload (JSON):**
  ```json
  {
    "event": "MATCH_FINISHED",
    "matchId": 15,
    "tournamentId": 1,
    "participant1": "CLB Bóng Đá Hà Nội",
    "participant2": "CLB Thể Công Viettel",
    "scoreParticipant1": 2,
    "scoreParticipant2": 1,
    "timestamp": "2026-11-02T16:50:00"
  }
  ```
