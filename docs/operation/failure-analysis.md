# PHÂN TÍCH SỰ CỐ THỰC TẾ KHI TRIỂN KHAI & VẬN HÀNH (THỰC HÀNH 8)

Tài liệu chỉ ghi nhận các lỗi và tình huống sự cố **đã thực sự gặp và kiểm chứng** trong quá trình đóng gói, chạy `docker compose` và thực thi kịch bản kiểm thử [`test_th8_deployment.ps1`](../../test_th8_deployment.ps1).

---

## 1. Tổng hợp các lỗi thực tế đã gặp và cách khắc phục

| STT | Thành phần / Kịch bản | Hiện tượng quan sát được | Nguyên nhân gốc rễ | Cách đã khắc phục |
| :-: |---|---|---|--|
| 1 | Actuator `/actuator/info` trong Maven test | `ActuatorHealthAndInfoTest` thất bại tại `$.app.name` dù `/actuator/info` trả `200 OK` (`{}`). | Spring Boot 4.1.1 tắt `management.info.env.enabled` mặc định (`false`) để tránh lộ thuộc tính ứng dụng/môi trường qua `/actuator/info`. | Giữ nguyên cấu hình bảo mật (`exposure=health,info`, `show-details=never`, không bật `info.env`), cập nhật test chỉ xác nhận `/actuator/health` trả `status=UP`, không lộ `components` và `/actuator/info` trả `200`. |
| 2 | Chuẩn bị `docker-compose.yml` trên máy có sẵn dịch vụ cũ | Máy đang chạy `sports-postgres` (`5432`), `sports-rabbit` (`5672`, `15672`) và các tiến trình cổng `8080..8091, 8761`. Nếu giữ `container_name` và port cố định sẽ đụng container/port khi test. | `docker-compose.yml` ban đầu dùng `container_name` cố định (`sports-postgres`, `sports-api-gateway`, ...) và hardcode host port. | Bỏ toàn bộ `container_name` cố định trong `docker-compose.yml`, tham số hóa toàn bộ host port trong `.env.example`. Chạy kiểm thử cách ly hoàn toàn qua project `sports_th8_test` với bộ cổng riêng (`18080..18091`, `18761`, `15432`, `15673`, `25672`) và volume riêng. |
| 3 | Script PowerShell chạy `docker compose up -d --build` | Lần chạy đầu dừng đột ngột ở dòng `Image ... Building` với `NativeCommandError` (exit code `1`). | Với `$ErrorActionPreference = "Stop"`, Windows PowerShell 5.1 coi các dòng tiến trình Docker BuildKit ghi ra `stderr` (khi chuyển hướng `2>&1`) là terminating error dù tiến trình không lỗi. | Bọc lệnh gọi Docker trong hàm `Invoke-Compose` với `$ErrorActionPreference = "Continue"`, khôi phục trong `finally` và kiểm tra kết quả bằng `$LASTEXITCODE`. |
| 4 | Kiểm thử `DEP02` (`/actuator/health`) | Assertion thất bại: `service-registry health status != UP: 123 34 103 114 ...` (chuỗi ASCII bytes của JSON). | Spring Boot Actuator trả `Content-Type: application/vnd.spring-boot.actuator.v3+json`; Windows PowerShell 5.1 `Invoke-WebRequest` coi MIME type này là binary nên `$resp.Content` là `[byte[]]`. | Giải mã UTF-8 khi `$resp.Content -is [byte[]]` trong `Invoke-HttpJson` của `test_th8_deployment.ps1`. |
| 5 | `DEP05` & `SPORT02` trên PostgreSQL volume bền vững | (a) `POST /api/v1/tournaments` trả `400 Bad Request`; (b) Khi chạy lại `test_th8_deployment.ps1` trên cùng volume `postgres_data`, `POST /api/v1/matches/generate-schedule` trả `409 Conflict`; (c) `PUT /api/v1/matches/{id}` báo `400` khi thiếu `participant1Id`/`participant2Id`. | (a) Nghiệp vụ yêu cầu `registrationEndDate <= startDate`; (b) dữ liệu trận đấu lượt trước vẫn còn trong `match_db` khiến tên sân cố định bị trùng lịch 120 phút; (c) API `PUT` và cấu trúc `ResultResponse`/`matchesPlayed` đòi hỏi đúng trường dữ liệu. | Cập nhật kịch bản dùng ngày động tương đối theo ngày hiện tại (`registrationEndDate = today = startDate`), tên sân duy nhất `San TH8 <suffix>` mỗi lượt chạy, truyền đủ `participant1Id`/`participant2Id`, đọc đúng `ResultResponse.result` và `RankingItem.matchesPlayed`. |
| 6 | Vòng 3 — Chạy `docker compose config` & `SPORT03` trên PowerShell 5.1 | (a) Chạy `docker compose config` trực tiếp khi repo chưa có `.env` báo lỗi thiếu biến `JWT_SECRET`/`DB_PASSWORD`; (b) Lệnh `psql -U team_user -d athlete_db -c "select 1"` trong `SPORT03` xuất `FATAL: permission denied` ra `stderr` làm PowerShell coi là `NativeCommandError` nếu `$ErrorActionPreference = "Stop"`. | (a) `docker-compose.yml` dùng `${VAR:?...}` để chặn chạy thiếu secret; (b) `psql` trả exit code `2` và ghi thông báo từ chối ra `stderr` khi kiểm thử cô lập DB (hành vi kỳ vọng của PostgreSQL). | (a) Truyền `--env-file <env-file>` khi kiểm tra `docker compose config` trong môi trường sạch không lưu `.env`; (b) Bọc lệnh `docker compose exec ... psql` trong `$ErrorActionPreference = "Continue"` và kiểm tra trực tiếp `$LASTEXITCODE -ne 0` cùng chuỗi `"permission denied"` trong `test_th8_deployment.ps1`. |

