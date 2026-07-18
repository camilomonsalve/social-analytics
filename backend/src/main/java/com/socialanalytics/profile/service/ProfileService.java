package com.socialanalytics.profile.service;

import com.socialanalytics.profile.dto.CategoryResponse;
import com.socialanalytics.profile.dto.ProfileResponse;
import com.socialanalytics.profile.entity.Profile;
import com.socialanalytics.profile.exception.ProfileNotFoundException;
import com.socialanalytics.profile.mapper.ProfileMapper;
import com.socialanalytics.profile.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final ProfileRepository profileRepository;
    private final ProfileMapper profileMapper;

    @Transactional(readOnly = true)
    public List<ProfileResponse> getAllProfiles() {
        return profileRepository.findAll().stream()
                .map(profileMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProfileResponse getProfileById(UUID id) {
        Profile profile = profileRepository.findById(id)
                .orElseThrow(() -> new ProfileNotFoundException("Profile not found with id: " + id));
        return profileMapper.toResponse(profile);
    }

    @Transactional(readOnly = true)
    public List<ProfileResponse> getProfilesByCategory(String categoria) {
        return profileRepository.findByCategoria(categoria).stream()
                .map(profileMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getCategories() {
        return profileRepository.countByCategory().stream()
                .map(row -> CategoryResponse.builder()
                        .categoria((String) row[0])
                        .count((Long) row[1])
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public ProfileResponse createProfile(Profile profile) {
        return profileMapper.toResponse(profileRepository.save(profile));
    }

    @Transactional
    public boolean upsertProfile(Profile profile) {
        return profileRepository.findByNombre(profile.getNombre())
                .map(existing -> {
                    existing.setDescripcion(profile.getDescripcion());
                    existing.setFoto(profile.getFoto());
                    existing.setCategoria(profile.getCategoria());
                    profileRepository.save(existing);
                    return true;
                })
                .orElseGet(() -> {
                    profileRepository.save(profile);
                    return false;
                });
    }
}
