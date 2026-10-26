package com.sports.management.athlete.repository;

import com.sports.management.athlete.model.Athlete;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AthleteRepository extends JpaRepository<Athlete, Long> {
    List<Athlete> findByFullNameContainingIgnoreCase(String name);
}
