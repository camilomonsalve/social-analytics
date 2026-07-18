package com.socialanalytics.profile.controller;

import com.socialanalytics.profile.dto.CategoryResponse;
import com.socialanalytics.profile.dto.ProfileResponse;
import com.socialanalytics.profile.entity.Profile;
import com.socialanalytics.profile.importer.CsvImporter;
import com.socialanalytics.profile.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/profiles")
@RequiredArgsConstructor
@Tag(name = "Profiles", description = "Profile management endpoints")
public class ProfileController {

    private final ProfileService profileService;
    private final CsvImporter csvImporter;

    @GetMapping
    @Operation(summary = "Get all profiles", description = "Retrieve all profiles in the system")
    public List<ProfileResponse> getAllProfiles() {
        return profileService.getAllProfiles();
    }

    @GetMapping("/by-category/{categoria}")
    @Operation(summary = "Get profiles by category", description = "Retrieve all profiles belonging to a specific category")
    public List<ProfileResponse> getProfilesByCategory(
            @Parameter(description = "Category name (artistas, empresas, medios, politica)", required = true)
            @PathVariable String categoria) {
        return profileService.getProfilesByCategory(categoria);
    }

    @GetMapping("/categories")
    @Operation(summary = "Get all categories", description = "Retrieve all profile categories with their counts")
    public List<CategoryResponse> getCategories() {
        return profileService.getCategories();
    }

    @PostMapping("/import")
    @Operation(summary = "Import profiles from CSV", description = "Upload a CSV file to import profiles into the system")
    public String importProfiles(
            @Parameter(description = "CSV file with profile data", required = true)
            @RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        List<Profile> profiles = csvImporter.importFromCsv(file.getInputStream());
        csvImporter.saveProfiles(profiles);

        return "Successfully imported " + profiles.size() + " profiles";
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get profile by ID", description = "Retrieve a specific profile by its UUID")
    public ProfileResponse getProfileById(
            @Parameter(description = "Profile UUID", required = true)
            @PathVariable UUID id) {
        return profileService.getProfileById(id);
    }
}
