# Báo Cáo Kết Quả Sửa Lỗi Bảo Mật & Logic — Sports Management System

Tài liệu tổng hợp kết quả khắc phục 12 nhóm lỗi (**A1–A6** và **B1–B6**) trong hệ thống `sports-management-system-main` (Spring Boot 4.1.1, 10 module Maven theo kiến trúc Database-per-Service).

## 1. Nguyên Tắc Thực Hiện Đã Tuân Thủ

- Sửa tuần tự từng mục từ **A1 -> A6**, **B1 -> B6**, mỗi mục được tách thành **01 commit Git độc lập** ghi rõ mã mục.
- Giữ nguyên mô hình **Database-per-Service**: không thêm khóa ngoại chéo database, không đọc DB của service khác.
- Cập nhật đồng bộ frontend (`api-gateway/src/main/resources/static/js/app.js`), Postman (`Sports_Management_System_SOA.postman_collection.json`), script test/seed (`test_th7_service_discovery.ps1`, `seed_all_soa_data.py`), và tài liệu trong `docs/`, `integration/`, `security-design/`.
- Không chỉnh sửa `.agents/` và `skills-lock.json`.

---

## 2. Bảng Tổng Hợp Commit Theo Từng Mục

| Mục | Commit | Tiêu đề | Trạng thái |
|---|---|---|---|
| **A1** | `cbaf2e4` | `fix(A1): require JWT and currentPassword for password change and add admin password reset` | Hoàn tất & Đã kiểm thử |
| **A2** | `f9719cd` | `fix(A2): restrict public registration roles, whitelist assignable roles, protect last admin, and update docs/tests` | Hoàn tất & Đã kiểm thử |
| **A3** | `f607d27` | `fix(A3): unify RabbitMQ sports.exchange, publish AFTER_COMMIT, deduplicate NotificationConsumer, update docs/tests` | Hoàn tất & Đã kiểm thử |
| **A4** | `3c874e6` | `fix(A4): bind matches to approved participants, fix scheduling algorithms and transitions, group ranking by participantId` | Hoàn tất & Đã kiểm thử |
| **A5** | `d8587e7` | `fix(A5): enforce registration ownership/dates/rules, add withdraw and re-registration, map duplicate/integrity to 409` | Hoàn tất & Đã kiểm thử |
| **A6** | `1121861` | `fix(A6): enforce result timing/referee/knockout rules, durable ranking recalc on reopen/delete, and fix ranking 404/batch/tie-break` | Hoàn tất & Đã kiểm thử |
| **B1** | `3223670` | `fix(B1): enforce competition/tournament ownership, FK/status/rule/format validation, safe deletes, and dashboard ranking scope` | Hoàn tất & Đã kiểm thử |
| **B2** | `bf1143f` | `fix(B2): harden team member validation, ownership/delete guards, patch type checks, and sortBy allowlists` | Hoàn tất & Đã kiểm thử |
| **B3** | `da6e68a` | `fix(B3): add athlete PII projection, CCCD authZ & uniqueness, field validation, and team/match delete guard` | Hoàn tất & Đã kiểm thử |
| **B4** | `131f644` | `fix(B4): enforce tournament organizer ownership on ranking writes, restrict referee, and null-safe item stats` | Hoàn tất & Đã kiểm thử |
| **B5** | `1fc020f` | `fix(B5): track notification read state per user, validate requests, and add GlobalExceptionHandler` | Hoàn tất & Đã kiểm thử |
| **B6** | `5260cf6` | `fix(B6): remove hardcoded JWT secret defaults and restrict Gateway CORS origins` | Hoàn tất & Đã kiểm thử |

---

## 3. Chi Tiết Thay Đổi & Tệp Đã Sửa Theo Từng Mục

