package com.sports.management.tournament.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class InstanceInfoController {

    @Value("${spring.application.name:tournament-service}")
    private String applicationName;

    @Value("${server.port:8081}")
    private int serverPort;

    @GetMapping({"/api/v1/instance-info", "/api/v1/tournaments/instance-info"})
    public ResponseEntity<Map<String, Object>> getInstanceInfo() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("serviceName", applicationName.toUpperCase());
        info.put("instanceId", applicationName + ":" + serverPort);
        info.put("instancePort", serverPort);
        info.put("port", serverPort);
        info.put("status", "UP");
        info.put("timestamp", LocalDateTime.now().toString());
        return ResponseEntity.ok(info);
    }
}

