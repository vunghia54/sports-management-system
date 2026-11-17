package com.sports.management.gateway.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
public class GatewayRoutingController {

    private static final Logger log = LoggerFactory.getLogger(GatewayRoutingController.class);

    private final RestClient restClient;
    private final DiscoveryClient discoveryClient;

    private static final Map<String, String> ROUTE_MAP = new LinkedHashMap<>();

    static {
        ROUTE_MAP.put("/api/v1/auth", "auth-service");
        ROUTE_MAP.put("/api/v1/tournaments", "tournament-service");
        ROUTE_MAP.put("/api/v1/sports", "tournament-service");
        ROUTE_MAP.put("/api/v1/competitions", "tournament-service");
        ROUTE_MAP.put("/api/v1/matches", "match-service");
        ROUTE_MAP.put("/api/v1/registrations", "match-service");
        ROUTE_MAP.put("/api/v1/participants", "match-service");
        ROUTE_MAP.put("/api/v1/teams", "team-service");
        ROUTE_MAP.put("/api/v1/athletes", "athlete-service");
        ROUTE_MAP.put("/api/v1/results", "result-service");
        ROUTE_MAP.put("/api/v1/rankings", "ranking-service");
        ROUTE_MAP.put("/api/v1/notifications", "notification-service");
    }

    public GatewayRoutingController(
            @LoadBalanced RestClient.Builder loadBalancedRestClientBuilder,
            DiscoveryClient discoveryClient) {
        this.restClient = loadBalancedRestClientBuilder.build();
        this.discoveryClient = discoveryClient;
    }

    @GetMapping("/")
    public ResponseEntity<Void> rootRedirect() {
        return ResponseEntity.status(HttpStatus.FOUND)
                .header("Location", "/index.html")
                .build();
    }

    @GetMapping("/api/v1/gateway/routes")
    public ResponseEntity<Map<String, Object>> getGatewayRoutes() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("gateway", "API-GATEWAY");
        response.put("port", 8080);
        response.put("routes", ROUTE_MAP);
        List<String> services = discoveryClient.getServices();
        Map<String, Object> discoveredInstances = new LinkedHashMap<>();
        for (String svc : services) {
            List<ServiceInstance> instances = discoveryClient.getInstances(svc);
            discoveredInstances.put(svc.toUpperCase(), instances.stream()
                    .map(i -> i.getInstanceId() + " (" + i.getUri() + ")")
                    .toList());
        }
        response.put("discoveredServicesInEureka", discoveredInstances);
        response.put("timestamp", LocalDateTime.now().toString());
        return ResponseEntity.ok(response);
    }

    @RequestMapping({
            "/api/v1/auth/**",
            "/api/v1/tournaments/**",
            "/api/v1/sports/**",
            "/api/v1/competitions/**",
            "/api/v1/matches/**",
            "/api/v1/registrations/**",
            "/api/v1/participants/**",
            "/api/v1/teams/**",
            "/api/v1/athletes/**",
            "/api/v1/results/**",
            "/api/v1/rankings/**",
            "/api/v1/notifications/**"
    })
    public ResponseEntity<byte[]> proxyRequest(HttpServletRequest request) throws IOException {
        String path = request.getRequestURI();
        String query = request.getQueryString();
        String targetService = resolveServiceId(path);

        if (targetService == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .header("Content-Type", "application/json")
                    .body(("{\"status\":404,\"message\":\"No route found for path: " + path + "\"}").getBytes());
        }

        String targetUrl = "http://" + targetService + path + (query != null && !query.isBlank() ? "?" + query : "");
        HttpMethod method = HttpMethod.valueOf(request.getMethod());
        byte[] bodyBytes = StreamUtils.copyToByteArray(request.getInputStream());

        try {
            RestClient.RequestBodySpec requestSpec = restClient
                    .method(method)
                    .uri(URI.create(targetUrl));

            Enumeration<String> headerNames = request.getHeaderNames();
            if (headerNames != null) {
                while (headerNames.hasMoreElements()) {
                    String headerName = headerNames.nextElement();
                    if (!headerName.equalsIgnoreCase("Host")
                            && !headerName.equalsIgnoreCase("Content-Length")
                            && !headerName.equalsIgnoreCase("Transfer-Encoding")) {
                        Enumeration<String> values = request.getHeaders(headerName);
                        while (values.hasMoreElements()) {
                            requestSpec.header(headerName, values.nextElement());
                        }
                    }
                }
            }

            if (bodyBytes.length > 0) {
                requestSpec.body(bodyBytes);
            }

            return requestSpec.exchange((clientRequest, clientResponse) -> {
                HttpHeaders responseHeaders = new HttpHeaders();
                clientResponse.getHeaders().forEach((k, v) -> {
                    if (!k.equalsIgnoreCase("X-Request-Id") && !k.equalsIgnoreCase("Transfer-Encoding")) {
                        responseHeaders.put(k, v);
                    }
                });
                responseHeaders.set("X-Gateway-Routed-Service", targetService.toUpperCase());
                byte[] respBody = StreamUtils.copyToByteArray(clientResponse.getBody());
                log.info("Gateway routed {} {} -> {} status={}",
                        method, path, targetService, clientResponse.getStatusCode().value());
                return new ResponseEntity<>(respBody, responseHeaders, clientResponse.getStatusCode());
            });
        } catch (Exception ex) {
            log.warn("Gateway proxy failed for {} {} -> {}: {}", method, path, targetService, ex.getMessage());
            String errJson = String.format(
                    "{\"status\":503,\"error\":\"Service Unavailable\",\"message\":\"Service %s is currently unavailable via Eureka Registry\",\"path\":\"%s\"}",
                    targetService.toUpperCase(), path
            );
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .header("Content-Type", "application/json")
                    .body(errJson.getBytes());
        }
    }

    private String resolveServiceId(String path) {
        for (Map.Entry<String, String> entry : ROUTE_MAP.entrySet()) {
            if (path.equals(entry.getKey()) || path.startsWith(entry.getKey() + "/")) {
                return entry.getValue();
            }
        }
        return null;
    }
}
