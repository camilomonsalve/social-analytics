package com.socialanalytics.profile.importer;

import com.socialanalytics.profile.entity.Profile;
import com.socialanalytics.profile.service.ProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class CsvImporter {

    private final ProfileService profileService;
    
    private static final Set<String> VALID_CATEGORIES = Set.of("artistas", "empresas", "medios", "politica");

    public List<Profile> importFromCsv(InputStream inputStream) throws IOException {
        List<Profile> profiles = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT
                     .builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .build())) {

            for (CSVRecord record : csvParser) {
                Profile profile = parseRecord(record);
                if (profile != null) {
                    profiles.add(profile);
                }
            }
        }

        log.info("Imported {} profiles from CSV", profiles.size());
        return profiles;
    }

    private Profile parseRecord(CSVRecord record) {
        try {
            String nombre = record.get("nombre");
            String descripcion = record.get("descripcion");
            String foto = record.get("foto");
            String categoria = record.get("categoria");

            if (nombre == null || nombre.trim().isEmpty()) {
                log.warn("Skipping record with empty nombre");
                return null;
            }

            String trimmedCategoria = categoria != null ? categoria.trim() : null;
            if (trimmedCategoria == null || !VALID_CATEGORIES.contains(trimmedCategoria)) {
                log.warn("Skipping record with invalid categoria: {}", trimmedCategoria);
                return null;
            }

            return Profile.builder()
                    .nombre(nombre.trim())
                    .descripcion(descripcion != null ? descripcion.trim() : null)
                    .foto(foto != null ? foto.trim() : null)
                    .categoria(trimmedCategoria)
                    .build();

        } catch (IllegalArgumentException e) {
            log.warn("Skipping malformed CSV record: {}", e.getMessage());
            return null;
        }
    }

    @Transactional
    public void saveProfiles(List<Profile> profiles) {
        int created = 0;
        int updated = 0;

        for (Profile profile : profiles) {
            boolean exists = profileService.upsertProfile(profile);
            if (exists) {
                updated++;
            } else {
                created++;
            }
        }

        log.info("Saved {} profiles to database ({} created, {} updated)", profiles.size(), created, updated);
    }
}