### A1 — Đổi mật khẩu không cần xác thực (`cbaf2e4`)
- **Nội dung chính:** `PUT /api/v1/auth/change-password` bắt buộc Bearer JWT hợp lệ, lấy `username` từ token (không nhận từ body), bắt buộc kiểm tra `currentPassword` khớp và `newPassword` tối thiểu 6 ký tự. Bổ sung endpoint riêng `PUT /api/v1/auth/users/{id}/password` chỉ dành cho `ROLE_ADMIN`. Cập nhật modal đổi mật khẩu trong `app.js`.
- **Tệp đã sửa (7):**
  - `Sports_Management_System_SOA.postman_collection.json`
  - `api-gateway/src/main/resources/static/js/app.js`
  - `auth-service/src/main/java/com/sports/management/auth/controller/AuthController.java`
  - `auth-service/src/main/java/com/sports/management/auth/service/AuthService.java`
  - `auth-service/src/test/java/com/sports/management/auth/AuthPasswordSecurityTest.java`
  - `security-design/api-security-design.md`
  - `security-design/role-permission-matrix.md`

### A2 — Tự đăng ký vai trò tùy ý (`f9719cd`)
- **Nội dung chính:** Đăng ký công khai chỉ chấp nhận `ROLE_VIEWER` và `ROLE_TEAM_MANAGER` (vai trò khác trả `400`), mật khẩu tối thiểu 6 ký tự. `updateUserRole` giới hạn trong 5 vai trò hợp lệ (`ADMIN`, `ORGANIZER`, `REFEREE`, `TEAM_MANAGER`, `VIEWER`), cấm tạo tài khoản mang `ROLE_INTERNAL_RESULT_SYNC`, bảo vệ không cho khóa/xóa/hạ quyền admin cuối cùng. Cập nhật `app.js`, `test_th7_service_discovery.ps1` và tài liệu 5 vai trò.
- **Tệp đã sửa (15):**
  - `Sports_Management_System_SOA.postman_collection.json`
  - `api-gateway/src/main/resources/static/js/app.js`
  - `auth-service/src/main/java/com/sports/management/auth/controller/AuthController.java`
  - `auth-service/src/main/java/com/sports/management/auth/dto/RegisterRequest.java`
  - `auth-service/src/main/java/com/sports/management/auth/repository/UserRepository.java`
  - `auth-service/src/main/java/com/sports/management/auth/service/AuthService.java`
  - `auth-service/src/test/java/com/sports/management/auth/AuthRoleAndRegistrationTest.java`
  - `docs/architecture/microservices-design.md`
  - `integration/postman/integration-tests.json`
  - `security-design/api-security-design.md`
  - `security-design/postman-collection.json`
  - `security-design/role-permission-matrix.md`
  - `security-design/security-requirements.md`
  - `security-design/test-results.md`
  - `test_th7_service_discovery.ps1`

### A3 — RabbitMQ lệch exchange/queue (`f607d27`)
- **Nội dung chính:** Thống nhất topic exchange `sports.exchange` và routing key `match.finished`; bỏ khai báo `match.finished.queue` ở producer, giữ `sports.notification.queue` bên `notification-service`. Phát sự kiện sau khi transaction commit qua `@TransactionalEventListener(AFTER_COMMIT)`. `NotificationConsumer` kiểm tra chống trùng `MATCH_FINISHED` theo `matchId`.
- **Tệp đã sửa (11):**
  - `docs/02-KIEN-TRUC-SOA-ERD-SEQUENCE.md`
  - `integration/api-contract.md`
  - `integration/sequence-diagram/README.md`
  - `match-service/src/main/java/com/sports/management/match/service/MatchService.java`
  - `match-service/src/main/java/com/sports/management/messaging/config/RabbitMQConfig.java`
  - `match-service/src/main/java/com/sports/management/messaging/producer/MatchEventProducer.java`
  - `match-service/src/test/java/com/sports/management/match/RabbitMQContractAndAfterCommitTest.java`
  - `notification-service/src/main/java/com/sports/management/notification/messaging/NotificationConsumer.java`
  - `notification-service/src/main/java/com/sports/management/notification/messaging/RabbitMQConfig.java`
  - `notification-service/src/main/java/com/sports/management/notification/repository/NotificationRepository.java`
  - `notification-service/src/test/java/com/sports/management/notification/NotificationRabbitMQAndConsumerTest.java`

