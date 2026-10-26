# THIẾT KẾ KIẾN TRÚC BẢO MẬT VÀ API (API SECURITY DESIGN)
**Học phần:** Phát triển phần mềm hướng dịch vụ (PTPMHDV) — Buổi thực hành 5  
**Hệ thống:** Sports Management Multi-Service System  

---

## 1. Sơ đồ kiến trúc bảo mật phân tán

Hệ thống áp dụng mô hình **Stateless JWT Authentication** kết hợp **Decentralized Verification**:

```
+---------------------------------------------------------------------------------+
|                                 CLIENT (Postman / Web App)                      |
+---------------------------------------------------------------------------------+
          |                                   |                             |
  1. POST /login                       2. GET /tournaments           3. POST /matches
     (credentials)                     (No Token - Public)             (Bearer JWT)
          |                                   |                             |
          v                                   v                             v
+-------------------+               +-------------------+         +-------------------+
|    AUTH SERVICE   |               | TOURNAMENT SERVICE|         |   MATCH SERVICE   |
|    (Port 8083)    |               |    (Port 8081)    |         |    (Port 8082)    |
+-------------------+               +-------------------+         +-------------------+
| - BCrypt Check    |               | - JwtAuthFilter   |         | - JwtAuthFilter   |
| - Generate JWT    |               | - Ownership Check |         | - RBAC Check      |
+-------------------+               +-------------------+         +-------------------+
          |                                   ^                             |
          |                                   |                             |
          |                                   +---- 4. GET /tournaments/{id}+
          |                                         (Token Propagation)
          v
   Returns JWT Token
   { accessToken, ... }
```

---

## 2. Cấu trúc JSON Web Token (JWT)

Token được tạo ra và kiểm tra tính hợp lệ tuân thủ chuẩn RFC 7519:

### 2.1. Header
```json
{
  "alg": "HS256",
  "typ": "JWT"
}
```

### 2.2. Payload (Claims)
```json
{
  "sub": "organizer1",
  "userId": 2,
  "role": "ROLE_ORGANIZER",
  "iat": 1729382400,
  "exp": 1729383300
}
```
* `sub` (Subject): Tên đăng nhập của người dùng.
* `userId`: ID định danh duy nhất của tài khoản.
* `role`: Vai trò của người dùng (`ROLE_ADMIN`, `ROLE_ORGANIZER`, `ROLE_REFEREE`, `ROLE_USER`).
* `iat` (Issued At): Thời điểm cấp phát token.
* `exp` (Expiration Time): Thời điểm hết hạn (sau 900 giây / 15 phút).

### 2.3. Signature
Được ký bằng thuật toán `HMAC-SHA256` sử dụng Secret Key bảo mật chung giữa các dịch vụ:
`HMACSHA256(base64UrlEncode(header) + "." + base64UrlEncode(payload), secretKey)`

---

## 3. Thiết kế các API Xác thực (Auth Service)

### 3.1. Đăng ký tài khoản (Register)
* **Endpoint:** `POST /api/v1/auth/register`
* **Request Body:**
```json
{
  "username": "new_organizer",
  "password": "Password123!",
  "fullName": "Nguyen Van A",
  "email": "organizer@example.com",
  "role": "ROLE_ORGANIZER"
}
```
* **Response:** `201 Created`
```json
{
  "id": 6,
  "username": "new_organizer",
  "fullName": "Nguyen Van A",
  "email": "organizer@example.com",
  "role": "ROLE_ORGANIZER",
  "message": "User registered successfully"
}
```

