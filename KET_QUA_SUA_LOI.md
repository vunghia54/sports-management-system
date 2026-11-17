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

---

## 5. Vòng 2 (C1–C6, D1 & Nghiệm thu mới)

Tiếp nối vòng 1 (**A1–B6**), vòng 2 xử lý các hạng mục **C1–C6** (bắt buộc) và **D1** (khuyến nghị), mỗi hạng mục gồm **01 commit Git độc lập**, test tự động trong `src/test` của module tương ứng, đồng bộ `app.js`, Postman, seed/docs và nghiệm thu E2E live trên cụm 10 service.

### Bảng Tổng Hợp Commit Vòng 2

| Mục | Commit | Tiêu đề | Trạng thái |
|---|---|---|---|
| **C1** | `1a43414` | `fix(C1): enforce single source of truth for competition format, group count splitting, and lock edits after matches exist` | Hoàn tất & Đã kiểm thử |
| **C2** | `ae673f4` | `fix(C2): lock registrations once matches exist, enforce rule registrationDeadline, and validate team member health and cross-team uniqueness` | Hoàn tất & Đã kiểm thử |
| **C3** | `444d54f` | `fix(C3): restrict FINISHED transition to ROLE_INTERNAL_RESULT_SYNC and include match score in MatchFinishedEvent notifications` | Hoàn tất & Đã kiểm thử |
| **C4** | `6aec4d7` | `fix(C4): enforce athlete ownership check when team manager adds team members` | Hoàn tất & Đã kiểm thử |
| **C5** | `4fd178e` | `fix(C5): remove test shortcuts from services, restrict sport PUT/DELETE to ADMIN, lower jwt.expiration to 3600s, handle 401 redirect, and add .env.example` | Hoàn tất & Đã kiểm thử |
| **C6** | `05306d2` | `docs(C6): rewrite README for capstone project and synchronize BAO_CAO_THUC_HANH_1_DEN_8` | Hoàn tất & Đã kiểm thử |
| **D1** | `c52daaa` | `fix(D1): support KNOCKOUT generate-next-round with third-place/byes and GROUP_STAGE per-group standings` | Hoàn tất & Đã kiểm thử |

### Chi Tiết Thay Đổi & Tệp Đã Sửa Theo Từng Mục (Vòng 2)

#### C1 — Thể thức thi đấu phải có một nguồn duy nhất (`1a43414`)
- **Nội dung chính:** `match-service` đọc trực tiếp `formatType`, `numberOfGroups`, `hasThirdPlaceMatch` từ `GET /api/v1/competitions/{id}/format` tại `tournament-service`; trả `400` nếu client gửi `format` khác cấu hình. `GROUP_STAGE` chia đúng `numberOfGroups` bảng; `HYBRID` trả `400` thông báo chưa hỗ trợ xếp lịch tự động. `tournament-service` kiểm tra qua `match-service` trước khi sửa `PUT /competitions/{id}/format` và `/rule` (trả `409` nếu đã có trận đấu, `503` nếu `match-service` không phản hồi). Cập nhật `app.js` bỏ dropdown `mGenFormat`, hiển thị thể thức chỉ đọc kèm nút mở cấu hình và trường `#mCfgThirdPlace`.
- **Kiểm thử chứng minh:** `CompetitionAndTournamentSecurityTest`, `MatchScheduleAndParticipantIntegrationTest`, `MatchSecurityAndConflictIntegrationTest`, `ResultValidationKnockoutAndReopenSyncTest`.
- **Tệp đã sửa (12):**
  - `Sports_Management_System_SOA.postman_collection.json`
  - `api-gateway/src/main/resources/static/js/app.js`
  - `integration/api-contract.md`
  - `match-service/src/main/java/com/sports/management/match/client/TournamentClient.java`
  - `match-service/src/main/java/com/sports/management/match/controller/MatchController.java`
  - `match-service/src/main/java/com/sports/management/match/service/MatchService.java`
  - `match-service/src/test/java/com/sports/management/match/MatchScheduleAndParticipantIntegrationTest.java`
  - `match-service/src/test/java/com/sports/management/match/MatchSecurityAndConflictIntegrationTest.java`
  - `result-service/src/test/java/com/sports/management/result/ResultValidationKnockoutAndReopenSyncTest.java`
  - `security-design/role-permission-matrix.md`
  - `tournament-service/src/main/java/com/sports/management/tournament/controller/CompetitionController.java`
  - `tournament-service/src/test/java/com/sports/management/tournament/CompetitionAndTournamentSecurityTest.java`

#### C2 — Khóa đăng ký sau khi đã có lịch & điều kiện đội tham dự (`ae673f4`)
- **Nội dung chính:** `RegistrationService.register` và `approveRegistration` trả `409 Conflict` nếu nội dung thi đấu đã có trận đấu; tôn trọng `CompetitionRule.registrationDeadline`; đăng ký theo `TEAM` yêu cầu mọi thành viên đều có `healthStatus=QUALIFIED` và không trùng VĐV với đội khác đang `PENDING`/`APPROVED` trong cùng nội dung (`409`). Bổ sung `#mCfgRegDeadline` vào `app.js`.
- **Kiểm thử chứng minh:** `RegistrationSecurityAndBusinessTest`.
- **Tệp đã sửa (5):**
  - `api-gateway/src/main/resources/static/js/app.js`
  - `match-service/src/main/java/com/sports/management/match/repository/RegistrationRepository.java`
  - `match-service/src/main/java/com/sports/management/match/service/RegistrationService.java`
  - `match-service/src/test/java/com/sports/management/match/RegistrationSecurityAndBusinessTest.java`
  - `security-design/role-permission-matrix.md`

