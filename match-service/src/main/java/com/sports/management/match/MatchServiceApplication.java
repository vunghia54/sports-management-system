package com.sports.management.match;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.sports.management")
public class MatchServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(
                MatchServiceApplication.class,
                args
        );
    }
}
