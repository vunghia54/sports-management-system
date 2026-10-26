package com.sports.management;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Arrays;

@SpringBootApplication
public class SportsManagementSystemApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(SportsManagementSystemApplication.class);
        boolean testOnly = Arrays.asList(args).contains("--test-only");
        if (testOnly) {
            app.setWebApplicationType(WebApplicationType.NONE);
        }
        app.run(args);
    }
}
