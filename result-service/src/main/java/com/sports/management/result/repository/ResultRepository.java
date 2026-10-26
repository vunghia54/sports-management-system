package com.sports.management.result.repository;

import com.sports.management.result.model.Result;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ResultRepository extends JpaRepository<Result, Long> {

    Optional<Result> findByMatchId(Long matchId);

    boolean existsByMatchId(Long matchId);
}