#### C3 — Trận chỉ được `FINISHED` khi kết quả xác nhận & kèm tỉ số trong thông báo (`444d54f`)
- **Nội dung chính:** `MatchController.verifyMatchUpdatePermission` giới hạn `ROLE_REFEREE` chỉ được chuyển `SCHEDULED`/`POSTPONED -> ONGOING`, chặn `ROLE_ORGANIZER`/`ROLE_ADMIN` đặt `FINISHED` trực tiếp (`403`); chỉ `ROLE_INTERNAL_RESULT_SYNC` được chuyển sang `FINISHED`. `result-service` (`markMatchFinished`) luôn sinh internal sync token và gửi kèm tỉ số/luân lưu; `MatchFinishedEvent` và `NotificationConsumer` đưa tỉ số (và luân lưu nếu có) vào tiêu đề/nội dung thông báo. Cập nhật `app.js` và `seed_all_soa_data.py`.
- **Kiểm thử chứng minh:** `MatchSecurityAndConflictIntegrationTest`, `MatchScheduleAndParticipantIntegrationTest`, `RabbitMQContractAndAfterCommitTest`, `ResultSecuritySyncAndBatchIntegrationTest`, `NotificationRabbitMQAndConsumerTest`.
- **Tệp đã sửa (17):**
  - `api-gateway/src/main/resources/static/js/app.js`
  - `integration/api-contract.md`
  - `match-service/src/main/java/com/sports/management/match/controller/MatchController.java`
  - `match-service/src/main/java/com/sports/management/match/model/Match.java`
  - `match-service/src/main/java/com/sports/management/match/service/MatchService.java`
  - `match-service/src/main/java/com/sports/management/messaging/event/MatchFinishedEvent.java`
  - `match-service/src/test/java/com/sports/management/match/MatchScheduleAndParticipantIntegrationTest.java`
  - `match-service/src/test/java/com/sports/management/match/MatchSecurityAndConflictIntegrationTest.java`
  - `match-service/src/test/java/com/sports/management/match/RabbitMQContractAndAfterCommitTest.java`
  - `notification-service/src/main/java/com/sports/management/notification/messaging/MatchFinishedEvent.java`
  - `notification-service/src/main/java/com/sports/management/notification/messaging/NotificationConsumer.java`
  - `notification-service/src/test/java/com/sports/management/notification/NotificationRabbitMQAndConsumerTest.java`
  - `result-service/src/main/java/com/sports/management/result/client/MatchAndRankingClient.java`
  - `result-service/src/main/java/com/sports/management/result/service/ResultService.java`
  - `result-service/src/test/java/com/sports/management/result/ResultSecuritySyncAndBatchIntegrationTest.java`
  - `security-design/role-permission-matrix.md`
  - `seed_all_soa_data.py`

#### C4 — Quyền sở hữu VĐV khi thêm thành viên vào đội (`6aec4d7`)
- **Nội dung chính:** `TeamController` và `TeamService.addMember` kiểm tra khi caller là `ROLE_TEAM_MANAGER` thì VĐV phải có `ownerUsername` trùng caller (VĐV không thuộc sở hữu hoặc bị ẩn `ownerUsername` trả `403`); `ROLE_ADMIN` được phép thao tác thay. Cập nhật `app.js` lọc danh sách đội/VĐV thuộc sở hữu trong `openAddAthleteToTeamModal`.
- **Kiểm thử chứng minh:** `TeamSecurityAndMemberValidationTest`.
- **Tệp đã sửa (5):**
  - `api-gateway/src/main/resources/static/js/app.js`
  - `security-design/role-permission-matrix.md`
  - `team-service/src/main/java/com/sports/management/team/controller/TeamController.java`
  - `team-service/src/main/java/com/sports/management/team/service/TeamService.java`
  - `team-service/src/test/java/com/sports/management/team/TeamSecurityAndMemberValidationTest.java`

#### C5 — Dọn shortcut test, siết quyền `/sports`, hạ TTL JWT & xử lý 401 (`4fd178e`)
- **Nội dung chính:** Xóa constructor/method shortcut phục vụ test cũ khỏi `RegistrationService`, `CompetitionController`, `MatchService`; giới hạn `PUT`/`PATCH`/`DELETE /api/v1/sports/**` chỉ cho `ROLE_ADMIN`; hạ `jwt.expiration` từ `86400` xuống `3600` giây; `app.js` điều hướng về trang đăng nhập khi nhận `401` ở API bảo vệ; bổ sung `.env.example` và ignore `.env`, `.agents/`, `skills-lock.json` trong `.gitignore`.
- **Kiểm thử chứng minh:** `DataSeederDemoDisabledTest`, `CompetitionAndTournamentSecurityTest`, `MatchServiceAndControllerTest`.
- **Tệp đã sửa (14):**
  - `.env.example`
  - `.gitignore`
  - `api-gateway/src/main/resources/static/js/app.js`
  - `auth-service/src/main/java/com/sports/management/auth/util/JwtTokenProvider.java`
  - `auth-service/src/main/resources/application.properties`
  - `auth-service/src/test/java/com/sports/management/auth/DataSeederDemoDisabledTest.java`
  - `match-service/src/main/java/com/sports/management/match/service/MatchService.java`
  - `match-service/src/main/java/com/sports/management/match/service/RegistrationService.java`
  - `match-service/src/test/java/com/sports/management/match/MatchServiceAndControllerTest.java`
  - `security-design/api-security-design.md`
  - `security-design/role-permission-matrix.md`
  - `tournament-service/src/main/java/com/sports/management/tournament/config/SecurityConfig.java`
  - `tournament-service/src/main/java/com/sports/management/tournament/controller/CompetitionController.java`
  - `tournament-service/src/test/java/com/sports/management/tournament/CompetitionAndTournamentSecurityTest.java`

#### C6 — Cập nhật tài liệu đồ án (`05306d2`)
- **Nội dung chính:** Viết lại `README.md` chuẩn đồ án (luồng nghiệp vụ, 5 vai trò, sơ đồ & bảng 10 service, RabbitMQ, biến môi trường bắt buộc, 6 tài khoản demo đúng `DataSeeder.java`, hướng dẫn IntelliJ `.run` / Docker / Maven / TH7) và đồng bộ `BAO_CAO_THUC_HANH_1_DEN_8.md`.
- **Tệp đã sửa (2):**
  - `BAO_CAO_THUC_HANH_1_DEN_8.md`
  - `README.md`