*(Ghi chú Vòng 3: Toàn bộ `118/118` test Maven, `10/10` kịch bản `test_th8_deployment.ps1`, `11/11` target Prometheus / `6/6` panel Grafana và bài kiểm thử `db-init` trên volume cũ `sports_th8_oldvol_test` đều chạy đạt ngay ở lần thực thi kiểm chứng mà không phát sinh lỗi mã nguồn hay cấu hình nào khác.)*

---

## 2. Phân tích tình huống sự cố chủ động kiểm thử (`DEP03`, `DEP04`, `SPORT01`)

### 2.1. Dừng toàn bộ `tournament-service` (`DEP03` -> `DEP04`)
- **Thao tác:** Dừng cả hai container `tournament-service` và `tournament-service-2` (`docker compose stop tournament-service tournament-service-2`), chờ Eureka tự hủy đăng ký (eviction) và chờ LoadBalancer TTL `3s` tự làm mới.
- **Tác động quan sát được:**
  - `GET /api/v1/matches/discovery/tournament-instance` tại `match-service` trả **`503 Service Unavailable`** với thông điệp: `"Tournament Service (TOURNAMENT-SERVICE) has no available instances in Eureka Registry"`.
  - `GET /api/v1/tournaments` qua `api-gateway` trả **`503 Service Unavailable`**.
  - Log thực tế tại [`dep03-match-service.log`](../test/test-results/dep03-match-service.log):
    ```text
    match-service-1  | 2026-11-17T00:58:28.252+07:00  WARN 1 --- [match-service] [nio-8082-exec-3] c.s.m.match.client.TournamentClient      : Provider http://tournament-service discovery failed via Eureka/LoadBalancer: No instances available for tournament-service
    match-service-1  | 2026-11-17T00:58:28.254+07:00  WARN 1 --- [match-service] [nio-8082-exec-3] c.s.m.exception.GlobalExceptionHandler   : HTTP 503 returned: Tournament Service (TOURNAMENT-SERVICE) has no available instances in Eureka Registry
    ```
  - Log thể hiện đủ thời điểm, tên service (`[match-service]`), nguyên nhân gốc (`No instances available for tournament-service`) và không làm lộ JWT/mật khẩu.
- **Phục hồi (`DEP04`):** Khi `docker compose start tournament-service tournament-service-2`, cả hai container lên `healthy` sau ~`16.6s`, tự đăng ký lại `UP` lên Eureka và `match-service` gọi thành công lại (`handledBy=tournament-service:8091`).

### 2.2. Dừng 1 trong 2 instance `tournament-service` (`SPORT01`)
- **Bình thường (2 instance):** Gửi 20 request qua `match-service` -> LoadBalancer phân phối đều **`10` request tới `tournament-service:8081` và `10` request tới `tournament-service:8091`** (luân phiên).
- **Khi dừng `tournament-service` (Instance 1):** Sau khi Eureka eviction loại bỏ `:8081`, gửi 10 request tiếp theo -> **10/10 request thành công (`200 OK`) qua `tournament-service:8091`**, không gián đoạn dịch vụ.
- **Khi khởi động lại Instance 1:** Container hồi phục `healthy` sau `13.1s` và đăng ký lại vào Eureka đủ 2 instance `UP`.
