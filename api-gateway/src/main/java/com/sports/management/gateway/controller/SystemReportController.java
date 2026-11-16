package com.sports.management.gateway.controller;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/reports")
public class SystemReportController {

    private static final int EXPORT_PAGE_SIZE = 50;
    private static final Set<String> FORBIDDEN_SECRET_KEYS = Set.of(
            "password", "passwordhash", "secret", "token", "accesstoken",
            "refreshtoken", "jwt", "privatekey", "apikey"
    );

    private final RestClient authClient;
    private final RestClient tournamentClient;
    private final RestClient teamClient;
    private final RestClient athleteClient;
    private final RestClient matchClient;
    private final RestClient resultClient;
    private final RestClient rankingClient;
    private final RestClient notificationClient;
    private final int maxExportPages;

    public SystemReportController(
            @LoadBalanced RestClient.Builder loadBalancedRestClientBuilder,
            @Qualifier("defaultRestClientBuilder") RestClient.Builder defaultRestClientBuilder,
            @Value("${services.auth.url:http://auth-service}") String authUrl,
            @Value("${services.tournament.url:http://tournament-service}") String tournamentUrl,
            @Value("${services.team.url:http://team-service}") String teamUrl,
            @Value("${services.athlete.url:http://athlete-service}") String athleteUrl,
            @Value("${services.match.url:http://match-service}") String matchUrl,
            @Value("${services.result.url:http://result-service}") String resultUrl,
            @Value("${services.ranking.url:http://ranking-service}") String rankingUrl,
            @Value("${services.notification.url:http://notification-service}") String notificationUrl,
            @Value("${app.report.max-export-pages:500}") int maxExportPages) {

        this.authClient = buildClient(loadBalancedRestClientBuilder, defaultRestClientBuilder, authUrl);
        this.tournamentClient = buildClient(loadBalancedRestClientBuilder, defaultRestClientBuilder, tournamentUrl);
        this.teamClient = buildClient(loadBalancedRestClientBuilder, defaultRestClientBuilder, teamUrl);
        this.athleteClient = buildClient(loadBalancedRestClientBuilder, defaultRestClientBuilder, athleteUrl);
        this.matchClient = buildClient(loadBalancedRestClientBuilder, defaultRestClientBuilder, matchUrl);
        this.resultClient = buildClient(loadBalancedRestClientBuilder, defaultRestClientBuilder, resultUrl);
        this.rankingClient = buildClient(loadBalancedRestClientBuilder, defaultRestClientBuilder, rankingUrl);
        this.notificationClient = buildClient(loadBalancedRestClientBuilder, defaultRestClientBuilder, notificationUrl);
        this.maxExportPages = Math.max(1, maxExportPages);
    }

    private static RestClient buildClient(RestClient.Builder lbBuilder, RestClient.Builder plainBuilder, String baseUrl) {
        boolean isLocal = baseUrl != null && (baseUrl.startsWith("http://localhost") || baseUrl.startsWith("http://127.0.0.1"));
        RestClient.Builder chosen = (isLocal && plainBuilder != null) ? plainBuilder : lbBuilder;
        return chosen.clone().baseUrl(baseUrl).build();
    }