#### D1 — Tạo vòng Knockout tiếp theo & BXH riêng theo từng bảng `GROUP_STAGE` (`c52daaa`)
- **Nội dung chính:** Bổ sung `POST /api/v1/matches/generate-next-round` đọc kết quả `CONFIRMED` từ `result-service` (`TournamentClient.getResultsByMatchIds`), lấy đội thắng (kể cả qua luân lưu) và đội bye vòng trước để sinh `Tứ kết`/`Bán kết`/`Chung kết` kèm `Tranh hạng ba` khi `hasThirdPlaceMatch=true` (trả `409` nếu còn trận chưa `FINISHED`/chưa có kết quả `CONFIRMED` hoặc đã có Chung kết). Bổ sung `RankingItem.groupName` và cập nhật `RankingService.recalculateRanking` tính điểm và gán `rankPosition` độc lập cho từng bảng trong thể thức `GROUP_STAGE`. Cập nhật UI nút tạo vòng tiếp theo & hiển thị BXH theo từng bảng trong `app.js`, kèm Postman và docs.
- **Kiểm thử chứng minh:** `MatchScheduleAndParticipantIntegrationTest`, `RankingServiceAndControllerTest`.
- **Tệp đã sửa (12):**
  - `Sports_Management_System_SOA.postman_collection.json`
  - `api-gateway/src/main/resources/static/js/app.js`
  - `integration/api-contract.md`
  - `match-service/src/main/java/com/sports/management/match/client/TournamentClient.java`
  - `match-service/src/main/java/com/sports/management/match/controller/MatchController.java`
  - `match-service/src/main/java/com/sports/management/match/service/MatchService.java`
  - `match-service/src/test/java/com/sports/management/match/MatchScheduleAndParticipantIntegrationTest.java`
  - `ranking-service/src/main/java/com/sports/management/ranking/controller/RankingController.java`
  - `ranking-service/src/main/java/com/sports/management/ranking/model/RankingItem.java`
  - `ranking-service/src/main/java/com/sports/management/ranking/service/RankingService.java`
  - `ranking-service/src/test/java/com/sports/management/ranking/RankingServiceAndControllerTest.java`
  - `security-design/role-permission-matrix.md`

### Kết Quả Kiểm Thử Tự Động Toàn Repo Vòng 2 (`.\mvnw.cmd test`)

Tổng cộng **98/98** test pass (0 fail, 0 error, 0 skipped):

| Module | Tests Run | Passed | Failed | Skipped |
|---|---:|---:|---:|---:|
| `service-registry` | 0 | 0 | 0 | 0 |
| `api-gateway` | 12 | 12 | 0 | 0 |
| `auth-service` | 16 | 16 | 0 | 0 |
| `tournament-service` | 11 | 11 | 0 | 0 |
| `team-service` | 8 | 8 | 0 | 0 |
| `athlete-service` | 4 | 4 | 0 | 0 |
| `match-service` | 20 | 20 | 0 | 0 |
| `result-service` | 10 | 10 | 0 | 0 |
| `ranking-service` | 13 | 13 | 0 | 0 |
| `notification-service` | 4 | 4 | 0 | 0 |
| **Tổng cộng** | **98** | **98** | **0** | **0** |

### Kết Quả Nghiệm Thu E2E Vòng 2 Cụm 10 Service (PostgreSQL & RabbitMQ)

Chạy đầy đủ cụm 10 module với `JWT_SECRET` ngẫu nhiên, `APP_DEMO_SEED_USERS=true`, `CORS_ALLOWED_ORIGINS="http://localhost:8080,http://localhost:3000"`, kết nối Docker PostgreSQL (`sports-postgres`) và RabbitMQ (`sports-rabbit`) qua `API Gateway` (`http://localhost:8080`):
1. **Bảo mật & phân quyền (A2, C4, C5):** Đăng ký công khai `ROLE_ORGANIZER` bị chặn (`400`); `organizer1` gọi `PUT /api/v1/sports/1` bị chặn (`403`); `manager1` thêm VĐV thuộc sở hữu `admin` vào đội bị chặn (`403`).
2. **Nội dung 1 — `ROUND_ROBIN` (C1, C2, C3):** Kiểm tra trùng VĐV khác đội cùng nội dung (`409`); gửi `format` lệch khi `generate-schedule` bị chặn (`400`); sinh lịch theo cấu hình `CompetitionFormat`; sửa format hoặc đăng ký thêm sau khi đã có lịch bị chặn (`409`); trọng tài chưa phân công -> `403`, sau phân công đặt trực tiếp `FINISHED` -> `403` (cả Organizer và Trọng tài), chuyển `ONGOING` -> `200`; ghi và xác nhận tỉ số hòa `1-1` thành công -> trận thành `FINISHED`, BXH đạt `1` điểm cho mỗi đội.
3. **Nội dung 2 — `KNOCKOUT` + `hasThirdPlaceMatch=true` (D1, C3):** `generate-next-round` khi bán kết chưa xong bị chặn (`409`); hòa `1-1` không có luân lưu bị chặn (`400`); xác nhận Bán kết 1 hòa `1-1` (luân lưu `5-4`) và Bán kết 2 (`2-0`) -> `POST /api/v1/matches/generate-next-round` sinh đủ `Chung kết - Trận 1` và `Tranh hạng ba`.
4. **Nội dung 3 — `GROUP_STAGE` 2 bảng (C1, D1):** Chia đúng 2 bảng (`Bảng A`, `Bảng B`) và BXH gán `rankPosition=1` độc lập theo từng bảng.
5. **Thông báo RabbitMQ (C3):** `notification-service` nhận đủ thông báo chứa tỉ số (`"1 - 1"` và `"luân lưu 5 - 4"`).
6. **Dọn dẹp:** Đã dừng toàn bộ 10 JVM và xóa toàn bộ dữ liệu kiểm thử khỏi 8 DB PostgreSQL, xác minh số dòng sau test khớp đúng trạng thái nền ban đầu.

---

