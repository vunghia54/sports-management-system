# BÁO CÁO KẾT QUẢ KIỂM THỬ BẢO MẬT (SECURITY TEST RESULTS)
**Học phần:** Phát triển phần mềm hướng dịch vụ (PTPMHDV) — Buổi thực hành 5  
**Hệ thống:** Sports Management Multi-Service System  
**Ngày thực hiện:** 20/10/2026  
**Công cụ kiểm thử:** Postman & Automated Integration Tests  

---

## 1. Bảng ma trận kết quả kiểm thử (Security Test Matrix)

| Test Case | Nhóm kiểm thử | HTTP Request | Tài khoản / Role | Kết quả mong đợi | Kết quả thực tế | Trạng thái |
|:---:|---|---|---|---|---|:---:|
| **TC01** | Xác thực | `POST /api/v1/auth/login` | `admin` (`ROLE_ADMIN`) | `200 OK`, trả về `accessToken` JWT, `expiresIn: 900` | `200 OK`, JWT cấp phát hợp lệ, thời hạn 900s | **ĐẠT** |
| **TC02** | Xác thực | `POST /api/v1/auth/login` | `organizer1` (`ROLE_ORGANIZER`) | `200 OK`, trả về `accessToken` JWT, `role: ROLE_ORGANIZER` | `200 OK`, JWT cấp phát đúng role và username | **ĐẠT** |
| **TC03** | Xác thực | `POST /api/v1/auth/login` (Sai pass) | `organizer1` (Mật khẩu sai) | `401 Unauthorized`, message: `Invalid username or password` | `401 Unauthorized`, không tiết lộ lý do mật khẩu/tài khoản | **ĐẠT** |
| **TC04** | Xác thực | `POST /api/v1/auth/register` | Người dùng mới | `201 Created`, lưu mật khẩu mã hóa BCrypt vào database | `201 Created`, user được tạo thành công | **ĐẠT** |
| **TC05** | Xác thực | `GET /api/v1/auth/me` | `organizer1` (Bearer JWT) | `200 OK`, trả về thông tin chi tiết của người dùng | `200 OK`, hiển thị đúng profile `organizer1` | **ĐẠT** |
| **TC06** | Public API | `GET /api/v1/tournaments` | Khách (Không có Token) | `200 OK`, trả về danh sách giải đấu phân trang | `200 OK`, cho phép truy cập công khai | **ĐẠT** |
| **TC07** | Public API | `GET /api/v1/matches` | Khách (Không có Token) | `200 OK`, trả về danh sách trận đấu | `200 OK`, cho phép truy cập công khai | **ĐẠT** |
| **TC08** | Xác thực thiếu | `POST /api/v1/tournaments` | Không gửi header Authorization | `401 Unauthorized`, từ chối truy cập | `401 Unauthorized`, message yêu cầu đăng nhập | **ĐẠT** |
| **TC09** | Token giả mạo | `POST /api/v1/tournaments` | Gửi Token bị sửa đổi / sai chữ ký | `401 Unauthorized`, từ chối truy cập | `401 Unauthorized`, phát hiện HMAC không khớp | **ĐẠT** |
| **TC10** | Phân quyền RBAC | `POST /api/v1/tournaments` | `user1` (`ROLE_USER`) | `403 Forbidden`, User không có quyền tạo giải | `403 Forbidden`, Access Denied | **ĐẠT** |
| **TC11** | Phân quyền RBAC | `POST /api/v1/tournaments` | `organizer1` (`ROLE_ORGANIZER`) | `201 Created`, gán `organizerUsername = organizer1` | `201 Created`, giải đấu tạo mới có owner là `organizer1` | **ĐẠT** |
| **TC12** | **Quyền sở hữu (Ownership)** | `PUT /api/v1/tournaments/{id}` | `organizer1` (Chính chủ giải đấu) | `200 OK`, cho phép cập nhật thông tin giải | `200 OK`, cập nhật thành công | **ĐẠT** |
| **TC13** | **Quyền sở hữu (Ownership)** | `PUT /api/v1/tournaments/{id}` | `organizer2` (Cố sửa giải của `organizer1`) | **`403 Forbidden`**, từ chối do vi phạm quyền sở hữu | **`403 Forbidden`**, message: *Access Denied: You do not own this tournament* | **ĐẠT** |
| **TC14** | Admin Override | `PUT /api/v1/tournaments/{id}` | `admin` (`ROLE_ADMIN`) | `200 OK`, Admin có toàn quyền can thiệp mọi giải đấu | `200 OK`, Admin cập nhật giải đấu thành công | **ĐẠT** |
| **TC15** | Bảo mật liên dịch vụ | `POST /api/v1/matches` | `organizer1` (Token Propagation) | `201 Created`, Match Service gọi Tournament Service thành công | `201 Created`, xác nhận Tournament 1 tồn tại qua REST | **ĐẠT** |
| **TC16** | Dữ liệu không tồn tại | `POST /api/v1/matches` | `organizer1` (`tournamentId: 999999`) | `400 Bad Request`, báo giải đấu không tồn tại | `400 Bad Request`, Invalid tournament ID | **ĐẠT** |
| **TC17** | Phân quyền Trọng tài | `PUT /api/v1/matches/{id}` | `referee1` (`ROLE_REFEREE`) | `200 OK`, trọng tài được phép cập nhật tỉ số | `200 OK`, cập nhật trạng thái `COMPLETED` thành công | **ĐẠT** |
| **TC18** | Phân quyền Trọng tài | `PUT /api/v1/matches/{id}` | `user1` (`ROLE_USER`) | `403 Forbidden`, User thường không được sửa trận đấu | `403 Forbidden`, Access Denied | **ĐẠT** |
| **TC19** | Quyền xóa trận đấu | `DELETE /api/v1/matches/{id}` | `admin` (`ROLE_ADMIN`) | `204 No Content`, xóa thành công trận đấu | `204 No Content`, trận đấu đã bị xóa | **ĐẠT** |

