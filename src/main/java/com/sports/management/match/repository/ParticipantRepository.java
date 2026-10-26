package com.sports.management.match.repository;

import com.sports.management.match.model.Participant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ParticipantRepository extends JpaRepository<Participant, Long> {

    List<Participant> findByCompetitionId(Long competitionId);

    Optional<Participant> findByCompetitionIdAndParticipantTypeAndReferenceId(
            Long competitionId, String participantType, Long referenceId);
}