## 6. Vòng 3 — Hardening TH8, Kiểm thử Docker Compose & Xử lý Rà soát Logic

Sau khi kiểm tra, biên dịch sạch và chạy thử toàn bộ thay đổi Vòng 3 trên Maven (`Spring Boot 4.1.1`) và Docker Compose cách ly (`sports_th8_v3_test`, `sports_th8_oldvol_test`), toàn bộ các hạng mục đều đạt yêu cầu (không có mục nào phải hoàn tác).

### Bảng Tổng Hợp Commit Vòng 3

| Nhóm | Commit | Tiêu đề & Nội dung chính | Trạng thái |
|---|---|---|---|
| **security,logic** | `38a20a0` | Chuẩn hóa username không phân biệt hoa/thường & khoảng trắng (`AuthService`, `DataSeeder`, ownership check ở `Team`/`Athlete`), sửa Stored XSS ở `app.js` (`UiSecurityXssTest`), siết quyền đọc thông báo riêng tư (`NotificationService`/`Repository`), kiểm tra biến động đội hình từ `team-service` sang `match-service`, khóa đổi `tournamentId`/`sportId` khi `Competition` đã có đăng ký/trận đấu, chặn xác nhận kết quả trận `CANCELLED`, và tái kiểm tra sức khỏe/đội hình khi `approveRegistration`. | Hoàn tất & Đã kiểm thử |
| **deploy** | `344c601` | Chuyển `seed-data.json` từ `api-gateway/.../static/js/` sang `data/seed-data.json` và cập nhật `seed_all_soa_data.py`; gỡ mật khẩu DB & `JWT_SECRET` khỏi `.run/*.xml` (đọc qua biến môi trường Windows); bổ sung `.dockerignore`, `.gitattributes` (`*.sh` LF), `.gitignore` (`tmp/`, `backups/`, `.env.*`) và multi-stage `Dockerfile` (non-root `appuser`) cho 10 module. | Hoàn tất & Đã kiểm thử |
| **timeout** | `7bde554` | Cấu hình `RestClientConfig` ở 6 service (`tournament`, `team`, `athlete`, `match`, `result`, `ranking`) dùng `JdkClientHttpRequestFactory` (`HTTP_1_1`) với `services.http.connect-timeout-ms` (`2000ms`) và `read-timeout-ms` (`10000ms`) cùng interceptor gắn `X-Request-Id`; thêm [`ResultTimeoutAndRequestIdTest.java`](result-service/src/test/java/com/sports/management/result/ResultTimeoutAndRequestIdTest.java) kiểm chứng trả `503` nhanh khi provider chậm hơn read timeout (`350ms` vs `2200ms`). | Hoàn tất & Đã kiểm thử |
| **monitoring** | `4acf347` | Thêm `spring-boot-starter-actuator` và `io.micrometer:micrometer-registry-prometheus` cho 10 `pom.xml`; mở `GET /actuator/prometheus` trong 8 `SecurityConfig`; bật `exposure=health,info,prometheus` và `management.metrics.tags.application`; bổ sung test `/actuator/prometheus` `200` / `/actuator/env` bị chặn trên 9 module có test; thêm cấu hình `docker/monitoring/` (Prometheus + Grafana datasource & dashboard `sports-overview.json`). | Hoàn tất & Đã kiểm thử |
| **db-users** | `26b4286` | Thêm `docker/create-service-db-users.sh` và service một lần `db-init` trong `docker-compose.yml` tạo 8 tài khoản `<service>_user` tách biệt, chuyển quyền sở hữu database/schema `public`/bảng và thu hồi `CONNECT` từ `PUBLIC`; cập nhật `.env.example` và bổ sung `SPORT03`, `SPORT04` cùng giải mã UTF-8 trong `test_th8_deployment.ps1`. | Hoàn tất & Đã kiểm thử |
| **request-id** | `5353e2d` | Thêm `RequestIdFilter` ở 8 business service và `api-gateway` (bọc request chuyển tiếp header); bỏ qua `X-Request-Id` từ downstream trong `GatewayRoutingController` tránh trùng header; chuẩn hóa log SLF4J và `logging.pattern.correlation=[%X{requestId:-}]`; thêm [`GatewayRequestIdPropagationTest.java`](api-gateway/src/test/java/com/sports/management/gateway/GatewayRequestIdPropagationTest.java). | Hoàn tất & Đã kiểm thử |
| **docs** | *(commit cuối Vòng 3)* | Cập nhật `docs/deployment/deployment-architecture.mmd` & `.png`, `docs/deployment/deployment-design.md`, `README.md`, `docs/test/deployment-test-cases.md` (`10/10` + monitoring), `docs/operation/failure-analysis.md`, bộ minh chứng trong `docs/test/test-results/` và mục Vòng 3 trong `KET_QUA_SUA_LOI.md`. | Hoàn tất & Đã kiểm thử |

### Kết Quả Kiểm Chứng Chi Tiết Vòng 3

1. **Biên dịch & Kiểm thử tự động Maven (`.\mvnw.cmd clean test`) — 118/118 PASS (0 fail, 0 error, 0 skipped):**
   - `service-registry`: build sạch (`0` test; `/actuator/prometheus` kiểm chứng trực tiếp qua container Docker trả `200 OK`).
   - `api-gateway`: **16/16** pass (gồm `ActuatorHealthAndInfoTest`, `GatewayRequestIdPropagationTest`, `GatewayCorsIntegrationTest`, `UiSecurityXssTest`, `DemoMode*`, `SystemReport*`).
   - `auth-service`: **18/18** pass (gồm `ActuatorSecurityTest`, `AuthRoleAndRegistrationTest`, `AuthPasswordSecurityTest`, `AuthControllerPagedUsersTest`, `DataSeeder*`, `JwtSecretStartupValidationTest`).
   - `tournament-service`: **13/13** pass.
   - `team-service`: **10/10** pass.
   - `athlete-service`: **5/5** pass.
   - `match-service`: **22/22** pass.
   - `result-service`: **14/14** pass (`ResultCompetitionAndConcurrencyTest` 4/4, `ResultSecuritySyncAndBatchIntegrationTest` 3/3 xác nhận `PATCH` sang `match-service` qua `JdkClientHttpRequestFactory`, `ResultTimeoutAndRequestIdTest` 1/1, `ResultValidationKnockoutAndReopenSyncTest` 6/6).
   - `ranking-service`: **14/14** pass.
   - `notification-service`: **6/6** pass.
   - Kiểm tra bổ trợ: `node --check api-gateway/src/main/resources/static/js/app.js` pass; `python -m py_compile seed_all_soa_data.py` pass; `.run/*.xml` sạch mật khẩu và `JWT_SECRET`.

