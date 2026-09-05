package com.socialanalytics.synchronization.service;

import com.socialanalytics.profile.entity.Profile;
import com.socialanalytics.profile.importer.CsvImportResult;
import com.socialanalytics.profile.importer.CsvImporter;
import com.socialanalytics.profile.service.ProfileService;
import com.socialanalytics.profile.service.UpsertResult;
import com.socialanalytics.synchronization.csv.CsvDownloader;
import com.socialanalytics.synchronization.entity.SyncJob;
import com.socialanalytics.synchronization.entity.TriggeredBy;
import com.socialanalytics.synchronization.exception.SyncException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SynchronizationService {

    private final CsvDownloader csvDownloader;
    private final CsvImporter csvImporter;
    private final ProfileService profileService;
    private final SyncJobTracker syncJobTracker;

    public SyncJob synchronize(TriggeredBy triggeredBy) {
        SyncJob job = syncJobTracker.startJob(triggeredBy);

        try {
            InputStream csvStream = csvDownloader.download();
            CsvImportResult importResult;
            try {
                importResult = csvImporter.importFromCsv(csvStream);
            } catch (IOException e) {
                throw new SyncException("Failed to parse downloaded CSV", e);
            }

            List<Profile> parsedProfiles = importResult.profiles();
            int skippedInvalidCategoria = importResult.skippedCount();

            int created = 0, updated = 0, unchanged = 0, failed = 0;
            for (Profile profile : parsedProfiles) {
                try {
                    UpsertResult result = profileService.upsertProfile(profile);
                    switch (result) {
                        case CREATED -> created++;
                        case UPDATED -> updated++;
                        case UNCHANGED -> unchanged++;
                    }
                } catch (Exception e) {
                    log.warn("Failed to upsert profile '{}': {}", profile.getNombre(), e.getMessage());
                    failed++;
                }
            }

            return syncJobTracker.finishJobSuccess(job, created, updated, unchanged, failed, skippedInvalidCategoria);

        } catch (SyncException e) {
            log.error("Sync failed during download/parse: {}", e.getMessage(), e);
            return syncJobTracker.finishJobFailed(job, e.getMessage());
        }
    }
}
