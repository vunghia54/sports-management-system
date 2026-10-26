package com.sports.management.result.service;

import com.sports.management.exception.ResourceNotFoundException;
import com.sports.management.match.model.Match;
import com.sports.management.match.repository.MatchRepository;
import com.sports.management.match.service.MatchService;
import com.sports.management.ranking.service.RankingService;
import com.sports.management.result.model.RecordResultRequest;
import com.sports.management.result.model.Result;
import com.sports.management.result.model.ResultDetail;
import com.sports.management.result.model.ResultResponse;
import com.sports.management.result.repository.ResultDetailRepository;
import com.sports.management.result.repository.ResultRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
public class ResultService {

    private final ResultRepository resultRepository;
    private final ResultDetailRepository resultDetailRepository;
    private final MatchRepository matchRepository;
    private final MatchService matchService;
    private final RankingService rankingService;

    public ResultService(
            ResultRepository resultRepository,
            ResultDetailRepository resultDetailRepository,
            MatchRepository matchRepository,
            MatchService matchService,
            @Lazy RankingService rankingService) {
        this.resultRepository = resultRepository;
        this.resultDetailRepository = resultDetailRepository;
        this.matchRepository = matchRepository;
        this.matchService = matchService;
        this.rankingService = rankingService;
    }

    public List<Result> getAllResults() {
        return resultRepository.findAll();
    }

    public Result getResultById(Long id) {
        return resultRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Result not found with id: " + id));
    }

    public ResultResponse getResultResponseByMatchId(Long matchId) {
        Result result = resultRepository.findByMatchId(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Result not found for match id: " + matchId));
        List<ResultDetail> details = resultDetailRepository.findByResultId(result.getId());
        return new ResultResponse(result, details);
    }

    @Transactional
    public ResultResponse recordResult(RecordResultRequest request) {
        Match match = matchRepository.findById(request.getMatchId())
                .orElseThrow(() -> new ResourceNotFoundException("Match not found with id: " + request.getMatchId()));

        // Neu ket qua da ton tai, cap nhat; nguoc lai tao moi
        Result result = resultRepository.findByMatchId(request.getMatchId())
                .orElse(new Result());

        result.setMatchId(match.getId());
        result.setScoreParticipant1(request.getScoreParticipant1());
        result.setScoreParticipant2(request.getScoreParticipant2());
        result.setNotes(request.getNotes());
        result.setStatus("PENDING_CONFIRMATION");

        // Xac dinh doi thang/hoa tu dong
        if (request.getScoreParticipant1() > request.getScoreParticipant2()) {
            result.setWinner(match.getParticipant1());
        } else if (request.getScoreParticipant2() > request.getScoreParticipant1()) {
            result.setWinner(match.getParticipant2());
        } else {
            result.setWinner("DRAW");
        }

        Result savedResult = resultRepository.save(result);

        // Luu chi tiet tran dau neu co
        List<ResultDetail> savedDetails = Collections.emptyList();
        if (request.getDetails() != null && !request.getDetails().isEmpty()) {
            resultDetailRepository.deleteByResultId(savedResult.getId());
            for (ResultDetail detail : request.getDetails()) {
                detail.setResultId(savedResult.getId());
            }
            savedDetails = resultDetailRepository.saveAll(request.getDetails());
        }

        return new ResultResponse(savedResult, savedDetails);
    }

    @Transactional
    public ResultResponse confirmResult(Long id, String confirmedBy) {
        Result result = getResultById(id);
        result.setStatus("CONFIRMED");
        result.setConfirmedBy(confirmedBy != null ? confirmedBy : "ORGANIZER");
        result.setConfirmedAt(LocalDateTime.now());
        Result savedResult = resultRepository.save(result);

        // Cap nhat trang thai tran dau sang FINISHED thong qua matchService (de ban event)
        Match match = matchRepository.findById(result.getMatchId()).orElse(null);
        if (match != null) {
            match.setStatus("FINISHED");
            matchService.updateMatch(match.getId(), match);

            // Tu dong tinh lai bang diem / xep hang neu co competitionId
            if (match.getCompetitionId() != null) {
                rankingService.recalculateRanking(match.getCompetitionId());
            }
        }

        List<ResultDetail> details = resultDetailRepository.findByResultId(savedResult.getId());
        return new ResultResponse(savedResult, details);
    }
}
