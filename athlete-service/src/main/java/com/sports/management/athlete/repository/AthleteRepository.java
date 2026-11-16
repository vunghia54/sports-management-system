package com.sports.management.athlete.repository;

import com.sports.management.athlete.model.Athlete;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AthleteRepository extends JpaRepository<Athlete, Long> {
    Optional<Athlete> findByIdentityCardNumber(String identityCardNumber);
    List<Athlete> findByFullNameContainingIgnoreCase(String fullName);
    List<Athlete> findByGenderIgnoreCase(String gender);
}
