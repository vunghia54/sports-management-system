# THIẾT KẾ UML HỆ THỐNG QUẢN LÝ THI ĐẤU THỂ THAO THEO KIẾN TRÚC HƯỚNG DỊCH VỤ (SOA / MICROSERVICES)

**Đề tài:** Xây dựng và phát triển hệ thống quản lý thi đấu thể thao theo kiến trúc hướng dịch vụ  
**Cơ sở phân tích:** Chuỗi nghiệp vụ tổng quát 6 Microservices:  
`Tournament Service` ➔ `Team Service` ➔ `Athlete Service` ➔ `Match Service` ➔ `Result Service` ➔ `Ranking Service`  
**Nguyên tắc kiến trúc:** Database-per-service, Loose Coupling, Tham chiếu thực thể liên dịch vụ qua ID logic (không tạo Foreign Key vật lý chéo database).

---

## MỤC LỤC
1. [Kiến trúc Phân rã Dịch vụ & Ranh giới Nghiệp vụ](#1-kiến-trúc-phân-rã-dịch-vụ--ranh-giới-nghiệp-vụ)
2. [Chi tiết Các Lớp (Classes), Thuộc tính và Kiểu dữ liệu](#2-chi-tiết-các-lớp-classes-thuộc-tính-và-kiểu-dữ-liệu)
3. [Biểu đồ Lớp Tổng thể (System Class Diagram)](#3-biểu-đồ-lớp-tổng-thể-system-class-diagram)
4. [Biểu đồ Ca sử dụng (Use Case Diagram)](#4-biểu-đồ-ca-sử-dụng-use-case-diagram)
5. [Các Biểu đồ Tuần tự Trọng tâm (Sequence Diagrams)](#5-các-biểu-đồ-tuần-tự-trọng-tâm-sequence-diagrams)
   * 5.1. Đăng ký tham gia nội dung thi đấu (Registration Flow)
   * 5.2. Lập lịch thi đấu (Match Scheduling Flow)
   * 5.3. Ghi nhận kết quả và Cập nhật bảng xếp hạng (Result to Ranking Flow)
6. [Biểu đồ Hoạt động Toàn trình (Activity Diagram)](#6-biểu-đồ-hoạt-động-toàn-trình-activity-diagram)
7. [Sơ đồ Kiến trúc & Luồng Dữ liệu Liên Dịch vụ](#7-sơ-đồ-kiến-trúc--luồng-dữ-liệu-liên-dịch-vụ)

---

## 1. Kiến trúc Phân rã Dịch vụ & Ranh giới Nghiệp vụ

Theo kết quả phân tích nghiệp vụ, hệ thống gồm 6 Service độc lập tương ứng với 6 vùng trách nhiệm:

| STT | Tên Service | Trách nhiệm chính | Các thực thể (Entities) trực thuộc | Cơ sở dữ liệu riêng |
|:---:|---|---|---|---|
| **1** | **Tournament Service** | Quản lý thông tin giải đấu, danh mục môn thi đấu, các nội dung cụ thể trong giải, quy định điều lệ và thể thức thi đấu. | `Tournament`, `Sport`, `Competition`, `CompetitionRule`, `CompetitionFormat` | `tournament_db` |
| **2** | **Team Service** | Quản lý thông tin câu lạc bộ/đội thể thao và danh sách vận động viên thuộc đội. Không phụ thuộc vào giải đấu. | `Team`, `TeamMember` | `team_db` |
| **3** | **Athlete Service** | Quản lý hồ sơ vận động viên cá nhân (dùng chung cho cả thi đấu cá nhân và đồng đội). | `Athlete` | `athlete_db` |
| **4** | **Match Service** | Quản lý việc đăng ký nội dung, quản lý danh sách đối tượng tham gia (`Participant`), phân vòng (`Round`) và lập lịch trận đấu (`Match`). | `Registration`, `Participant`, `Round`, `Match` | `match_db` |
| **5** | **Result Service** | Tiếp nhận, lưu trữ chi tiết điểm số/thời gian và phê duyệt kết quả thi đấu chính thức. | `Result`, `ResultDetail` | `result_db` |
| **6** | **Ranking Service** | Tự động tính toán điểm số, hiệu số, và cập nhật bảng xếp hạng theo kết quả thi đấu đã xác nhận. | `Ranking`, `RankingItem` | `ranking_db` |

---

## 2. Chi tiết Các Lớp (Classes), Thuộc tính và Kiểu dữ liệu

### 2.1. Dịch vụ Quản lý Giải đấu (Tournament Service)

#### Lớp `Tournament` (Giải đấu)
* `id: Long` (PK) — Mã định danh duy nhất của giải đấu.
* `name: String` — Tên giải đấu (VD: "Hội thao Sinh viên 2026").
* `description: String` — Mô tả chi tiết, mục đích, đơn vị đăng cai.
* `startDate: LocalDate` — Ngày bắt đầu giải.
* `endDate: LocalDate` — Ngày bế mạc giải.
* `location: String` — Địa điểm tổ chức tổng thể.
* `registrationStartDate: LocalDate` — Thời gian mở cổng tiếp nhận đăng ký.
* `registrationEndDate: LocalDate` — Thời hạn đóng cổng đăng ký.
* `status: String` — Trạng thái giải (`PLANNING`, `OPEN_REGISTRATION`, `ONGOING`, `COMPLETED`, `CANCELLED`).

#### Lớp `Sport` (Môn thể thao)
* `id: Long` (PK) — Mã môn thể thao.
* `name: String` — Tên môn (VD: "Bóng đá", "Cầu lông", "Điền kinh", "Bơi lội").
* `sportType: String` — Phân loại hình thức (`TEAM`, `INDIVIDUAL`, `DUAL`).
* `description: String` — Mô tả tóm tắt quy chuẩn môn thi đấu.

#### Lớp `Competition` (Nội dung thi đấu trong giải)
* `id: Long` (PK) — Mã nội dung thi đấu.
* `tournamentId: Long` — Mã giải đấu sở hữu nội dung này.
* `sportId: Long` — Môn thể thao tương ứng.
* `name: String` — Tên nội dung (VD: "Bóng đá nam 11 người", "Cầu lông đơn nam", "Bơi tự do 100m").
* `status: String` — Trạng thái nội dung (`DRAFT`, `REGISTRATION_OPEN`, `SCHEDULED`, `ONGOING`, `FINISHED`).

#### Lớp `CompetitionRule` (Điều lệ thi đấu)
* `id: Long` (PK) — Mã bản điều lệ.
* `competitionId: Long` (FK nội bộ) — Thuộc nội dung thi đấu nào.
* `maxParticipants: Integer` — Số lượng đối tượng (đội hoặc VĐV) tối đa tham gia.
* `minTeamMembers: Integer` — Số VĐV tối thiểu của 1 đội (nếu là môn đồng đội).
* `maxTeamMembers: Integer` — Số VĐV tối đa đăng ký trong 1 đội.
* `registrationDeadline: LocalDateTime` — Hạn chót hoàn tất hồ sơ đăng ký.
* `scoringMethod: String` — Cách tính điểm (`STANDARD_POINTS`, `TIME_BASED`, `SCORE_BASED`).
* `eligibilityCriteria: String` — Tiêu chuẩn điều kiện tham dự (độ tuổi, khoa/viện, chứng chỉ y tế).

#### Lớp `CompetitionFormat` (Thể thức thi đấu)
* `id: Long` (PK) — Mã thể thức.
* `competitionId: Long` (FK nội bộ) — Thuộc nội dung thi đấu nào.
* `formatType: String` — Loại thể thức (`ROUND_ROBIN`, `KNOCKOUT`, `GROUP_STAGE`, `HYBRID_GROUP_KNOCKOUT`).
* `numberOfGroups: Integer` — Số lượng bảng đấu (nếu có thi đấu vòng bảng).
* `qualifyingSpotsPerGroup: Integer` — Số lượng đối tượng nhất/nhì mỗi bảng được vào vòng sau.
* `hasThirdPlaceMatch: Boolean` — Có tổ chức trận tranh hạng ba hay không.

---

### 2.2. Dịch vụ Quản lý Đội (Team Service)

#### Lớp `Team` (Đội thi đấu)
* `id: Long` (PK) — Mã định danh đội bóng/câu lạc bộ.
* `name: String` — Tên đầy đủ của đội (VD: "CNTT FC", "Đội tuyển Điền kinh Kỹ thuật").
* `shortName: String` — Tên viết tắt (VD: "CNTT", "BK-ATHLETICS").
* `managerName: String` — Họ tên Huấn luyện viên hoặc Trưởng đoàn phụ trách.
* `contactPhone: String` — Số điện thoại liên hệ.
* `contactEmail: String` — Email tiếp nhận thông báo chính thức.
* `logoUrl: String` — Đường dẫn ảnh đại diện/logo đội.
* `createdAt: LocalDateTime` — Ngày thành lập/khởi tạo đội trên hệ thống.

#### Lớp `TeamMember` (Thành viên đội)
* `id: Long` (PK) — Mã bản ghi thành viên.
* `teamId: Long` (FK nội bộ) — Thuộc đội nào.
* `athleteId: Long` (ID tham chiếu Athlete Service) — Mã vận động viên liên kết.
* `jerseyNumber: Integer` — Số áo thi đấu.
* `position: String` — Vị trí thi đấu (VD: "Tiền đạo", "Thủ môn", "Chuyền hai").
* `joinDate: LocalDate` — Ngày gia nhập đội.
* `status: String` — Trạng thái thành viên (`ACTIVE`, `INACTIVE`, `INJURED`).

---

### 2.3. Dịch vụ Quản lý Vận động viên (Athlete Service)

#### Lớp `Athlete` (Vận động viên)
* `id: Long` (PK) — Mã định danh vận động viên.
* `fullName: String` — Họ và tên đầy đủ.
* `dateOfBirth: LocalDate` — Ngày tháng năm sinh.
* `gender: String` — Giới tính (`MALE`, `FEMALE`, `OTHER`).
* `identityCardNumber: String` — Số CCCD / Hộ chiếu / Thẻ sinh viên.
* `phone: String` — Số điện thoại cá nhân.
* `email: String` — Email liên hệ cá nhân.
* `nationality: String` — Quốc tịch.
* `avatarUrl: String` — Ảnh chân dung vận động viên.
* `healthStatus: String` — Tình trạng y tế, chứng nhận sức khỏe.

---

### 2.4. Dịch vụ Quản lý Trận đấu (Match Service)

#### Lớp `Registration` (Đăng ký nội dung thi đấu)
* `id: Long` (PK) — Mã đơn đăng ký.
* `competitionId: Long` (ID tham chiếu Tournament Service) — Đăng ký vào nội dung nào.
* `participantType: String` — Loại đối tượng đăng ký (`TEAM` hoặc `ATHLETE`).
* `participantReferenceId: Long` — ID của Đội (Team Service) hoặc VĐV (Athlete Service).
* `participantName: String` — Snapshot tên hiển thị tại thời điểm đăng ký.
* `registrationDate: LocalDateTime` — Thời gian nộp đơn đăng ký.
* `status: String` — Trạng thái duyệt (`PENDING`, `APPROVED`, `REJECTED`, `WITHDRAWN`).
* `rejectionReason: String` — Lý do nếu đơn bị từ chối.

#### Lớp `Participant` (Đối tượng thi đấu chính thức)
* `id: Long` (PK) — Mã đối tượng thi đấu trong nội dung.
* `competitionId: Long` (ID tham chiếu Tournament Service) — Thuộc nội dung thi đấu.
* `registrationId: Long` (FK nội bộ) — Gắn với đơn đăng ký đã được duyệt (`APPROVED`).
* `participantType: String` — Phân loại (`TEAM` hoặc `ATHLETE`).
* `referenceId: Long` — Team ID hoặc Athlete ID gốc.
* `displayName: String` — Tên hiển thị trong giải (VD: "CNTT FC", "Nguyễn Văn An").
* `seedNumber: Integer` — Hạt giống thi đấu (nếu có xếp hạt giống bốc thăm).

#### Lớp `Round` (Vòng thi đấu)
* `id: Long` (PK) — Mã vòng thi đấu.
* `competitionId: Long` (ID tham chiếu Tournament Service) — Thuộc nội dung thi đấu.
* `roundName: String` — Tên giai đoạn (`Vòng bảng`, `Vòng 16 đội`, `Tứ kết`, `Bán kết`, `Chung kết`).
* `roundOrder: Integer` — Thứ tự diễn ra của vòng (1, 2, 3...).
* `status: String` — Trạng thái vòng (`PENDING`, `IN_PROGRESS`, `COMPLETED`).

#### Lớp `Match` (Trận/Lượt thi đấu)
* `id: Long` (PK) — Mã trận đấu.
* `competitionId: Long` (ID tham chiếu Tournament Service) — Thuộc nội dung thi đấu.
* `roundId: Long` (FK nội bộ) — Thuộc vòng thi đấu nào.
* `participant1Id: Long` (FK tới Participant) — Đối tượng thi đấu 1 (Đội 1 hoặc VĐV 1).
* `participant2Id: Long` (FK tới Participant) — Đối tượng thi đấu 2 (Đội 2 hoặc VĐV 2).
* `matchTime: LocalDateTime` — Thời gian diễn ra trận đấu.
* `location: String` — Địa điểm/sân thi đấu cụ thể (VD: "Sân bóng đá số 1", "Bàn 3 Nhà thi đấu").
* `status: String` — Trạng thái trận (`SCHEDULED`, `IN_PROGRESS`, `FINISHED`, `POSTPONED`, `CANCELLED`).

---

### 2.5. Dịch vụ Quản lý Kết quả (Result Service)

#### Lớp `Result` (Kết quả chung cuộc trận đấu)
* `id: Long` (PK) — Mã kết quả.
* `matchId: Long` (ID tham chiếu Match Service) — Mã trận đấu có kết quả.
* `competitionId: Long` — Mã nội dung thi đấu (phục vụ đối soát).
* `winnerParticipantId: Long` (Nullable) — Mã đối tượng chiến thắng (null nếu hòa).
* `isDraw: Boolean` — Có kết quả hòa hay không.
* `status: String` — Trạng thái biên bản (`DRAFT`, `CONFIRMED`, `DISPUTED`).
* `confirmedBy: String` — Tên/Mã trọng tài hoặc giám sát viên xác nhận kết quả.
* `confirmedAt: LocalDateTime` — Thời điểm biên bản kết quả được ký duyệt.

#### Lớp `ResultDetail` (Chi tiết kết quả từng đối tượng)
* `id: Long` (PK) — Mã chi tiết kết quả.
* `resultId: Long` (FK nội bộ) — Thuộc biên bản kết quả nào.
* `participantId: Long` — Mã đối tượng thi đấu (`Participant`).
* `score: Double` — Điểm số / Bàn thắng (VD: 3 bàn trong bóng đá, 21 điểm trong cầu lông).
* `timeRecord: Double` — Thời gian thi đấu tính bằng giây (VD: 10.81s môn điền kinh, bơi lội).
* `rankInMatch: Integer` — Thứ hạng trong lượt thi đấu (1, 2, 3...).
* `extraInfo: String` — Thông tin bổ sung (thẻ vàng, thẻ đỏ, lỗi kỹ thuật, điểm set).

---

### 2.6. Dịch vụ Quản lý Bảng xếp hạng (Ranking Service)

#### Lớp `Ranking` (Bảng xếp hạng nội dung)
* `id: Long` (PK) — Mã bảng xếp hạng.
* `competitionId: Long` (ID tham chiếu Tournament Service) — Bảng xếp hạng của nội dung thi đấu nào.
* `groupName: String` — Tên bảng nếu thi đấu chia bảng (VD: "Bảng A", "Bảng B", "Chung cuộc").
* `lastUpdatedAt: LocalDateTime` — Thời điểm cập nhật bảng xếp hạng gần nhất.
* `status: String` — Trạng thái (`ACTIVE`, `FINALIZED`).

#### Lớp `RankingItem` (Dòng xếp hạng của từng đối tượng)
* `id: Long` (PK) — Mã dòng xếp hạng.
* `rankingId: Long` (FK nội bộ) — Thuộc bảng xếp hạng nào.
* `participantId: Long` — Mã đối tượng thi đấu (`Participant`).
* `participantName: String` — Tên đối tượng hiển thị trên bảng xếp hạng.
* `rankPosition: Integer` — Vị trí xếp hạng hiện tại (1, 2, 3...).
* `matchesPlayed: Integer` — Số trận/lượt đã thi đấu.
* `matchesWon: Integer` — Số trận thắng.
* `matchesDrawn: Integer` — Số trận hòa.
* `matchesLost: Integer` — Số trận thua.
* `points: Integer` — Tổng điểm tích lũy (theo quy chế: Thắng 3, Hòa 1, Thua 0).
* `goalsFor: Integer` — Tổng bàn thắng/điểm ghi được.
* `goalsAgainst: Integer` — Tổng bàn thua/điểm bị đối phương ghi.
* `goalDifference: Integer` — Hiệu số bàn thắng/bại (`goalsFor - goalsAgainst`).

---

## 3. Biểu đồ Lớp Tổng thể (System Class Diagram)

Biểu đồ dưới đây thể hiện toàn bộ 6 Service boundaries và các quan hệ nội bộ lẫn quan hệ tham chiếu liên dịch vụ:

```mermaid
classDiagram
    %% ==========================================
    %% 1. TOURNAMENT SERVICE
    %% ==========================================
    namespace Tournament_Service {
        class Tournament {
            +Long id
            +String name
            +String description
            +LocalDate startDate
            +LocalDate endDate
            +String location
            +LocalDate registrationStartDate
            +LocalDate registrationEndDate
            +String status
        }

        class Sport {
            +Long id
            +String name
            +String sportType
            +String description
        }

        class Competition {
            +Long id
            +Long tournamentId
            +Long sportId
            +String name
            +String status
        }

        class CompetitionRule {
            +Long id
            +Long competitionId
            +Integer maxParticipants
            +Integer minTeamMembers
            +Integer maxTeamMembers
            +LocalDateTime registrationDeadline
            +String scoringMethod
            +String eligibilityCriteria
        }

        class CompetitionFormat {
            +Long id
            +Long competitionId
            +String formatType
            +Integer numberOfGroups
            +Integer qualifyingSpotsPerGroup
            +Boolean hasThirdPlaceMatch
        }
    }

    Tournament "1" *-- "*" Competition : contains
    Sport "1" <-- "*" Competition : classified by
    Competition "1" *-- "1" CompetitionRule : defines
    Competition "1" *-- "1" CompetitionFormat : uses

    %% ==========================================
    %% 2. TEAM SERVICE
    %% ==========================================
    namespace Team_Service {
        class Team {
            +Long id
            +String name
            +String shortName
            +String managerName
            +String contactPhone
            +String contactEmail
            +String logoUrl
            +LocalDateTime createdAt
        }

        class TeamMember {
            +Long id
            +Long teamId
            +Long athleteId
            +Integer jerseyNumber
            +String position
            +LocalDate joinDate
            +String status
        }
    }

    Team "1" *-- "*" TeamMember : consists of

    %% ==========================================
    %% 3. ATHLETE SERVICE
    %% ==========================================
    namespace Athlete_Service {
        class Athlete {
            +Long id
            +String fullName
            +LocalDate dateOfBirth
            +String gender
            +String identityCardNumber
            +String phone
            +String email
            +String nationality
            +String avatarUrl
            +String healthStatus
        }
    }

    Athlete "1" <.. "*" TeamMember : references by athleteId

    %% ==========================================
    %% 4. MATCH SERVICE
    %% ==========================================
    namespace Match_Service {
        class Registration {
            +Long id
            +Long competitionId
            +String participantType
            +Long participantReferenceId
            +String participantName
            +LocalDateTime registrationDate
            +String status
            +String rejectionReason
        }

        class Participant {
            +Long id
            +Long competitionId
            +Long registrationId
            +String participantType
            +Long referenceId
            +String displayName
            +Integer seedNumber
        }

        class Round {
            +Long id
            +Long competitionId
            +String roundName
            +Integer roundOrder
            +String status
        }

        class Match {
            +Long id
            +Long competitionId
            +Long roundId
            +Long participant1Id
            +Long participant2Id
            +LocalDateTime matchTime
            +String location
            +String status
        }
    }

    Registration "1" <-- "0..1" Participant : generates upon approval
    Round "1" *-- "*" Match : organizes
    Participant "1" <-- "*" Match : participant1
    Participant "1" <-- "*" Match : participant2

    %% Loose Cross-service reference to Match
    Competition "1" <.. "*" Registration : references competitionId
    Competition "1" <.. "*" Match : references competitionId
    Team "1" <.. "*" Registration : references participantReferenceId
    Athlete "1" <.. "*" Registration : references participantReferenceId

    %% ==========================================
    %% 5. RESULT SERVICE
    %% ==========================================
    namespace Result_Service {
        class Result {
            +Long id
            +Long matchId
            +Long competitionId
            +Long winnerParticipantId
            +Boolean isDraw
            +String status
            +String confirmedBy
            +LocalDateTime confirmedAt
        }

        class ResultDetail {
            +Long id
            +Long resultId
            +Long participantId
            +Double score
            +Double timeRecord
            +Integer rankInMatch
            +String extraInfo
        }
    }

    Result "1" *-- "1..*" ResultDetail : contains
    Match "1" <.. "1" Result : records for matchId

    %% ==========================================
    %% 6. RANKING SERVICE
    %% ==========================================
    namespace Ranking_Service {
        class Ranking {
            +Long id
            +Long competitionId
            +String groupName
            +LocalDateTime lastUpdatedAt
            +String status
        }

        class RankingItem {
            +Long id
            +Long rankingId
            +Long participantId
            +String participantName
            +Integer rankPosition
            +Integer matchesPlayed
            +Integer matchesWon
            +Integer matchesDrawn
            +Integer matchesLost
            +Integer points
            +Integer goalsFor
            +Integer goalsAgainst
            +Integer goalDifference
        }
    }

    Ranking "1" *-- "1..*" RankingItem : computes
    Competition "1" <.. "1" Ranking : references competitionId
    Result "1" <.. "1" Ranking : calculated from confirmed result
```

---

## 4. Biểu đồ Ca sử dụng (Use Case Diagram)

Dựa trên 4 đối tượng sử dụng hệ thống được phân tích trong đề tài:
* **Administrator (Quản trị viên):** Cấu hình danh mục thể thao, giám sát hệ thống.
* **Organizer (Ban tổ chức):** Tạo giải đấu, thiết lập nội dung thi đấu, phê duyệt đăng ký, bốc thăm xếp lịch, xác nhận kết quả và chốt bảng xếp hạng.
* **Team Manager (Trưởng đoàn / Quản lý đội):** Quản lý hồ sơ đội, đăng ký danh sách đội vào các nội dung đồng đội.
* **Athlete (Vận động viên):** Cập nhật hồ sơ cá nhân, đăng ký trực tiếp các nội dung cá nhân, theo dõi lịch và thứ hạng.

```mermaid
flowchart LR
    %% Actors
    Admin["Quản trị viên (Administrator)"]
    Org["Ban tổ chức (Organizer)"]
    TM["Quản lý đội (Team Manager)"]
    Ath["Vận động viên (Athlete)"]

    subgraph SportsSystem["Hệ thống Quản lý Thi đấu Thể thao (Sports Management System)"]
        %% Tournament & Competition
        UC_Sport(["UC01: Quản lý danh mục Môn thể thao (Sport)"])
        UC_Tour(["UC02: Khởi tạo & Quản lý Giải đấu (Tournament)"])
        UC_Comp(["UC03: Thiết lập Nội dung thi đấu (Competition)"])
        UC_Rule(["UC04: Thiết lập Điều lệ & Thể thức thi đấu"])

        %% Team & Athlete
        UC_Team(["UC05: Quản lý Hồ sơ Đội & Thành viên (Team)"])
        UC_AthProfile(["UC06: Quản lý Hồ sơ Vận động viên (Athlete)"])

        %% Registration & Participant
        UC_RegTeam(["UC07: Đăng ký nội dung đồng đội"])
        UC_RegIndiv(["UC08: Đăng ký nội dung cá nhân"])
        UC_ApproveReg(["UC09: Xét duyệt đăng ký & Lập danh sách Participant"])

        %% Match & Schedule
        UC_Schedule(["UC10: Xếp lịch & Phân vòng thi đấu (Round & Match)"])
        UC_ViewMatch(["UC11: Tra cứu lịch thi đấu & sân bãi"])

        %% Result & Ranking
        UC_InputResult(["UC12: Ghi nhận biên bản kết quả thi đấu"])
        UC_ConfirmResult(["UC13: Xác nhận & Phê duyệt kết quả chính thức"])
        UC_ViewRanking(["UC14: Xem bảng xếp hạng & Thống kê kết quả"])
    end

    %% Admin
    Admin --> UC_Sport
    Admin --> UC_Tour

    %% Organizer
    Org --> UC_Tour
    Org --> UC_Comp
    Org --> UC_Rule
    Org --> UC_ApproveReg
    Org --> UC_Schedule
    Org --> UC_ConfirmResult
    Org --> UC_ViewRanking

    %% Team Manager
    TM --> UC_Team
    TM --> UC_RegTeam
    TM --> UC_ViewMatch
    TM --> UC_ViewRanking

    %% Athlete
    Ath --> UC_AthProfile
    Ath --> UC_RegIndiv
    Ath --> UC_ViewMatch
    Ath --> UC_ViewRanking
```

---

## 5. Các Biểu đồ Tuần tự Trọng tâm (Sequence Diagrams)

### 5.1. Luồng 1: Đăng ký tham gia nội dung thi đấu (Registration Flow)
Thể hiện cách thức Team Manager hoặc Athlete đăng ký vào một nội dung thi đấu thuộc Tournament Service, thông qua việc kiểm tra chéo điều lệ:

```mermaid
sequenceDiagram
    autonumber
    actor TM as Quản lý đội / VĐV
    actor Org as Ban tổ chức
    participant MatchSvc as Match Service (Registration Component)
    participant TourSvc as Tournament Service
    participant TeamSvc as Team / Athlete Service
    participant DB as Match Database

    TM->>MatchSvc: POST /api/registrations (competitionId, participantReferenceId, type)
    activate MatchSvc

    %% Verify Competition & Rules
    MatchSvc->>TourSvc: GET /api/competitions/{id}/rule (Đồng bộ qua REST)
    activate TourSvc
    TourSvc-->>MatchSvc: Trả về Rule (Hạn đăng ký, số lượng tối đa)
    deactivate TourSvc

    %% Verify Team/Athlete validity
    MatchSvc->>TeamSvc: GET /api/teams/{id} hoặc /api/athletes/{id}
    activate TeamSvc
    TeamSvc-->>MatchSvc: Thông tin hợp lệ (Đủ thành viên, trạng thái ACTIVE)
    deactivate TeamSvc

    %% Save Pending Registration
    MatchSvc->>DB: INSERT INTO registrations (status: "PENDING")
    DB-->>MatchSvc: Saved (ID = 101)
    MatchSvc-->>TM: 201 Created (Đơn đăng ký đang chờ duyệt)
    deactivate MatchSvc

    %% Organizer approves registration
    Note over Org, MatchSvc: Ban tổ chức kiểm tra và phê duyệt
    Org->>MatchSvc: PUT /api/registrations/101/approve
    activate MatchSvc
    MatchSvc->>DB: UPDATE registrations SET status = "APPROVED"
    MatchSvc->>DB: INSERT INTO participants (competitionId, registrationId, referenceId)
    DB-->>MatchSvc: Participant Created (ID = P_01)
    MatchSvc-->>Org: 200 OK (Chính thức trở thành Participant của nội dung)
    deactivate MatchSvc
```

---

### 5.2. Luồng 2: Lập lịch thi đấu (Match Scheduling Flow)
Ban tổ chức tiến hành bốc thăm, chia vòng và lập lịch trận đấu cho các đối tượng đã được duyệt:

```mermaid
sequenceDiagram
    autonumber
    actor Org as Ban tổ chức
    participant MatchSvc as Match Service
    participant TourSvc as Tournament Service
    participant DB as Match Database

    Org->>MatchSvc: POST /api/competitions/{id}/matches/schedule
    activate MatchSvc

    %% Check Format
    MatchSvc->>TourSvc: GET /api/competitions/{id}/format
    activate TourSvc
    TourSvc-->>MatchSvc: Trả về thể thức (KNOCKOUT hoặc ROUND_ROBIN)
    deactivate TourSvc

    %% Load approved participants
    MatchSvc->>DB: SELECT * FROM participants WHERE competition_id = {id}
    DB-->>MatchSvc: Danh sách Participants (P_01, P_02, P_03, P_04)

    %% Create Round and Matches
    Note over MatchSvc: Tự động ghép cặp theo thuật toán thể thức
    MatchSvc->>DB: INSERT INTO rounds (roundName: "Bán kết", roundOrder: 1)
    DB-->>MatchSvc: Round ID = R_1

    MatchSvc->>DB: INSERT INTO matches (roundId, p1Id: P_01, p2Id: P_02, matchTime, status: "SCHEDULED")
    MatchSvc->>DB: INSERT INTO matches (roundId, p1Id: P_03, p2Id: P_04, matchTime, status: "SCHEDULED")
    DB-->>MatchSvc: Matches Created

    MatchSvc-->>Org: 201 Created (Danh sách lịch thi đấu đã được thiết lập)
    deactivate MatchSvc
```

---

### 5.3. Luồng 3: Ghi nhận kết quả và Tự động Cập nhật Bảng xếp hạng (Result to Ranking Flow)
Minh họa giao tiếp giữa **Match Service** ➔ **Result Service** ➔ **Ranking Service** (phù hợp với nội dung TH2 về Service-to-Service Communication):

```mermaid
sequenceDiagram
    autonumber
    actor Referee as Trọng tài / Giám sát
    participant MatchSvc as Match Service
    participant ResultSvc as Result Service
    participant RankingSvc as Ranking Service
    participant RabbitMQ as Message Broker / Event Bus

    %% Step 1: Input Result
    Referee->>ResultSvc: POST /api/results (matchId, p1Score, p2Score, winnerId)
    activate ResultSvc
    ResultSvc->>ResultSvc: Lưu biên bản nháp (status: "DRAFT")
    ResultSvc-->>Referee: 201 Created

    %% Step 2: Confirm Result
    Referee->>ResultSvc: PUT /api/results/{id}/confirm
    ResultSvc->>ResultSvc: Chuyển status = "CONFIRMED"

    %% Step 3: Update Match status to FINISHED
    rect rgb(235, 245, 255)
        Note over ResultSvc, MatchSvc: Gọi đồng bộ cập nhật trạng thái trận đấu
        ResultSvc->>MatchSvc: PATCH /api/matches/{matchId} (status = "FINISHED")
        MatchSvc-->>ResultSvc: 200 OK
    end

    %% Step 4: Trigger Event to Ranking Service
    rect rgb(230, 255, 240)
        Note over ResultSvc, RankingSvc: Giao tiếp bất đồng bộ qua Sự kiện (TH2 & TH6)
        ResultSvc->>RabbitMQ: Publish Event: "RESULT_CONFIRMED"<br/>{competitionId, winnerId, scores, matchId}
        ResultSvc-->>Referee: 200 OK (Kết quả thi đấu đã được duyệt chính thức)
        deactivate ResultSvc

        RabbitMQ->>RankingSvc: Deliver Event: "RESULT_CONFIRMED"
        activate RankingSvc
        Note over RankingSvc: Tính lại điểm số, số trận thắng/hòa/thua, hiệu số
        RankingSvc->>RankingSvc: UPDATE ranking_items SET points = points + 3, ...
        RankingSvc->>RankingSvc: Sắp xếp lại thứ tự (Rank Position)
        deactivate RankingSvc
    end
```

---

## 6. Biểu đồ Hoạt động Toàn trình (Activity Diagram)

Mô tả tiến trình từ khi khởi tạo giải đấu cho đến khi khép lại với bảng xếp hạng chung cuộc:

```mermaid
flowchart TD
    Start([Bắt đầu]) --> TaoGiai["Khởi tạo Giải đấu (Tournament)"]
    TaoGiai --> ThietLapMon["Thiết lập Môn & Nội dung thi đấu (Competition)"]
    ThietLapMon --> ThietLapDieuLe["Ban hành Điều lệ & Thể thức (Rule & Format)"]
    ThietLapDieuLe --> MoCongDangKy["Mở cổng Đăng ký (Open Registration)"]

    MoCongDangKy --> NopDon{"Đội / Vận động viên<br/>nộp hồ sơ đăng ký"}
    NopDon --> KiemTraHoSo{"Ban tổ chức<br/>kiểm tra điều kiện?"}

    KiemTraHoSo -- "Không hợp lệ / Quá hạn" --> TuChoi["Từ chối đăng ký (REJECTED)"] --> KetThucDon([Hết quyền tham gia])
    KiemTraHoSo -- "Đủ điều kiện" --> PheDuyet["Phê duyệt (APPROVED)<br/>Tạo bản ghi Participant"]

    PheDuyet --> DongCongDangKy{"Hết thời hạn đăng ký?"}
    DongCongDangKy -- Chưa --> NopDon
    DongCongDangKy -- Đã đóng --> BocThamXepLich["Bốc thăm chia bảng & Xếp lịch thi đấu (Round & Match)"]

    BocThamXepLich --> ToChucThiDau["Tổ chức các trận thi đấu (Match Ongoing)"]
    ToChucThiDau --> GhiNhanKetQua["Trọng tài lập biên bản Kết quả (Result & Detail)"]
    GhiNhanKetQua --> XacNhanKetQua{"Xác nhận kết quả chính thức?"}

    XacNhanKetQua -- "Có khiếu nại (DISPUTED)" --> GhiNhanKetQua
    XacNhanKetQua -- "Chấp thuận (CONFIRMED)" --> CapNhatBXH["Tự động tính điểm & Cập nhật Bảng xếp hạng (Ranking)"]

    CapNhatBXH --> KiemTraHetTran{"Còn trận đấu tiếp theo trong giải?"}
    KiemTraHetTran -- Còn --> ToChucThiDau
    KiemTraHetTran -- Hết --> TongKetGiai["Chốt Bảng xếp hạng chung cuộc & Trao giải"]
    TongKetGiai --> End([Kết thúc Giải đấu])
```

---

## 7. Sơ đồ Kiến trúc & Luồng Dữ liệu Liên Dịch vụ

Minh họa cách thức 6 dịch vụ duy trì cơ sở dữ liệu riêng biệt và trao đổi thông tin bằng định dạng RESTful JSON và Message Bus:

```mermaid
graph TD
    subgraph ClientZone["Tầng Người dùng & Giao diện"]
        ClientApp["Postman / Web Browser / Mobile App"]
    end

    subgraph ServiceMesh["Tầng 6 Microservices Độc lập"]
        subgraph Svc1["Tournament Service (Port 8081)"]
            T_Ctrl["TournamentController"]
            T_DB[("tournament_db<br/>- tournaments<br/>- sports<br/>- competitions<br/>- rules<br/>- formats")]
        end

        subgraph Svc2["Team Service (Port 8085)"]
            TM_Ctrl["TeamController"]
            TM_DB[("team_db<br/>- teams<br/>- team_members")]
        end

        subgraph Svc3["Athlete Service (Port 8086)"]
            A_Ctrl["AthleteController"]
            A_DB[("athlete_db<br/>- athletes")]
        end

        subgraph Svc4["Match Service (Port 8082)"]
            M_Ctrl["MatchController"]
            M_DB[("match_db<br/>- registrations<br/>- participants<br/>- rounds<br/>- matches")]
        end

        subgraph Svc5["Result Service (Port 8087)"]
            Res_Ctrl["ResultController"]
            Res_DB[("result_db<br/>- results<br/>- result_details")]
        end

        subgraph Svc6["Ranking Service (Port 8088)"]
            Rnk_Ctrl["RankingController"]
            Rnk_DB[("ranking_db<br/>- rankings<br/>- ranking_items")]
        end
    end

    subgraph AsyncBroker["Tầng Message Broker (Event-Driven)"]
        RabbitMQ{{"RabbitMQ Broker<br/>Exchange: sports.event.exchange"}}
    end

    %% Client Interactions
    ClientApp -->|HTTP REST| T_Ctrl
    ClientApp -->|HTTP REST| TM_Ctrl
    ClientApp -->|HTTP REST| A_Ctrl
    ClientApp -->|HTTP REST| M_Ctrl
    ClientApp -->|HTTP REST| Res_Ctrl
    ClientApp -->|HTTP REST| Rnk_Ctrl

    %% Internal Data stores
    T_Ctrl --- T_DB
    TM_Ctrl --- TM_DB
    A_Ctrl --- A_DB
    M_Ctrl --- M_DB
    Res_Ctrl --- Res_DB
    Rnk_Ctrl --- Rnk_DB

    %% Inter-service Sync Calls
    M_Ctrl -.->|1. HTTP GET verify competition & rule| T_Ctrl
    M_Ctrl -.->|2. HTTP GET verify team/athlete profile| TM_Ctrl
    TM_Ctrl -.->|3. HTTP GET verify athlete ID| A_Ctrl
    Res_Ctrl -.->|4. HTTP PATCH update match status to FINISHED| M_Ctrl

    %% Inter-service Async Events
    Res_Ctrl ==>|5. Publish 'RESULT_CONFIRMED'| RabbitMQ
    RabbitMQ ==>|6. Deliver Event to update points| Rnk_Ctrl
```

---

## 8. Kết luận và Hướng dẫn Tiếp theo

Bản thiết kế UML trên đã chuẩn hóa toàn bộ các lớp thực thể, thuộc tính nghiệp vụ, quan hệ logic và phân rã chính xác 6 microservices theo đúng nội dung phân tích của bạn.

Các bước tiếp theo cần triển khai:
1. **Thiết kế API Contract (JSON Request / Response):** Định nghĩa chi tiết schema JSON cho các endpoint trọng tâm của từng service.
2. **Khởi tạo Skeleton Service:** Tạo các module con tương ứng trong Maven Multi-module.
3. **Cấu hình Spring Web / RestClient:** Hiện thực hóa các bước gọi đồng bộ và kiểm thử độc lập từng service qua Postman.
