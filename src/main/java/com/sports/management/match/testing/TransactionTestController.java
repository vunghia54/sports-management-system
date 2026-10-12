
package com.sports.management.match.testing;

import com.sports.management.match.model.Match;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/test")
public class TransactionTestController {

    private final TransactionTestService transactionTestService;

    public TransactionTestController(
            TransactionTestService transactionTestService) {
        this.transactionTestService = transactionTestService;
    }

    @PostMapping("/rollback")
    public ResponseEntity<String> testRollback() {

        Match match = new Match();

        match.setTournamentId(1L);
        match.setRound("ROLLBACK_TEST");
        match.setMatchTime(LocalDateTime.of(2026, 11, 15, 10, 0));
        match.setLocation("Test Stadium");
        match.setParticipant1("Rollback Team A");
        match.setParticipant2("Rollback Team B");
        match.setStatus("SCHEDULED");

        try {
            transactionTestService.testRollback(match);

            return ResponseEntity.ok("Unexpected success");

        } catch (RuntimeException ex) {

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Rollback test triggered: " + ex.getMessage());
        }
    }
}
