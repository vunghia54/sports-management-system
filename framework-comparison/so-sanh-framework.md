# So sánh Spring Boot và Node.js + Express khi xây dựng và tiêu thụ REST API (TH2)

Cùng một nghiệp vụ được thực hiện ở hai nền tảng:

- **Spring Boot (Consumer gốc):** `match-service` gọi `tournament-service` qua `TournamentClient` (`match-service/src/main/java/com/sports/management/match/client/TournamentClient.java`).
- **Node.js + Express (Consumer thay thế):** `express-tournament-consumer` gọi cùng API `GET /api/v1/tournaments/{id}` và `GET /api/v1/competitions?tournamentId={id}` (`framework-comparison/express-tournament-consumer/src/app.js`).

Provider trong cả hai trường hợp là `tournament-service` (Spring Boot). Provider không biết và không cần biết Consumer viết bằng gì: hợp đồng giữa hai bên chỉ là HTTP + JSON.

## Bảng so sánh

| Tiêu chí | Spring Boot (`match-service`) | Node.js + Express (`express-tournament-consumer`) |
|---|---|---|
| Ngôn ngữ | Java 17, kiểu tĩnh | JavaScript (Node.js 18+), kiểu động |
| Tạo REST API | Annotation `@RestController`, `@GetMapping`, `@PathVariable`; framework tự ánh xạ request | Hàm `app.get('/path/:id', handler)`; tự đọc `req.params`, tự viết phản hồi |
| HTTP Client | `RestClient` của Spring, có `@LoadBalanced` để gọi theo Service ID qua Eureka | `fetch` có sẵn trong Node.js; gọi theo URL cấu hình, không tích hợp Eureka |
| JSON | Jackson tự chuyển đổi đối tượng Java ↔ JSON | JSON là kiểu dữ liệu gốc của JavaScript (`res.json`, `response.json()`) |
| Validation | Bean Validation (`@Valid`, `@NotBlank`, ...) | Tự kiểm tra bằng mã (biểu thức chính quy cho mã giải đấu) |
| Xử lý lỗi tập trung | `@RestControllerAdvice` | Hàm `sendError` và middleware cuối chuỗi |
| Timeout / retry | `JdkClientHttpRequestFactory` (connect 2s, read 10s) qua `RestClientConfig`; retry có giới hạn trong client | `AbortSignal.timeout` (mặc định 3s) và vòng lặp thử lại tối đa 3 lần |
| Dependency | Nhiều starter (web, security, data-jpa, eureka-client, loadbalancer, actuator, ...) quản lý bằng Maven | Một thư viện `express`, quản lý bằng npm |
| Cấu hình | `application.properties` + biến môi trường, `@Value` | Chỉ biến môi trường (`process.env`) |
| Thời gian khởi động | `44.855 seconds` (Spring Boot `MatchServiceApplication` trong container Docker, nối PostgreSQL/RabbitMQ/Eureka) | `18 ms` (Express server `Started on port 8095 in 18 ms` trong container Docker) |
| Dung lượng mã cho cùng chức năng gọi Provider | Lớp client riêng, cấu hình bean, lớp ngoại lệ, handler | Khoảng 200 dòng trong một tệp `app.js` |
| Mức độ phức tạp | Cao hơn khi khởi tạo, nhưng có sẵn bảo mật, JPA, discovery, quan sát | Thấp khi khởi tạo; các tính năng hệ thống (discovery, bảo mật, giám sát) phải tự thêm |

## Nhận xét

1. **REST API không đồng nghĩa với Spring Boot.** Provider Spring Boot phục vụ Consumer Express mà không thay đổi dòng mã nào; điều kiện duy nhất là cả hai tuân theo cùng hợp đồng HTTP/JSON.
2. **Spring Boot phù hợp cho các service nghiệp vụ chính** của hệ thống này vì cần Spring Security, JPA, Eureka và Actuator đồng bộ trên 10 module.
3. **Express phù hợp cho service nhỏ, tập trung vào tích hợp** (tổng hợp dữ liệu, adapter), khởi động nhanh và ít phụ thuộc; đổi lại phải tự viết validation, xử lý lỗi và không có sẵn Service Discovery.
4. **Các vấn đề của hệ thống phân tán không phụ thuộc framework:** cả hai Consumer đều phải tự quyết định timeout, số lần thử lại và phản hồi khi Provider tắt.
