package com.sports.management.result.controller;

import com.sports.management.result.model.RecordResultRequest;
import com.sports.management.result.model.Result;
import com.sports.management.result.model.ResultResponse;
import com.sports.management.result.service.ResultService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/results")
public class ResultController {

    private final ResultService resultService;

    public ResultController(ResultService resultService) {
        this.resultService = resultService;
    }

    @PostMapping
    public ResponseEntity<ResultResponse> recordResult(@Valid @RequestBody RecordResultRequest request) {
        ResultResponse response = resultService.recordResult(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<Result>> getAllResults() {
        return ResponseEntity.ok(resultService.getAllResults());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Result> getResultById(@PathVariable Long id) {
        return ResponseEntity.ok(resultService.getResultById(id));
    }

    @GetMapping("/match/{matchId}")
    public ResponseEntity<ResultResponse> getResultByMatchId(@PathVariable Long matchId) {
        return ResponseEntity.ok(resultService.getResultResponseByMatchId(matchId));
    }

    @PutMapping("/{id}/confirm")
    public ResponseEntity<ResultResponse> confirmResult(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        String confirmedBy = (body != null && body.containsKey("confirmedBy"))
                ? body.get("confirmedBy") : "ORGANIZER";
        return ResponseEntity.ok(resultService.confirmResult(id, confirmedBy));
    }
}
