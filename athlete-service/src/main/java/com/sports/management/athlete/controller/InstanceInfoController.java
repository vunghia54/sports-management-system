package com.sports.management.athlete.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class InstanceInfoController {

    @Value("${spring.application.name:athlete-service}")
    private String serviceName;

    @Value("${server.port:8086}")
    private int serverPort;

    @GetMapping({"/api/v1/instance-info", "/api/v1/athletes/instance-info"})
    public ResponseEntity<Map<String, Object>> getInstanceInfo() {
        String host = "localhost";
        try {
            host = InetAddress.getLocalHost().getHostAddress();
        } catch (Exception ignored) {
        }

        Map<String, Object> info = new LinkedHashMap<>();
        info.put("serviceName", serviceName.toUpperCase());
        info.put("instanceId", serviceName + ":" + serverPort);
        info.put("instancePort", serverPort);
        info.put("host", host);
        info.put("status", "UP");
        info.put("timestamp", LocalDateTime.now().toString());
        return ResponseEntity.ok(info);
    }
}

