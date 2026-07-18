package com.socialanalytics.profile.importer;

import com.socialanalytics.profile.entity.Profile;
import com.socialanalytics.profile.service.ProfileService;
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
        String csvContent = "nombre,descripcion,foto,categoria\n" +
                "Juanes,Cantante,https://example.com/juanes.jpg,artistas\n" +
                "Shakira,Cantante,https://example.com/shakira.jpg,artistas";
        
        ByteArrayInputStream inputStream = new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8));

        List<Profile> profiles = csvImporter.importFromCsv(inputStream);

        assertEquals(2, profiles.size());
        assertEquals("Juanes", profiles.get(0).getNombre());
        assertEquals("artistas", profiles.get(0).getCategoria());
        assertEquals("Shakira", profiles.get(1).getNombre());
    }

    @Test
    void importFromCsvHandlesEmptyNombre() throws IOException {
        String csvContent = "nombre,descripcion,foto,categoria\n" +
                ",Descripcion test,https://example.com/test.jpg,artistas\n" +
                "Valid Name,Description,https://example.com/valid.jpg,artistas";
        
        ByteArrayInputStream inputStream = new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8));

        List<Profile> profiles = csvImporter.importFromCsv(inputStream);

        assertEquals(1, profiles.size());
        assertEquals("Valid Name", profiles.get(0).getNombre());
    }

    @Test
    void importFromCsvHandlesNullFields() throws IOException {
        String csvContent = "nombre,descripcion,foto,categoria\n" +
                "Test Profile,,,artistas";
        
        ByteArrayInputStream inputStream = new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8));

        List<Profile> profiles = csvImporter.importFromCsv(inputStream);

        assertEquals(1, profiles.size());
        assertEquals("Test Profile", profiles.get(0).getNombre());
        // CSV parser returns empty string instead of null for empty fields
        assertTrue(profiles.get(0).getDescripcion() == null || profiles.get(0).getDescripcion().isEmpty());
        assertTrue(profiles.get(0).getFoto() == null || profiles.get(0).getFoto().isEmpty());
    }

    @Test
    void importFromCsvSkipsInvalidCategoria() throws IOException {
        String csvContent = "nombre,descripcion,foto,categoria\n" +
                "Valid Name,Description,https://example.com/valid.jpg,invalid_category\n" +
                "Another Valid,Description,https://example.com/another.jpg,artistas";

        ByteArrayInputStream inputStream = new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8));

        List<Profile> profiles = csvImporter.importFromCsv(inputStream);

        assertEquals(1, profiles.size());
        assertEquals("Another Valid", profiles.get(0).getNombre());
        assertEquals("artistas", profiles.get(0).getCategoria());
    }

    @Test
    void importFromCsvSkipsNullCategoria() throws IOException {
        String csvContent = "nombre,descripcion,foto,categoria\n" +
                "Valid Name,Description,https://example.com/valid.jpg,\n" +
                "Another Valid,Description,https://example.com/another.jpg,artistas";

        ByteArrayInputStream inputStream = new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8));

        List<Profile> profiles = csvImporter.importFromCsv(inputStream);

        assertEquals(1, profiles.size());
        assertEquals("Another Valid", profiles.get(0).getNombre());
        assertEquals("artistas", profiles.get(0).getCategoria());
    }

    @Test
    void saveProfilesCreatesNewProfiles() {
        Profile profile = Profile.builder().nombre("Test").categoria("artistas").build();
        when(profileService.upsertProfile(any(Profile.class))).thenReturn(false);

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

        when(profileService.upsertProfile(any(Profile.class))).thenReturn(true);

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

        when(profileService.upsertProfile(any(Profile.class))).thenReturn(false, true);

        csvImporter.saveProfiles(Arrays.asList(newProfile, updatedData));

        verify(profileService, times(2)).upsertProfile(any(Profile.class));
    }
}
