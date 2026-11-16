# THIẾT KẾ VÀ CẤU HÌNH SERVICE REGISTRY VỚI NETFLIX EUREKA (THỰC HÀNH 7)

## 1. Vai trò của Service Registry trong Hệ thống Quản lý Thi đấu Thể thao

Trong kiến trúc Microservices của hệ thống Quản lý Thi đấu Thể thao, **Service Registry (`service-registry` - Netflix Eureka Server)** đóng vai trò là "danh bạ trung tâm" quản lý vòng đời và địa chỉ mạng của toàn bộ các dịch vụ:

1. **Loại bỏ sự phụ thuộc vào IP/Port cố định:** Các dịch vụ Consumer (`match-service`, `api-gateway`) chỉ cần biết tên định danh (`TOURNAMENT-SERVICE`, `AUTH-SERVICE`, ...) thay vì địa chỉ vật lý.
2. **Cơ chế Đăng ký (Registration) & Duy trì nhịp tim (Heartbeat):**
   - Mỗi Microservice khởi động sẽ gửi yêu cầu `POST /eureka/apps/{appName}` để đăng ký.
   - Định kỳ mỗi `5 giây` (`lease-renewal-interval-in-seconds=5`), các instance gửi tín hiệu Heartbeat (`PUT /eureka/apps/{appName}/{instanceId}`) để báo rằng instance vẫn đang hoạt động tốt (`UP`).
3. **Loại bỏ Instance ngừng hoạt động (Eviction):**
   - Nếu quá `10 giây` (`lease-expiration-duration-in-seconds=10`) mà Eureka Server không nhận được Heartbeat từ một instance, Eureka sẽ tự động xóa instance đó khỏi danh sách khả dụng.
4. **Cung cấp danh bạ cho Consumer (Registry Fetch):**
   - Các Consumer định kỳ tải danh sách instance khả dụng mỗi `3 giây` (`registry-fetch-interval-seconds=3`) và kết hợp với **Spring Cloud LoadBalancer** để phân phối tải.

---

## 2. Cấu hình kỹ thuật của Eureka Server (`service-registry`)

- **Thư mục mã nguồn:** [`service-registry/`](file:///d:/PTPMHDV/sports-management-system-main/service-registry)
- **Main Class:** [`ServiceRegistryApplication.java`](file:///d:/PTPMHDV/sports-management-system-main/service-registry/src/main/java/com/sports/management/registry/ServiceRegistryApplication.java) với annotation `@EnableEurekaServer`.
- **Cổng mặc định:** `8761`
- **Giao diện quản trị (Dashboard):** `http://localhost:8761`
- **REST Endpoint tra cứu danh bạ (JSON/XML):** `http://localhost:8761/eureka/apps`

### Cấu hình [`application.properties`](file:///d:/PTPMHDV/sports-management-system-main/service-registry/src/main/resources/application.properties):
```properties
spring.application.name=service-registry
server.port=8761

eureka.instance.hostname=localhost
eureka.client.register-with-eureka=false
eureka.client.fetch-registry=false
eureka.client.service-url.defaultZone=http://${eureka.instance.hostname}:${server.port}/eureka/

# Tối ưu tốc độ cập nhật danh bạ khi kiểm thử dừng/khởi động lại instance
eureka.server.enable-self-preservation=false
eureka.server.eviction-interval-timer-in-ms=3000
eureka.server.response-cache-update-interval-ms=3000
```

---

## 3. Quy trình Khám phá Dịch vụ (Service Discovery Flow)

```mermaid
sequenceDiagram
    autonumber
    participant Tour1 as TOURNAMENT-SERVICE<br/>Instance 1 (:8081)
    participant Tour2 as TOURNAMENT-SERVICE<br/>Instance 2 (:8091)
    participant Eureka as EUREKA SERVER<br/>Service Registry (:8761)
    participant Match as MATCH-SERVICE<br/>Consumer (:8082)
    participant Client as Client / Postman

    Tour1->>Eureka: Đăng ký Instance (tournament-service:8081)
    Tour2->>Eureka: Đăng ký Instance (tournament-service:8091)
    Match->>Eureka: Đăng ký Instance (match-service:8082) & Fetch Registry
    Eureka-->>Match: Trả về danh sách [8081, 8091] cho TOURNAMENT-SERVICE

    loop Heartbeat mỗi 5 giây
        Tour1->>Eureka: Renew Lease (Heartbeat)
        Tour2->>Eureka: Renew Lease (Heartbeat)
    end

    Client->>Match: Request 1: POST /api/v1/matches (tournamentId=1)
    Note over Match: LoadBalancer chọn Instance 1 (:8081)
    Match->>Tour1: GET http://tournament-service/api/v1/tournaments/1
    Tour1-->>Match: 200 OK (Header: X-Instance-Id: tournament-service:8081)
    Match-->>Client: 201 Created

    Client->>Match: Request 2: POST /api/v1/matches (tournamentId=1)
    Note over Match: LoadBalancer chọn Instance 2 (:8091)
    Match->>Tour2: GET http://tournament-service/api/v1/tournaments/1
    Tour2-->>Match: 200 OK (Header: X-Instance-Id: tournament-service:8091)
    Match-->>Client: 201 Created
```