### A4 — Lịch thi đấu & Participant (`3c874e6`)
- **Nội dung chính:** Ràng buộc `Match` (`participant1Id`, `participant2Id`) vào `Participant` thuộc đúng `competitionId` và `tournamentId`; kiểm tra thời gian đấu trong `[startDate, endDate]`; chuẩn hóa tạo lịch vòng tròn / loại trực tiếp kèm xử lý bye và chặn trùng lịch (`409`); cập nhật `RankingItem.participantId` tại `ranking-service`.
- **Tệp đã sửa (18):**
  - `Sports_Management_System_SOA.postman_collection.json`
  - `api-gateway/src/main/resources/static/js/app.js`
  - `integration/api-contract.md`
  - `integration/postman/integration-tests.json`
  - `match-service/src/main/java/com/sports/management/match/controller/MatchController.java`
  - `match-service/src/main/java/com/sports/management/match/model/Match.java`
  - `match-service/src/main/java/com/sports/management/match/service/MatchService.java`
  - `match-service/src/main/resources/application.properties`
  - `match-service/src/test/java/com/sports/management/match/MatchScheduleAndParticipantIntegrationTest.java`
  - `match-service/src/test/java/com/sports/management/match/MatchSecurityAndConflictIntegrationTest.java`
  - `match-service/src/test/java/com/sports/management/match/MatchServiceAndControllerTest.java`
  - `match-service/src/test/java/com/sports/management/match/RabbitMQContractAndAfterCommitTest.java`
  - `ranking-service/src/main/java/com/sports/management/ranking/model/RankingItem.java`
  - `ranking-service/src/main/java/com/sports/management/ranking/service/RankingService.java`
  - `ranking-service/src/test/java/com/sports/management/ranking/RankingServiceAndControllerTest.java`
  - `seed_all_soa_data.py`
  - `test_th7_service_discovery.ps1`
  - `tests/service-discovery/postman-collection.json`

### A5 — Quyền sở hữu, thời hạn & quy tắc đăng ký thi đấu (`d8587e7`)
- **Nội dung chính:** Kiểm tra quyền sở hữu đội/VĐV khi đăng ký, thời hạn đăng ký giải, luật số lượng thành viên từ `tournament-service`, hỗ trợ rút lui (`withdraw`) và đăng ký lại hợp lệ, ánh xạ lỗi trùng lặp/ràng buộc dữ liệu sang HTTP `409`.
- **Tệp đã sửa (11):**
  - `Sports_Management_System_SOA.postman_collection.json`
  - `api-gateway/src/main/resources/static/js/app.js`
  - `match-service/src/main/java/com/sports/management/exception/GlobalExceptionHandler.java`
  - `match-service/src/main/java/com/sports/management/match/client/TournamentClient.java`
  - `match-service/src/main/java/com/sports/management/match/config/SecurityConfig.java`
  - `match-service/src/main/java/com/sports/management/match/controller/RegistrationController.java`
  - `match-service/src/main/java/com/sports/management/match/model/Registration.java`
  - `match-service/src/main/java/com/sports/management/match/repository/MatchRepository.java`
  - `match-service/src/main/java/com/sports/management/match/service/RegistrationService.java`
  - `match-service/src/test/java/com/sports/management/match/RegistrationSecurityAndBusinessTest.java`
  - `security-design/role-permission-matrix.md`

