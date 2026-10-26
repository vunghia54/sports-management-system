package com.sports.management.tournament;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.sports.management")
public class TournamentServiceApplication {

    public static void main(String[] args) {

        SpringApplication.run(
                TournamentServiceApplication.class,
                args
        );
    }
}