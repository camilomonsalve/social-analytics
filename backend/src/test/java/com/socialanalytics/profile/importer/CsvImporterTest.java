package com.socialanalytics.profile.importer;

import com.socialanalytics.profile.entity.Profile;
import com.socialanalytics.profile.service.ProfileService;
import com.socialanalytics.profile.service.UpsertResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CsvImporterTest {

    @Mock
    private ProfileService profileService;

    @InjectMocks
    private CsvImporter csvImporter;

    @Test
    void importFromCsvReturnsProfiles() throws IOException {
        String csvContent = "nombre,categoria,descripcion\n" +
                "Juanes,artistas,Cantante\n" +
                "Shakira,artistas,Cantante";
        
        ByteArrayInputStream inputStream = new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8));

        CsvImportResult result = csvImporter.importFromCsv(inputStream);
        List<Profile> profiles = result.profiles();

        assertEquals(2, profiles.size());
        assertEquals("Juanes", profiles.get(0).getNombre());
        assertEquals("artistas", profiles.get(0).getCategoria());
        assertEquals("Shakira", profiles.get(1).getNombre());
        assertEquals(0, result.skippedCount());
    }

    @Test
    void importFromCsvHandlesEmptyNombre() throws IOException {
        String csvContent = "nombre,categoria,descripcion\n" +
                ",artistas,Descripcion test\n" +
                "Valid Name,artistas,Description";
        
        ByteArrayInputStream inputStream = new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8));

        CsvImportResult result = csvImporter.importFromCsv(inputStream);
        List<Profile> profiles = result.profiles();

        assertEquals(1, profiles.size());
        assertEquals("Valid Name", profiles.get(0).getNombre());
        assertEquals(1, result.skippedCount());
    }

    @Test
    void importFromCsvHandlesNullFields() throws IOException {
        String csvContent = "nombre,categoria,descripcion\n" +
                "Test Profile,artistas,";
        
        ByteArrayInputStream inputStream = new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8));

        CsvImportResult result = csvImporter.importFromCsv(inputStream);
        List<Profile> profiles = result.profiles();

        assertEquals(1, profiles.size());
        assertEquals("Test Profile", profiles.get(0).getNombre());
        // CSV parser returns empty string instead of null for empty fields
        assertTrue(profiles.get(0).getDescripcion() == null || profiles.get(0).getDescripcion().isEmpty());
        assertEquals(0, result.skippedCount());
    }

    @Test
    void importFromCsvSkipsInvalidCategoria() throws IOException {
        String csvContent = "nombre,categoria,descripcion\n" +
                "Valid Name,invalid_category,Description\n" +
                "Another Valid,artistas,Description";

        ByteArrayInputStream inputStream = new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8));

        CsvImportResult result = csvImporter.importFromCsv(inputStream);
        List<Profile> profiles = result.profiles();

        assertEquals(1, profiles.size());
        assertEquals("Another Valid", profiles.get(0).getNombre());
        assertEquals("artistas", profiles.get(0).getCategoria());
        assertEquals(1, result.skippedCount());
    }

    @Test
    void importFromCsvSkipsNullCategoria() throws IOException {
        String csvContent = "nombre,categoria,descripcion\n" +
                "Valid Name,,Description\n" +
                "Another Valid,artistas,Description";

        ByteArrayInputStream inputStream = new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8));

        CsvImportResult result = csvImporter.importFromCsv(inputStream);
        List<Profile> profiles = result.profiles();

        assertEquals(1, profiles.size());
        assertEquals("Another Valid", profiles.get(0).getNombre());
        assertEquals("artistas", profiles.get(0).getCategoria());
        assertEquals(1, result.skippedCount());
    }

    @Test
    void importFromCsvAcceptsNewCategories() throws IOException {
        String csvContent = "nombre,categoria,descripcion\n" +
                "Valid Name,partidos,Description\n" +
                "Another Valid,lideres,Description\n" +
                "Third Name,sindicatos,Description";

        ByteArrayInputStream inputStream = new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8));

        CsvImportResult result = csvImporter.importFromCsv(inputStream);
        List<Profile> profiles = result.profiles();

        assertEquals(3, profiles.size());
        assertEquals("partidos", profiles.get(0).getCategoria());
        assertEquals("lideres", profiles.get(1).getCategoria());
        assertEquals("sindicatos", profiles.get(2).getCategoria());
        assertEquals(0, result.skippedCount());
    }

    @Test
    void saveProfilesCreatesNewProfiles() {
        Profile profile = Profile.builder().nombre("Test").categoria("artistas").build();
        when(profileService.upsertProfile(any(Profile.class))).thenReturn(UpsertResult.CREATED);

        csvImporter.saveProfiles(Arrays.asList(profile));

        verify(profileService).upsertProfile(profile);
    }

    @Test
    void saveProfilesUpdatesExistingProfiles() {
        Profile updated = Profile.builder()
                .nombre("Test")
                .categoria("empresas")
                .descripcion("New description")
                .build();

        when(profileService.upsertProfile(any(Profile.class))).thenReturn(UpsertResult.UPDATED);

        csvImporter.saveProfiles(Arrays.asList(updated));

        verify(profileService).upsertProfile(updated);
    }

    @Test
    void saveProfilesHandlesMixedCreateAndUpdate() {
        Profile newProfile = Profile.builder().nombre("New").categoria("artistas").build();
        Profile updatedData = Profile.builder()
                .nombre("Existing")
                .categoria("empresas")
                .build();

        when(profileService.upsertProfile(any(Profile.class))).thenReturn(UpsertResult.CREATED, UpsertResult.UPDATED);

        csvImporter.saveProfiles(Arrays.asList(newProfile, updatedData));

        verify(profileService, times(2)).upsertProfile(any(Profile.class));
    }
}