### A6 — Kết quả thi đấu & Đồng bộ Ranking (`1121861`)
- **Nội dung chính:** Kiểm tra trọng tài phụ trách trận đấu, mốc thời gian hợp lệ, luật hòa/luân lưu ở vòng loại trực tiếp; bổ sung `RankingSyncTask` & `ResultSyncRecoveryWorker` để đồng bộ bền vững khi mở lại hoặc xóa kết quả; sửa lỗi `404`/batch/tie-break ở `ranking-service`.
- **Tệp đã sửa (19):**
  - `Sports_Management_System_SOA.postman_collection.json`
  - `api-gateway/src/main/resources/static/js/app.js`
  - `ranking-service/src/main/java/com/sports/management/ranking/client/RankingProviderClient.java`
  - `ranking-service/src/main/java/com/sports/management/ranking/service/RankingService.java`
  - `ranking-service/src/test/java/com/sports/management/ranking/RankingSecurityIntegrationTest.java`
  - `ranking-service/src/test/java/com/sports/management/ranking/RankingServiceAndControllerTest.java`
  - `result-service/src/main/java/com/sports/management/result/client/MatchAndRankingClient.java`
  - `result-service/src/main/java/com/sports/management/result/controller/ResultController.java`
  - `result-service/src/main/java/com/sports/management/result/model/RankingSyncTask.java`
  - `result-service/src/main/java/com/sports/management/result/model/RecordResultRequest.java`
  - `result-service/src/main/java/com/sports/management/result/model/Result.java`
  - `result-service/src/main/java/com/sports/management/result/repository/RankingSyncTaskRepository.java`
  - `result-service/src/main/java/com/sports/management/result/repository/ResultRepository.java`
  - `result-service/src/main/java/com/sports/management/result/service/ResultPersistenceService.java`
  - `result-service/src/main/java/com/sports/management/result/service/ResultService.java`
  - `result-service/src/main/java/com/sports/management/result/service/ResultSyncRecoveryWorker.java`
  - `result-service/src/test/java/com/sports/management/result/ResultValidationKnockoutAndReopenSyncTest.java`
  - `security-design/role-permission-matrix.md`
  - `test_th7_service_discovery.ps1`

### B1 — Giải đấu & Nội dung thi đấu (`3223670`)
- **Nội dung chính:** Siết kiểm tra quyền sở hữu khi cập nhật/xóa `Tournament` và `Competition`, kiểm tra FK/trạng thái/Rule/Format hợp lệ, chặn xóa giải/nội dung đang được tham chiếu (`409`/`503`), phạm vi `rankingsCount` trên dashboard, bổ sung modal Rule/Format trong `app.js`.
- **Tệp đã sửa (10):**
  - `Sports_Management_System_SOA.postman_collection.json`
  - `api-gateway/src/main/resources/static/js/app.js`
  - `security-design/role-permission-matrix.md`
  - `seed_all_soa_data.py`
  - `test_th7_service_discovery.ps1`
  - `tournament-service/src/main/java/com/sports/management/tournament/controller/CompetitionController.java`
  - `tournament-service/src/main/java/com/sports/management/tournament/controller/TournamentController.java`
  - `tournament-service/src/main/java/com/sports/management/tournament/service/CompetitionService.java`
  - `tournament-service/src/main/java/com/sports/management/tournament/service/TournamentService.java`
  - `tournament-service/src/test/java/com/sports/management/tournament/CompetitionAndTournamentSecurityTest.java`

### B2 — Đội thi đấu, thành viên & kiểm tra tham số sắp xếp (`bf1143f`)
- **Nội dung chính:** Kiểm tra `member.teamId == id` khi xóa thành viên (`404`), xác thực `athleteId`, trạng thái sức khỏe `QUALIFIED` và trùng VĐV/số áo (`409`), chặn xóa đội đang tham gia giải (`409`/`503`), bỏ quyền `ROLE_ORGANIZER` khỏi `TeamController.checkOwnership`, siết allowlist `sortBy`/`direction` ở `team-service` và `athlete-service`.
- **Tệp đã sửa (16):**
  - `athlete-service/pom.xml`
  - `athlete-service/src/main/java/com/sports/management/athlete/service/AthleteService.java`
  - `athlete-service/src/test/java/com/sports/management/athlete/AthleteSortValidationTest.java`
  - `match-service/src/main/java/com/sports/management/match/controller/RegistrationController.java`
  - `match-service/src/main/java/com/sports/management/match/repository/ParticipantRepository.java`
  - `match-service/src/main/java/com/sports/management/match/repository/RegistrationRepository.java`
  - `match-service/src/main/java/com/sports/management/match/service/RegistrationService.java`
  - `security-design/role-permission-matrix.md`
  - `team-service/src/main/java/com/sports/management/team/client/AthleteClient.java`
  - `team-service/src/main/java/com/sports/management/team/client/MatchClient.java`
  - `team-service/src/main/java/com/sports/management/team/controller/TeamController.java`
  - `team-service/src/main/java/com/sports/management/team/model/TeamMember.java`
  - `team-service/src/main/java/com/sports/management/team/repository/TeamMemberRepository.java`
  - `team-service/src/main/java/com/sports/management/team/service/TeamService.java`
  - `team-service/src/test/java/com/sports/management/team/TeamSecurityAndMemberValidationTest.java`
  - `team-service/src/test/java/com/sports/management/team/TeamServiceAndControllerTest.java`

