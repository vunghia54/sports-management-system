
package com.sports.management.match.testing;

import com.sports.management.match.model.Match;
import com.sports.management.match.repository.MatchRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransactionTestService {

    private final MatchRepository matchRepository;

    public TransactionTestService(MatchRepository matchRepository) {
        this.matchRepository = matchRepository;
    }

    @Transactional
    public void testRollback(Match match) {

        // Luu Match vao database
        matchRepository.saveAndFlush(match);

        System.out.println(
                "[TEST] Match inserted, preparing to rollback..."
        );

        // Co tinh gay loi de kich hoat rollback
        throw new RuntimeException(
                "Simulated error for transaction rollback test"
        );
    }
}