### 3.2. Đăng nhập và cấp phát Token (Login)
* **Endpoint:** `POST /api/v1/auth/login`
* **Request Body:**
```json
{
  "username": "organizer1",
  "password": "organizer123"
}
```
* **Response Thành công (`200 OK`):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "username": "organizer1",
  "role": "ROLE_ORGANIZER"
}
```
* **Response Thất bại (`401 Unauthorized`):**
```json
{
  "status": 401,
  "message": "Invalid username or password"
}
```

### 3.3. Lấy thông tin tài khoản hiện tại (Get Profile)
* **Endpoint:** `GET /api/v1/auth/me`
* **Headers:** `Authorization: Bearer <accessToken>`
* **Response:** `200 OK`
```json
{
  "id": 2,
  "username": "organizer1",
  "fullName": "Tournament Organizer One",
  "email": "organizer1@sports.com",
  "role": "ROLE_ORGANIZER",
  "active": true
}
```

---

## 4. Thiết kế Bộ lọc xác thực và Phân quyền (SecurityFilterChain & JwtFilter)

Cả `tournament-service` và `match-service` đều hoạt động như các Resource Server độc lập:

1. **`JwtAuthenticationFilter` (Kế thừa `OncePerRequestFilter`):**
   * Đọc Header `Authorization: Bearer <token>`.
   * Sử dụng `JwtTokenProvider` để kiểm tra chữ ký HMAC và hạn dùng.
   * Trích xuất `username` và `role`.
   * Thiết lập `UsernamePasswordAuthenticationToken` vào `SecurityContextHolder`.

2. **Cấu hình Phân quyền (`SecurityConfig`):**
   * Tắt CSRF (do API ở trạng thái Stateless).
   * Phiên làm việc (Session): `SessionCreationPolicy.STATELESS`.
   * Phân quyền đường dẫn theo HTTP Method và Authority.

---

## 5. Cơ chế kiểm soát quyền sở hữu tài nguyên (Resource Ownership Verification)

Tại `TournamentController` và `TournamentService`:
- Khi tạo mới giải đấu (`POST /api/v1/tournaments`), hệ thống tự động gán:
  `tournament.setOrganizerUsername(authentication.getName())`
- Khi thực hiện sửa đổi (`PUT`, `PATCH`) hoặc xóa (`DELETE`):
  Hàm kiểm tra `checkOwnership(tournament, authentication)` được kích hoạt:
  * Nếu người dùng có `ROLE_ADMIN`: Cho phép (Admin Override).
  * Nếu `tournament.getOrganizerUsername().equalsIgnoreCase(authentication.getName())`: Cho phép.
  * Ngược lại: Ném lỗi `403 Forbidden` với thông điệp: `"Access Denied: You do not own this tournament"`.

---

## 6. Bảo vệ giao tiếp giữa các dịch vụ (Service-to-Service Security)

Khi `match-service` nhận request tạo trận đấu (`POST /api/v1/matches`):
1. `match-service` cần xác thực xem `tournamentId` có thực sự tồn tại trong `tournament-service` hay không.
2. `TournamentClient` sử dụng kỹ thuật **Token Propagation**:
   * Trích xuất header `Authorization: Bearer <JWT>` từ request hiện tại của client thông qua `RequestContextHolder`.
   * Đính kèm nguyên trạng header này vào lời gọi HTTP REST outbound tới `tournament-service`.
3. Điều này đảm bảo:
   * Ngữ cảnh xác thực của người dùng được bảo toàn xuyên suốt chuỗi gọi dịch vụ phân tán.
   * `tournament-service` xác thực và ghi nhận danh tính caller một cách minh bạch.

---

## 7. Chuẩn hóa mã phản hồi HTTP (HTTP Status Codes)

| Mã HTTP | Tên chuẩn | Tình huống sử dụng trong hệ thống |
|:---:|---|---|
| **200** | OK | Truy vấn dữ liệu thành công, cập nhật thành công, login thành công. |
| **201** | Created | Tạo mới thành công giải đấu, trận đấu, hoặc đăng ký tài khoản. |
| **204** | No Content | Xóa thành công giải đấu hoặc trận đấu. |
| **400** | Bad Request | Dữ liệu đầu vào không hợp lệ (sai định dạng ngày, thiếu trường bắt buộc, ID giải đấu không tồn tại). |
| **401** | Unauthorized | Chưa cung cấp token, token hết hạn, token giả mạo, hoặc sai thông tin đăng nhập. |
| **403** | Forbidden | Người dùng đã đăng nhập nhưng không đủ quyền (VD: USER cố tạo giải đấu, Organizer cố sửa giải đấu của người khác). |
| **404** | Not Found | Tài nguyên (giải đấu, trận đấu, user) không tồn tại trong hệ thống. |
