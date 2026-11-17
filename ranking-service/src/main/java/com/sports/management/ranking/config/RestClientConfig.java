package com.sports.management.ranking.config;

import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * RestClient cho loi goi lien dich vu, co gioi han thoi gian ket noi va thoi gian cho phan hoi
 * de mot provider bi treo khong keo treo consumer.
 * Cau hinh qua SERVICES_CONNECT_TIMEOUT_MS / SERVICES_READ_TIMEOUT_MS.
 */
@Configuration
public class RestClientConfig {

    private static final long DEFAULT_CONNECT_TIMEOUT_MS = 2000L;
    private static final long DEFAULT_READ_TIMEOUT_MS = 10000L;

    @Value("${services.http.connect-timeout-ms:2000}")
    private long connectTimeoutMs;

    @Value("${services.http.read-timeout-ms:10000}")
    private long readTimeoutMs;

    @Bean
    @Primary
    public RestClient.Builder defaultRestClientBuilder() {
        return builderWithTimeouts();
    }

    @Bean
    @LoadBalanced
    public RestClient.Builder loadBalancedRestClientBuilder() {
        return builderWithTimeouts();
    }

    private RestClient.Builder builderWithTimeouts() {
        long connectMs = connectTimeoutMs > 0 ? connectTimeoutMs : DEFAULT_CONNECT_TIMEOUT_MS;
        long readMs = readTimeoutMs > 0 ? readTimeoutMs : DEFAULT_READ_TIMEOUT_MS;
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofMillis(connectMs))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofMillis(readMs));
        return RestClient.builder()
                .requestFactory(requestFactory)
                .requestInterceptor((request, body, execution) -> {
                    String requestId = MDC.get(RequestIdFilter.MDC_KEY);
                    if (requestId != null && request.getHeaders().getFirst(RequestIdFilter.HEADER) == null) {
                        request.getHeaders().set(RequestIdFilter.HEADER, requestId);
                    }
                    return execution.execute(request, body);
                });
    }
}
