package com.sports.management.match.repository;

import com.sports.management.match.model.Registration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface RegistrationRepository extends JpaRepository<Registration, Long> {

    List<Registration> findByCompetitionId(Long competitionId);

    List<Registration> findByStatus(String status);

    List<Registration> findByCompetitionIdAndStatus(Long competitionId, String status);

    Optional<Registration> findByCompetitionIdAndParticipantTypeAndParticipantReferenceId(
            Long competitionId, String participantType, Long participantReferenceId);

    List<Registration> findByCompetitionIdAndParticipantTypeAndStatusIn(
            Long competitionId, String participantType, Collection<String> statuses);

    boolean existsByParticipantTypeAndParticipantReferenceIdAndStatusIn(
            String participantType, Long participantReferenceId, Collection<String> statuses);
}

