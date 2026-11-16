# MA TRẬN PHÂN QUYỀN (ROLE-PERMISSION MATRIX)
**Học phần:** Phát triển phần mềm hướng dịch vụ (PTPMHDV) — Buổi thực hành 5  
**Hệ thống:** Sports Management Multi-Service System  

---

## 1. Định nghĩa các vai trò (Roles Definition)

Hệ thống Quản lý Thể thao định nghĩa 5 vai trò rõ rệt (`ROLE_ADMIN`, `ROLE_ORGANIZER`, `ROLE_REFEREE`, `ROLE_TEAM_MANAGER`, `ROLE_VIEWER`):

1. **`ROLE_ADMIN` (Quản trị viên hệ thống):**
   * Có toàn quyền trên tất cả các dịch vụ (Auth, Tournament, Team, Athlete, Match, Result, Ranking, Notification).
   * Có quyền can thiệp, phê duyệt hoặc sửa đổi bất kỳ tài nguyên nào (Admin Override) và cấp/đổi 5 vai trò hợp lệ qua `PUT /api/v1/auth/users/{id}/role`.

2. **`ROLE_ORGANIZER` (Ban tổ chức giải đấu):**
   * Chỉ do `ROLE_ADMIN` cấp quyền (không thể tự đăng ký công khai).
   * Có quyền khởi tạo giải đấu thể thao mới (`POST /api/v1/tournaments`).
   * **Quyền sở hữu tài nguyên (Resource Ownership):** Chỉ có quyền chỉnh sửa (`PUT`), cập nhật từng phần (`PATCH`) hoặc xóa (`DELETE`) các giải đấu **do chính mình tạo ra** (`organizerUsername == currentUsername`), duyệt đăng ký và lên lịch/xác nhận kết quả thuộc giải mình quản lý.

3. **`ROLE_REFEREE` (Trọng tài điều hành trận đấu):**
   * Chỉ do `ROLE_ADMIN` cấp quyền (không thể tự đăng ký công khai).
   * Có quyền nhập/sửa nháp kết quả cho trận đấu được phân công (`refereeUsername == currentUsername`), không được tự xác nhận kết quả chính thức.

4. **`ROLE_TEAM_MANAGER` (Lãnh đội / Huấn luyện viên):**
   * Được phép đăng ký công khai (`POST /api/v1/auth/register` chỉ nhận `ROLE_VIEWER` hoặc `ROLE_TEAM_MANAGER`).
   * Quản lý đội tuyển, thành viên đội, hồ sơ VĐV và gửi đăng ký tham gia giải đấu cho đội của mình.

5. **`ROLE_VIEWER` (Khán giả / Người dùng thông thường):**
   * Vai trò mặc định khi đăng ký công khai.
   * Được phép tra cứu công khai danh sách giải đấu, chi tiết giải đấu, lịch thi đấu, kết quả và bảng xếp hạng.
   * Không có quyền thao tác làm biến đổi dữ liệu (Create/Update/Delete).

---

## 2. Ma trận phân quyền chi tiết (API Permission Matrix)

