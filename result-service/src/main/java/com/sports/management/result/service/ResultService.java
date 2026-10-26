package com.sports.management.result.service;

import com.sports.management.exception.ResourceNotFoundException;
import com.sports.management.result.model.RecordResultRequest;
import com.sports.management.result.model.Result;
import com.sports.management.result.model.ResultDetail;
import com.sports.management.result.model.ResultResponse;
import com.sports.management.result.repository.ResultDetailRepository;
import com.sports.management.result.repository.ResultRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
public class ResultService {

    private final ResultRepository resultRepository;
    private final ResultDetailRepository resultDetailRepository;

    public ResultService(
            ResultRepository resultRepository,
            ResultDetailRepository resultDetailRepository) {
        this.resultRepository = resultRepository;
        this.resultDetailRepository = resultDetailRepository;
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
        Result result = resultRepository.findByMatchId(request.getMatchId())
                .orElse(new Result());

        result.setMatchId(request.getMatchId());
        result.setScoreParticipant1(request.getScoreParticipant1());
        result.setScoreParticipant2(request.getScoreParticipant2());
        result.setNotes(request.getNotes());
        result.setStatus("PENDING_CONFIRMATION");

        if (request.getScoreParticipant1() > request.getScoreParticipant2()) {
            result.setWinner("PARTICIPANT_1");
        } else if (request.getScoreParticipant2() > request.getScoreParticipant1()) {
            result.setWinner("PARTICIPANT_2");
        } else {
            result.setWinner("DRAW");
        }

        Result savedResult = resultRepository.save(result);

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

        List<ResultDetail> details = resultDetailRepository.findByResultId(savedResult.getId());
        return new ResultResponse(savedResult, details);
    }
}
