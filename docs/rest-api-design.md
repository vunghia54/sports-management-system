# Thiết kế REST API (TH3)

Tài liệu này trình bày các quyết định thiết kế REST áp dụng thống nhất cho mọi service của hệ thống. Ví dụ cụ thể lấy từ `athlete-service` (`/api/v1/athletes`) và `match-service` (`/api/v1/matches`). Danh sách endpoint đầy đủ: `docs/03-API-DOCS-CAI-DAT-DEMO-KIEM-THU.md` và Swagger UI của từng service (`/swagger-ui.html`).

## 1. Thiết kế tài nguyên

### 1.1 Nguyên tắc

- URI là **danh từ số nhiều**, chữ thường, phân cách bằng gạch ngang: `/api/v1/athletes`, `/api/v1/tournament-summaries`.
- Hành động thể hiện bằng **HTTP method**, không đặt động từ trong URI.
- Quan hệ cha–con dùng tham số truy vấn khi tài nguyên con thuộc service khác hoặc có thể lọc độc lập: `/api/v1/competitions?tournamentId=1`, `/api/v1/matches?competitionId=3`.
- Thao tác nghiệp vụ không ánh xạ được vào CRUD (xác nhận kết quả, đồng bộ lại) được mô hình hóa như **tài nguyên con dạng hành động** và chỉ dùng cho trường hợp đổi trạng thái có kiểm soát: `PUT /api/v1/results/{id}/confirm`, `POST /api/v1/results/{id}/sync`.
- Mọi đường dẫn có tiền tố phiên bản `/api/v1`.

### 1.2 Thiết kế lại API chưa tốt

| API chưa tốt | Vấn đề | API trong hệ thống |
|---|---|---|
| `GET /getAllAthletes` | Động từ trong URI, không có phiên bản | `GET /api/v1/athletes` |
| `POST /createAthlete` | Động từ trong URI | `POST /api/v1/athletes` |
| `POST /updateAthlete?id=5` | Sai method, id nằm ở query | `PUT /api/v1/athletes/5` hoặc `PATCH /api/v1/athletes/5` |
| `GET /deleteAthlete/5` | GET làm thay đổi dữ liệu, không an toàn | `DELETE /api/v1/athletes/5` |
| `GET /athlete/searchByName?n=An` | Danh từ số ít, tìm kiếm tách thành endpoint riêng | `GET /api/v1/athletes?name=An` |
| `POST /finishMatch/15` | Động từ, bỏ qua kiểm soát trạng thái | Trọng tài `PATCH /api/v1/matches/15` (ONGOING) → ghi kết quả → `PUT /api/v1/results/{id}/confirm`; trạng thái FINISHED do `result-service` cập nhật |

## 2. Mã trạng thái HTTP

| Mã | Khi nào dùng | Ví dụ trong hệ thống |
|---|---|---|
| 200 OK | Đọc hoặc cập nhật thành công | `GET /api/v1/athletes/1`, `PUT /api/v1/athletes/1` |
| 201 Created | Tạo mới thành công | `POST /api/v1/athletes` |
| 204 No Content | Xóa thành công, không có nội dung trả về | `DELETE /api/v1/athletes/1` |
| 400 Bad Request | Dữ liệu sai định dạng hoặc vi phạm ràng buộc | Thiếu `fullName`, `sortBy` không hợp lệ |
| 401 Unauthorized | Không có token, token sai chữ ký hoặc hết hạn | Gọi API không kèm `Authorization` |
| 403 Forbidden | Có token nhưng không đủ quyền | VIEWER gọi `POST /api/v1/tournaments` |
| 404 Not Found | Tài nguyên không tồn tại | `GET /api/v1/athletes/999999` |
| 409 Conflict | Vi phạm tính duy nhất hoặc trạng thái không cho phép | Trùng số CCCD, xóa trận đã ONGOING/FINISHED, xóa VĐV đang thuộc đội |
| 500 Internal Server Error | Lỗi không lường trước | — |
| 502/503/504 | Lỗi khi gọi service khác (xem TH2, TH6) | Provider tắt → 503, Provider chậm → 504 |

Phản hồi lỗi có cấu trúc thống nhất `{timestamp, status, error, message, path, errors}`; với lỗi validation, `errors` là bản đồ `tên trường → thông báo`.

