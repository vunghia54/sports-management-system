# BÁO CÁO KẾT QUẢ KIỂM THỬ CÂN BẰNG TẢI NHIỀU INSTANCE (LOAD BALANCING RESULTS - TH7)

## 1. Cấu hình môi trường kiểm thử nhiều Instance

Để chứng minh khả năng khám phá dịch vụ (**Service Discovery**) kết hợp cân bằng tải phía Client (**Spring Cloud LoadBalancer**), nhóm triển khai **2 instance** của dịch vụ `TOURNAMENT-SERVICE` cùng đăng ký vào Eureka Server (`http://localhost:8761`) và cùng kết nối tới cơ sở dữ liệu `tournament_db`:

| Instance | Service ID trên Eureka | Instance ID | Địa chỉ & Cổng | Cơ sở dữ liệu | Trạng thái |
| :--- | :--- | :--- | :---: | :---: | :---: |
| **Tournament Service 1** | `TOURNAMENT-SERVICE` | `tournament-service:8081` | `localhost:8081` | `tournament_db` | `UP` |
| **Tournament Service 2** | `TOURNAMENT-SERVICE` | `tournament-service:8091` | `localhost:8091` | `tournament_db` | `UP` |

- **Consumer thực hiện lời gọi:** `MATCH-SERVICE` (`localhost:8082`)
- **Cấu hình lời gọi trong `Match Service`:** `services.tournament.url=http://tournament-service` (Không sử dụng địa chỉ IP/Port cố định).
- **Endpoint kiểm thử:**
  1. `GET http://localhost:8082/api/v1/matches/discovery/tournament-instance` (`Match Service` gọi `http://tournament-service/api/v1/instance-info`)
  2. `POST http://localhost:8082/api/v1/matches` (`Match Service` gọi `http://tournament-service/api/v1/tournaments/{id}`)

---

## 2. Bảng thống kê 20 Request liên tiếp từ Consumer (`Match Service`) sang `Tournament Service`

Khi cả 2 instance (`8081` và `8091`) đều đã đăng ký `UP` trên Eureka Server, gửi liên tiếp **20 request** từ `Match Service` và ghi nhận instance của `Tournament Service` xử lý từng request:

| Lần gửi (Request #) | Consumer gọi qua Service ID | Instance `TOURNAMENT-SERVICE` xử lý | Cổng (Port) | HTTP Status | Kết quả |
| :---: | :--- | :--- | :---: | :---: | :---: |
| **1** | `http://tournament-service/api/v1/instance-info` | `tournament-service:8081` (Instance 1) | `8081` | `200 OK` | Thành công |
| **2** | `http://tournament-service/api/v1/instance-info` | `tournament-service:8091` (Instance 2) | `8091` | `200 OK` | Thành công |
| **3** | `http://tournament-service/api/v1/instance-info` | `tournament-service:8081` (Instance 1) | `8081` | `200 OK` | Thành công |
| **4** | `http://tournament-service/api/v1/instance-info` | `tournament-service:8091` (Instance 2) | `8091` | `200 OK` | Thành công |
| **5** | `http://tournament-service/api/v1/instance-info` | `tournament-service:8081` (Instance 1) | `8081` | `200 OK` | Thành công |
| **6** | `http://tournament-service/api/v1/instance-info` | `tournament-service:8091` (Instance 2) | `8091` | `200 OK` | Thành công |
| **7** | `http://tournament-service/api/v1/instance-info` | `tournament-service:8081` (Instance 1) | `8081` | `200 OK` | Thành công |
| **8** | `http://tournament-service/api/v1/instance-info` | `tournament-service:8091` (Instance 2) | `8091` | `200 OK` | Thành công |
| **9** | `http://tournament-service/api/v1/instance-info` | `tournament-service:8081` (Instance 1) | `8081` | `200 OK` | Thành công |
| **10** | `http://tournament-service/api/v1/instance-info` | `tournament-service:8091` (Instance 2) | `8091` | `200 OK` | Thành công |
| **11** | `http://tournament-service/api/v1/instance-info` | `tournament-service:8081` (Instance 1) | `8081` | `200 OK` | Thành công |
| **12** | `http://tournament-service/api/v1/instance-info` | `tournament-service:8091` (Instance 2) | `8091` | `200 OK` | Thành công |
| **13** | `http://tournament-service/api/v1/instance-info` | `tournament-service:8081` (Instance 1) | `8081` | `200 OK` | Thành công |
| **14** | `http://tournament-service/api/v1/instance-info` | `tournament-service:8091` (Instance 2) | `8091` | `200 OK` | Thành công |
| **15** | `http://tournament-service/api/v1/instance-info` | `tournament-service:8081` (Instance 1) | `8081` | `200 OK` | Thành công |
| **16** | `http://tournament-service/api/v1/instance-info` | `tournament-service:8091` (Instance 2) | `8091` | `200 OK` | Thành công |
| **17** | `http://tournament-service/api/v1/instance-info` | `tournament-service:8081` (Instance 1) | `8081` | `200 OK` | Thành công |
| **18** | `http://tournament-service/api/v1/instance-info` | `tournament-service:8091` (Instance 2) | `8091` | `200 OK` | Thành công |
| **19** | `http://tournament-service/api/v1/instance-info` | `tournament-service:8081` (Instance 1) | `8081` | `200 OK` | Thành công |
| **20** | `http://tournament-service/api/v1/instance-info` | `tournament-service:8091` (Instance 2) | `8091` | `200 OK` | Thành công |

---

## 3. Tổng hợp và Giải thích cơ chế LoadBalancer

| Chỉ số thống kê | Giá trị đạt được | Tỷ lệ |
| :--- | :---: | :---: |
| **Tổng số Request gửi qua Consumer** | **20** | **100%** |
| **Số Request xử lý bởi Instance 1 (`tournament-service:8081`)** | **10** | **50%** |
| **Số Request xử lý bởi Instance 2 (`tournament-service:8091`)** | **10** | **50%** |
| **Số Request thất bại** | **0** | **0%** |

### Nhận xét kỹ thuật:
1. **Vai trò của Spring Cloud LoadBalancer:** Mặc định, `Spring Cloud LoadBalancer` sử dụng thuật toán **`RoundRobinLoadBalancer`** (xoay vòng tuần tự). Khi danh sách instance của `TOURNAMENT-SERVICE` lấy từ Eureka gồm 2 phần tử `[localhost:8081, localhost:8091]`, bộ đếm nguyên tử (`AtomicInteger position`) tăng lên sau mỗi lần gọi và chia lấy dư cho số lượng instance (`position % 2`), giúp phân bổ tải đều đặn 50% - 50% giữa hai instance.
2. **Tính nhất quán dữ liệu giữa các Instance:** Vì cả hai instance `8081` và `8091` đều kết nối tới cùng một cơ sở dữ liệu `tournament_db`, nên khi `Match Service` kiểm tra giải đấu `GET /api/v1/tournaments/{id}` ở bất kỳ instance nào cũng nhận được kết quả chính xác và đồng nhất.