    @GetMapping("/system-export")
    public ResponseEntity<Map<String, Object>> exportSystemReport(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(defaultValue = "false") boolean allowPartial) {

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "status", 401,
                            "error", "Unauthorized",
                            "message", "Full authentication is required to export full system report"
                    ));
        }

        Map<String, Object> callerProfile;
        try {
            callerProfile = authClient.get()
                    .uri("/api/v1/auth/me")
                    .header("Authorization", authHeader)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});
        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode().value() == 401) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of(
                                "status", 401,
                                "error", "Unauthorized",
                                "message", "Session expired or invalid token"
                        ));
            }
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of(
                            "status", 403,
                            "error", "Forbidden",
                            "message", "Access Denied: Unable to authorize export request"
                    ));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of(
                            "status", 503,
                            "error", "Service Unavailable",
                            "message", "Auth Service is unavailable to verify export permissions"
                    ));
        }

        String callerRole = callerProfile != null && callerProfile.get("role") != null
                ? String.valueOf(callerProfile.get("role"))
                : "";
        String callerUsername = callerProfile != null && callerProfile.get("username") != null
                ? String.valueOf(callerProfile.get("username"))
                : "unknown";
        boolean hasExportPerm = false;
        if (callerProfile != null && callerProfile.get("permissions") instanceof Collection<?> perms) {
            hasExportPerm = perms.stream().anyMatch(p -> "reports.read_all".equals(String.valueOf(p)));
        }
        if (!"ROLE_ADMIN".equalsIgnoreCase(callerRole) && !hasExportPerm) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of(
                            "status", 403,
                            "error", "Forbidden",
                            "message", "Access Denied: Only Administrator has permission to export the full system report"
                    ));
        }

        String generatedAt = OffsetDateTime.now().toString();
        Map<String, String> sourceStatuses = new LinkedHashMap<>();
        List<Map<String, Object>> missingSources = new ArrayList<>();
        Map<String, Object> data = new LinkedHashMap<>();

        List<Object> tournaments = fetchAllPages(tournamentClient, "tournaments", "/api/v1/tournaments", "sortBy=id&direction=asc", null, sourceStatuses, missingSources);
        List<Object> sports = fetchList(tournamentClient, "sports", "/api/v1/sports", null, sourceStatuses, missingSources);
        List<Object> competitions = fetchList(tournamentClient, "competitions", "/api/v1/competitions", null, sourceStatuses, missingSources);
        List<Object> teams = fetchAllPages(teamClient, "teams", "/api/v1/teams/paged", "sortBy=id&direction=asc", null, sourceStatuses, missingSources);
        List<Object> athletes = fetchAllPages(athleteClient, "athletes", "/api/v1/athletes/paged", "sortBy=id&direction=asc", null, sourceStatuses, missingSources);
        List<Object> matches = fetchAllPages(matchClient, "matches", "/api/v1/matches/paged", "sortBy=id&sortDir=asc", null, sourceStatuses, missingSources);
        List<Object> registrations = fetchAllPages(matchClient, "registrations", "/api/v1/registrations/paged", "sortBy=id&sortDir=asc", null, sourceStatuses, missingSources);
        List<Object> results = fetchAllPages(resultClient, "results", "/api/v1/results/paged", "sortBy=id&sortDir=asc", null, sourceStatuses, missingSources);
        List<Object> rankings = fetchList(rankingClient, "rankings", "/api/v1/rankings/details", null, sourceStatuses, missingSources);
        List<Object> notifications = fetchAllPages(notificationClient, "notifications", "/api/v1/notifications/paged", "sortBy=createdAt&sortDir=desc", null, sourceStatuses, missingSources);
        List<Object> users = fetchAllPages(authClient, "users", "/api/v1/auth/users/paged", "", authHeader, sourceStatuses, missingSources);

        data.put("tournaments", sanitizeValue(tournaments));
        data.put("sports", sanitizeValue(sports));
        data.put("competitions", sanitizeValue(competitions));
        data.put("teams", sanitizeValue(teams));
        data.put("athletes", sanitizeValue(athletes));
        data.put("matches", sanitizeValue(matches));
        data.put("registrations", sanitizeValue(registrations));
        data.put("results", sanitizeValue(results));
        data.put("rankings", sanitizeValue(rankings));
        data.put("notifications", sanitizeValue(notifications));
        data.put("users", sanitizeValue(users));

        if (!missingSources.isEmpty() && !allowPartial) {
            Map<String, Object> errorBody = new LinkedHashMap<>();
            errorBody.put("status", 503);
            errorBody.put("error", "Export Incomplete");
            errorBody.put("exportStatus", "FAILED");
            errorBody.put("generatedAt", generatedAt);
            errorBody.put("missingSources", missingSources);
            errorBody.put("sourceStatuses", sourceStatuses);
            errorBody.put("message", "Không thể xuất báo cáo đầy đủ do lỗi từ: " +
                    String.join(", ", missingSources.stream().map(m -> String.valueOf(m.get("source"))).toList()));
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorBody);
        }

        // Consistent summary computed directly from the snapshot just retrieved
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("tournamentsCount", tournaments.size());
        summary.put("sportsCount", sports.size());
        summary.put("competitionsCount", competitions.size());
        summary.put("teamsCount", teams.size());
        summary.put("athletesCount", athletes.size());
        summary.put("matchesCount", matches.size());
        summary.put("registrationsCount", registrations.size());
        summary.put("resultsCount", results.size());
        summary.put("rankingsCount", rankings.size());
        summary.put("notificationsCount", notifications.size());
        summary.put("usersCount", users.size());
        summary.put("failedSourcesCount", missingSources.size());

        boolean partial = !missingSources.isEmpty();
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("reportType", "VN_SPORTS_FULL_SYSTEM_EXPORT");
        report.put("exportStatus", partial ? "PARTIAL" : "COMPLETE");
        report.put("partial", partial);
        report.put("generatedAt", generatedAt);
        report.put("exportedBy", callerUsername);
        report.put("missingSources", missingSources);
        report.put("sourceStatuses", sourceStatuses);
        report.put("summary", summary);
        report.put("data", data);

        return ResponseEntity
                .status(partial ? HttpStatus.PARTIAL_CONTENT : HttpStatus.OK)
                .body(report);
    }

    private List<Object> fetchAllPages(
            RestClient client,
            String sourceKey,
            String basePath,
            String extraQuery,
            String authHeader,
            Map<String, String> sourceStatuses,
            List<Map<String, Object>> missingSources) {
        List<Object> collected = new ArrayList<>();
        try {
            int page = 0;
            int totalPages = 1;
            long reportedTotalElements = -1L;
            while (page < totalPages && page < maxExportPages) {
                String sep = extraQuery != null && !extraQuery.isBlank() ? "&" + extraQuery : "";
                String uri = basePath + "?page=" + page + "&size=" + EXPORT_PAGE_SIZE + sep;
                var spec = client.get().uri(uri);
                if (authHeader != null && !authHeader.isBlank()) {
                    spec.header("Authorization", authHeader);
                }
                Map<String, Object> pageMap = spec.retrieve()
                        .body(new ParameterizedTypeReference<Map<String, Object>>() {});
                if (pageMap == null) {
                    sourceStatuses.put(sourceKey, "INCOMPLETE");
                    missingSources.add(Map.of(
                            "source", sourceKey,
                            "reason", "Dịch vụ trả về phản hồi rỗng (null) tại trang " + page
                    ));
                    return collected;
                }

                Object tpObj = pageMap.get("totalPages");
                totalPages = tpObj != null ? Math.max(0, Integer.parseInt(String.valueOf(tpObj))) : 1;
                Object teObj = pageMap.get("totalElements");
                if (teObj != null) {
                    reportedTotalElements = Long.parseLong(String.valueOf(teObj));
                }

                Object contentObj = pageMap.get("content");
                List<?> items = contentObj instanceof List<?> list ? list : null;

                // Legitimately empty dataset
                if (page == 0 && totalPages == 0 && (items == null || items.isEmpty()) && reportedTotalElements <= 0) {
                    sourceStatuses.put(sourceKey, "OK");
                    return collected;
                }

                // Abnormal empty page while remaining pages or elements are still declared
                if (items == null || items.isEmpty()) {
                    if (page + 1 < totalPages || (reportedTotalElements >= 0 && collected.size() < reportedTotalElements)) {
                        sourceStatuses.put(sourceKey, "INCOMPLETE");
                        missingSources.add(Map.of(
                                "source", sourceKey,
                                "pagesFetched", page,
                                "totalPages", totalPages,
                                "reason", "Trang " + page + " trả về danh sách rỗng bất thường trong khi vẫn còn dữ liệu (totalPages="
                                        + totalPages + ", đã lấy " + collected.size()
                                        + (reportedTotalElements >= 0 ? "/" + reportedTotalElements : "") + " bản ghi)"
                        ));
                        return collected;
                    }
                    break;
                }

                collected.addAll(items);
                page++;
            }

            // Detect hard page-limit truncation without silently marking OK
            if (page < totalPages || (reportedTotalElements >= 0 && page >= maxExportPages && collected.size() < reportedTotalElements)) {
                sourceStatuses.put(sourceKey, "INCOMPLETE");
                Map<String, Object> detail = new LinkedHashMap<>();
                detail.put("source", sourceKey);
                detail.put("truncated", true);
                detail.put("pagesFetched", page);
                detail.put("totalPages", totalPages);
                detail.put("maxExportPages", maxExportPages);
                detail.put("reason", "Chạm giới hạn xuất tối đa " + maxExportPages + " trang trong khi nguồn còn "
                        + totalPages + " trang dữ liệu");
                missingSources.add(detail);
                return collected;
            }

            sourceStatuses.put(sourceKey, "OK");
            return collected;
        } catch (Exception ex) {
            sourceStatuses.put(sourceKey, "FAILED");
            missingSources.add(Map.of(
                    "source", sourceKey,
                    "reason", ex.getMessage() != null ? ex.getMessage() : "Service unavailable"
            ));
            return List.of();
        }
    }

    private List<Object> fetchList(
            RestClient client,
            String sourceKey,
            String uri,
            String authHeader,
            Map<String, String> sourceStatuses,
            List<Map<String, Object>> missingSources) {
        try {
            var spec = client.get().uri(uri);
            if (authHeader != null && !authHeader.isBlank()) {
                spec.header("Authorization", authHeader);
            }
            List<Object> list = spec.retrieve()
                    .body(new ParameterizedTypeReference<List<Object>>() {});
            sourceStatuses.put(sourceKey, "OK");
            return list != null ? list : List.of();
        } catch (Exception ex) {
            sourceStatuses.put(sourceKey, "FAILED");
            missingSources.add(Map.of(
                    "source", sourceKey,
                    "reason", ex.getMessage() != null ? ex.getMessage() : "Service unavailable"
            ));
            return List.of();
        }
    }

    private Object sanitizeValue(Object val) {
        if (val instanceof Map<?, ?> map) {
            Map<String, Object> clean = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() == null) {
                    continue;
                }
                String key = String.valueOf(entry.getKey());
                String lower = key.toLowerCase(Locale.ROOT);
                if (FORBIDDEN_SECRET_KEYS.contains(lower)) {
                    continue;
                }
                if ("identitycardnumber".equals(lower) && entry.getValue() != null) {
                    String raw = String.valueOf(entry.getValue());
                    clean.put(key, raw.length() >= 4 ? "********" + raw.substring(raw.length() - 4) : "****");
                    continue;
                }
                clean.put(key, sanitizeValue(entry.getValue()));
            }
            return clean;
        }
        if (val instanceof List<?> list) {
            List<Object> cleanList = new ArrayList<>(list.size());
            for (Object item : list) {
                cleanList.add(sanitizeValue(item));
            }
            return cleanList;
        }
        return val;
    }
}
