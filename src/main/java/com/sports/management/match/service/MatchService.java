
package com.sports.management.match.service;

import com.sports.management.match.client.TournamentClient;
import com.sports.management.match.model.Match;
import com.sports.management.match.repository.MatchRepository;
import com.sports.management.messaging.event.MatchFinishedEvent;
import com.sports.management.messaging.producer.MatchEventProducer;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MatchService {

    private final MatchRepository matchRepository;
    private final TournamentClient tournamentClient;
    private final MatchEventProducer matchEventProducer;
    private final com.sports.management.tournament.service.TournamentService tournamentService;

    public MatchService(
            MatchRepository matchRepository,
            TournamentClient tournamentClient,
            MatchEventProducer matchEventProducer,
            com.sports.management.tournament.service.TournamentService tournamentService) {

        this.matchRepository = matchRepository;
        this.tournamentClient = tournamentClient;
        this.matchEventProducer = matchEventProducer;
        this.tournamentService = tournamentService;
    }

    // GET ALL
    public List<Match> getAllMatches() {
        return matchRepository.findAll();
    }

    // GET BY ID
    public Match getMatchById(Long id) {
        if (id == null) {
            return null;
        }

        return matchRepository.findById(id).orElse(null);
    }

    // KIEM TRA TOURNAMENT TRUC TIEP TRONG CUNG APPLICATION (HOAC QUA CLIENT)
    public boolean tournamentExists(Long tournamentId) {
        if (tournamentId == null) {
            return false;
        }

        if (tournamentService != null) {
            return tournamentService.getTournamentById(tournamentId) != null;
        }

        if (tournamentClient != null) {
            return tournamentClient.tournamentExists(tournamentId);
        }

        return false;
    }

    // GET MATCHES BY TOURNAMENT
    public List<Match> getMatchesByTournamentId(Long tournamentId) {
        return matchRepository.findByTournamentId(tournamentId);
    }

    // GET MATCHES BY COMPETITION
    public List<Match> getMatchesByCompetitionId(Long competitionId) {
        return matchRepository.findByCompetitionId(competitionId);
    }

    // CREATE MATCH
    @Transactional
    public Match createMatch(Match match) {

        if (!tournamentExists(match.getTournamentId())) {
            return null;
        }

        match.setId(null);

        Match savedMatch = matchRepository.save(match);

        System.out.println(
                "[MATCH CREATED] ID = " + savedMatch.getId()
        );

        return savedMatch;
    }

    // UPDATE MATCH
    @Transactional
    public Match updateMatch(Long id, Match updatedMatch) {

        Match existingMatch = getMatchById(id);

        if (existingMatch == null) {
            return null;
        }

        if (!tournamentExists(updatedMatch.getTournamentId())) {
            return null;
        }

        String oldStatus = existingMatch.getStatus();

        existingMatch.setTournamentId(updatedMatch.getTournamentId());
        existingMatch.setRound(updatedMatch.getRound());
        existingMatch.setMatchTime(updatedMatch.getMatchTime());
        existingMatch.setLocation(updatedMatch.getLocation());
        existingMatch.setParticipant1(updatedMatch.getParticipant1());
        existingMatch.setParticipant2(updatedMatch.getParticipant2());
        existingMatch.setStatus(updatedMatch.getStatus());

        Match savedMatch = matchRepository.save(existingMatch);

        // CHI GUI EVENT KHI CHUYEN SANG FINISHED
        boolean wasFinished =
                "FINISHED".equalsIgnoreCase(oldStatus);

        boolean isFinished =
                "FINISHED".equalsIgnoreCase(savedMatch.getStatus());

        if (!wasFinished && isFinished) {

            MatchFinishedEvent event = new MatchFinishedEvent();

            event.setEvent("MATCH_FINISHED");
            event.setMatchId(savedMatch.getId());
            event.setTournamentId(savedMatch.getTournamentId());
            event.setParticipant1(savedMatch.getParticipant1());
            event.setParticipant2(savedMatch.getParticipant2());

            matchEventProducer.publishMatchFinished(event);

            System.out.println(
                    "[MATCH FINISHED] Event published for Match ID = "
                            + savedMatch.getId()
            );
        }

        return savedMatch;
    }

    // DELETE MATCH
    @Transactional
    public boolean deleteMatch(Long id) {

        if (id == null || !matchRepository.existsById(id)) {
            return false;
        }

        matchRepository.deleteById(id);
        return true;
    }
}