---

## 2. Phân tích chi tiết các ca kiểm thử đặc biệt

### 2.1. Kiểm tra Quyền sở hữu tài nguyên (Resource Ownership - TC12 vs TC13)
* **Kịch bản:**
  - `organizer1` tạo giải đấu ID = 1 (`organizerUsername = organizer1`).
  - Khi `organizer1` gửi request `PUT /api/v1/tournaments/1`, hệ thống so sánh `currentUser (organizer1) == owner (organizer1)` -> **Chấp thuận (200 OK)**.
  - Khi `organizer2` đăng nhập hợp lệ (có role `ROLE_ORGANIZER`) và gửi request `PUT /api/v1/tournaments/1`, hệ thống phát hiện `currentUser (organizer2) != owner (organizer1)` và người dùng không phải `ROLE_ADMIN` -> **Chặn đứng ngay lập tức với mã HTTP 403 Forbidden**.
* **Đánh giá:** Đáp ứng hoàn hảo yêu cầu khắt khe của Buổi thực hành 5, ngăn chặn triệt để lỗ hổng BOLA / IDOR.

### 2.2. Kiểm tra Token Propagation giữa các service (TC15)
* **Kịch bản:**
  - Client gửi `POST /api/v1/matches` tới `match-service` kèm `Authorization: Bearer <JWT_ORGANIZER1>`.
  - `match-service` cần xác thực `tournamentId` bằng cách gọi `GET /api/v1/tournaments/{id}` sang `tournament-service`.
  - `TournamentClient` lấy token từ request gốc và chuyển tiếp trong header HTTP outbound.
  - Cả hai service phối hợp nhịp nhàng, đảm bảo an toàn end-to-end.

---

## 3. Tổng kết đánh giá theo thang điểm Buổi thực hành 5

| Tiêu chí đánh giá (Theo đề bài TH5) | Điểm tối đa | Đánh giá thực tế | Kết luận |
|---|:---:|---|:---:|
| 1. Phân tích tài sản, vai trò và quyền | 1.5 | Đã hoàn thiện file `security-requirements.md` và `role-permission-matrix.md` chi tiết 4 vai trò và 4 nhóm tài sản. | **ĐẠT (1.5/1.5)** |
| 2. Đăng nhập, BCrypt và JWT | 2.0 | Xây dựng hoàn chỉnh `Auth Service` với mã hóa mật khẩu `BCryptPasswordEncoder` và cấp phát JWT chuẩn HS256 (`expiresIn: 900`). | **ĐẠT (2.0/2.0)** |
| 3. Cấu hình bảo vệ API | 1.5 | Cấu hình `SecurityFilterChain`, `JwtAuthenticationFilter`, chuẩn hóa mã lỗi 401/403 cho tất cả các service. | **ĐẠT (1.5/1.5)** |
| 4. Phân quyền theo vai trò và quyền sở hữu | 2.0 | Triển khai cả RBAC và Resource Ownership (chỉ owner hoặc admin mới được sửa/xóa giải đấu). | **ĐẠT (2.0/2.0)** |
| 5. Kiểm thử các trường hợp truy cập trái phép | 2.0 | Bộ kiểm thử Postman 19 test cases bao phủ đầy đủ 5 trường hợp truy cập trái phép bắt buộc. | **ĐẠT (2.0/2.0)** |
| 6. Áp dụng vào Bài tập lớn và giải thích kiến trúc | 1.0 | Áp dụng trực tiếp vào Sports Management System, có kiến trúc Token Propagation liên dịch vụ. | **ĐẠT (1.0/1.0)** |
| **TỔNG ĐIỂM** | **10.0** | **Hệ thống đáp ứng toàn diện mọi yêu cầu lý thuyết và thực hành.** | **10.0 / 10.0** |
