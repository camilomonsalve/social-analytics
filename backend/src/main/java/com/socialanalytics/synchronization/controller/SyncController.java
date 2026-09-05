package com.socialanalytics.synchronization.controller;

import com.socialanalytics.synchronization.dto.SyncJobResponse;
import com.socialanalytics.synchronization.entity.SyncStatus;
import com.socialanalytics.synchronization.entity.TriggeredBy;
import com.socialanalytics.synchronization.exception.SyncException;
import com.socialanalytics.synchronization.mapper.SyncJobMapper;
import com.socialanalytics.synchronization.repository.SyncJobRepository;
import com.socialanalytics.synchronization.service.SynchronizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/sync")
@RequiredArgsConstructor
@Tag(name = "Synchronization", description = "Manual sync trigger and history")
public class SyncController {

    private final SynchronizationService synchronizationService;
    private final SyncJobRepository syncJobRepository;
    private final SyncJobMapper syncJobMapper;

    @PostMapping("/trigger")
    @Operation(summary = "Manually trigger a CSV sync")
    public SyncJobResponse trigger() {
        var job = synchronizationService.synchronize(TriggeredBy.MANUAL);
        if (job.getStatus() == SyncStatus.FAILED) {
            throw new SyncException(job.getErrorMessage(), null);
        }
        return syncJobMapper.toResponse(job);
    }

    @GetMapping("/status")
    @Operation(summary = "Get the most recent sync job")
    public SyncJobResponse status() {
        return syncJobRepository.findFirstByOrderByStartedAtDesc()
                .map(syncJobMapper::toResponse)
                .orElseThrow(() -> new SyncException("No sync has run yet", null));
    }

    @GetMapping("/history")
    @Operation(summary = "Get recent sync job history")
    public List<SyncJobResponse> history(@RequestParam(defaultValue = "20") int limit) {
        return syncJobRepository.findAllByOrderByStartedAtDesc(PageRequest.of(0, limit)).stream()
                .map(syncJobMapper::toResponse)
                .toList();
    }
}
