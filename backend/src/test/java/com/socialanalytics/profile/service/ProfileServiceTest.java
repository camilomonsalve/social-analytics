package com.socialanalytics.profile.service;

import com.socialanalytics.profile.dto.CategoryResponse;
import com.socialanalytics.profile.dto.ProfileResponse;
import com.socialanalytics.profile.entity.Profile;
import com.socialanalytics.profile.exception.ProfileNotFoundException;
import com.socialanalytics.profile.mapper.ProfileMapper;
import com.socialanalytics.profile.repository.ProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private ProfileMapper profileMapper;

    @InjectMocks
    private ProfileService profileService;

    @Test
    void getAllProfilesReturnsEmptyList() {
        when(profileRepository.findAll()).thenReturn(List.of());

        List<ProfileResponse> result = profileService.getAllProfiles();

        assertTrue(result.isEmpty());
        verify(profileRepository).findAll();
    }

    @Test
    void getAllProfilesReturnsProfiles() {
        UUID id = UUID.randomUUID();
        Profile profile = Profile.builder()
                .id(id)
                .nombre("Test Profile")
                .categoria("artistas")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        ProfileResponse response = ProfileResponse.builder()
                .id(id)
                .nombre("Test Profile")
                .categoria("artistas")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(profileRepository.findAll()).thenReturn(List.of(profile));
        when(profileMapper.toResponse(profile)).thenReturn(response);

        List<ProfileResponse> result = profileService.getAllProfiles();

        assertEquals(1, result.size());
        assertEquals("Test Profile", result.get(0).getNombre());
        verify(profileRepository).findAll();
        verify(profileMapper).toResponse(profile);
    }

    @Test
    void getProfileByIdReturnsProfile() {
        UUID id = UUID.randomUUID();
        Profile profile = Profile.builder()
                .id(id)
                .nombre("Test Profile")
                .categoria("artistas")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        ProfileResponse response = ProfileResponse.builder()
                .id(id)
                .nombre("Test Profile")
                .categoria("artistas")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(profileRepository.findById(id)).thenReturn(Optional.of(profile));
        when(profileMapper.toResponse(profile)).thenReturn(response);

        ProfileResponse result = profileService.getProfileById(id);

        assertNotNull(result);
        assertEquals("Test Profile", result.getNombre());
        verify(profileRepository).findById(id);
        verify(profileMapper).toResponse(profile);
    }

    @Test
    void getProfileByIdThrowsExceptionWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(profileRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(ProfileNotFoundException.class, () -> profileService.getProfileById(id));
        verify(profileRepository).findById(id);
    }

    @Test
    void getProfilesByCategoryReturnsProfiles() {
        UUID id = UUID.randomUUID();
        Profile profile = Profile.builder()
                .id(id)
                .nombre("Test Artist")
                .categoria("artistas")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        ProfileResponse response = ProfileResponse.builder()
                .id(id)
                .nombre("Test Artist")
                .categoria("artistas")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(profileRepository.findByCategoria("artistas")).thenReturn(List.of(profile));
        when(profileMapper.toResponse(profile)).thenReturn(response);

        List<ProfileResponse> result = profileService.getProfilesByCategory("artistas");

        assertEquals(1, result.size());
        assertEquals("artistas", result.get(0).getCategoria());
        verify(profileRepository).findByCategoria("artistas");
    }

    @Test
    void getCategoriesReturnsCategories() {
        when(profileRepository.countByCategory()).thenReturn(List.of(
                new Object[]{"artistas", 5L},
                new Object[]{"empresas", 3L}
        ));

        List<CategoryResponse> result = profileService.getCategories();

        assertEquals(2, result.size());
        assertEquals("artistas", result.get(0).getCategoria());
        assertEquals(5L, result.get(0).getCount());
        verify(profileRepository).countByCategory();
    }

    @Test
    void createProfileReturnsProfile() {
        UUID id = UUID.randomUUID();
        Profile profile = Profile.builder()
                .id(id)
                .nombre("New Profile")
                .categoria("artistas")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        ProfileResponse response = ProfileResponse.builder()
                .id(id)
                .nombre("New Profile")
                .categoria("artistas")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(profileRepository.save(profile)).thenReturn(profile);
        when(profileMapper.toResponse(profile)).thenReturn(response);

        ProfileResponse result = profileService.createProfile(profile);

        assertNotNull(result);
        assertEquals("New Profile", result.getNombre());
        verify(profileRepository).save(profile);
        verify(profileMapper).toResponse(profile);
    }

    @Test
    void upsertProfileCreatesWhenNotExists() {
        Profile profile = Profile.builder().nombre("New").categoria("artistas").build();
        when(profileRepository.findByNombre("New")).thenReturn(Optional.empty());

        UpsertResult result = profileService.upsertProfile(profile);

        assertEquals(UpsertResult.CREATED, result);
        verify(profileRepository).save(profile);
    }

    @Test
    void upsertProfileUpdatesWhenExists() {
        Profile existing = Profile.builder().nombre("Test").categoria("artistas").descripcion("Old").contentHash("oldhash").build();
        Profile incoming = Profile.builder().nombre("Test").categoria("empresas").descripcion("New").build();
        when(profileRepository.findByNombre("Test")).thenReturn(Optional.of(existing));

        UpsertResult result = profileService.upsertProfile(incoming);

        assertEquals(UpsertResult.UPDATED, result);
        assertEquals("New", existing.getDescripcion());
        assertEquals("empresas", existing.getCategoria());
        assertNotNull(existing.getContentHash());
        verify(profileRepository).save(existing);
    }

    @Test
    void upsertProfileReturnsUnchangedWhenContentIdentical() {
        Profile initial = Profile.builder()
                .nombre("Test")
                .categoria("artistas")
                .descripcion("Same")
                .foto("same.jpg")
                .build();
        when(profileRepository.findByNombre("Test")).thenReturn(Optional.empty());
        profileService.upsertProfile(initial); // CREATED — mutates `initial`, setting its real contentHash

        Profile incoming = Profile.builder()
                .nombre("Test")
                .categoria("artistas")
                .descripcion("Same")
                .foto("same.jpg")
                .build();
        when(profileRepository.findByNombre("Test")).thenReturn(Optional.of(initial));

        UpsertResult result = profileService.upsertProfile(incoming);

        assertEquals(UpsertResult.UNCHANGED, result);
        verify(profileRepository, times(1)).save(any()); // only the first (CREATE) call should have saved
    }
}
