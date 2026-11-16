# BÁO CÁO THIẾT KẾ TÍCH HỢP VÀ TIÊU THỤ DỊCH VỤ (THỰC HÀNH SỐ 6)
**Học phần:** Phát triển phần mềm hướng dịch vụ (PTPMHDV 2026)  
**Đề tài:** Hệ thống Quản lý thi đấu Thể thao (Sports Management Multi-Service System)  
**Nhóm sinh viên thực hiện:** Nhóm Bài tập lớn  

---

## 1. Phân tích nhu cầu tích hợp trong Hệ thống Thể thao

Trong một hệ thống hướng dịch vụ thực tế, một dịch vụ không thể hoạt động hoàn toàn biệt lập mà phải tích hợp và phối hợp với các dịch vụ khác để hoàn thành các quy trình nghiệp vụ xuyên suốt:

1. **Nghiệp vụ Lập lịch thi đấu (Match Scheduling):**
   - Khi Ban tổ chức tạo một trận đấu mới tại `Match Service`, hệ thống **bắt buộc** phải kiểm tra xem Giải đấu (`Tournament Service`) và các Đội thi đấu (`Team Service`) có thực sự tồn tại, hợp lệ và đang mở đăng ký hay không.
   - `Match Service` không được phép truy cập trực tiếp vào cơ sở dữ liệu `tournament_db` hay `team_db` (vi phạm nguyên tắc Database-per-service). Thay vào đó, nó phải **đóng vai trò Consumer gọi REST API sang các Provider**.
2. **Nghiệp vụ Bảo mật liên dịch vụ (Token Propagation):**
   - Lời gọi từ `Match Service` sang `Tournament Service` cần chuyển tiếp nguyên trạng mã Bearer JWT Token của người dùng đang đăng nhập để đảm bảo tính xác thực và phân quyền nhất quán xuyên suốt chuỗi lời gọi.
3. **Nghiệp vụ Bắn sự kiện bất đồng bộ khi trận đấu kết thúc (Event-Driven Notification):**
   - Khi trận đấu kết thúc (`status = FINISHED`), `Match Service` xuất bản sự kiện `MatchFinishedEvent` lên RabbitMQ Broker.
   - `Notification Service` lắng nghe và tự động tạo thông báo kết quả cho khán giả và ban huấn luyện mà không làm nghẽn tiến trình của `Match Service`.

---

## 2. Bảng phân định vai trò Consumer và Provider

| Dịch vụ | Vai trò trong luồng tích hợp | Trách nhiệm chính | Giao thức |
| :--- | :---: | :--- | :---: |
| **`Match Service` (Port 8082)** | **Consumer chính** (đồng bộ) <br/> **Producer** (bất đồng bộ) | - Gọi API `Tournament Service` để xác thực giải đấu.<br/>- Đính kèm Token Authorization (`Token Propagation`).<br/>- Phát sự kiện `MatchFinishedEvent` khi kết thúc trận. | HTTP REST / RabbitMQ |
| **`Tournament Service` (Port 8081)** | **Provider trọng tâm** | - Cung cấp API `GET /api/v1/tournaments/{id}`.<br/>- Xác thực token và trả về thông tin giải đấu hoặc mã lỗi `404 Not Found`. | HTTP REST |
| **`Auth Service` (Port 8083)** | **Security Provider** | - Cấp phát JWT Bearer Token cho Ban tổ chức (`ROLE_ORGANIZER`). | HTTP REST |
| **`Notification Service` (Port 8084)** | **Async Consumer** | - Lắng nghe queue `sports.notification.queue` từ RabbitMQ.<br/>- Lưu thông báo vào `notification_db`. | AMQP (RabbitMQ) |

---

## 3. Quy trình nghiệp vụ tích hợp xuyên suốt

```text
[ Client (Ban tổ chức) ]
        │
   1. Đăng nhập lấy JWT Token (Role: ROLE_ORGANIZER)
        ▼
[ Auth Service (Port 8083) ]
        │
   2. Gửi yêu cầu tạo trận đấu: POST /api/v1/matches (kèm Bearer Token)
        ▼
[ Match Service - Consumer (Port 8082) ]
        │
   3. Tích hợp REST: GET /api/v1/tournaments/{id} (chuyển tiếp Bearer Token)
        ▼
[ Tournament Service - Provider (Port 8081) ]
        │
        ├── Nếu Tournament không tồn tại (404) ──► Match Service trả về 400 Bad Request
        └── Nếu Tournament hợp lệ (200 OK)    ──► Match Service lưu trận đấu vào match_db (201 Created)
```

---

## 4. Xử lý các tình huống lỗi tích hợp & Tính chịu lỗi

Hệ thống đã triển khai đầy đủ các cơ chế xử lý lỗi trong mã nguồn [`TournamentClient.java`](file:///d:/PTPMHDV/sports-management-system-main/match-service/src/main/java/com/sports/management/match/client/TournamentClient.java):

1. **Giải đấu không tồn tại (Resource Not Found):**
   - Khi `TournamentClient` nhận status code `404` từ `Tournament Service`, client bắt ngoại lệ nội bộ và trả về `false`.
   - `MatchService` chặn không cho tạo trận đấu và trả về phản hồi chuẩn: `400 Bad Request: "Tournament does not exist with id: {id}"`.
2. **Dịch vụ Provider không hoạt động (Provider Unavailable / Connection Refused):**
   - Khi `Tournament Service` bị sập hoặc tắt máy, `RestClient` ném ngoại lệ `RestClientException`.
   - `TournamentClient` bắt ngoại lệ và trả về mã lỗi HTTP chuẩn: `503 Service Unavailable: "Tournament Service is temporarily unavailable"`. Không làm sập `Match Service` và không để lộ stack trace cho client.
3. **Lỗi xác thực (Authentication Required):**
   - Nếu client không truyền JWT Token hoặc token hết hạn, `JwtAuthenticationFilter` chặn ngay tại cửa ngõ và trả về `401 Unauthorized`.
4. **Token Propagation (Chuyển tiếp danh tính người dùng):**
   - Lớp `TournamentClient` đọc `Authorization` header từ context request hiện tại và tự động gán vào request gọi sang `Tournament Service`:
   ```java
   String authHeader = getCurrentAuthHeader();
   if (authHeader != null && !authHeader.isBlank()) {
       requestSpec.header("Authorization", authHeader);
   }
   ```
