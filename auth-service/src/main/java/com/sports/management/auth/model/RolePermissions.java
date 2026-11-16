package com.sports.management.auth.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Ma trận phân quyền RBAC tập trung cho hệ thống VN Sports SOA
 */
public class RolePermissions {

    private static final Map<String, List<String>> PERMISSIONS_BY_ROLE = new LinkedHashMap<>();

    static {
        // 1. ROLE_ADMIN: Toàn quyền quản trị hệ thống
        PERMISSIONS_BY_ROLE.put("ROLE_ADMIN", List.of(
                "users.read",
                "users.manage",
                "roles.manage",
                "tournaments.read_public",
                "tournaments.create",
                "tournaments.manage",
                "teams.read",
                "teams.manage_own",
                "athletes.manage_own",
                "registrations.manage_own",
                "registrations.review",
                "matches.read_public",
                "matches.schedule",
                "results.submit_assigned",
                "results.approve",
                "rankings.read_public",
                "reports.read_own_tournaments",
                "reports.read_all",
                "services.monitor"
        ));

        // 2. ROLE_ORGANIZER: Quản lý giải đấu, phê duyệt đăng ký, xếp lịch, xác nhận KQ
        PERMISSIONS_BY_ROLE.put("ROLE_ORGANIZER", List.of(
                "tournaments.read_public",
                "tournaments.create",
                "tournaments.manage",
                "teams.read",
                "registrations.review",
                "matches.read_public",
                "matches.schedule",
                "results.submit_assigned",
                "results.approve",
                "rankings.read_public",
                "reports.read_own_tournaments"
        ));

        // 3. ROLE_REFEREE: Nhập kết quả trận đấu được phân công, xem lịch & kết quả
        PERMISSIONS_BY_ROLE.put("ROLE_REFEREE", List.of(
                "tournaments.read_public",
                "teams.read",
                "matches.read_public",
                "results.submit_assigned",
                "rankings.read_public"
        ));

        // 4. ROLE_TEAM_MANAGER: Quản lý đội của mình, thành viên đội, đăng ký giải đấu
        PERMISSIONS_BY_ROLE.put("ROLE_TEAM_MANAGER", List.of(
                "tournaments.read_public",
                "teams.read",
                "teams.manage_own",
                "athletes.manage_own",
                "registrations.manage_own",
                "matches.read_public",
                "rankings.read_public"
        ));

        // 5. ROLE_VIEWER: Khán giả xem giải đấu, lịch thi đấu, kết quả, bảng xếp hạng
        List<String> viewerPerms = List.of(
                "tournaments.read_public",
                "matches.read_public",
                "rankings.read_public"
        );
        PERMISSIONS_BY_ROLE.put("ROLE_VIEWER", viewerPerms);
        PERMISSIONS_BY_ROLE.put("ROLE_USER", viewerPerms);
    }

    public static List<String> getPermissionsForRole(String role) {
        if (role == null || role.isBlank()) {
            return List.of("tournaments.read_public", "matches.read_public", "rankings.read_public");
        }
        String normalized = role.startsWith("ROLE_") ? role.toUpperCase() : "ROLE_" + role.toUpperCase();
        List<String> list = PERMISSIONS_BY_ROLE.get(normalized);
        return list != null ? list : List.of("tournaments.read_public", "matches.read_public", "rankings.read_public");
    }
}

