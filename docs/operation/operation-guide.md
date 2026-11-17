# HƯỚNG DẪN VẬN HÀNH HỆ THỐNG (DOCKER COMPOSE)

Tài liệu này hướng dẫn cài đặt, khởi động, giám sát, bật/tắt tài khoản demo, bảo toàn dữ liệu và xử lý lỗi khi vận hành cụm microservices bằng Docker Compose.

---

## 1. Điều kiện tiên quyết

- Đã cài **Docker** (khuyến nghị Docker Desktop hoặc Docker Engine 24+) và **Docker Compose v2** (`docker compose version`).
- Không bắt buộc phải cài sẵn JDK 17 hay Maven trên máy chạy vì toàn bộ image được build bằng **multi-stage build** từ mã nguồn mới nhất bên trong Docker.
- Cụm chạy 11 tiến trình Java (mỗi tiến trình giới hạn heap `-Xmx224m`, metaspace `128m` qua `JAVA_TOOL_OPTIONS`) cùng PostgreSQL và RabbitMQ; ước tính nên cấp cho Docker khoảng **4–6 GB RAM** (Docker Desktop: *Settings → Resources*). Nếu container bị dừng với mã thoát `137`, đó là dấu hiệu thiếu bộ nhớ.
- Đảm bảo các cổng mặc định (`8080`, `8761`, `8081`–`8088`, `8091`, `5432`, `5672`, `15672`) chưa bị chiếm dụng, hoặc cấu hình đổi cổng trong file `.env`.

---

## 2. Chuẩn bị file môi trường (`.env`)

1. Sao chép file mẫu [`.env.example`](../../.env.example) thành `.env` (file `.env` đã nằm trong `.gitignore`, tuyệt đối không commit lên Git):
   ```powershell
   Copy-Item .env.example .env
   ```
2. Mở `.env` và chỉnh sửa các giá trị bắt buộc:
   - `JWT_SECRET`: chuỗi ngẫu nhiên dài tối thiểu **32 ký tự**.
   - `DB_USERNAME` / `DB_PASSWORD`: tài khoản PostgreSQL (mặc định `sports`) và mật khẩu tương ứng.
   - `CORS_ALLOWED_ORIGINS`: danh sách origin hợp lệ phân tách bằng dấu phẩy (mặc định `http://localhost:8080`, cấm wildcard `*`).
3. **(Tùy chọn) Bật tài khoản demo để kiểm thử:**
   - Mặc định `APP_DEMO_ENABLED=false` và `APP_DEMO_SEED_USERS=false` (không tự tạo user demo ở mọi môi trường).
   - Nếu cần đăng nhập nhanh hoặc chạy script demo/kiểm thử, đặt trong `.env`:
     ```env
     APP_DEMO_ENABLED=true
     APP_DEMO_SEED_USERS=true
     ```

---

## 3. Lưu ý quan trọng khi khởi tạo PostgreSQL & ổn định RabbitMQ

- Script [`docker/init-multiple-dbs.sql`](../../docker/init-multiple-dbs.sql) tạo sẵn 8 database độc lập (`auth_db`, `tournament_db`, `team_db`, `athlete_db`, `match_db`, `result_db`, `ranking_db`, `notification_db`). Vì script chạy dưới tài khoản `POSTGRES_USER` (`${DB_USERNAME:-sports}`), user cấu hình qua `DB_USERNAME` mặc nhiên là chủ sở hữu cả 8 database mà không cần `GRANT ... TO sports` cố định.
- **Cơ chế của PostgreSQL image:** Thư mục `/docker-entrypoint-initdb.d/` **chỉ tự động chạy duy nhất một lần khi named volume `postgres_data` được tạo mới (trống)**.
- **Không được** xóa volume (`docker compose down -v` hoặc `docker volume rm`) khi đang lưu dữ liệu nghiệp vụ chỉ để ép chạy lại script init. Nếu cần bổ sung một database mới trên volume cũ đang có dữ liệu, kết nối trực tiếp vào container đang chạy để tạo thủ công:
  ```powershell
  docker compose exec postgres-db psql -U <DB_USERNAME> -d postgres -c "CREATE DATABASE new_db;"
  ```
- **Hostname cố định của RabbitMQ:** Service `rabbitmq` dùng `hostname: rabbitmq` để giữ ổn định Erlang node name (`rabbit@rabbitmq`) khi container bị xóa và tạo lại bằng `docker compose down && docker compose up -d`. Nếu trên môi trường từng có `rabbitmq_data` được tạo trước đó với hostname ngẫu nhiên khác, việc thêm `hostname: rabbitmq` **không tự động chuyển dữ liệu** từ node name cũ sang `rabbit@rabbitmq` (cần giữ nguyên cấu hình cũ hoặc chủ động sao lưu/khởi tạo volume sạch ở môi trường mới nếu muốn chuyển node name).