### B3 — Bảo vệ PII Vận động viên & kiểm tra xóa (`da6e68a`)
- **Nội dung chính:** Thêm `AthleteResponse` ẩn PII (`identityCardNumber`, `phone`, `email`, `dateOfBirth`) với người dùng không có quyền (`ADMIN`, `ORGANIZER`, hoặc chủ sở hữu), bảo vệ endpoint CCCD và tìm kiếm theo keyword PII, kiểm tra trùng `identityCardNumber` (`409`), kiểm tra `dateOfBirth`/`gender`/`healthStatus` (`400`), chặn xóa VĐV còn gắn với `team-service` hoặc `match-service` (`409`/`503`). Chuẩn hóa CCCD duy nhất trong `seed-data.json`.
- **Tệp đã sửa (15):**
  - `Sports_Management_System_SOA.postman_collection.json`
  - `api-gateway/src/main/resources/static/js/app.js`
  - `api-gateway/src/main/resources/static/js/seed-data.json`
  - `athlete-service/pom.xml`
  - `athlete-service/src/main/java/com/sports/management/athlete/client/TeamAndMatchClient.java`
  - `athlete-service/src/main/java/com/sports/management/athlete/config/RestClientConfig.java`
  - `athlete-service/src/main/java/com/sports/management/athlete/config/SecurityConfig.java`
  - `athlete-service/src/main/java/com/sports/management/athlete/controller/AthleteController.java`
  - `athlete-service/src/main/java/com/sports/management/athlete/model/Athlete.java`
  - `athlete-service/src/main/java/com/sports/management/athlete/model/AthleteResponse.java`
  - `athlete-service/src/main/java/com/sports/management/athlete/service/AthleteService.java`
  - `athlete-service/src/test/java/com/sports/management/athlete/AthletePrivacyValidationAndDeleteTest.java`
  - `security-design/role-permission-matrix.md`
  - `team-service/src/main/java/com/sports/management/team/controller/TeamController.java`
  - `team-service/src/main/java/com/sports/management/team/service/TeamService.java`

### B4 — Phân quyền Ranking & an toàn null (`131f644`)
- **Nội dung chính:** Giới hạn quyền ghi trên `POST /recalculate` và `PUT /items` (`ROLE_ADMIN` hoặc `ROLE_ORGANIZER` sở hữu giải đấu qua `RankingProviderClient.getTournamentById`; riêng `/recalculate` cho phép `ROLE_INTERNAL_RESULT_SYNC`; loại bỏ `ROLE_REFEREE`), kiểm tra null-safe khi cập nhật chỉ số item và allowlist `sortBy`.
- **Tệp đã sửa (6):**
  - `ranking-service/src/main/java/com/sports/management/ranking/client/RankingProviderClient.java`
  - `ranking-service/src/main/java/com/sports/management/ranking/controller/RankingController.java`
  - `ranking-service/src/main/java/com/sports/management/ranking/security/SecurityConfig.java`
  - `ranking-service/src/main/java/com/sports/management/ranking/service/RankingService.java`
  - `ranking-service/src/test/java/com/sports/management/ranking/RankingSecurityIntegrationTest.java`
  - `security-design/role-permission-matrix.md`

