# DANH SÁCH DỊCH VỤ ĐĂNG KÝ TRONG EUREKA SERVICE REGISTRY (THỰC HÀNH 7)

Dưới đây là danh sách toàn bộ các dịch vụ trong **Hệ thống Quản lý Thi đấu Thể thao** đăng ký vào Eureka Server (`http://localhost:8761`):

| STT | Service ID trên Eureka | `spring.application.name` | Instance ID | Cổng (Port) | Số lượng Instance | Vai trò trong kiến trúc |
| :-: | :--- | :--- | :--- | :-: | :-: | :--- |
| 1 | **`API-GATEWAY`** | `api-gateway` | `api-gateway:8080` | `8080` | 1 | Cổng giao tiếp tập trung (Single Entry Point), định tuyến qua `lb://` |
| 2 | **`TOURNAMENT-SERVICE`** | `tournament-service` | `tournament-service:8081`<br/>`tournament-service:8091` | `8081`<br/>`8091` | **2 (Multi-Instance)** | **Provider trọng tâm:** Quản lý giải đấu, minh họa cân bằng tải (Load Balancing) và chịu lỗi (Failover) |
| 3 | **`MATCH-SERVICE`** | `match-service` | `match-service:8082` | `8082` | 1 | **Consumer trọng tâm:** Quản lý lịch thi đấu, gọi `TOURNAMENT-SERVICE` qua Service Discovery |
| 4 | **`AUTH-SERVICE`** | `auth-service` | `auth-service:8083` | `8083` | 1 | Dịch vụ xác thực & cấp phát JWT Token |
| 5 | **`NOTIFICATION-SERVICE`** | `notification-service` | `notification-service:8084` | `8084` | 1 | Dịch vụ thông báo bất đồng bộ qua RabbitMQ |
| 6 | **`TEAM-SERVICE`** | `team-service` | `team-service:8085` | `8085` | 1 | Dịch vụ quản lý đội thi đấu |
| 7 | **`ATHLETE-SERVICE`** | `athlete-service` | `athlete-service:8086` | `8086` | 1 | Dịch vụ quản lý hồ sơ vận động viên |
| 8 | **`RESULT-SERVICE`** | `result-service` | `result-service:8087` | `8087` | 1 | Dịch vụ ghi nhận và xác nhận kết quả trận đấu |
| 9 | **`RANKING-SERVICE`** | `ranking-service` | `ranking-service:8088` | `8088` | 1 | Dịch vụ quản lý bảng xếp hạng |

---

## Endpoint kiểm tra thông tin Instance phục vụ Load Balancing

Để kiểm chứng rõ ràng instance nào đang xử lý request khi chạy nhiều instance, hệ thống cung cấp các API:

1. **Gọi trực tiếp trên từng instance `TOURNAMENT-SERVICE`:**
   - `GET http://localhost:8081/api/v1/instance-info` -> trả về `"instanceId": "tournament-service:8081"`, `"instancePort": 8081`
   - `GET http://localhost:8091/api/v1/instance-info` -> trả về `"instanceId": "tournament-service:8091"`, `"instancePort": 8091`
2. **Gọi thông qua Consumer `MATCH-SERVICE` (sử dụng Service Discovery + LoadBalancer):**
   - `GET http://localhost:8082/api/v1/matches/discovery/tournament-instance`
   - Mỗi lần gọi, `Match Service` sẽ tra cứu `http://tournament-service/api/v1/instance-info` qua Eureka & LoadBalancer và trả về instance thực tế vừa xử lý yêu cầu (`8081` hoặc `8091` luân phiên).
3. **Gọi thông qua `API-GATEWAY` (Port 8080):**
   - `GET http://localhost:8080/api/v1/tournaments/instance-info`
   - `GET http://localhost:8080/api/v1/gateway/routes`

