# YÊU CẦU BẢO MẬT HỆ THỐNG QUẢN LÝ THỂ THAO (SPORTS MANAGEMENT SYSTEM)
**Học phần:** Phát triển phần mềm hướng dịch vụ (PTPMHDV) — Buổi thực hành 5  
**Hệ thống:** Sports Management Multi-Service System  

---

## 1. Giới thiệu tổng quan
Hệ thống Quản lý Thể thao (Sports Management System) được thiết kế theo kiến trúc Microservices phân tán, bao gồm các dịch vụ cốt lõi:
- **Auth Service (Port 8083):** Quản lý tài khoản, danh tính, xác thực và cấp phát JWT Token.
- **Tournament Service (Port 8081):** Quản lý thông tin giải đấu thể thao, thời gian đăng ký và điều lệ.
- **Match Service (Port 8082):** Quản lý lịch thi đấu, phân công trọng tài, cập nhật tỉ số và diễn biến trận đấu.

Trong môi trường phân tán, các ranh giới tin cậy (trust boundaries) bị xóa nhòa, đòi hỏi hệ thống phải triển khai cơ chế xác thực tập trung và phân quyền đa tầng (RBAC kết hợp Resource Ownership) để đảm bảo an toàn dữ liệu và tính toàn vẹn của kết quả thi đấu.

---

## 2. Nhận diện tài sản cần bảo vệ (Protected Assets)

Hệ thống xác định 4 nhóm tài sản nghiệp vụ quan trọng cần được bảo vệ nghiêm ngặt:

| STT | Tài sản cần bảo vệ | Dịch vụ quản lý | Mức độ nhạy cảm | Rủi ro khi bị tấn công |
|---|---|---|---|---|
| 1 | **Dữ liệu Giải đấu (Tournament Data)** | `tournament-service` | Cao | Bị đối thủ hoặc người dùng lạ sửa đổi thời gian, địa điểm, điều lệ thi đấu, hoặc xóa bỏ giải đấu đang diễn ra. |
| 2 | **Lịch & Kết quả Trận đấu (Match & Results)** | `match-service` | Tối mật / Toàn vẹn cao | Bị gian lận tỉ số, thay đổi cặp đấu, thay đổi kết quả thi đấu dẫn đến sai lệch bảng xếp hạng. |
| 3 | **Thông tin Tài khoản & Danh tính (User Credentials)** | `auth-service` | Tối mật | Bị lộ mật khẩu người dùng, bị leo thang đặc quyền (Privilege Escalation), mạo danh ban tổ chức hoặc trọng tài. |
| 4 | **Giao tiếp liên dịch vụ (Service-to-Service Calls)** | `match-service` ↔ `tournament-service` | Cao | Bị giả mạo request nội bộ (Internal Spoofing), bypass qua các cơ chế kiểm tra tính hợp lệ của giải đấu. |

---

## 3. Phân tích mối đe dọa an toàn thông tin (Threat Modeling)

1. **Broken Object Level Authorization (BOLA / IDOR):**
   * *Nguy cơ:* Một ban tổ chức (Organizer A) đăng nhập hợp lệ nhưng lại gửi request thay đổi giải đấu do Organizer B tạo ra thông qua việc thay đổi `{id}` trên URL.
   * *Biện pháp:* Bắt buộc kiểm tra **Resource Ownership** tại tầng nghiệp vụ trước khi cho phép cập nhật (`PUT`), cập nhật từng phần (`PATCH`), hoặc xóa (`DELETE`).

2. **Broken Authentication & Session Management:**
   * *Nguy cơ:* Kẻ tấn công thực hiện brute-force mật khẩu hoặc giải mã mật khẩu lưu dưới dạng plain-text.
   * *Biện pháp:* Băm mật khẩu một chiều bằng thuật toán `BCrypt` có độ phức tạp cao; cấp phát `JSON Web Token (JWT)` có thời hạn ngắn (15 phút / 900 giây).

3. **Privilege Escalation (Leo thang đặc quyền):**
   * *Nguy cơ:* Người dùng thông thường (`ROLE_USER`) hoặc khán giả gửi request tạo giải đấu hoặc sửa tỉ số trận đấu.
   * *Biện pháp:* Áp dụng kiểm soát truy cập dựa trên vai trò (`RBAC`) chặt chẽ thông qua `@PreAuthorize` và bộ lọc `SecurityFilterChain`.

4. **Information Disclosure qua thông báo lỗi:**
   * *Nguy cơ:* Trả về chi tiết stack trace, cấu trúc database hoặc thông báo tài khoản có tồn tại hay không khi đăng nhập sai.
   * *Biện pháp:* Ẩn thông báo chi tiết khi login thất bại (trả về thông điệp chung `401: Invalid username or password`), sử dụng `@RestControllerAdvice` để chuẩn hóa `ErrorResponse`.

---

## 4. Yêu cầu phi chức năng về an ninh (Security Non-Functional Requirements)

* **Xác thực (Authentication):**
  * Sử dụng chuẩn công nghiệp JWT (JSON Web Token - RFC 7519).
  * Thuật toán ký: `HMAC-SHA256 (HS256)` với secret key có độ dài tối thiểu 256 bits.
  * Thời hạn sống của token (`expiresIn`): 900 giây (15 phút).
  * Mã hóa mật khẩu: `BCryptPasswordEncoder` của Spring Security Crypto.

* **Phân quyền (Authorization):**
  * Áp dụng nguyên tắc đặc quyền tối thiểu (Principle of Least Privilege).
  * Tách biệt rõ ràng giữa quyền theo vai trò (`RBAC`) và quyền trên thực thể dữ liệu (`Resource Ownership`).
  * Chỉ `ROLE_ADMIN` mới có quyền can thiệp vào tài nguyên của người khác (Admin Override).

* **Giao tiếp giữa các dịch vụ:**
  * Client gọi API thông qua Header chuẩn: `Authorization: Bearer <JWT>`.
  * Khi `match-service` gọi sang `tournament-service` để kiểm tra sự tồn tại của giải đấu, `TournamentClient` thực hiện kỹ thuật **Token Propagation** (chuyển tiếp token) để duy trì ngữ cảnh bảo mật.