---

## 4. Các lệnh vận hành thường dùng

### Build từ mã nguồn mới nhất & khởi động toàn bộ cụm
```powershell
docker compose up -d --build
```
*(File `docker-compose.yml` đặt `name: sports-management-system` thống nhất với mặc định của `test_th8_deployment.ps1`. Khuyến nghị chạy từ thư mục gốc repository nơi đặt `docker-compose.yml`; khi cần dựng cụm kiểm thử cách ly, truyền thêm `-p <ten-project>` / `-ComposeProjectName` và `--env-file` / `-EnvFile`.)*

### Kiểm tra trạng thái container và healthcheck
```powershell
docker compose ps
```
Trạng thái đạt chuẩn khi tất cả 13 container báo `Up (... healthy)`.

### Kiểm tra Eureka Dashboard & Actuator Health
- **Web UI (API Gateway):** `http://localhost:8080`
- **Eureka Server Dashboard:** `http://localhost:8761` (kiểm tra có đủ 9 Application / 10 instance nghiệp vụ + gateway, trong đó `TOURNAMENT-SERVICE` có 2 instance `:8081` và `:8091`)
- **Actuator Health & Info từng service (trả `{"status":"UP"}`, ẩn chi tiết nhạy cảm):**
  ```powershell
  foreach ($p in @(8761,8080,8081,8091,8082,8083,8084,8085,8086,8087,8088)) {
    Write-Host "Port $p ->" (Invoke-RestMethod "http://127.0.0.1:$p/actuator/health" | ConvertTo-Json -Compress)
  }
  ```

### Xem log ứng dụng qua Docker Compose
```powershell
# Xem log toàn hệ thống (100 dòng gần nhất)
docker compose logs --tail=100

# Theo dõi log của từng service cụ thể (ví dụ match-service khi kiểm tra lỗi kết nối provider)
docker compose logs -f match-service
docker compose logs -f notification-service
```

### Dừng, khởi động lại một dịch vụ (Failover / Bảo trì)
```powershell
# Dừng tạm một instance để kiểm tra LoadBalancer / Failover
docker compose stop tournament-service

# Khởi động lại instance
docker compose start tournament-service

# Restart cơ sở dữ liệu hoặc dịch vụ cụ thể
docker compose restart postgres-db tournament-service
```

### Dừng và khởi động lại toàn cụm (bảo toàn dữ liệu)
```powershell
# Dừng và gỡ container/network NHƯNG GIỮ NGUYÊN named volume postgres_data và rabbitmq_data
docker compose down

# Khởi động lại toàn bộ hệ thống với dữ liệu đã lưu
docker compose up -d
```
> **Cảnh báo:** Tuyệt đối **KHÔNG** chạy `docker compose down -v` khi muốn giữ dữ liệu, vì cờ `-v` sẽ xóa toàn bộ named volume (`postgres_data`, `rabbitmq_data`).

---

## 5. Khắc phục sự cố thường gặp

1. **Lỗi port đã được sử dụng (`Bind for 0.0.0.0:8080 failed: port is already allocated`):**
   - Kiểm tra tiến trình hoặc container cũ đang chiếm cổng trên máy (`docker ps`, `netstat -ano`), dừng cụm thử nghiệm cũ nếu không còn dùng, hoặc đổi cổng publish trong `.env` (ví dụ `GATEWAY_PORT=18080`, `CORS_ALLOWED_ORIGINS=http://localhost:18080`, `REGISTRY_PORT=18761`...) và chạy kèm `-p <ten-project>`.
2. **Service thoát ngay khi khởi động với lỗi `jwt.secret`:**
   - Kiểm tra `.env` đã có `JWT_SECRET` dài tối thiểu **32 ký tự** và truyền đúng file `.env` khi chạy `docker compose`.
3. **Consumer gọi Provider báo `503 Service Unavailable`:**
   - Kiểm tra container provider có đang `healthy` bằng `docker compose ps`, kiểm tra instance đó đã đăng ký `UP` trên Eureka (`http://localhost:8761/eureka/apps`), sau đó đọc log của consumer (`docker compose logs --tail=50 match-service`).
   - Sau khi provider khởi động lại, chờ khoảng `5–15` giây để Eureka cập nhật registry và cache của Spring Cloud LoadBalancer (`3s`) tự làm mới.
