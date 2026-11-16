# BÁO CÁO KIỂM THỬ KHẢ NĂNG KHÁM PHÁ DỊCH VỤ, CHỊU LỖI VÀ PHỤC HỒI (FAILURE & DISCOVERY TESTS - TH7)

Tài liệu này ghi lại kết quả thực nghiệm 6 kịch bản kiểm thử bắt buộc và khuyến khích (`TC01` → `TC06`) theo đúng yêu cầu của **Bài thực hành số 7 – Làm việc với Microservices và Service Registry**.

---

## 1. Bảng tổng hợp kết quả kiểm thử (TC01 – TC06)

| Mã TC | Tình huống kiểm thử | Thao tác thực hiện | Kết quả mong đợi | Kết quả thực tế | Trạng thái |
| :---: | :--- | :--- | :--- | :--- | :---: |
| **TC01** | **Khởi động Registry và các dịch vụ** | Khởi chạy `service-registry` (`:8761`), `api-gateway` (`:8080`) và 8 Microservices (trong đó `tournament-service` chạy 2 instance `:8081` và `:8091`). | Tất cả dịch vụ xuất hiện trên Eureka Dashboard với trạng thái `UP`. | Eureka trả về đầy đủ danh sách dịch vụ `UP`, trong đó `TOURNAMENT-SERVICE` có 2 instance (`tournament-service:8081` và `tournament-service:8091`). | **PASSED** |
| **TC02** | **Consumer gọi Provider theo Service ID** | Gửi request `POST /api/v1/matches` tới `Match Service` (`:8082`). `Match Service` gọi `http://tournament-service/api/v1/tournaments/{id}`. | Không dùng IP/port cố định, kiểm tra giải đấu thành công và tạo trận đấu `201 Created`. | `Match Service` phân giải thành công `http://tournament-service`, trả về `201 Created` kèm header `X-Tournament-Provider-Instance`. | **PASSED** |
| **TC03** | **Một Provider có hai instance (Load Balancing)** | Gửi 20 request liên tiếp tới `Match Service` (`GET /api/v1/matches/discovery/tournament-instance` và `POST /api/v1/matches`). | Request được phân phối luân phiên tới cả 2 instance `8081` và `8091`. | Instance 1 (`8081`) xử lý 10 request, Instance 2 (`8091`) xử lý 10 request theo cơ chế Round-Robin. | **PASSED** |
| **TC04** | **Dừng một instance của Provider (Failover)** | Dừng đột ngột `Tournament Service Instance 1` (`:8081`), giữ `Instance 2` (`:8091`) hoạt động. Tiếp tục gửi request tới `Match Service`. | `Match Service` tự động chuyển toàn bộ request sang `Instance 2` (`:8091`), nghiệp vụ vẫn hoạt động bình thường. | Sau khi Eureka & LoadBalancer cập nhật danh bạ, 100% request mới được định tuyến tới `tournament-service:8091` và trả về `200 OK` / `201 Created`. | **PASSED** |
| **TC05** | **Dừng toàn bộ instance của Provider** | Dừng tiếp `Tournament Service Instance 2` (`:8091`) (Không còn instance nào của `TOURNAMENT-SERVICE` hoạt động). Gọi `Match Service`. | Consumer nhận lỗi rõ ràng (`503 Service Unavailable`), không bị treo vô hạn và không lộ stack trace. | `Match Service` trả về ngay HTTP `503 Service Unavailable` với JSON chuẩn: `{"status": 503, "message": "Tournament Service (TOURNAMENT-SERVICE) is temporarily unavailable via Service Discovery"}`. | **PASSED** |
| **TC06** | **Khởi động lại Provider (Self-Healing & Re-registration)** | Khởi động lại `Tournament Service` (`:8081`). Đợi dịch vụ tự đăng ký lại vào Eureka rồi gửi lại request từ `Match Service`. | Dịch vụ tự động đăng ký lại vào Eureka (`UP`) và `Match Service` khám phá, gọi lại thành công mà không cần khởi động lại Consumer. | `tournament-service:8081` xuất hiện lại trên Eureka Dashboard; `Match Service` gọi lại thành công (`200 OK` / `201 Created`). | **PASSED** |

---

## 2. Chi tiết Log và Minh chứng cho từng kịch bản lỗi

### 2.1. Minh chứng TC03 – Phân phối tải giữa 2 Instance (`8081` và `8091`)
Response khi gọi `GET http://localhost:8082/api/v1/matches/discovery/tournament-instance`:

**Lần gọi lẻ (Request 1, 3, 5...):**
```json
{
  "consumerService": "MATCH-SERVICE",
  "discoveryTarget": "http://tournament-service",
  "providerInstance": {
    "serviceName": "TOURNAMENT-SERVICE",
    "instanceId": "tournament-service:8081",
    "instancePort": 8081,
    "port": 8081,
    "status": "UP"
  }
}
```

**Lần gọi chẵn (Request 2, 4, 6...):**
```json
{
  "consumerService": "MATCH-SERVICE",
  "discoveryTarget": "http://tournament-service",
  "providerInstance": {
    "serviceName": "TOURNAMENT-SERVICE",
    "instanceId": "tournament-service:8091",
    "instancePort": 8091,
    "port": 8091,
    "status": "UP"
  }
}
```

### 2.2. Minh chứng TC04 – Dừng Instance 1 (`:8081`), Instance 2 (`:8091`) tiếp tục phục vụ
- Thao tác: Dừng tiến trình `tournament-service` tại cổng `8081`.
- Quan sát Eureka Server: `tournament-service:8081` bị loại khỏi danh sách, chỉ còn `tournament-service:8091` ở trạng thái `UP`.
- Gửi 5 request liên tiếp tới `Match Service`: Cả 5 request đều được xử lý thành công bởi `tournament-service:8091` (`200 OK`), chứng minh hệ thống không bị gián đoạn khi một instance gặp sự cố.

### 2.3. Minh chứng TC05 – Dừng toàn bộ cả 2 Instance (`:8081` và `:8091`)
- Thao tác: Dừng nốt tiến trình `tournament-service` tại cổng `8091`.
- Gửi request tạo trận đấu `POST http://localhost:8082/api/v1/matches`:
- Kết quả trả về từ `Match Service` (HTTP `503 Service Unavailable`):
```json
{
  "timestamp": "2026-11-09T13:05:00",
  "status": 503,
  "error": "Service Unavailable",
  "message": "Tournament Service (TOURNAMENT-SERVICE) is temporarily unavailable via Service Discovery",
  "path": "/api/v1/matches"
}
```

### 2.4. Minh chứng TC06 – Khởi động lại Provider và Tự động phục hồi
- Thao tác: Khởi chạy lại `tournament-service` tại cổng `8081` (Không cần khởi động lại `Match Service`).
- Sau vài giây, `tournament-service:8081` gửi heartbeat đăng ký lại với Eureka Server.
- Gửi lại request tạo trận đấu `POST http://localhost:8082/api/v1/matches`:
- Kết quả: Trả về `201 Created` thành công ngay lập tức.

