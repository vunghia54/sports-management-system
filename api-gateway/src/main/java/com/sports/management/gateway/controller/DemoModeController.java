package com.sports.management.gateway.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
public class DemoModeController {

    private final boolean demoEnabled;
    private final ObjectMapper objectMapper;

    private static final List<Map<String, String>> DEMO_ACCOUNTS = List.of(
            Map.of("label", "Quản trị viên (Admin)", "username", "admin", "secret", "admin123", "role", "ROLE_ADMIN"),
            Map.of("label", "Ban tổ chức (Organizer 1)", "username", "organizer1", "secret", "organizer123", "role", "ROLE_ORGANIZER"),
            Map.of("label", "Trọng tài (Referee 1)", "username", "referee1", "secret", "referee123", "role", "ROLE_REFEREE"),
            Map.of("label", "Lãnh đội (Manager 1)", "username", "manager1", "secret", "manager123", "role", "ROLE_TEAM_MANAGER"),
            Map.of("label", "Khán giả (Viewer)", "username", "user1", "secret", "user123", "role", "ROLE_VIEWER")
    );

    public DemoModeController(
            @Value("${app.demo.enabled:false}") boolean demoEnabled,
            ObjectMapper objectMapper) {
        this.demoEnabled = demoEnabled;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/api/v1/demo/config")
    public ResponseEntity<Map<String, Object>> getDemoConfig() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("enabled", demoEnabled);
        body.put("accounts", demoEnabled ? DEMO_ACCOUNTS : List.of());
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(body);
    }

    @GetMapping(value = "/js/demo-mode.js", produces = "application/javascript")
    public ResponseEntity<byte[]> getDemoModeScript() throws Exception {
        if (!demoEnabled) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .header(HttpHeaders.CACHE_CONTROL, "no-store")
                    .build();
        }
        String json = objectMapper.writeValueAsString(Map.of("accounts", DEMO_ACCOUNTS));
        String js = "/* Demo quick-login accounts served strictly because app.demo.enabled=true */\n"
                + "window.__VN_DEMO_MODE__ = " + json + ";\n";
        return ResponseEntity.ok()
                .contentType(new MediaType("application", "javascript", StandardCharsets.UTF_8))
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(js.getBytes(StandardCharsets.UTF_8));
    }
}