2. **Kiểm thử Docker Compose (`test_th8_deployment.ps1` trên `sports_th8_v3_test` + volume cũ `sports_th8_oldvol_test` + `--profile monitoring`):**
   - `docker compose --env-file ... config -q` hợp lệ (exit `0`).
   - `Wait-ComposeServicesHealthy` hoạt động ổn định, không bị kẹt khi container `db-init` thoát (`Exited (0)`).
   - **10/10 kịch bản PASS (`DEP01`–`DEP06` + `SPORT01`–`SPORT04`)** trong [`docs/test/test-results/summary.json`](docs/test/test-results/summary.json):
     - `DEP02`: 11/11 service trả `/actuator/health` `UP` (không lộ `components`), `/actuator/prometheus` `200`, `/actuator/env` bị chặn (`401`/`403`/`404`).
     - `SPORT03`: `psql -U team_user -d athlete_db -c "select 1"` bị từ chối (`exitCode=2`, `FATAL: permission denied for database "athlete_db"`); toàn bộ 8/8 DB thuộc quyền `<service>_user`.
     - Kiểm chứng thêm trên volume cũ mô phỏng (`sports_th8_oldvol_test` có bảng `public.sports` do user `sports` tạo trước cùng dữ liệu cũ): `db-init` chuyển quyền sở hữu sang `tournament_user`, bảo toàn bản ghi và `tournament-service` (`Hibernate ddl-auto=update`) khởi động `healthy` sau `16s`.
     - `SPORT04`: Request `GET /api/v1/tournaments/1` mang `X-Request-Id: sport04-trace-81580` trả về đúng 1 header `X-Request-Id` không trùng lặp và xuất hiện khớp trong log của cả `api-gateway-1` và `tournament-service-1`.
   - **Monitoring (`--profile monitoring`):** Prometheus thấy đủ `11/11` target `up`; cả `6/6` PromQL query của [`sports-overview.json`](docker/monitoring/grafana/dashboards/sports-overview.json) trả `status="success"`; Grafana datasource `Prometheus` đạt `healthStatus="OK"` và dashboard `Sports Management - Tong quan dich vu` nạp đủ `6` panel ([`monitoring-check.json`](docs/test/test-results/monitoring-check.json)).
   - Toàn bộ container và volume tạm của các project kiểm thử cách ly (`sports_th8_v3_test`, `sports_th8_oldvol_test`) đã được dọn sạch (`down -v`) sau khi lưu minh chứng.

## 7. Vòng 4 – đối chiếu TH1–TH8 (17/11/2026)

1. **TH2 — Express Consumer (`framework-comparison/express-tournament-consumer`):**
   - Triển khai & đóng gói service Node.js + Express (`package.json`, `package-lock.json`, `Dockerfile`, profile `th2-demo` trong `docker-compose.yml`, thêm `node_modules/` vào `.gitignore`). Xác nhận `GET /api/v1/tournaments/**` và `GET /api/v1/competitions/**` công khai (`permitAll()`) qua `tournament-service` (`SecurityConfig.java`) và ghi rõ trong `README.md`.
   - Unit/mock test `npm test`: **6/6 pass** (`fail 0`, `duration_ms 1850.5661`: 200, 400, 404, 503 connection refused, 503 provider 503, 504 timeout).
   - Chạy thật trên Docker & local (`framework-comparison/ket-qua-chay.md`): `200` trên `id=1` cân bằng tải qua cả `tournament-service:8081` và `tournament-service:8091`; `404` cho `999999`; `400` cho `abc`; `503 TEMPORARILY_UNAVAILABLE` (`attempts: 3`) khi dừng `tournament-service` và `tournament-service-2` rồi phục hồi `healthy` sau `16s`; `504` qua `npm run slow-provider` (`TOURNAMENT_PROVIDER_URL=http://localhost:8099`) sau **`3067 ms`**.
   - Cập nhật thời gian khởi động thật vào [`framework-comparison/so-sanh-framework.md`](framework-comparison/so-sanh-framework.md): Express **`18 ms`** (`Started on port 8095 in 18 ms`) so với Spring Boot **`44.855 seconds`** (`Started MatchServiceApplication in 44.855 seconds`; lần cold-start sau reboot ghi nhận `66.652 seconds`).

2. **TH3/TH4 — Đối chiếu tài liệu, Swagger, Mermaid & Chuẩn hóa `ErrorResponse`:**
   - Bổ sung & rà soát [`docs/rest-api-design.md`](docs/rest-api-design.md), [`docs/data-and-service-design.md`](docs/data-and-service-design.md), [`integration/sequence-diagram/README.md`](integration/sequence-diagram/README.md) khớp cổng, DTO, hành vi S2S (`/tournaments/{id}`, `/competitions/{id}`), `DELETE -> 204`, trùng CCCD `-> 409`, `sortBy` sai `-> 400`, luồng kết thúc trận thật và minh chứng `RabbitMQContractAndAfterCommitTest`.
   - Kiểm chứng render **7/7** sơ đồ Mermaid (5 sơ đồ trong `docs/data-and-service-design.md`, 2 sơ đồ trong `integration/sequence-diagram/README.md`) bằng Chrome + `mermaid@11` -> **7/7 SVG sạch (0 lỗi)**.
   - Bổ sung `timestamp`, `error`, `path` vào `ErrorResponse` và `GlobalExceptionHandler` trên toàn bộ 8 service (giữ nguyên `status`, `message`, `errors`); cập nhật `RegistrationSecurityAndBusinessTest.java` và chạy **`.\mvnw.cmd test -q` đạt 118/118 pass** (exit `0`).

