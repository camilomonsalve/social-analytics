package com.socialanalytics.synchronization.controller;

import com.socialanalytics.common.GlobalExceptionHandler;
import com.socialanalytics.synchronization.dto.SyncJobResponse;
import com.socialanalytics.synchronization.entity.SyncJob;
import com.socialanalytics.synchronization.entity.SyncStatus;
import com.socialanalytics.synchronization.entity.TriggeredBy;
import com.socialanalytics.synchronization.exception.SyncException;
import com.socialanalytics.synchronization.mapper.SyncJobMapper;
import com.socialanalytics.synchronization.repository.SyncJobRepository;
import com.socialanalytics.synchronization.service.SynchronizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SyncControllerTest {

    @Mock
    private SynchronizationService synchronizationService;

    @Mock
    private SyncJobRepository syncJobRepository;

    @Mock
    private SyncJobMapper syncJobMapper;

    @InjectMocks
    private SyncController syncController;

    private MockMvc mockMvc;

    private SyncJob successfulJob;
    private SyncJob failedJob;
    private SyncJobResponse successfulResponse;
    private SyncJobResponse failedResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(syncController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        successfulJob = SyncJob.builder()
                .id(UUID.randomUUID())
                .status(SyncStatus.SUCCESS)
                .triggeredBy(TriggeredBy.MANUAL)
                .profilesCreated(5)
                .profilesUpdated(3)
                .profilesUnchanged(2)
                .profilesFailed(0)
                .build();

        failedJob = SyncJob.builder()
                .id(UUID.randomUUID())
                .status(SyncStatus.FAILED)
                .triggeredBy(TriggeredBy.MANUAL)
                .errorMessage("Download failed")
                .build();

        successfulResponse = SyncJobResponse.builder()
                .id(successfulJob.getId())
                .status("SUCCESS")
                .triggeredBy("MANUAL")
                .profilesCreated(5)
                .profilesUpdated(3)
                .profilesUnchanged(2)
                .profilesFailed(0)
                .build();

        failedResponse = SyncJobResponse.builder()
                .id(failedJob.getId())
                .status("FAILED")
                .triggeredBy("MANUAL")
                .errorMessage("Download failed")
                .build();
    }

    @Test
    void triggerReturnsSuccessWhenSyncSucceeds() throws Exception {
        when(synchronizationService.synchronize(TriggeredBy.MANUAL)).thenReturn(successfulJob);
        when(syncJobMapper.toResponse(successfulJob)).thenReturn(successfulResponse);

        mockMvc.perform(post("/api/v1/sync/trigger"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.profilesCreated").value(5))
                .andExpect(jsonPath("$.profilesUpdated").value(3))
                .andExpect(jsonPath("$.profilesUnchanged").value(2));
    }

    @Test
    void triggerThrowsSyncExceptionWhenSyncFails() throws Exception {
        when(synchronizationService.synchronize(TriggeredBy.MANUAL)).thenReturn(failedJob);
        when(syncJobMapper.toResponse(failedJob)).thenReturn(failedResponse);

        mockMvc.perform(post("/api/v1/sync/trigger"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error").value("Bad Gateway"))
                .andExpect(jsonPath("$.message").value("Download failed"));
    }

    @Test
    void statusReturnsMostRecentJob() throws Exception {
        when(syncJobRepository.findFirstByOrderByStartedAtDesc()).thenReturn(Optional.of(successfulJob));
        when(syncJobMapper.toResponse(successfulJob)).thenReturn(successfulResponse);

        mockMvc.perform(get("/api/v1/sync/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.profilesCreated").value(5));
    }

    @Test
    void statusThrowsExceptionWhenNoPriorRuns() throws Exception {
        when(syncJobRepository.findFirstByOrderByStartedAtDesc()).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/sync/status"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error").value("Bad Gateway"))
                .andExpect(jsonPath("$.message").value("No sync has run yet"));
    }

    @Test
    void historyReturnsJobsInDescendingOrder() throws Exception {
        SyncJob job1 = SyncJob.builder().id(UUID.randomUUID()).status(SyncStatus.SUCCESS).build();
        SyncJob job2 = SyncJob.builder().id(UUID.randomUUID()).status(SyncStatus.SUCCESS).build();
        
        SyncJobResponse response1 = SyncJobResponse.builder().id(job1.getId()).status("SUCCESS").build();
        SyncJobResponse response2 = SyncJobResponse.builder().id(job2.getId()).status("SUCCESS").build();

        when(syncJobRepository.findAllByOrderByStartedAtDesc(any(PageRequest.class)))
                .thenReturn(List.of(job1, job2));
        when(syncJobMapper.toResponse(job1)).thenReturn(response1);
        when(syncJobMapper.toResponse(job2)).thenReturn(response2);

        mockMvc.perform(get("/api/v1/sync/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(job1.getId().toString()))
                .andExpect(jsonPath("$[1].id").value(job2.getId().toString()));
    }

    @Test
    void historyRespectsLimitParameter() throws Exception {
        when(syncJobRepository.findAllByOrderByStartedAtDesc(any(PageRequest.class)))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/v1/sync/history?limit=10"))
                .andExpect(status().isOk());
    }
}
