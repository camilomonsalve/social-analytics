package com.socialanalytics.synchronization.scheduler;

import com.socialanalytics.synchronization.entity.TriggeredBy;
import com.socialanalytics.synchronization.service.SynchronizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "sync.scheduler", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SyncScheduler {

    private final SynchronizationService synchronizationService;

    @Scheduled(cron = "${sync.scheduler.cron}")
    public void scheduledSync() {
        synchronizationService.synchronize(TriggeredBy.SCHEDULED);
    }
}