3. **TH1/TH3/TH5/TH6/TH7/TH8 — Bộ Postman (Newman) & Script kiểm thử:**
   - Tạo [`tests/postman/th1-th3-crud.postman_collection.json`](tests/postman/th1-th3-crud.postman_collection.json) (dữ liệu động, đăng nhập `manager1`/`admin`, không hardcode ID) -> Newman: **20/20 request, 20/20 assertion pass** (avg `40.9 ms`), kết quả [`docs/test/test-results/th1-th3-crud-newman.json`](docs/test/test-results/th1-th3-crud-newman.json).
   - Cập nhật [`security-design/postman-collection.json`](security-design/postman-collection.json) (TC20 token JWT hết hạn `401`, thiếu token `401`, VIEWER `403`, TC17 luồng referee `ONGOING -> POST /results -> organizer PUT confirm -> FINISHED`, dữ liệu động) -> Newman: **45/45 request, 46/46 assertion pass** (avg `34.0 ms`), [`docs/test/test-results/security-th5-newman.json`](docs/test/test-results/security-th5-newman.json) & cập nhật [`security-design/test-results.md`](security-design/test-results.md) (TTL `3600`, `FINISHED`).
   - Viết lại [`integration/postman/integration-tests.json`](integration/postman/integration-tests.json) (bỏ organizer `PUT /matches/{id}` `FINISHED`, thay bằng luồng `ONGOING -> POST /results -> confirm -> GET FINISHED -> wait -> GET /notifications` assert `type=MATCH_FINISHED` & `referenceId == matchId`) -> Newman: **29/29 request, 29/29 assertion pass** (avg `34.2 ms`), [`docs/test/test-results/integration-th6-newman.json`](docs/test/test-results/integration-th6-newman.json) & cập nhật [`integration/test-results.md`](integration/test-results.md).
   - Sửa [`tests/service-discovery/postman-collection.json`](tests/service-discovery/postman-collection.json) (`organizer123`, dữ liệu & `matchTime` động) -> Newman: **27/27 request, 32/32 assertion pass** (avg `26.2 ms`), [`docs/test/test-results/service-discovery-th7-newman.json`](docs/test/test-results/service-discovery-th7-newman.json).
   - Sửa [`test_th7_service_discovery.ps1`](test_th7_service_discovery.ps1) (ngày động, không kill tiến trình ngoài script, assert cân bằng tải cả `8081` và `8091`, assert `503`, bỏ DELETE thủ công Eureka, kiểm chứng notification qua RabbitMQ theo `matchId`) -> chạy với `-KeepRunning` (exit `0`, **9/9 ứng dụng UP**, `Match ID=28`, `Notification ID=20` `referenceId=28`, **`Instance 8081 = 10 lan | Instance 8091 = 10 lan`**), lưu [`docs/test/test-results/th7-service-discovery-run.txt`](docs/test/test-results/th7-service-discovery-run.txt).
   - Chạy lại [`test_th8_deployment.ps1`](test_th8_deployment.ps1) trên cụm Compose đang chạy -> **10/10 PASS (`DEP01`–`DEP06`, `SPORT01`–`SPORT04`)** trong [`docs/test/test-results/summary.json`](docs/test/test-results/summary.json) (eviction tự nhiên `10/10 -> 8091`, `503`, bền vững DB & RabbitMQ, cô lập `8/8` DB user, `X-Request-Id=sport04-trace-90810`), và ánh xạ `TC01–TC06` trong [`BAO_CAO_THUC_HANH_1_DEN_8.md`](BAO_CAO_THUC_HANH_1_DEN_8.md).

4. **Front-end (`api-gateway/src/main/resources/static`) — Sửa lỗi & Kiểm thử Chrome thật (`http://localhost:8080`):**
   - Sửa [`app.js`](api-gateway/src/main/resources/static/js/app.js) và [`styles.css`](api-gateway/src/main/resources/static/css/styles.css): hiển thị toast lỗi `5xx` khi tải danh sách phân trang (`fetchPagedResource`), thống nhất bộ lọc/biểu mẫu về `FINISHED` (vẫn hiển thị nhãn cho dữ liệu cũ `COMPLETED`), khóa nút xóa đội/VĐV đúng quyền `ADMIN` và phạm vi nút nhập/sửa kết quả theo trọng tài được phân công/BTC, bổ sung điều hướng hash URL (hiển thị trang `403` khi mở trực tiếp trang không đủ quyền), sửa lỗi runtime `ReferenceError: getStatusLabel is not defined` khi bấm `▶ Bắt đầu trận` bằng helper dùng chung `STATUS_LABEL_TONES`/`getStatusLabel`, rà soát `escapeHtml`/`.textContent` và thêm CSS `.btn-warning`.
   - Rebuild `api-gateway` (`healthy`) và kiểm chứng trực tiếp trên Chrome (`playwright-core` headless + DevTools Console/Network):
     - Đi qua đủ **14/14** trang: **`0` consoleErrors, `0` pageErrors**.
     - Kiểm tra đủ **5 vai trò** (`admin`, `organizer1`, `referee1`, `manager1`, `user1`): ẩn đúng menu/nút thao tác và trả trang `403` khi truy cập URL ngoài quyền.
     - Luồng nghiệp vụ E2E qua UI (`tournamentId=22`, `competitionId=21`, `matchId=34`, `resultId=24`): tỉ số `3 - 1`, trận chuyển `Đã kết thúc`, BXH cập nhật, trang Thông báo có `Trận đấu kết thúc`.
     - Kiểm tra biểu mẫu & `409`: trường bắt buộc rỗng không gửi form; ngày kết thúc trước ngày khai mạc hiện đúng lỗi tiếng Việt (`Ngày kết thúc phải sau hoặc bằng ngày khai mạc`); CCCD trùng (`409`) và xóa VĐV đang thuộc đội (`409`) hiện đúng thông báo backend và **nút submit không bị khóa**.
     - Kiểm tra sự cố: dừng `tournament-service` & `tournament-service-2` -> UI báo toast kèm `Mã yêu cầu: e798ebc8-8985-4dcb-bdce-5a3c9d51a103` khớp với log của `api-gateway`; sửa JWT giả mạo trong `localStorage` -> chuyển về `#/auth`, không lặp vô hạn (`navEventsCount: 6`).
     - Kiểm tra responsive (`1366px`, `768px`, `375px` — `docOverflowX: 0`, bảng cuộn ngang, menu mobile mở/đóng), theme (`dark`/`light`/`system`), chặn CDN (`cdn.jsdelivr.net`, `fonts.googleapis.com` — giao diện hoạt động bình thường). Ảnh chụp lưu tại [`docs/test/screenshots/frontend/`](docs/test/screenshots/frontend/).

