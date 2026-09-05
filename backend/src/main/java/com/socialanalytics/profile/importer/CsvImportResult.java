package com.socialanalytics.profile.importer;

import com.socialanalytics.profile.entity.Profile;

import java.util.List;

public record CsvImportResult(List<Profile> profiles, int skippedCount) {
}
