package com.socialanalytics.synchronization.service;

import com.socialanalytics.configuration.SyncProperties;
import com.socialanalytics.synchronization.entity.SyncJob;
import com.socialanalytics.synchronization.entity.SyncStatus;
import com.socialanalytics.synchronization.entity.TriggeredBy;
import com.socialanalytics.synchronization.repository.SyncJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SyncJobTracker {

    private final SyncJobRepository syncJobRepository;
    private final SyncProperties syncProperties;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SyncJob startJob(TriggeredBy triggeredBy) {
        SyncJob job = SyncJob.builder()
                .startedAt(java.time.LocalDateTime.now())
                .status(SyncStatus.RUNNING)
                .triggeredBy(triggeredBy)
                .sourceUrl(syncProperties.csv().sourceUrl())
                .build();
        return syncJobRepository.save(job);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SyncJob finishJobSuccess(SyncJob job, int created, int updated, int unchanged, int failed, int skippedInvalid) {
        job.setFinishedAt(java.time.LocalDateTime.now());
        job.setStatus(SyncStatus.SUCCESS);
        job.setProfilesCreated(created);
        job.setProfilesUpdated(updated);
        job.setProfilesUnchanged(unchanged);
        job.setProfilesFailed(failed);
        job.setProfilesSkippedInvalid(skippedInvalid);
        return syncJobRepository.save(job);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SyncJob finishJobFailed(SyncJob job, String errorMessage) {
        job.setFinishedAt(java.time.LocalDateTime.now());
        job.setStatus(SyncStatus.FAILED);
        job.setErrorMessage(errorMessage);
        return syncJobRepository.save(job);
    }
}
