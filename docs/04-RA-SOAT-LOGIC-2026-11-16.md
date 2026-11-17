# Báo cáo rà soát logic — 16/11/2026

## Kết luận

**Chưa nên chốt toàn bộ logic là hoàn tất.** Đã tái hiện 8 vấn đề ngoài các kịch bản thông thường, gồm 4 vấn đề bảo mật cần ưu tiên và 4 vấn đề nhất quán nghiệp vụ.

Kết quả 99/99 test tự động và 6/6 kịch bản TH7 của lần kiểm tra trước vẫn có giá trị với phạm vi chúng bao phủ. Chúng chưa bao phủ các tình huống dưới đây. Không có sửa đổi mã nghiệp vụ trong lần rà soát này.

## Phạm vi và bằng chứng

- Rà các điểm vào API, kiểm tra quyền và nhánh nghiệp vụ chính của auth, tournament, athlete, team, match, result, ranking, notification; rà gateway, registry và các đoạn JavaScript liên quan.
- Tái hiện qua cụm 10 module / 11 JVM, gồm hai tournament instance, trên cổng riêng 18080–18091 và 18761. Dùng H2 riêng cho từng dịch vụ; hai tournament instance dùng chung DB của riêng chúng. RabbitMQ dùng virtual host riêng.
- Lần chạy bổ sung có bằng chứng trong `logs/logic-audit-e2e.log` và `logs/th7-review-b602895de9/logic-findings.json`.
- Kiểm tra hàm render của giao diện và thực thi handler được sinh ra trong Node VM: `logs/check_username_ui.cjs`, kết quả `logs/logic-audit-ui.json`. Không thao tác trình duyệt để khai thác.
- Bộ Maven hiện có: 99 test, 0 failure, 0 error, 0 skipped; build/package thành công trong lần chạy ngay trước đợt rà logic, tại `logs/recheck-th8-build.log`. Không sửa mã ứng dụng sau lần build đó.
- Script tái hiện bổ sung: `logs/logic_audit_full.ps1` và `logs/logic_audit_cases.ps1`; chạy bằng PowerShell 7. Các script và DB tạm trong `logs/` không được Git theo dõi.

P1: cần xử lý sớm do ảnh hưởng phân quyền hoặc dữ liệu riêng tư. P2: lỗi nghiệp vụ cần sửa trước khi chốt nghiệm thu đầy đủ.

## 1. [P1] Hai username khác hoa/thường có thể dùng chung quyền sở hữu

**Bằng chứng:** tạo một tài khoản TEAM_MANAGER với username viết hoa của một tài khoản đã tồn tại. Đăng ký trả HTTP 201. Dùng token tài khoản mới PATCH đội của tài khoản cũ, API trả HTTP 200 và cập nhật thành công.

**Nguyên nhân:** `AuthService.register` kiểm tra tồn tại bằng `existsByUsername`, lưu username mà không thống nhất hoa/thường; kiểm tra ownership ở `TeamController.checkOwnership` dùng `equalsIgnoreCase`. Cùng kiểu đối chiếu còn xuất hiện ở athlete và registration.

**Vị trí:** `auth-service/src/main/java/com/sports/management/auth/service/AuthService.java:80` và `team-service/src/main/java/com/sports/management/team/controller/TeamController.java:273`.

**Ảnh hưởng:** tài khoản riêng biệt có thể sửa tài sản của người khác bằng cách đăng ký tên khác hoa/thường. Tái hiện trực tiếp trên đội; các chỗ dùng cùng quy tắc so sánh cần rà khi sửa.

**Hướng sửa:** thống nhất định danh username từ đăng ký/đăng nhập đến DB, có ràng buộc duy nhất cho định danh chuẩn hóa; ưu tiên lưu ownership theo user ID ổn định. Xử lý dữ liệu username trùng trước khi thêm constraint.