### B5 — Trạng thái đã đọc thông báo theo người dùng (`1fc020f`)
- **Nội dung chính:** Chuyển trạng thái đã đọc từ cờ dùng chung sang bảng riêng theo từng người dùng (`NotificationRead` / `NotificationReadRepository`, DTO `NotificationView` trả `@JsonProperty("isRead")` và `eventType`), thêm `@Valid` và `GlobalExceptionHandler`, hỗ trợ cả `PATCH` và `PUT /{id}/read`, cập nhật `app.js` và Postman.
- **Tệp đã sửa (13):**
  - `Sports_Management_System_SOA.postman_collection.json`
  - `api-gateway/src/main/resources/static/js/app.js`
  - `notification-service/src/main/java/com/sports/management/notification/config/SecurityConfig.java`
  - `notification-service/src/main/java/com/sports/management/notification/controller/NotificationController.java`
  - `notification-service/src/main/java/com/sports/management/notification/exception/GlobalExceptionHandler.java`
  - `notification-service/src/main/java/com/sports/management/notification/model/Notification.java`
  - `notification-service/src/main/java/com/sports/management/notification/model/NotificationRead.java`
  - `notification-service/src/main/java/com/sports/management/notification/model/NotificationView.java`
  - `notification-service/src/main/java/com/sports/management/notification/repository/NotificationReadRepository.java`
  - `notification-service/src/main/java/com/sports/management/notification/repository/NotificationRepository.java`
  - `notification-service/src/main/java/com/sports/management/notification/service/NotificationService.java`
  - `notification-service/src/test/java/com/sports/management/notification/NotificationPerUserReadAndValidationTest.java`
  - `security-design/role-permission-matrix.md`

### B6 — Loại bỏ JWT secret hardcode & giới hạn CORS Gateway (`5260cf6`)
- **Nội dung chính:** Bỏ giá trị mặc định `jwt.secret` khỏi toàn bộ 8 lớp `JwtTokenProvider` và ánh xạ `jwt.secret=${JWT_SECRET}` kèm kiểm tra tối thiểu 32 byte khi khởi động; xóa `@CrossOrigin(origins = "*")` trên `GatewayRoutingController`, bổ sung `CorsConfig` (`app.cors.allowed-origins=${CORS_ALLOWED_ORIGINS:http://localhost:8080}`, từ chối wildcard `*`). Cập nhật `docker-compose.yml`, `.run/02..11.run.xml`, `test_th7_service_discovery.ps1`, `README.md`.
- **Tệp đã sửa (41):**
  - `.run/02_AuthService_8083.run.xml`
  - `.run/03_TournamentService1_8081.run.xml`
  - `.run/04_TournamentService2_8091.run.xml`
  - `.run/05_TeamService_8085.run.xml`
  - `.run/06_AthleteService_8086.run.xml`
  - `.run/07_MatchService_8082.run.xml`
  - `.run/08_ResultService_8087.run.xml`
  - `.run/09_RankingService_8088.run.xml`
  - `.run/10_NotificationService_8084.run.xml`
  - `.run/11_ApiGateway_8080.run.xml`
  - `README.md`
  - `api-gateway/src/main/java/com/sports/management/gateway/config/CorsConfig.java`
  - `api-gateway/src/main/java/com/sports/management/gateway/controller/GatewayRoutingController.java`
  - `api-gateway/src/main/resources/application.properties`
  - `api-gateway/src/test/java/com/sports/management/gateway/GatewayCorsIntegrationTest.java`
  - `athlete-service/src/main/java/com/sports/management/athlete/security/JwtTokenProvider.java`
  - `athlete-service/src/main/resources/application.properties`
  - `athlete-service/src/test/java/com/sports/management/athlete/AthletePrivacyValidationAndDeleteTest.java`
  - `athlete-service/src/test/java/com/sports/management/athlete/AthleteSortValidationTest.java`
  - `auth-service/src/main/java/com/sports/management/auth/util/JwtTokenProvider.java`
  - `auth-service/src/main/resources/application.properties`
  - `auth-service/src/test/java/com/sports/management/auth/DataSeederDemoDisabledTest.java`
  - `auth-service/src/test/java/com/sports/management/auth/DataSeederDemoEnabledTest.java`
  - `auth-service/src/test/java/com/sports/management/auth/JwtSecretStartupValidationTest.java`
  - `docker-compose.yml`
  - `match-service/src/main/java/com/sports/management/match/security/JwtTokenProvider.java`
  - `match-service/src/main/resources/application.properties`
  - `notification-service/src/main/java/com/sports/management/notification/security/JwtTokenProvider.java`
  - `notification-service/src/main/resources/application.properties`
  - `ranking-service/src/main/java/com/sports/management/ranking/security/JwtTokenProvider.java`
  - `ranking-service/src/main/resources/application.properties`
  - `result-service/src/main/java/com/sports/management/result/security/JwtTokenProvider.java`
  - `result-service/src/main/resources/application.properties`
  - `security-design/role-permission-matrix.md`
  - `team-service/src/main/java/com/sports/management/team/security/JwtTokenProvider.java`
  - `team-service/src/main/resources/application.properties`
  - `team-service/src/test/java/com/sports/management/team/TeamSecurityAndMemberValidationTest.java`
  - `test_th7_service_discovery.ps1`
  - `tournament-service/src/main/java/com/sports/management/tournament/security/JwtTokenProvider.java`
  - `tournament-service/src/main/resources/application.properties`
  - `tournament-service/src/test/java/com/sports/management/tournament/CompetitionAndTournamentSecurityTest.java`