## 8. Vòng 5 – sửa bằng chứng token hết hạn (TH5) và số đo khởi động (TH2) (18/11/2026)

1. **TH5 — Kiểm chứng tách biệt JWT ký đúng secret nhưng hết hạn (`401`) so với sai chữ ký (`TC09`):**
   - **JUnit (`match-service`):** Bổ sung `expiredTokenSignedWithCorrectSecretIsRejectedWith401` trong [`MatchSecurityAndConflictIntegrationTest.java`](match-service/src/test/java/com/sports/management/match/MatchSecurityAndConflictIntegrationTest.java): token ký bằng đúng `JWT_SECRET` nhưng `exp` đã qua bị từ chối `401` trên `PATCH /api/v1/matches/{id}`, đối chứng cùng user + cùng `JWT_SECRET` còn hạn trả `200` trên cả `PATCH` và `GET /api/v1/matches/{id}`. Chạy lại `.\mvnw.cmd test -q` toàn repo đạt **119/119 pass** (`match-service` `23/23`, `0` fail, `0` error, `0` skip).
   - **Postman / Newman (`security-design`):** Bổ sung `TC20b` (cùng script ký HMAC-SHA256 bằng `jwtSecret` thật, `exp = now + 3600` -> `200 OK`, `username = organizer1`) ngay trước `TC20` (cùng `jwtSecret`, `exp = now - 3600` -> `401 Unauthorized`), mở rộng cửa sổ `pastMatchTime` 72h tránh xung đột ca trực 00:00, và gọi `pm.environment.unset('jwtSecret')` ngay sau khi ký. Chạy Newman với `--env-var "jwtSecret=$s"` lấy trực tiếp từ `.env` đạt **47/47 request, 48/48 assertion pass** (`0` fail, avg `40 ms`); kiểm tra `Select-String -SimpleMatch $s` trên [`docs/test/test-results/security-th5-newman.json`](docs/test/test-results/security-th5-newman.json) cho **`0` kết quả** (không lộ secret). Cập nhật [`security-design/test-results.md`](security-design/test-results.md).

2. **TH2 — Đo thời gian khởi động Express từ lúc tiến trình Node bắt đầu (`process.uptime()`):**
   - Cập nhật [`server.js`](framework-comparison/express-tournament-consumer/src/server.js) đo `process.uptime() * 1000` (tương đương `Started ... in` từ lúc JVM chạy của Spring Boot) kèm `app init ... ms`.
   - Rebuild `express-consumer` (`docker compose --profile th2-demo up -d --build express-consumer`) và đo đối chiếu trên container thực tế:
     - Ngay sau `--build`: `Started on port 8095 in 320 ms (app init 9 ms)` (`StartedAt -> healthy`: `10.21 s`).
     - Recreate đơn lẻ trên hạ tầng ấm (`--force-recreate --no-deps`): Express đạt `Started on port 8095 in 140 ms (app init 6 ms)` (`StartedAt -> healthy`: `10.20 s`, `docker compose up -> healthy`: `14.20 s`); đối chứng Spring Boot (`match-service`) cùng điều kiện đạt `Started MatchServiceApplication in 16.571 seconds (process running for 18.947)` (`StartedAt -> healthy`: `20.90 s`, `docker compose up -> healthy`: `25.17 s`; khi khởi động đồng thời cả cụm: `44.855 seconds` / `72 s` healthy cho toàn cụm 13 container).
   - Cập nhật [`framework-comparison/so-sanh-framework.md`](framework-comparison/so-sanh-framework.md) và [`framework-comparison/ket-qua-chay.md`](framework-comparison/ket-qua-chay.md).

3. **Kiểm tra hồi quy:**
   - `npm test` (`express-tournament-consumer`): **6/6 pass** (`duration_ms 1474.6864`).
   - `test_th8_deployment.ps1` trên cụm Docker: **10/10 PASS (`DEP01`–`DEP06`, `SPORT01`–`SPORT04`)** (`Tournament ID=26`, `Notif ID=26`, `X-Request-Id=sport04-trace-85998`); `express-consumer` được bật lại `healthy` cùng 13 container chính (**14/14 healthy**).

## 9. Vòng 6 – front-end (18/11/2026)

1. **Sửa số đếm Dashboard, bổ sung Favicon SVG & Meta (`index.html`, `app.js`):**
   - [`app.js`](api-gateway/src/main/resources/static/js/app.js): Bảng `"Trận đấu Hôm nay"` chỉ lấy các trận có `matchTime` bắt đầu bằng ngày hôm nay theo giờ địa phương (`todayIso`); giá trị dự phòng của `todayCount` dùng `todayMatches.length` thay vì `d.matches.length`.
   - [`index.html`](api-gateway/src/main/resources/static/index.html): Nhúng favicon SVG trực tiếp (`data:image/svg+xml,...`) loại bỏ lỗi `404 /favicon.ico`, bổ sung `<meta name="description">` và `<meta name="theme-color" content="#1d4ed8">`.