**Kiểm thử cần giữ:** đăng ký biến thể hoa/thường không tạo được danh tính có quyền với tài sản người khác; token của tài khoản khác phải bị từ chối truy cập tài sản đó.

## 2. [P1] Stored XSS tại nút “Đặt lại MK” của trang quản trị

**Bằng chứng:** API cho phép đăng ký username chứa dấu nháy đơn và đoạn JavaScript thử vô hại (HTTP 201). Dùng chính `renderUsersTableInner` trong mã nguồn để render, handler `onclick` tạo ra chạy được đoạn thử khi thực thi trong VM (`executed: true`).

**Điều kiện kích hoạt:** quản trị viên bấm nút “Đặt lại MK” trên dòng tài khoản có tên đặc biệt; không phải tự động chạy ngay khi tải trang.

**Nguyên nhân:** username được đặt trực tiếp trong chuỗi JavaScript của thuộc tính `onclick`. `escapeHtml` chỉ xử lý ngữ cảnh HTML, không bảo vệ ngữ cảnh chuỗi JavaScript; hàm hiện tại còn không escape dấu nháy đơn.

**Vị trí:** `api-gateway/src/main/resources/static/js/app.js:4060`, hàm `escapeHtml` tại dòng 404. `RegisterRequest` chỉ yêu cầu username không rỗng.

**Ảnh hưởng:** mã do người đăng ký cung cấp có thể chạy trong phiên giao diện của admin khi nút được bấm.

**Hướng sửa:** dùng event listener hoặc `data-username` và đọc `this.dataset.username`, tương tự các nút khác đã làm. Không chỉ thêm escape nháy đơn vào HTML rồi vẫn nhúng vào inline JavaScript.

## 3. [P1] Khóa tài khoản chưa chặn token đã cấp

**Bằng chứng:** admin khóa TEAM_MANAGER; đăng nhập mới bị trả HTTP 401, nhưng token được cấp trước khi khóa vẫn POST tạo đội thành công HTTP 201.

**Nguyên nhân:** filter tại các dịch vụ chỉ kiểm tra chữ ký/hạn token rồi lấy role từ token. `updateUserStatus` chỉ sửa cờ `active` trong auth DB. Không có kiểm tra hiệu lực phiên hoặc cơ chế thu hồi đồng bộ.

**Vị trí:** `team-service/src/main/java/com/sports/management/team/security/JwtAuthenticationFilter.java:37`; `auth-service/src/main/java/com/sports/management/auth/service/AuthService.java:173`. Auth controller cũng quyết định quyền admin từ role trong JWT tại `AuthController.java:85`.

**Ảnh hưởng:** thao tác khóa không có hiệu lực đối với các yêu cầu dùng token cũ, cho tới khi token hết hạn (cấu hình hiện tại 3600 giây). Việc đổi quyền/xóa tài khoản cũng cần kiểm tra cùng cơ chế; lần này chỉ tái hiện trực tiếp tình huống khóa.

**Hướng sửa:** xác thực trạng thái tài khoản/phiên tại gateway và mọi lối truy cập trực tiếp được phép, hoặc dùng token version/revocation có cơ chế phân phối đáng tin cậy. Thống nhất thời điểm hiệu lực của khóa/đổi quyền/reset mật khẩu.

## 4. [P1] Thông báo gửi riêng vẫn đọc được khi chưa đăng nhập

**Bằng chứng:** tạo thông báo với `recipient` là username cụ thể. Gọi GET theo ID không gửi Authorization vẫn nhận HTTP 200 và nguyên nội dung thông báo đó.

**Nguyên nhân:** toàn bộ GET `/api/v1/notifications/**` được `permitAll`. Các hàm lấy theo ID/danh sách chỉ tính trạng thái đã đọc, không lọc quyền xem theo recipient. Chỉ bước đánh dấu đã đọc mới kiểm tra recipient.

**Vị trí:** `notification-service/src/main/java/com/sports/management/notification/config/SecurityConfig.java:34`; `notification-service/src/main/java/com/sports/management/notification/service/NotificationService.java:100`, cả danh sách tại dòng 47.

