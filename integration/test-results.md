# Kết quả chạy kiểm thử tích hợp thực tế (TH6)

**Ngày thực hiện:** 17/11/2026  
**Môi trường thực thi:** Docker Compose (`api-gateway:8080`, `auth-service:8083`, `tournament-service:8081` & `8091`, `team-service:8085`, `athlete-service:8086`, `match-service:8082`, `result-service:8087`, `ranking-service:8088`, `notification-service:8084`, `rabbitmq`, `postgres-db`, `service-registry:8761`).  
**Công cụ:** Newman 6.2.3 chạy `integration/postman/integration-tests.json` qua `http://localhost:8080`.  
**Báo cáo JSON gốc:** [`docs/test/test-results/integration-th6-newman.json`](../docs/test/test-results/integration-th6-newman.json)

---

## 1. Tổng hợp kết quả Newman run thực tế

```text
TH6 - Sports Service Integration & Consumption Suite
iterations:          1   (failed: 0)
requests:           29   (failed: 0)
test-scripts:       28   (failed: 0)
prerequest-scripts:  2   (failed: 0)
assertions:         29   (failed: 0)
total run duration: 5s
average response:   34.2 ms [min: 7ms, max: 137ms]
```

## 2. Chi tiết kết quả các nhóm ca kiểm thử

| Bước | Nhóm kiểm thử | Endpoint & Vai trò | HTTP thực tế | Bằng chứng ghi nhận | Trạng thái |
|---|---|---|---:|---|:---:|
| **1.1–1.3** | Đăng nhập JWT | `POST /api/v1/auth/login` (`organizer1`, `manager1`, `referee1`) | `200 OK` | Cấp 3 Bearer token theo đúng RBAC (`expiresIn: 3600`) | **ĐẠT** |
| **2.1–2.16** | Tạo dữ liệu động (không hardcode ID/ngày) | `sports`, `tournaments`, `competitions`, `rule`, `format`, `athletes`, `teams`, `members`, `registrations`, `approve`, `participants` | `200`/`201` | Tournament ID `12` (`TH6 Integration Cup 8596273`), Competition ID `12`, Team Alpha & Beta đã duyệt tham gia | **ĐẠT** |
| **3.1** | Tích hợp đồng bộ S2S thành công | `POST /api/v1/matches` (`organizer1`, `tournamentId=12`, `competitionId=12`) | `201 Created` | `match-service` xác thực tournament & competition qua `TournamentClient`; tạo Match ID `27` (`SCHEDULED`), gán `referee1` | **ĐẠT** |
| **3.2** | Tích hợp xử lý lỗi 400 (Invalid Tournament ID) | `POST /api/v1/matches` (`tournamentId=999999`) | `400 Bad Request` | `TournamentClient` phát hiện giải đấu không tồn tại, trả về lỗi 400 rõ ràng | **ĐẠT** |
| **3.3** | Bảo mật Token thiếu (AuthN) | `POST /api/v1/matches` (không có `Authorization`) | `401 Unauthorized` | `JwtAuthenticationFilter` chặn với status `401` | **ĐẠT** |
| **3.4** | Bảo mật quy trình kết thúc trận (RBAC) | `PUT /api/v1/matches/27` (`status=FINISHED` bởi `organizer1`) | `403 Forbidden` | Organizer bị chặn đặt trực tiếp `FINISHED`; bắt buộc đi qua luồng trọng tài ghi điểm + xác nhận | **ĐẠT** |
| **4.1** | Trọng tài bắt đầu trận đấu | `PATCH /api/v1/matches/27` (`status=ONGOING` bởi `referee1`) | `200 OK` | Trận `27` chuyển sang trạng thái `ONGOING` | **ĐẠT** |
| **4.2** | Trọng tài nhập kết quả | `POST /api/v1/results` (`3-1`, bởi `referee1`) | `201 Created` | Tạo Result ID `19`, trạng thái `PENDING_CONFIRMATION` | **ĐẠT** |
| **4.3** | Tổ chức xác nhận kết quả | `PUT /api/v1/results/19/confirm` (`organizer1`) | `200 OK` | `result-service` đồng bộ sang `match-service` + `ranking-service`, Result thành `CONFIRMED`, `syncStatus=SYNCED` | **ĐẠT** |
| **4.4** | Xác minh trận đã chuyển `FINISHED` | `GET /api/v1/matches/27` | `200 OK` | Trường `status` = `"FINISHED"` | **ĐẠT** |
| **4.5** | Tiêu thụ bất đồng bộ RabbitMQ | Chờ `2.5s` -> `GET /api/v1/notifications` | `200 OK` | Tồn tại bản ghi ID `19`: `type="MATCH_FINISHED"`, `referenceId=27`, tiêu đề `"Trận đấu đã kết thúc: TH6 Đội Alpha 8596273 vs TH6 Đội Beta 8596273 (3-1)"` | **ĐẠT** |

## 3. Kết luận

- Toàn bộ chuỗi tích hợp **S2S đồng bộ** (Gateway -> Match -> Tournament -> Result -> Ranking) và **bất đồng bộ Event-Driven** (Match -> RabbitMQ `sports.exchange` / `match.finished` -> `notification.queue` -> Notification Service -> PostgreSQL) hoạt động ổn định, không phụ thuộc dữ liệu hardcode.
- **Tỷ lệ đạt:** `29/29` assertions (**100% PASSED**).