4. **Dịch vụ DB (`auth_db`, `tournament_db`, ...) báo không tồn tại khi dùng volume cũ:**
   - Do `postgres_data` đã tồn tại trước khi thêm DB mới (init script chỉ chạy khi volume mới rỗng). Tạo DB còn thiếu qua `psql` hoặc gắn volume mới riêng cho môi trường mới.

---

## 6. Sao lưu và phục hồi một database (quy trình thủ công)

> Quy trình dưới đây dùng công cụ chuẩn của PostgreSQL (`pg_dump`, `pg_restore`) chạy ngay trong container `postgres-db`. Nhóm **chưa lưu minh chứng chạy thử** cho mục này trong `docs/test/test-results/`; khi chạy thử, hãy lưu lại kết quả lệnh để bổ sung minh chứng.

Ví dụ với `tournament_db` (các database khác làm tương tự, chỉ đổi tên database và service sở hữu):

```powershell
# 1. Sao lưu: tạo file dump bên trong container rồi sao chép ra máy chủ
New-Item -ItemType Directory -Force backups | Out-Null
docker compose exec postgres-db sh -c 'pg_dump -U $POSTGRES_USER -d tournament_db -Fc -f /tmp/tournament_db.dump'
docker compose cp postgres-db:/tmp/tournament_db.dump ./backups/tournament_db.dump

# 2. Phục hồi: dừng service sở hữu database, nạp lại dump, rồi khởi động lại
docker compose stop tournament-service tournament-service-2
docker compose cp ./backups/tournament_db.dump postgres-db:/tmp/tournament_db.dump
docker compose exec postgres-db sh -c 'pg_restore -U $POSTGRES_USER -d tournament_db --clean --if-exists /tmp/tournament_db.dump'
docker compose start tournament-service tournament-service-2
```

Lưu ý:
- Dùng dấu nháy đơn quanh lệnh `sh -c '...'` để PowerShell không tự thay `$POSTGRES_USER`; biến này được đọc bên trong container.
- Không dùng toán tử `>` của Windows PowerShell 5.1 để ghi file dump vì nó ghi theo UTF-16 và làm hỏng file.
- Thư mục `backups/` chứa dữ liệu nghiệp vụ, không commit lên Git.

---

## 7. Tài khoản database riêng cho từng service

- Container một lần `db-init` chạy [`docker/create-service-db-users.sh`](../../docker/create-service-db-users.sh) mỗi khi `docker compose up`: tạo 8 tài khoản (`auth_user`, `tournament_user`, `team_user`, `athlete_user`, `match_user`, `result_user`, `ranking_user`, `notification_user`), chuyển quyền sở hữu database và bảng tương ứng, và thu hồi quyền `CONNECT` của mọi tài khoản khác. Script chạy lại nhiều lần được nên dùng được cho cả volume đã có dữ liệu; không cần `down -v`.
- Mật khẩu từng tài khoản đặt qua `AUTH_DB_PASSWORD`, `TOURNAMENT_DB_PASSWORD`, ... trong `.env`; để trống thì dùng `DB_PASSWORD`.
- Tài khoản `DB_USERNAME` vẫn là tài khoản quản trị (dùng cho `psql`, sao lưu, phục hồi).
- Kiểm tra nhanh việc cô lập (kỳ vọng bị từ chối `permission denied for database`):
  ```powershell
  docker compose exec postgres-db psql -U team_user -d athlete_db -c "select 1"
  ```
- Xem kết quả lần chạy gần nhất: `docker compose logs db-init`.

## 8. Mã theo dõi request (`X-Request-Id`)

- `api-gateway` sinh `X-Request-Id` cho mỗi request (hoặc dùng lại mã client gửi), trả về trong header phản hồi và chuyển xuống các service; các service chuyển tiếp mã này khi gọi nhau.
- Mỗi dòng log có mã trong ngoặc vuông. Tìm toàn bộ log của một request trên mọi service:
  ```powershell
  docker compose logs | Select-String "<ma-request-id>"
  ```

## 9. Giám sát bằng Prometheus và Grafana (tùy chọn)

```powershell
docker compose --profile monitoring up -d
```
- Prometheus: `http://localhost:9090` (mục *Status → Targets* phải thấy 11 đích `UP`), đọc số liệu từ `/actuator/prometheus` của từng service.
- Grafana: `http://localhost:3000`, đăng nhập bằng `GRAFANA_ADMIN_USER` / `GRAFANA_ADMIN_PASSWORD` trong `.env`; dashboard dựng sẵn *Sports Management - Tong quan dich vu*.
- Dừng riêng phần giám sát: `docker compose --profile monitoring stop prometheus grafana`.