**Hướng sửa:** chỉ công khai `ALL_USERS`; thông báo đích danh phải xác thực và kiểm tra đúng người nhận/quyền admin. Áp dụng nhất quán cho danh sách, theo ID, phân trang, tìm kiếm và thống kê.

## 5. [P2] Có thể phá điều kiện đội hình sau khi đăng ký đã được duyệt

**Bằng chứng:** hai đội đã APPROVED trong cùng nội dung. VĐV của đội thứ nhất được thêm vào đội thứ hai qua POST members, API trả HTTP 201.

**Nguyên nhân:** đăng ký kiểm tra trùng VĐV, nhưng `TeamController.addMember`/`TeamService.addMember` chỉ kiểm tra ownership, sức khỏe và trùng trong chính đội đang sửa. Không kiểm tra lại các đăng ký đang hiệu lực ở đội khác. Xóa thành viên cũng không khóa theo đăng ký/lịch thi đấu.

**Vị trí:** `team-service/src/main/java/com/sports/management/team/controller/TeamController.java:208`; `team-service/src/main/java/com/sports/management/team/service/TeamService.java`, phương thức `addMember`.

**Ảnh hưởng:** ràng buộc “mỗi VĐV không thuộc hai đội cùng nội dung” có thể bị vượt qua sau khi gửi hồ sơ. Quy tắc số thành viên cũng có thể bị thay đổi sau kiểm tra.

**Hướng sửa:** quản lý đội hình theo từng đăng ký/nội dung với thời điểm chốt, hoặc kiểm tra các đăng ký hiệu lực trước mọi thay đổi đội hình. Phải xác định rõ cách xử lý lịch sử và thay người hợp lệ.

## 6. [P2] Chuyển nội dung sang giải khác làm dữ liệu trận đấu lệch liên kết

**Bằng chứng:** nội dung đã có trận kết thúc và kết quả xác nhận. PUT chuyển `competition.tournamentId` sang giải khác trả HTTP 200; GET trận cũ vẫn trả `tournamentId` của giải ban đầu.

**Vị trí:** `tournament-service/src/main/java/com/sports/management/tournament/service/CompetitionService.java:95`, gán tournament ID tại dòng 101. `CompetitionController.updateCompetition` kiểm tra ownership hai giải nhưng không khóa quan hệ đã được sử dụng.

**Ảnh hưởng:** cùng một trận có competition thuộc giải B nhưng tournament của trận là A; lọc dữ liệu, kiểm tra ownership và báo cáo giữa các service có thể cho kết quả khác nhau.

**Hướng sửa:** chặn thay đổi các quan hệ định danh (tournament/sport) khi đã phát sinh hồ sơ/trận/kết quả, hoặc triển khai luồng chuyển đổi có kiểm soát và đồng bộ tất cả dữ liệu liên quan.

## 7. [P2] Xác nhận được kết quả của trận đã CANCELLED

**Bằng chứng:** tạo trận, chuyển ONGOING, ghi nháp kết quả; sau đó hủy trận rồi xác nhận kết quả. API trả HTTP 202, kết quả thành `CONFIRMED`, đồng bộ `FAILED`, trong khi trận vẫn `CANCELLED`.

**Nguyên nhân:** `recordResult` kiểm tra trạng thái trận, nhưng `confirmResult` chỉ lấy competition ID trước khi commit CONFIRMED. Việc đồng bộ sang match-service sau commit mới bị từ chối bởi state machine của trận.

**Vị trí:** `result-service/src/main/java/com/sports/management/result/service/ResultService.java:190`, commit tại dòng 210; điều kiện hợp lệ hiện chỉ được gọi trong record tại dòng 141.

**Hướng sửa:** kiểm tra lại trạng thái hợp lệ tại thời điểm xác nhận và thiết kế xử lý trường hợp trạng thái thay đổi đồng thời. Có thể bổ sung chặn hủy trận khi đã có kết quả cần xử lý, tùy quy tắc nghiệp vụ được chọn.

