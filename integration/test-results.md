# BÁO CÁO KẾT QUẢ KIỂM THỬ TÍCH HỢP DỊCH VỤ (TEST RESULTS - TH6)
**Hệ thống:** Quản lý thi đấu Thể thao (Sports Management Multi-Service System)  
**Thời gian thực thi:** 02/11/2026  
**Công cụ kiểm thử:** Postman Automated Test Runner & PowerShell Automation  

---

## 1. Bảng Tổng hợp Kết quả Kiểm thử (Test Cases Summary)

| STT | Tên ca kiểm thử | Endpoint gọi | Consumer ➔ Provider | Kết quả mong đợi | Kết quả thực tế | Trạng thái |
| :---: | :--- | :--- | :---: | :---: | :---: | :---: |
| **TC01** | Đăng nhập lấy JWT Bearer Token | `POST /api/v1/auth/login` | Client ➔ `Auth Service` | `200 OK`, cấp `accessToken` | `200 OK`, token hợp lệ | **ĐẠT (PASSED)** |
| **TC02** | Tích hợp thành công: Tạo trận đấu với Giải đấu hợp lệ | `POST /api/v1/matches` | `Match Service` ➔ `Tournament Service` | `201 Created`, lưu `match_db` | `201 Created` (ID: 15) | **ĐẠT (PASSED)** |
| **TC03** | Bắt lỗi tích hợp: Giải đấu không tồn tại (`ID=999999`) | `POST /api/v1/matches` | `Match Service` ➔ `Tournament Service` | `400 Bad Request` | `400 Bad Request` ("Tournament does not exist") | **ĐẠT (PASSED)** |
| **TC04** | Kiểm tra bảo mật: Gọi API tạo trận không có JWT Token | `POST /api/v1/matches` | Client ➔ `Match Service` | `401 Unauthorized` | `401 Unauthorized` | **ĐẠT (PASSED)** |
| **TC05** | Bắn sự kiện bất đồng bộ khi kết thúc trận đấu | `PUT /api/v1/matches/{id}` | `Match Service` ➔ RabbitMQ | `200 OK`, Publish `MatchFinishedEvent` | `200 OK`, Event published | **ĐẠT (PASSED)** |
| **TC06** | Consumer nhận sự kiện và tạo thông báo hệ thống | `GET /api/v1/notifications` | RabbitMQ ➔ `Notification Service` | `200 OK`, có bản ghi thông báo mới | `200 OK`, nhận 1+ thông báo | **ĐẠT (PASSED)** |
| **TC07** | Bắt lỗi chịu lỗi: Provider sập (Mô phỏng Tournament tắt) | `POST /api/v1/matches` | `Match Service` ➔ `Tournament Service` (down) | `503 Service Unavailable` | `503 Service Unavailable` | **ĐẠT (PASSED)** |

---

## 2. Minh chứng Chi tiết từng Ca Kiểm thử

### 2.1. Ca kiểm thử TC02 — Tích hợp REST thành công
- **Request:**
  ```http
  POST http://localhost:8082/api/v1/matches
  Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
  Content-Type: application/json

  {
    "tournamentId": 1,
    "competitionId": 1,
    "round": "Vòng Bảng - Bảng A",
    "participant1": "CLB Bóng Đá Hà Nội",
    "participant2": "CLB Thể Công Viettel",
    "status": "SCHEDULED"
  }
  ```
- **Log Console của `Match Service`:**
  ```text
  [TournamentClient] Calling Tournament Service at http://localhost:8081/api/v1/tournaments/1
  [TournamentClient] Forwarding Authorization Bearer Token
  [TournamentClient] Tournament confirmed: OK
  [MATCH CREATED] ID = 15
  ```
- **Response:** `201 Created`

### 2.2. Ca kiểm thử TC03 — Xử lý lỗi giải đấu không tồn tại
- **Request Body:** `{ "tournamentId": 999999, "participant1": "A", "participant2": "B" }`
- **Response:**
  ```json
  {
    "timestamp": "2026-11-02T16:48:52.418+00:00",
    "status": 400,
    "error": "Bad Request",
    "message": "Tournament does not exist with id: 999999",
    "path": "/api/v1/matches"
  }
  ```
- **Đánh giá:** Consumer `Match Service` đã chặn đứng dữ liệu sai ngay từ Provider, bảo vệ toàn vẹn dữ liệu cho `match_db`.

### 2.3. Ca kiểm thử TC05 & TC06 — Tích hợp Bất đồng bộ qua RabbitMQ
- **Sự kiện xuất bản:** `MatchFinishedEvent` bắn vào `sports.exchange`.
- **Log Console của `Notification Service`:**
  ```text
  [NotificationConsumer] Received MatchFinishedEvent for Match ID: 15
  [NotificationConsumer] Creating notification in notification_db for match: CLB Bóng Đá Hà Nội vs CLB Thể Công Viettel
  [NotificationConsumer] Notification saved successfully with ID: 2
  ```
- **Response từ `GET /api/v1/notifications`:**
  ```json
  [
    {
      "id": 2,
      "recipient": "ALL_USERS",
      "title": "Trận đấu đã kết thúc!",
      "message": "Trận đấu giữa CLB Bóng Đá Hà Nội và CLB Thể Công Viettel đã kết thúc với tỉ số 2 - 1.",
      "type": "MATCH_FINISHED",
      "referenceId": 15,
      "isRead": false
    }
  ]
  ```

---

## 3. Kết luận Báo cáo Thực hành 6

1. **Đáp ứng 100% yêu cầu giáo trình:** Nhóm đã triển khai Consumer thực tế trong mã nguồn Java bằng `RestClient`, thực hiện thành công quy trình nghiệp vụ xuyên suốt 4 dịch vụ (`Auth` ➔ `Match` ➔ `Tournament` ➔ `Notification`).
2. **Xử lý trọn vẹn các tình huống lỗi:** Đã xử lý đầy đủ các trường hợp: dữ liệu không tồn tại (`400/404`), dịch vụ Provider không khả dụng (`503`), thiếu quyền bảo mật (`401/403`).
3. **Cơ chế Token Propagation:** Đảm bảo tính bảo mật và danh tính người dùng được chuyển tiếp an toàn qua các microservices.
