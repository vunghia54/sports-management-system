# Express Tournament Consumer (TH2 — framework thay thế)

Service viết bằng **Node.js + Express**, đóng vai **Consumer** tiêu thụ REST API của `tournament-service` (Spring Boot, Provider). Mục đích: chứng minh giao tiếp REST/JSON độc lập với framework triển khai (TH2, Bài 3 — Phương án B).

## API

| Method | Đường dẫn | Mô tả |
|---|---|---|
| GET | `/api/v1/hello` | Lời chào, cho biết framework |
| GET | `/api/v1/info` | Thông tin service và cấu hình Provider |
| GET | `/api/v1/tournament-summaries/{id}` | Gọi `GET /api/v1/tournaments/{id}` và `GET /api/v1/competitions?tournamentId={id}` của Provider, trả về bản tổng hợp |
| GET | `/health` | Trạng thái service |

## Xử lý lỗi khi gọi Provider (TH2 — Bài 4, Bài 5)

| Tình huống | Phản hồi cho client |
|---|---|
| Mã giải đấu không phải số nguyên dương | `400 Bad Request` |
| Provider trả 404 | `404 Not Found` |
| Provider tắt (connection refused) hoặc trả 502/503 | Thử lại tối đa 3 lần, sau đó `503` với `code=TEMPORARILY_UNAVAILABLE` (fallback) |
| Provider chậm hơn `PROVIDER_TIMEOUT_MS` (mặc định 3000 ms) | `504 Gateway Timeout`, không thử lại |
| Provider trả mã lỗi khác hoặc JSON hỏng | `502 Bad Gateway` |

Mọi lỗi có cùng cấu trúc `{timestamp, status, error, message, path}` như các service Spring Boot. Header `X-Request-Id` được nhận/sinh và chuyển tiếp sang Provider.

## Cấu hình (biến môi trường)

| Biến | Mặc định | Ý nghĩa |
|---|---|---|
| `PORT` | `8095` | Cổng của service |
| `TOURNAMENT_PROVIDER_URL` | `http://localhost:8080` | URL Provider (mặc định đi qua API Gateway) |
| `PROVIDER_TIMEOUT_MS` | `3000` | Thời gian chờ tối đa mỗi lời gọi |
| `PROVIDER_MAX_ATTEMPTS` | `3` | Số lần thử tối đa (không vượt quá 3) |
| `PROVIDER_RETRY_DELAY_MS` | `200` | Khoảng chờ giữa các lần thử |

## Chạy

Yêu cầu Node.js 18 trở lên. Hệ thống Spring Boot phải đang chạy (Docker Compose hoặc IntelliJ).

```powershell
cd framework-comparison/express-tournament-consumer
npm install
npm test
npm start
# Thử:
Invoke-RestMethod http://localhost:8095/api/v1/tournament-summaries/1
```

Hoặc chạy cùng Docker Compose (profile riêng, không ảnh hưởng cụm chính):

```powershell
docker compose --profile th2-demo up -d --build express-consumer
```

### Minh họa timeout bằng Provider chậm

```powershell
# Cửa sổ 1: Provider giả lập trả lời sau 10 giây (cổng 8099)
npm run slow-provider
# Cửa sổ 2: trỏ Consumer sang Provider chậm
$env:TOURNAMENT_PROVIDER_URL="http://localhost:8099"; npm start
Invoke-WebRequest http://localhost:8095/api/v1/tournament-summaries/1   # -> 504 sau khoảng 3 giây
```

### Minh họa Provider tắt

```powershell
docker compose stop tournament-service tournament-service-2
Invoke-WebRequest http://localhost:8095/api/v1/tournament-summaries/1    # -> 503 TEMPORARILY_UNAVAILABLE
docker compose start tournament-service tournament-service-2
```

So sánh với Spring Boot: xem [`../so-sanh-framework.md`](../so-sanh-framework.md).

## Quyền truy cập Provider (JWT)

Các endpoint đọc công khai `GET /api/v1/tournaments/**` và `GET /api/v1/competitions/**` được cấu hình `permitAll()` trong `tournament-service` (`SecurityConfig`), và API Gateway chuyển tiếp trực tiếp không chặn GET công khai. Do đó `/api/v1/tournament-summaries/{id}` gọi Provider không cần gửi kèm Bearer token (đã kiểm chứng trên cụm Docker đang chạy).