## 8. [P2] Duyệt hồ sơ không tái kiểm tra điều kiện thi đấu

**Bằng chứng:** đăng ký đội có một VĐV QUALIFIED, sau đó đổi VĐV sang SUSPENDED trước khi duyệt. PUT approve vẫn trả HTTP 200 và hồ sơ APPROVED.

**Nguyên nhân:** `approveRegistration` chỉ kiểm tra trạng thái PENDING, ownership, lịch thi đấu và giới hạn số participant; không kiểm tra lại sức khỏe/đội hình đã được đọc khi đăng ký.

**Vị trí:** `match-service/src/main/java/com/sports/management/match/service/RegistrationService.java:318`, chuyển APPROVED tại dòng 341.

**Hướng sửa:** tái kiểm tra dữ liệu điều kiện hiện hành lúc duyệt, hoặc duyệt dựa trên bản chốt hồ sơ có quy trình cập nhật rõ ràng. Khi dữ liệu sau đăng ký thay đổi, phải yêu cầu xác nhận lại/chặn hồ sơ không còn đủ điều kiện.

## Điểm bổ sung qua đọc mã, chưa thử sự cố thực tế trong lần này

**RabbitMQ có đường làm mất sự kiện:** `MatchEventProducer.publishMatchFinished` bắt exception và chỉ ghi log “skipped publishing” (dòng 36–41). Sự kiện phát sau DB commit, không có outbox hoặc tác vụ gửi lại trong đường xử lý này. `MatchService.publishIfFinished` chỉ phát khi chuyển lần đầu sang FINISHED (dòng 1323), nên gọi đồng bộ lại trên trận đã FINISHED cũng không tự phát lại sự kiện bị mất. Cần cơ chế lưu bền vững và retry/publisher confirmation nếu yêu cầu là không mất thông báo khi broker lỗi. Đây là kết luận từ luồng mã; chưa dừng broker dùng chung để tái hiện.

Các vấn đề script TH7 gốc đã báo trước (quản lý tiến trình, assertion, ngày cố định) vẫn chưa được sửa; lần này dùng harness tách biệt có kiểm tra chặt hơn.

## Phần đã kiểm tra hoạt động và giới hạn

- Luồng tạo giải/nội dung, đội/VĐV, đăng ký và duyệt hợp lệ, tạo trận, ghi/xác nhận kết quả, tính BXH và nhận sự kiện RabbitMQ hoạt động trong kịch bản bình thường.
- Bộ test hiện có bao phủ nhiều nhánh ownership, trường hợp ngày/đội/sân/trọng tài không hợp lệ, round-robin, knockout 4/5 đội, chia bảng, đồng bộ lại kết quả và chống trùng sự kiện.
- Kiểm thử TH7 kiểm tra Eureka, hai instance, phân phối request, tự eviction và failover, 503 khi thiếu provider, phục hồi không restart consumer.
- Chưa thực hiện stress test, kiểm chứng tất cả race condition bằng tải đồng thời, kiểm thử toàn bộ giao diện bằng trình duyệt hoặc chạy lại toàn bộ ca mới trên PostgreSQL. Không suy rộng kết quả H2 thành bằng chứng cho mọi khác biệt giao dịch PostgreSQL.

## Thứ tự xử lý đề xuất

1. Sửa định danh/ownership, điểm chèn mã giao diện, hiệu lực khóa tài khoản và quyền đọc notification.
2. Chốt quy tắc đội hình/hồ sơ, khóa liên kết competition đã sử dụng, bảo vệ bước confirm kết quả.
3. Giữ các ca đã tái hiện thành regression test; bổ sung cơ chế gửi lại sự kiện nếu bài yêu cầu khả năng chịu lỗi broker.
4. Chạy lại test và E2E trước khi chốt nền tảng cho thực hành 8.
