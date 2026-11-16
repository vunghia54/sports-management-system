# MA TRẬN PHÂN QUYỀN (ROLE-PERMISSION MATRIX)
**Học phần:** Phát triển phần mềm hướng dịch vụ (PTPMHDV) — Buổi thực hành 5  
**Hệ thống:** Sports Management Multi-Service System  

---

## 1. Định nghĩa các vai trò (Roles Definition)

Hệ thống Quản lý Thể thao định nghĩa 4 vai trò rõ rệt:

1. **`ROLE_ADMIN` (Quản trị viên hệ thống):**
   * Có toàn quyền trên tất cả các dịch vụ (Auth, Tournament, Match).
   * Có quyền can thiệp, phê duyệt hoặc sửa đổi bất kỳ giải đấu và trận đấu nào mà không bị ràng buộc bởi quyền sở hữu tài nguyên (Admin Override).

2. **`ROLE_ORGANIZER` (Ban tổ chức giải đấu):**
   * Có quyền khởi tạo giải đấu thể thao mới (`POST /api/v1/tournaments`).
   * **Quyền sở hữu tài nguyên (Resource Ownership):** Chỉ có quyền chỉnh sửa (`PUT`), cập nhật từng phần (`PATCH`) hoặc xóa (`DELETE`) các giải đấu **do chính mình tạo ra** (`organizerUsername == currentUsername`).
   * Có quyền lên lịch các trận đấu mới cho giải đấu mà mình tổ chức (`POST /api/v1/matches`).

3. **`ROLE_REFEREE` (Trọng tài điều hành trận đấu):**
   * Có quyền cập nhật tỉ số, thẻ phạt, thời gian và kết quả trận đấu (`PUT /api/v1/matches/{id}`).
   * Không có quyền tạo giải đấu, xóa giải đấu hoặc xóa trận đấu.

4. **`ROLE_USER` (Người dùng thông thường / Khán giả / Vận động viên):**
   * Được phép tra cứu công khai danh sách giải đấu, chi tiết giải đấu, lịch thi đấu và kết quả các trận đấu.
   * Không có quyền thao tác làm biến đổi dữ liệu (Create/Update/Delete).

---

## 2. Ma trận phân quyền chi tiết (API Permission Matrix)

| STT | HTTP Method | Endpoint URI | Dịch vụ | Loại API | Vai trò được phép | Kiểm tra quyền sở hữu (Resource Ownership)? | HTTP Status mong đợi |
|:---:|:---:|---|---|:---:|---|:---:|:---:|
| 1 | `POST` | `/api/v1/auth/register` | Auth | Công khai | Mọi người | Không | 201 Created / 400 Bad Request |
| 2 | `POST` | `/api/v1/auth/login` | Auth | Công khai | Mọi người | Không | 200 OK / 401 Unauthorized |
| 3 | `GET` | `/api/v1/auth/me` | Auth | Bảo vệ | `ADMIN`, `ORGANIZER`, `REFEREE`, `USER` | Bản thân | 200 OK / 401 Unauthorized |
| 4 | `GET` | `/api/v1/tournaments` | Tournament | Công khai | Mọi người (Kể cả chưa đăng nhập) | Không | 200 OK |
| 5 | `GET` | `/api/v1/tournaments/{id}` | Tournament | Công khai | Mọi người (Kể cả chưa đăng nhập) | Không | 200 OK / 404 Not Found |
| 6 | `POST` | `/api/v1/tournaments` | Tournament | Bảo vệ | `ROLE_ORGANIZER`, `ROLE_ADMIN` | Tự động gán quyền sở hữu cho người tạo | 201 Created / 401 / 403 |
| 7 | `PUT` | `/api/v1/tournaments/{id}` | Tournament | Bảo vệ | `ROLE_ORGANIZER`, `ROLE_ADMIN` | **BẮT BUỘC:** Chỉ Admin hoặc chính Organizer tạo giải đấu | 200 OK / 401 / **403 Forbidden** / 404 |
| 8 | `PATCH` | `/api/v1/tournaments/{id}` | Tournament | Bảo vệ | `ROLE_ORGANIZER`, `ROLE_ADMIN` | **BẮT BUỘC:** Chỉ Admin hoặc chính Organizer tạo giải đấu | 200 OK / 401 / **403 Forbidden** / 404 |
| 9 | `DELETE` | `/api/v1/tournaments/{id}` | Tournament | Bảo vệ | `ROLE_ORGANIZER`, `ROLE_ADMIN` | **BẮT BUỘC:** Chỉ Admin hoặc chính Organizer tạo giải đấu | 204 No Content / 401 / **403 Forbidden** / 404 |
| 10 | `GET` | `/api/v1/matches` | Match | Công khai | Mọi người (Kể cả chưa đăng nhập) | Không | 200 OK |
| 11 | `GET` | `/api/v1/matches/{id}` | Match | Công khai | Mọi người (Kể cả chưa đăng nhập) | Không | 200 OK / 404 Not Found |
| 12 | `GET` | `/api/v1/matches/tournament/{id}` | Match | Công khai | Mọi người | Không | 200 OK / 404 Not Found |
| 13 | `POST` | `/api/v1/matches` | Match | Bảo vệ | `ROLE_ORGANIZER`, `ROLE_ADMIN` | Kiểm tra giải đấu hợp lệ qua Tournament Service | 201 Created / 400 / 401 / 403 |
| 14 | `PUT` | `/api/v1/matches/{id}` | Match | Bảo vệ | `ROLE_REFEREE`, `ROLE_ORGANIZER`, `ROLE_ADMIN` | Cho phép trọng tài cập nhật tỉ số | 200 OK / 400 / 401 / 403 / 404 |
| 15 | `DELETE` | `/api/v1/matches/{id}` | Match | Bảo vệ | `ROLE_ORGANIZER`, `ROLE_ADMIN` | Chỉ Admin hoặc Organizer | 204 No Content / 401 / 403 / 404 |

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
