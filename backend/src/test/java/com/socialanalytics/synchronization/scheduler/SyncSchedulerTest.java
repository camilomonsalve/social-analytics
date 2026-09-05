package com.socialanalytics.synchronization.scheduler;

import com.socialanalytics.synchronization.entity.TriggeredBy;
import com.socialanalytics.synchronization.service.SynchronizationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SyncSchedulerTest {

    @Mock
    private SynchronizationService synchronizationService;

    @InjectMocks
    private SyncScheduler syncScheduler;

    @Test
    void scheduledSyncDelegatesToSynchronizationService() {
        syncScheduler.scheduledSync();

        verify(synchronizationService).synchronize(TriggeredBy.SCHEDULED);
    }
}
