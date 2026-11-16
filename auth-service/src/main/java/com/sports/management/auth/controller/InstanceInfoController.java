package com.sports.management.auth.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/instance-info")
public class InstanceInfoController {

    @Value("${spring.application.name:auth-service}")
    private String serviceName;

    @Value("${server.port:8083}")
    private int port;

    @Value("${eureka.instance.instance-id:auth-service:8083}")
    private String instanceId;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getInstanceInfo() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("serviceName", serviceName.toUpperCase());
        info.put("instanceId", instanceId);
        info.put("port", port);
        info.put("status", "UP");
        info.put("timestamp", LocalDateTime.now().toString());
        return ResponseEntity.ok()
                .header("X-Instance-Id", instanceId)
                .body(info);
    }
}