| STT | HTTP Method | Endpoint URI | Dịch vụ | Loại API | Vai trò được phép | Kiểm tra quyền sở hữu (Resource Ownership)? | HTTP Status mong đợi |
|:---:|:---:|---|---|:---:|---|:---:|:---:|
| 1 | `POST` | `/api/v1/auth/register` | Auth | Công khai | Mọi người | Không | 201 Created / 400 Bad Request |
| 2 | `POST` | `/api/v1/auth/login` | Auth | Công khai | Mọi người | Không | 200 OK / 401 Unauthorized |
| 3 | `GET` | `/api/v1/auth/me` | Auth | Bảo vệ | `ADMIN`, `ORGANIZER`, `REFEREE`, `TEAM_MANAGER`, `VIEWER` | Bản thân | 200 OK / 401 Unauthorized |
| 3b | `PUT` | `/api/v1/auth/change-password` | Auth | Bảo vệ | Mọi vai trò đã đăng nhập | Bản thân (lấy từ JWT) | 200 OK / 400 / 401 / 403 |
| 3c | `PUT` | `/api/v1/auth/users/{id}/password` | Auth | Bảo vệ | `ROLE_ADMIN` | Admin đặt lại mật khẩu | 200 OK / 400 / 401 / 403 / 404 |
| 4 | `GET` | `/api/v1/tournaments` | Tournament | Công khai | Mọi người (Kể cả chưa đăng nhập) | Không | 200 OK |
| 5 | `GET` | `/api/v1/tournaments/{id}` | Tournament | Công khai | Mọi người (Kể cả chưa đăng nhập) | Không | 200 OK / 404 Not Found |
| 6 | `POST` | `/api/v1/tournaments` | Tournament | Bảo vệ | `ROLE_ORGANIZER`, `ROLE_ADMIN` | Tự động gán quyền sở hữu cho người tạo | 201 Created / 401 / 403 |
| 7 | `PUT` | `/api/v1/tournaments/{id}` | Tournament | Bảo vệ | `ROLE_ORGANIZER`, `ROLE_ADMIN` | **BẮT BUỘC:** Chỉ Admin hoặc chính Organizer tạo giải đấu | 200 OK / 401 / **403 Forbidden** / 404 |
| 8 | `PATCH` | `/api/v1/tournaments/{id}` | Tournament | Bảo vệ | `ROLE_ORGANIZER`, `ROLE_ADMIN` | **BẮT BUỘC:** Chỉ Admin hoặc chính Organizer tạo giải đấu (`organizerUsername == null` chỉ Admin sửa) | 200 OK / 401 / **403 Forbidden** / 404 |
| 9 | `DELETE` | `/api/v1/tournaments/{id}` | Tournament | Bảo vệ | `ROLE_ORGANIZER`, `ROLE_ADMIN` | **BẮT BUỘC:** Chỉ Admin hoặc chính Organizer tạo giải đấu (kiểm tra 409 nếu còn nội dung/trận; 503 nếu Match lỗi) | 204 No Content / 401 / **403 Forbidden** / 404 / 409 / 503 |
| 9b | `POST/PUT/DELETE` | `/api/v1/competitions/**` | Tournament | Bảo vệ | `ROLE_ORGANIZER`, `ROLE_ADMIN` | Kiểm tra sở hữu giải đấu cha (`tournamentId`); `PUT /{id}/rule`, `PUT /{id}/format`; `DELETE` chặn 409 nếu có trận/đăng ký, 503 nếu Match lỗi | 200/201/204 / 400 / 403 / 409 / 503 |
| 9c | `POST/PUT/PATCH` | `/api/v1/teams/**` | Team | Bảo vệ | `ROLE_TEAM_MANAGER` (sở hữu đội), `ROLE_ADMIN` | **`ROLE_ORGANIZER` không còn quyền sửa/xóa đội hoặc thành viên**; đội `ownerUsername == null` chỉ `ROLE_ADMIN` sửa được | 200/201 / 400 / 403 / 409 |
| 9d | `DELETE` | `/api/v1/teams/{id}` | Team | Bảo vệ | `ROLE_ADMIN` | Kiểm tra `team_members` trống và chưa tham gia giải/đăng ký tại `match-service` (`409` nếu còn ràng buộc, `503` nếu Match lỗi) | 204 / 403 / 409 / 503 |
| 9e | `POST/DELETE` | `/api/v1/teams/{id}/members/**` | Team | Bảo vệ | `ROLE_TEAM_MANAGER` (sở hữu đội), `ROLE_ADMIN` | VĐV phải `QUALIFIED`, không trùng VĐV/trùng số áo trong đội (`409`), xóa phải khớp `member.teamId == id` (`404`) | 201/204 / 400 / 403 / 404 / 409 |
| 9f | `GET` | `/api/v1/athletes/**` | Athlete | Công khai (ẩn PII) | Công khai chỉ thấy trường công khai; PII (`identityCardNumber`, `phone`, `email`, `dateOfBirth`) chỉ trả cho `ADMIN`, `ORGANIZER` hoặc chủ hồ sơ | Ẩn danh / người khác bị che PII | 200 OK / 404 |
| 9g | `GET` | `/api/v1/athletes/cccd/{cccd}` | Athlete | Bảo vệ | `ROLE_ADMIN`, `ROLE_ORGANIZER`, `ROLE_TEAM_MANAGER` (chỉ hồ sơ của mình) | Chặn tra cứu chéo CCCD (`401` nếu chưa đăng nhập, `403` nếu không đủ quyền) | 200 / 401 / 403 / 404 |
| 9h | `POST/PUT/PATCH/DELETE` | `/api/v1/athletes/**` | Athlete | Bảo vệ | `ROLE_TEAM_MANAGER` (chỉ hồ sơ mình sở hữu), `ROLE_ORGANIZER`, `ROLE_ADMIN` (`DELETE` chỉ `ADMIN`) | CCCD duy nhất (`409`); `gender`, `healthStatus` (`QUALIFIED`/`INJURED`/`SUSPENDED`), `dateOfBirth` hợp lệ (`400`); `DELETE` chặn `409` nếu thuộc đội/giải (`503` nếu lỗi kết nối) | 200/201/204 / 400 / 403 / 409 / 503 |
| 10 | `GET` | `/api/v1/matches` | Match | Công khai | Mọi người (Kể cả chưa đăng nhập) | Không | 200 OK |
| 11 | `GET` | `/api/v1/matches/{id}` | Match | Công khai | Mọi người (Kể cả chưa đăng nhập) | Không | 200 OK / 404 Not Found |
| 12 | `GET` | `/api/v1/matches/tournament/{id}` | Match | Công khai | Mọi người | Không | 200 OK / 404 Not Found |
| 13 | `POST` | `/api/v1/matches` | Match | Bảo vệ | `ROLE_ORGANIZER`, `ROLE_ADMIN` | Kiểm tra giải đấu hợp lệ qua Tournament Service | 201 Created / 400 / 401 / 403 |
| 14 | `PUT` | `/api/v1/matches/{id}` | Match | Bảo vệ | `ROLE_REFEREE`, `ROLE_ORGANIZER`, `ROLE_ADMIN` | Cho phép trọng tài cập nhật trạng thái | 200 OK / 400 / 401 / 403 / 404 |
| 15 | `DELETE` | `/api/v1/matches/{id}` | Match | Bảo vệ | `ROLE_ORGANIZER`, `ROLE_ADMIN` | Chỉ Admin hoặc Organizer sở hữu giải | 204 No Content / 401 / 403 / 404 |
| 15b | `POST` | `/api/v1/registrations` | Match | Bảo vệ | `ROLE_TEAM_MANAGER`, `ROLE_VIEWER`, `ROLE_ADMIN` | `TEAM_MANAGER` chỉ đăng ký đội/VĐV của mình | 201 Created / 400 / 403 / **409 Conflict** |
| 15c | `PUT` | `/api/v1/registrations/{id}/approve` | Match | Bảo vệ | `ROLE_ORGANIZER`, `ROLE_ADMIN` | Chỉ Organizer sở hữu giải (hoặc Admin) | 200 OK / 400 / 403 / 409 |
| 15d | `PUT` | `/api/v1/registrations/{id}/reject` | Match | Bảo vệ | `ROLE_ORGANIZER`, `ROLE_ADMIN` | Organizer sở hữu giải (409 nếu đã có trận) | 200 OK / 400 / 403 / 409 |
| 15e | `PUT` | `/api/v1/registrations/{id}/withdraw` | Match | Bảo vệ | Người đã đăng ký (`registeredBy`), `ROLE_ADMIN` | Rút đơn (409 nếu đã có trận) | 200 OK / 400 / 403 / 409 |
| 16 | `POST` | `/api/v1/results` | Result | Bảo vệ | `ROLE_REFEREE` (được phân công), `ROLE_ORGANIZER` (sở hữu giải), `ROLE_ADMIN` | Trọng tài không ghi đè được `CONFIRMED`; KNOCKOUT hòa phải có luân lưu | 201 Created / 400 / 403 |
| 17 | `PUT` | `/api/v1/results/{id}/confirm` | Result | Bảo vệ | `ROLE_ORGANIZER` (sở hữu giải), `ROLE_ADMIN` | Xác nhận chính thức & đồng bộ Match/Ranking | 200 / 202 / 403 |
| 18 | `PATCH` | `/api/v1/results/{id}` | Result | Bảo vệ | Trọng tài (`notes` khi chưa confirm); Organizer/Admin được mở lại `PENDING_CONFIRMATION` | Mở lại/xóa kết quả tự tính lại BXH | 200 OK / 400 / 403 |

---

## 3. Quy tắc kiểm tra quyền sở hữu tài nguyên (Resource Ownership Verification)

Quy tắc xử lý khi người dùng gửi yêu cầu chỉnh sửa/xóa giải đấu:
```
IF (User is NOT Authenticated):
    RETURN 401 Unauthorized

IF ("ROLE_ADMIN" in User.Authorities):
    ALLOW Operation (Admin có quyền quản trị toàn hệ thống)

ELSE IF (Tournament.organizerUsername == User.Username):
    ALLOW Operation (User là chủ sở hữu hợp pháp của giải đấu)

ELSE:
    RETURN 403 Forbidden
    Message: "Access Denied: You do not own this tournament"
```
Quy tắc này giải quyết triệt để lỗ hổng phân quyền tài nguyên phổ biến nhất trong các bài tập kiến trúc hướng dịch vụ.
