package com.sports.management.result.service;

import com.sports.management.result.model.Result;
import com.sports.management.result.repository.ResultRepository;
import com.sports.management.result.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class ResultSyncRecoveryWorker {

    private final ResultRepository resultRepository;
    private final ResultService resultService;
    private final JwtTokenProvider jwtTokenProvider;
    private final boolean syncEnabled;

    public ResultSyncRecoveryWorker(
            ResultRepository resultRepository,
            ResultService resultService,
            JwtTokenProvider jwtTokenProvider,
            @Value("${app.result.sync-enabled:true}") boolean syncEnabled) {
        this.resultRepository = resultRepository;
        this.resultService = resultService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.syncEnabled = syncEnabled;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void recoverInterruptedJobsOnStartup() {
        if (!syncEnabled) {
            return;
        }
        processPendingAndRetryableJobs();
    }

    @Scheduled(fixedDelayString = "${app.result.sync-interval-ms:15000}")
    public void scheduledRetryTick() {
        if (!syncEnabled) {
            return;
        }
        processPendingAndRetryableJobs();
    }

    public int processPendingAndRetryableJobs() {
        List<Result> dueJobs = resultRepository.findPendingOrRetryableSyncJobs(
                ResultService.MAX_AUTO_SYNC_ATTEMPTS,
                LocalDateTime.now()
        );
        if (dueJobs.isEmpty()) {
            return 0;
        }
        String internalBearer = jwtTokenProvider.generateInternalSyncToken();
        int processed = 0;
        for (Result job : dueJobs) {
            try {
                resultService.executeSyncPipeline(job.getId(), internalBearer);
                processed++;
            } catch (Exception ex) {
                System.out.println("[WARN] Sync worker failed processing resultId=" + job.getId() + ": " + ex.getMessage());
            }
        }
        return processed;
    }
}
