# 03. TÀI LIỆU API, HƯỚNG DẪN CHẠY, KỊCH BẢN DEMO & KẾT QUẢ KIỂM THỬ

---

## 1. Bảng Tổng hợp Cổng và Swagger/OpenAPI của 10 Microservices

| STT | Tên Microservice | Cổng (Port) | Đường dẫn Swagger UI | Đường dẫn chính qua API Gateway (`:8080`) |
|---|---|---|---|---|
| 1 | **Service Registry (Eureka)** | `8761` | http://localhost:8761 | `/eureka/apps` |
| 2 | **API Gateway & Web UI** | `8080` | http://localhost:8080/index.html | `/api/v1/gateway/routes` |
| 3 | **Auth Service** | `8083` | http://localhost:8083/swagger-ui.html | `/api/v1/auth/**` |
| 4 | **Tournament Service (Inst 1 & 2)** | `8081`, `8091` | http://localhost:8081/swagger-ui.html | `/api/v1/tournaments/**`, `/api/v1/sports/**` |
| 5 | **Team Service** | `8085` | http://localhost:8085/swagger-ui.html | `/api/v1/teams/**` |
| 6 | **Athlete Service** | `8086` | http://localhost:8086/swagger-ui.html | `/api/v1/athletes/**` |
| 7 | **Match Service** | `8082` | http://localhost:8082/swagger-ui.html | `/api/v1/matches/**`, `/api/v1/registrations/**` |
| 8 | **Result Service** | `8087` | http://localhost:8087/swagger-ui.html | `/api/v1/results/**` |
| 9 | **Ranking Service** | `8088` | http://localhost:8088/swagger-ui.html | `/api/v1/rankings/**` |
| 10 | **Notification Service** | `8084` | http://localhost:8084/swagger-ui.html | `/api/v1/notifications/**` |

---

## 2. Danh sách Tài khoản Mặc định theo 5 Vai trò (RBAC)

| Vai trò | Username | Password | Quyền hạn |
|---|---|---|---|
| **Admin** | `admin` | `admin123` | Toàn quyền hệ thống, đổi quyền người dùng |
| **Organizer** | `organizer1` | `organizer123` | Tạo/sửa giải đấu, duyệt đăng ký, xếp lịch tự động |
| **Referee** | `referee1` | `referee123` | Nhập tỷ số, xác nhận kết quả chính thức |
| **Team Manager** | `manager1` | `manager123` | Quản lý CLB, thêm VĐV, đăng ký tham gia giải |
| **Viewer** | `user1` | `user123` | Xem giải đấu, lịch thi đấu, kết quả, bảng xếp hạng |

---

## 3. Kịch bản Demo Bảo vệ Đồ án (5 Phút Ấn Tượng)

1. **Bước 1 — Giới thiệu Kiến trúc SOA & Eureka Registry (1 phút):**
   - Mở http://localhost:8761 trình bày 10 Microservices (11 instances, trong đó `TOURNAMENT-SERVICE` có 2 instances `8081` và `8091`).
2. **Bước 2 — Demo Giao diện Web Tiếng Việt & Dual Theme System (1 phút):**
   - Mở http://localhost:8080/index.html.
   - Bấm nút chuyển đổi **Sáng (`Light`) $\leftrightarrow$ Tối (`Dark`) $\leftrightarrow$ Hệ thống (`System`)** trên thanh Header. Nhấn `F5` tải lại trang để chứng minh theme được ghi nhớ trong `localStorage` và **không bị chớp sáng (No-FOUC)**.
3. **Bước 3 — Demo Phân quyền 5 Vai trò & Luồng Nghiệp vụ Bóng đá (2 phút):**
   - Vào trang **2. Đăng nhập & Đăng ký**, bấm nút chuyển nhanh vai trò **Ban Tổ Chức (`organizer1`)**.
   - Vào trang **8. Lịch thi đấu**, bấm **🔍 Kiểm tra Trùng lịch** (chứng minh hệ thống phát hiện trùng sân/trọng tài/đội bóng) và bấm **⚡ Tự động Xếp lịch theo Thể thức**.
   - Vào trang **10. Quản lý kết quả**, bấm **✓ Trọng tài Xác nhận** $\rightarrow$ Hệ thống tự động gọi liên dịch vụ từ `result-service` sang `match-service` và `ranking-service`.
   - Mở trang **11. Bảng xếp hạng** & **12. Thống kê & Báo cáo** để xem thứ hạng tự động cập nhật và bấm xuất file `.CSV` / `.JSON`.
4. **Bước 4 — Demo Cân bằng tải (Load Balancing) trực tiếp trên giao diện (1 phút):**
   - Vào trang **14. Cài đặt & SOA Monitor**, bấm nút **▶ Chạy Kiểm thử 6 Requests Load Balancing** để hội đồng thấy trực tiếp 3 requests vào cổng `8081` và 3 requests vào cổng `8091`.

---

## 4. Kết quả Kiểm thử Thực tế (QA Verification Summary)

| Hạng mục Kiểm thử | Kịch bản | Kết quả |
|---|---|---|
| **Kiến trúc SOA & Eureka** | 10 Microservices (11 instances) đăng ký `UP` trên `:8761` | **PASS (100%)** |
| **Load Balancing & Failover (TH7)** | Chia tải Round-Robin `8081`/`8091`, tắt `8081` tự chuyển sang `8091` | **PASS (100%)** |
| **Xác thực JWT & Phân quyền 5 Vai trò** | Đăng nhập, đổi mật khẩu, đổi vai trò RBAC, chặn truy cập trái phép | **PASS (100%)** |
| **Tự động xếp lịch & Kiểm tra trùng lịch** | Phát hiện trùng sân/trọng tài/đội bóng trong khung $\pm 2$ giờ | **PASS (100%)** |
| **Xác nhận kết quả & Tự động tính BXH** | `Result` $\rightarrow$ `Match (FINISHED)` $\rightarrow$ `Ranking (3-1-0, GD, GF)` | **PASS (100%)** |
| **Giao diện Dual-Theme (Light/Dark/System)** | Đúng mã màu thiết kế, lưu `localStorage`, không FOUC, Responsive | **PASS (100%)** |