---

## 4. Kết Quả Kiểm Thử Tự Động & Nghiệm Thu E2E Thực Tế

### Kiểm thử tự động toàn repo (`.\mvnw.cmd clean test package`)
Tổng cộng **94/94** test pass (0 fail, 0 error, 0 skipped):

| Module | Tests Run | Passed | Failed | Skipped |
|---|---:|---:|---:|---:|
| `service-registry` | 0 | 0 | 0 | 0 |
| `api-gateway` | 12 | 12 | 0 | 0 |
| `auth-service` | 16 | 16 | 0 | 0 |
| `tournament-service` | 10 | 10 | 0 | 0 |
| `team-service` | 8 | 8 | 0 | 0 |
| `athlete-service` | 4 | 4 | 0 | 0 |
| `match-service` | 18 | 18 | 0 | 0 |
| `result-service` | 10 | 10 | 0 | 0 |
| `ranking-service` | 12 | 12 | 0 | 0 |
| `notification-service` | 4 | 4 | 0 | 0 |
| **Tổng cộng** | **94** | **94** | **0** | **0** |

### Kiểm thử nghiệm thu E2E cụm 10 service (Docker PostgreSQL `sports-postgres` & RabbitMQ `sports-rabbit`)
- Toàn bộ 9 microservice nghiệp vụ đăng ký thành công lên `service-registry` (`ATHLETE-SERVICE`, `API-GATEWAY`, `TOURNAMENT-SERVICE`, `AUTH-SERVICE`, `MATCH-SERVICE`, `RANKING-SERVICE`, `TEAM-SERVICE`, `NOTIFICATION-SERVICE`, `RESULT-SERVICE`) với `JWT_SECRET` động và `CORS_ALLOWED_ORIGINS="http://localhost:8080,http://localhost:3000"`.
- Luồng nghiệp vụ E2E qua `API Gateway` đạt **PASS** toàn bộ 6 bước:
  1. Đăng nhập tài khoản demo & Admin cấp `ROLE_ORGANIZER`.
  2. Organizer tạo Sport, Tournament, Competition, cấu hình `ROUND_ROBIN` và Rule.
  3. Manager tạo VĐV, Đội thi đấu, thành viên và gửi 2 đơn đăng ký.
  4. Organizer duyệt đăng ký, sinh lịch tự động và phân công trọng tài `referee1`.
  5. Trọng tài bắt đầu trận đấu, ghi kết quả -> Organizer xác nhận kết quả (`CONFIRMED`, trận chuyển `FINISHED`).
  6. Xác minh bảng xếp hạng tự động cập nhật (`pts=3, gd=2`), thông báo RabbitMQ `MATCH_FINISHED` được nhận cùng trạng thái đọc riêng theo người dùng, và Gateway cho phép `http://localhost:3000` / chặn `https://evil.example.com`.
- Sau khi hoàn tất E2E, toàn bộ tiến trình cụm test đã dừng và dữ liệu tạm `e2e_*` trong PostgreSQL đã được dọn sạch.