## 3. Phân trang, lọc, sắp xếp

`GET /api/v1/athletes/paged?keyword=an&gender=MALE&page=0&size=10&sortBy=fullName&direction=asc`

- `page` bắt đầu từ 0, `size` mặc định 10.
- `sortBy` chỉ nhận danh sách trắng (`id, fullName, dateOfBirth, gender, nationality, healthStatus`); giá trị khác trả 400 để tránh lộ cấu trúc bảng.
- `direction` chỉ nhận `asc` hoặc `desc`.
- Phản hồi kèm siêu dữ liệu trang (tổng số phần tử, tổng số trang, trang hiện tại).

## 4. PUT và PATCH

| | PUT `/api/v1/athletes/{id}` | PATCH `/api/v1/athletes/{id}` |
|---|---|---|
| Ngữ nghĩa | Thay thế **toàn bộ** biểu diễn của tài nguyên | Cập nhật **một phần** các trường được gửi |
| Body | Đầy đủ các trường bắt buộc, chạy validation đầy đủ | Chỉ các trường cần đổi; trường không gửi giữ nguyên |
| Idempotent | Có | Có với cách cài đặt của hệ thống (gán giá trị), nhưng PATCH nói chung không được đảm bảo idempotent |
| Dùng khi | Biểu mẫu chỉnh sửa đầy đủ | Đổi trạng thái sức khỏe, trọng tài chuyển trận sang ONGOING |

## 5. Tính idempotent

| Method | Safe | Idempotent | Ghi chú trong hệ thống |
|---|---|---|---|
| GET | Có | Có | Không thay đổi dữ liệu |
| PUT | Không | Có | Gửi lại cùng body cho cùng kết quả |
| DELETE | Không | Có | Lần đầu 204, lần sau 404, trạng thái máy chủ không đổi |
| PATCH | Không | Phụ thuộc cài đặt | Gán giá trị cố định nên lặp lại không gây khác biệt |
| POST | Không | Không | Gửi lại có thể tạo bản ghi trùng; hệ thống chặn trùng bằng ràng buộc duy nhất (CCCD ở athlete-service, đăng ký trùng ở match-service) và trả 409 |

Các thao tác nội bộ được thiết kế an toàn khi lặp lại: sự kiện `MATCH_FINISHED` được notification-service xử lý theo `referenceId`, đồng bộ kết quả dùng `resultVersion` và `syncClaimId` nên thử lại không ghi đè dữ liệu mới hơn.

## 6. Quản lý phiên bản (v1 → v2)

Hệ thống dùng **phiên bản trong URI** (`/api/v1/...`) vì dễ đọc, dễ định tuyến ở API Gateway và dễ thử bằng Postman.

Quy tắc tương thích ngược:

1. **Thay đổi tương thích** — thêm trường mới vào phản hồi, thêm tham số truy vấn tùy chọn, thêm endpoint mới — được thực hiện ngay trên v1. Client cũ bỏ qua trường lạ.
2. **Thay đổi phá vỡ** — đổi tên hoặc xóa trường, đổi kiểu dữ liệu, đổi ngữ nghĩa mã trạng thái — phải tạo v2 song song.
3. v1 tiếp tục hoạt động trong thời gian chuyển đổi; phản hồi v1 có thể kèm header `Deprecation` và `Sunset` để báo thời điểm ngừng hỗ trợ.

Ví dụ thiết kế v2 cho vận động viên (chưa cài đặt, minh họa quy tắc):

| | v1 `GET /api/v1/athletes/1` | v2 `GET /api/v2/athletes/1` |
|---|---|---|
| Họ tên | `fullName: "Nguyen Van An"` | `name: {first: "An", last: "Nguyen", middle: "Van"}` |
| Liên hệ | `phone`, `email` ở cấp gốc | `contact: {phone, email}` |
| Lý do tách v2 | — | Đổi cấu trúc trường là thay đổi phá vỡ, client v1 sẽ lỗi nếu sửa trực tiếp |

Trong Gateway, v2 chỉ cần thêm quy tắc định tuyến `/api/v2/athletes/**`; hai phiên bản có thể cùng do một service phục vụ (hai controller dùng chung lớp service) hoặc do hai service khác nhau.
