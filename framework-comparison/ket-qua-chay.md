# Kết quả chạy thực tế Express Tournament Consumer (TH2)

Ghi nhận từ lần chạy thật trên cụm Docker Compose (`docker compose --profile th2-demo up -d --build express-consumer`, API Gateway `http://localhost:8080`, Provider `tournament-service:8081` & `tournament-service:8091`, Express Consumer `http://localhost:8095`).

## 1. Unit test (`npm test` trong `framework-comparison/express-tournament-consumer`)

```text
pass 6
fail 0
duration_ms 1533.3339
```

Các test pass (`test/app.test.js`):
1. `GET /api/v1/tournament-summaries/1 trả 200 kèm handledByProviderInstance`
2. `id không hợp lệ trả 400 cùng schema lỗi chung`
3. `Provider trả 404 trả 404`
4. `Provider tắt (connection refused) thử lại 3 lần và trả 503 TEMPORARILY_UNAVAILABLE`
5. `Provider trả 503 thử lại đủ 3 lần rồi trả 503`
6. `Provider vượt quá timeout trả 504 ngay không thử lại`

## 2. Thời gian khởi động đo từ log Docker thật

| Service | Lệnh kiểm tra | Dòng log thực tế |
|---|---|---|
| Spring Boot (`match-service`) | `docker compose logs match-service \| Select-String "Started MatchServiceApplication"` | `Started MatchServiceApplication in 44.855 seconds (process running for 49.914)` |
| Node.js + Express (`express-consumer`) | `docker compose logs express-consumer \| Select-String "Started on port"` | `Started on port 8095 in 18 ms (provider=http://api-gateway:8080, timeout=3000ms, maxAttempts=3)` |

## 3. Kiểm thử thực tế qua HTTP (status + body thật)

### 3.1. Giải đấu có thật (`id = 1`) — `200 OK` và cân bằng tải Provider

Mỗi yêu cầu `GET /api/v1/tournament-summaries/1` thực hiện **2** lời gọi tuần tự sang API Gateway (`/api/v1/tournaments/1` và `/api/v1/competitions?tournamentId=1`), trong đó trường `handledByProviderInstance` lấy header `X-Instance-Id` của lời gọi `/tournaments/1`.

- Lần đo chuỗi 6 request đầu tiên: cả 6/6 đều ghi nhận `tournament-service:8091`.
- Lần đo chuỗi 8 request tiếp theo (sau khi một số lời gọi lẻ làm dịch chỉ số RoundRobin): cả 8/8 đều ghi nhận `tournament-service:8081`.
- Khi chèn 1 lời gọi lẻ trực tiếp `GET http://localhost:8080/api/v1/tournaments/1` giữa 2 lần gọi `express-consumer`:
  - Call A: `handledByProviderInstance = tournament-service:8081`
  - Call B (sau 1 call Gateway): `handledByProviderInstance = tournament-service:8091`

Phản hồi mẫu (`HTTP 200`):

```json
{"tournamentId":1,"name":"Giải Bóng Đá Vô Địch Quốc Gia V.League 1 - 2026","status":"ONGOING","location":"SVĐ Quốc Gia Mỹ Đình, Hà Nội","startDate":"2026-10-15","endDate":"2026-12-20","competitionCount":1,"competitions":[{"id":1,"name":"Bóng đá Nam Chuyên nghiệp V.League","status":"OPEN_REGISTRATION"}],"handledByProviderInstance":"tournament-service:8091","consumer":"express-tournament-consumer","attempts":1}
```

> Lưu ý: `GET /api/v1/tournaments/**` và `GET /api/v1/competitions/**` trong `tournament-service` (`SecurityConfig`) được cấu hình `permitAll()` nên `express-consumer` gọi được qua Gateway mà không cần JWT.

### 3.2. Giải đấu không tồn tại (`id = 999999`) — `404 Not Found`

```text
GET http://localhost:8095/api/v1/tournament-summaries/999999
HTTP 404
```

```json
{"timestamp":"2026-11-17T09:29:15.743Z","status":404,"error":"Not Found","message":"Khong tim thay tai nguyen tai Provider: /api/v1/tournaments/999999","path":"/api/v1/tournament-summaries/999999","attempts":1}
```

### 3.3. Mã giải đấu sai định dạng (`id = abc`) — `400 Bad Request`

```text
GET http://localhost:8095/api/v1/tournament-summaries/abc
HTTP 400
```

```json
{"timestamp":"2026-11-17T09:29:15.773Z","status":400,"error":"Bad Request","message":"Ma giai dau phai la so nguyen duong","path":"/api/v1/tournament-summaries/abc"}
```

### 3.4. Provider ngừng hoạt động (`docker compose stop tournament-service tournament-service-2`) — `503 Service Unavailable`

```text
docker compose stop tournament-service tournament-service-2
GET http://localhost:8095/api/v1/tournament-summaries/1
HTTP 503
```

```json
{"timestamp":"2026-11-17T09:29:30.536Z","status":503,"error":"Service Unavailable","message":"Tournament service is currently unavailable","path":"/api/v1/tournament-summaries/1","code":"TEMPORARILY_UNAVAILABLE","attempts":3}
```

Sau đó `docker compose start tournament-service tournament-service-2` khôi phục cả hai instance về trạng thái `healthy` trong `16s`.

### 3.5. Provider phản hồi chậm (`npm run slow-provider`, delay 10s, cổng 8099) — `504 Gateway Timeout`

Chạy local với `TOURNAMENT_PROVIDER_URL=http://localhost:8099`:

```text
GET http://localhost:8095/api/v1/tournament-summaries/1
Elapsed ms: 3067
HTTP 504
```

```json
{"timestamp":"2026-11-17T09:30:29.155Z","status":504,"error":"Gateway Timeout","message":"Tournament service khong phan hoi trong 3000ms","path":"/api/v1/tournament-summaries/1","attempts":1}
```
