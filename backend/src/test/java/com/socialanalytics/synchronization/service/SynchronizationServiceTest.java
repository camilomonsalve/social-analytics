package com.socialanalytics.synchronization.service;

import com.socialanalytics.profile.entity.Profile;
import com.socialanalytics.profile.importer.CsvImportResult;
import com.socialanalytics.profile.importer.CsvImporter;
import com.socialanalytics.profile.service.ProfileService;
import com.socialanalytics.profile.service.UpsertResult;
import com.socialanalytics.synchronization.csv.CsvDownloader;
import com.socialanalytics.synchronization.entity.SyncJob;
import com.socialanalytics.synchronization.entity.SyncStatus;
import com.socialanalytics.synchronization.entity.TriggeredBy;
import com.socialanalytics.synchronization.exception.SyncException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SynchronizationServiceTest {

    @Mock
    private CsvDownloader csvDownloader;

    @Mock
    private CsvImporter csvImporter;

    @Mock
    private ProfileService profileService;

    @Mock
    private SyncJobTracker syncJobTracker;

    @InjectMocks
    private SynchronizationService synchronizationService;

    private SyncJob runningJob;

    @BeforeEach
    void setUp() {
        runningJob = SyncJob.builder()
                .id(UUID.randomUUID())
                .status(SyncStatus.RUNNING)
                .triggeredBy(TriggeredBy.MANUAL)
                .sourceUrl("https://example.com/datos.csv")
                .build();
        stubTrackerToMutateAndReturn();
    }

    private void stubTrackerToMutateAndReturn() {
        lenient().when(syncJobTracker.finishJobSuccess(any(SyncJob.class), anyInt(), anyInt(), anyInt(), anyInt(), anyInt()))
                .thenAnswer(invocation -> {
                    SyncJob job = invocation.getArgument(0);
                    job.setStatus(SyncStatus.SUCCESS);
                    job.setFinishedAt(LocalDateTime.now());
                    job.setProfilesCreated(invocation.getArgument(1));
                    job.setProfilesUpdated(invocation.getArgument(2));
                    job.setProfilesUnchanged(invocation.getArgument(3));
                    job.setProfilesFailed(invocation.getArgument(4));
                    job.setProfilesSkippedInvalid(invocation.getArgument(5));
                    return job;
                });

        lenient().when(syncJobTracker.finishJobFailed(any(SyncJob.class), anyString()))
                .thenAnswer(invocation -> {
                    SyncJob job = invocation.getArgument(0);
                    job.setStatus(SyncStatus.FAILED);
                    job.setFinishedAt(LocalDateTime.now());
                    job.setErrorMessage(invocation.getArgument(1));
                    return job;
                });
    }

    @Test
    void synchronizeSuccessPath() throws IOException {
        InputStream csvStream = new ByteArrayInputStream("test data".getBytes());
        Profile profile1 = Profile.builder().nombre("Profile1").categoria("artistas").build();
        Profile profile2 = Profile.builder().nombre("Profile2").categoria("artistas").build();
        Profile profile3 = Profile.builder().nombre("Profile3").categoria("artistas").build();

        when(csvDownloader.download()).thenReturn(csvStream);
        when(csvImporter.importFromCsv(csvStream)).thenReturn(new CsvImportResult(List.of(profile1, profile2, profile3), 0));
        when(profileService.upsertProfile(profile1)).thenReturn(UpsertResult.CREATED);
        when(profileService.upsertProfile(profile2)).thenReturn(UpsertResult.UPDATED);
        when(profileService.upsertProfile(profile3)).thenReturn(UpsertResult.UNCHANGED);
        when(syncJobTracker.startJob(any(TriggeredBy.class))).thenReturn(runningJob);

        SyncJob result = synchronizationService.synchronize(TriggeredBy.MANUAL);

        assertNotNull(result);
        assertEquals(SyncStatus.SUCCESS, result.getStatus());
        assertEquals(1, result.getProfilesCreated());
        assertEquals(1, result.getProfilesUpdated());
        assertEquals(1, result.getProfilesUnchanged());
        assertEquals(0, result.getProfilesFailed());
        assertEquals(0, result.getProfilesSkippedInvalid());
        assertNotNull(result.getFinishedAt());
        verify(syncJobTracker).startJob(TriggeredBy.MANUAL);
        verify(syncJobTracker).finishJobSuccess(any(SyncJob.class), eq(1), eq(1), eq(1), eq(0), eq(0));
    }

    @Test
    void synchronizeDownloadFailure() {
        when(csvDownloader.download()).thenThrow(new SyncException("Download failed", null));
        when(syncJobTracker.startJob(any(TriggeredBy.class))).thenReturn(runningJob);

        SyncJob result = synchronizationService.synchronize(TriggeredBy.MANUAL);

        assertNotNull(result);
        assertEquals(SyncStatus.FAILED, result.getStatus());
        assertEquals("Download failed", result.getErrorMessage());
        assertNotNull(result.getFinishedAt());
    }

    @Test
    void synchronizeOneRowFailsMidBatch() throws IOException {
        InputStream csvStream = new ByteArrayInputStream("test data".getBytes());
        Profile profile1 = Profile.builder().nombre("Profile1").categoria("artistas").build();
        Profile profile2 = Profile.builder().nombre("Profile2").categoria("artistas").build();
        Profile profile3 = Profile.builder().nombre("Profile3").categoria("artistas").build();

        when(csvDownloader.download()).thenReturn(csvStream);
        when(csvImporter.importFromCsv(csvStream)).thenReturn(new CsvImportResult(List.of(profile1, profile2, profile3), 0));
        when(profileService.upsertProfile(profile1)).thenReturn(UpsertResult.CREATED);
        when(profileService.upsertProfile(profile2)).thenThrow(new RuntimeException("DB constraint violation"));
        when(profileService.upsertProfile(profile3)).thenReturn(UpsertResult.UPDATED);
        when(syncJobTracker.startJob(any(TriggeredBy.class))).thenReturn(runningJob);

        SyncJob result = synchronizationService.synchronize(TriggeredBy.MANUAL);

        assertNotNull(result);
        assertEquals(SyncStatus.SUCCESS, result.getStatus());
        assertEquals(1, result.getProfilesCreated());
        assertEquals(1, result.getProfilesUpdated());
        assertEquals(0, result.getProfilesUnchanged());
        assertEquals(1, result.getProfilesFailed());
        verify(profileService).upsertProfile(profile1);
        verify(profileService).upsertProfile(profile2);
        verify(profileService).upsertProfile(profile3);
    }

    @Test
    void synchronizeWithSkippedInvalidCategoria() throws IOException {
        InputStream csvStream = new ByteArrayInputStream("test data".getBytes());
        Profile profile1 = Profile.builder().nombre("Profile1").categoria("artistas").build();
        Profile profile2 = Profile.builder().nombre("Profile2").categoria("artistas").build();

        when(csvDownloader.download()).thenReturn(csvStream);
        when(csvImporter.importFromCsv(csvStream)).thenReturn(new CsvImportResult(List.of(profile1, profile2), 3));
        when(profileService.upsertProfile(profile1)).thenReturn(UpsertResult.CREATED);
        when(profileService.upsertProfile(profile2)).thenReturn(UpsertResult.UPDATED);
        when(syncJobTracker.startJob(any(TriggeredBy.class))).thenReturn(runningJob);

        SyncJob result = synchronizationService.synchronize(TriggeredBy.MANUAL);

        assertNotNull(result);
        assertEquals(SyncStatus.SUCCESS, result.getStatus());
        assertEquals(1, result.getProfilesCreated());
        assertEquals(1, result.getProfilesUpdated());
        assertEquals(0, result.getProfilesUnchanged());
        assertEquals(0, result.getProfilesFailed());
        assertEquals(3, result.getProfilesSkippedInvalid());
        verify(syncJobTracker).finishJobSuccess(any(SyncJob.class), eq(1), eq(1), eq(0), eq(0), eq(3));
    }
}