2. **Bổ sung CSP & Security Headers cho giao diện tĩnh trên `api-gateway`:**
   - Thêm [`SecurityHeadersFilter.java`](api-gateway/src/main/java/com/sports/management/gateway/config/SecurityHeadersFilter.java) gắn 5 header bảo mật (`Content-Security-Policy`, `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Referrer-Policy: strict-origin-when-cross-origin`, `Permissions-Policy: camera=(), microphone=(), geolocation=()`) cho tài nguyên giao diện web, bỏ qua `/api/**` để không trùng lặp header của các microservice phía sau.
   - Bổ sung [`UiSecurityHeadersTest.java`](api-gateway/src/test/java/com/sports/management/gateway/UiSecurityHeadersTest.java) (2 test) và biến mẫu `EXPRESS_CONSUMER_PORT`, `EXPRESS_PROVIDER_TIMEOUT_MS` trong [`.env.example`](.env.example).
   - Chạy `.\mvnw.cmd test -q` toàn repo đạt **121/121 pass** (`0` fail, `0` error, `0` skip; riêng `api-gateway` `18/18`); `node --check api-gateway/src/main/resources/static/js/app.js` hợp lệ; rebuild container `api-gateway` đạt `healthy`; kiểm chứng `curl.exe -sI` trên `/index.html` có đủ 5 header bảo mật và trên `/api/v1/gateway/routes` không gắn `Content-Security-Policy`.

3. **Kiểm tra giao diện trên Chrome thật & 2 lỗi phát hiện/sửa ở mục 2.5 (`app.js`, `styles.css`):**
   - **CSP & Tài nguyên (`2.1`, `2.2`):** Đi qua đủ **14/14** trang trên 5 vai trò (`admin`, `organizer1`, `referee1`, `manager1`, `user1` — tổng cộng 34 lượt điều hướng, bật `CacheDisabled` như `Ctrl+F5`): **`0` vi phạm CSP** (`cspViolations = []`, không cần nới thêm nguồn nào trong `SecurityHeadersFilter`), **`0` request `/favicon.ico` lỗi**, **`0` `pageErrors`**. Chart.js (`2` canvas trên Dashboard), GSAP, font `Be Vietnam Pro` và chức năng xuất báo cáo JSON (`Bao_Cao_He_Thong_Sports_SOA_2026-11-18.json`, `291.672 bytes`) đều hoạt động bình thường.
   - **Dashboard & Luồng nghiệp vụ E2E (`2.3`, `2.4`):** Tạo trận đấu có giờ hôm nay (`TodayArena_131027`) và trận ngày khác (`FutureArena_131027`) -> bảng `"Trận đấu Hôm nay"` hiện đúng trận hôm nay và loại trừ trận ngày khác, ô `"Trận hôm nay"` khớp số lượng. Luồng đăng nhập/đăng xuất, tạo giải (`id=34`), tạo nội dung (`id=33`), đăng ký đội, xếp lịch, trọng tài `referee1` bắt đầu trận (`matchId=52`) + nhập tỷ số `2 - 0`, `organizer1` xác nhận kết quả (`resultId=31`) -> trận chuyển `"Đã kết thúc"`, BXH nội dung `33` và Thông báo cập nhật đầy đủ, không phát sinh lỗi JS.
   - **Phát hiện & sửa 2 lỗi ở mục `2.5` (giữ nguyên HTML/CSS/JS thuần):**
     1. *Bàn phím & Bẫy focus Modal (`app.js`):* Khi mở modal có hiệu ứng GSAP `fromTo(..., { autoAlpha: 0 }, ...)`, thuộc tính `visibility: hidden` ở frame đầu khiến `firstInput.focus()` không ăn (`initialFocusInsideModal = false`), đồng thời nhánh nhấn `Tab` xuôi chưa bắt trường hợp focus đang nằm ngoài `.modal-dialog` và bộ lọc `getFocusableElements` dùng `offsetParent !== null` kém ổn định trong lớp phủ `position: fixed`. **Cách sửa:** Đổi hiệu ứng mở modal sang `{ scale: 0.97, opacity: 0 } -> { scale: 1, opacity: 1 }`, kiểm tra hiển thị bằng `getClientRects().length > 0 && visibility !== 'hidden'`, và chặn cả `Tab` lẫn `Shift+Tab` khi `!dialog.contains(document.activeElement)` -> kiểm tra lại đạt `initialFocusInsideModal = true`, `focusStayedInsideOnTabAndShiftTab = true`, `Esc` đóng modal và trả focus về đúng nút `+ Tạo giải đấu`.
     2. *Tên dài ~80 ký tự không dấu cách làm tràn thẻ `.card` và vỡ khung `375px` (`styles.css`):* Với chuỗi ~80 ký tự liền không khoảng trắng trên trang Đội tuyển (`#/teams`), ở `1366px` chữ tràn khỏi khung `.card` (`overflowingCards = 1`) và ở `375px` quy tắc `@media` `grid-template-columns: 1fr` (`min-width: auto`) đẩy chiều ngang trang lên `846px` (`pageOverflowPx = 471px`). **Cách sửa:** Đổi lưới responsive sang `minmax(0, 1fr)` và thêm `min-width: 0; overflow-wrap: anywhere` cho `.card`, `.card-title` cùng `overflow-wrap: break-word` cho `.data-table td` -> kiểm tra lại ở cả `1366px` và `375px` đạt `pageOverflowPx = 0`, `overflowingCards = 0` cho cả tên tiếng Việt có khoảng trắng lẫn chuỗi liền ~80 ký tự.
   - **Trạng thái rỗng / đang tải / lỗi & Phân trang (`2.5`):** Cả 8 vùng danh sách phân trang hiển thị thông báo rỗng rõ ràng, không kẹt `"Đang tải"`; giả lập lỗi `503` trên `/api/v1/athletes/paged` hiển thị đúng `.state-banner.error` kèm mã yêu cầu và khôi phục dữ liệu khi bấm `"↻ Thử lại"`. Phân trang tự lùi từ trang `2/2` về trang `1/1` khi xóa phần tử cuối cùng trên trang cuối và tự quay về trang `1` (`page = 0`) khi đổi bộ lọc.





