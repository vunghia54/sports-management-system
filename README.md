# HỆ THỐNG QUẢN LÝ THI ĐẤU THỂ THAO THEO KIẾN TRÚC HƯỚNG DỊCH VỤ (MICROSERVICES & SERVICE REGISTRY)

**Học phần:** Phát triển phần mềm hướng dịch vụ (PTPMHDV 2026)  
**Đề tài Bài tập lớn:** Xây dựng và phát triển Hệ thống Quản lý Thi đấu Thể thao theo Kiến trúc Hướng dịch vụ  
**Giai đoạn hiện tại:** Hoàn thành **Bài thực hành số 7 – Làm việc với Microservices và Service Registry (Netflix Eureka & Spring Cloud LoadBalancer)**  

---

## 1. Cấu trúc thư mục dự án (Chuẩn Bài thực hành số 7)

```text
sports-management-system-main/
├── service-registry/                 # [TH7] Netflix Eureka Server (Port 8761)
├── api-gateway/                      # [TH7 Mở rộng] API Gateway định tuyến động qua Eureka (Port 8080)
├── tournament-service/               # Provider trọng tâm: Quản lý Giải đấu (Chạy 2 instances: Port 8081 & 8091)
├── match-service/                    # Consumer trọng tâm: Quản lý Lịch thi đấu (Gọi http://tournament-service)
├── auth-service/                     # Dịch vụ Xác thực & Cấp phát JWT Token (Port 8083)
├── notification-service/             # Dịch vụ Thông báo bất đồng bộ qua RabbitMQ (Port 8084)
├── team-service/                     # Dịch vụ Quản lý Đội thi đấu (Port 8085)
├── athlete-service/                  # Dịch vụ Quản lý Vận động viên (Port 8086)
├── result-service/                   # Dịch vụ Ghi nhận & Xác nhận Kết quả trận đấu (Port 8087)
├── ranking-service/                  # Dịch vụ Quản lý Bảng xếp hạng (Port 8088)
├── docker-compose.yml                # Khởi chạy toàn bộ hệ thống + Eureka + Gateway + Multi-instance
├── docs/                             # Hồ sơ thiết kế kiến trúc Bài thực hành số 7
│   ├── architecture/
│   │   ├── architecture-before.png
│   │   ├── architecture-after.png
│   │   └── microservices-design.md   # Báo cáo 5 phần theo chuẩn đầu ra TH7
│   └── service-registry/
│       ├── registry-design.md        # Thiết kế Eureka Server, Heartbeat & Eviction
│       ├── service-list.md           # Danh bạ 9 dịch vụ đăng ký trên Eureka
│       └── discovery-flow.png
├── tests/                            # Bộ kiểm thử & Minh chứng Bài thực hành số 7
│   └── service-discovery/
│       ├── postman-collection.json   # Postman Collection kiểm thử TC01 -> TC06
│       ├── load-balancing-results.md # Thống kê phân phối 20 requests qua 2 instances (8081 & 8091)
│       └── failure-tests.md          # Kịch bản chịu lỗi khi dừng 1 instance / dừng toàn bộ / khởi động lại
├── integration/                      # Hồ sơ thiết kế Tích hợp dịch vụ (Bài thực hành số 6)
├── security-design/                  # Hồ sơ thiết kế Bảo mật & Phân quyền (Bài thực hành số 5)
└── test_th7_service_discovery.ps1    # Script tự động hóa kiểm thử toàn bộ TC01 -> TC06 của TH7
```

---

## 2. Hướng dẫn biên dịch và khởi chạy hệ thống

### Bước 1: Biên dịch các dịch vụ bằng Maven
```powershell
.\mvnw.cmd -f service-registry/pom.xml clean package -DskipTests
.\mvnw.cmd -f api-gateway/pom.xml clean package -DskipTests
.\mvnw.cmd -f tournament-service/pom.xml clean package -DskipTests
.\mvnw.cmd -f match-service/pom.xml clean package -DskipTests
.\mvnw.cmd -f auth-service/pom.xml clean package -DskipTests
.\mvnw.cmd -f notification-service/pom.xml clean package -DskipTests
.\mvnw.cmd -f team-service/pom.xml clean package -DskipTests
.\mvnw.cmd -f athlete-service/pom.xml clean package -DskipTests
.\mvnw.cmd -f result-service/pom.xml clean package -DskipTests
.\mvnw.cmd -f ranking-service/pom.xml clean package -DskipTests
```

### Bước 2: Chạy kiểm thử tự động toàn bộ kịch bản Bài thực hành số 7 (TC01 → TC06)
```powershell
powershell -ExecutionPolicy Bypass -File .\test_th7_service_discovery.ps1
```
*(Thêm tham số `-KeepRunning` nếu muốn giữ tất cả dịch vụ tiếp tục chạy sau khi test để mở trình duyệt xem Eureka Dashboard tại `http://localhost:8761` và kiểm thử bằng Postman).*

### Bước 3: Hoặc khởi chạy toàn bộ bằng Docker Compose
```powershell
$env:JWT_SECRET = "<chuoi-bi-mat-toi-thieu-32-ky-tu>"
$env:CORS_ALLOWED_ORIGINS = "http://localhost:8080"
docker compose up --build -d
```

> **Lưu ý bảo mật (B6):** Mọi microservice xác thực JWT (`auth`, `tournament`, `team`, `athlete`, `match`, `result`, `ranking`, `notification`) bắt buộc cấu hình biến môi trường `JWT_SECRET` (>= 32 bytes, không còn khóa mặc định trong mã nguồn). API Gateway cấu hình `CORS_ALLOWED_ORIGINS` (mặc định `http://localhost:8080`, cấm wildcard `*`).

---

## 3. Các địa chỉ truy cập quan trọng khi Demo

- **Eureka Server Dashboard:** `http://localhost:8761`
- **API Gateway Routes & Discovered Services:** `http://localhost:8080/api/v1/gateway/routes`
- **Tournament Service – Instance 1 Info:** `http://localhost:8081/api/v1/instance-info`
- **Tournament Service – Instance 2 Info:** `http://localhost:8091/api/v1/instance-info`
- **Match Service – Kiểm tra LoadBalancer & Service Discovery:** `http://localhost:8082/api/v1/matches/discovery/tournament-instance`

