package com.socialanalytics.profile.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getAllProfilesReturnsOk() throws Exception {
        mockMvc.perform(get("/api/v1/profiles"))
                .andExpect(status().isOk());
    }

    @Test
    void getCategoriesReturnsOk() throws Exception {
        mockMvc.perform(get("/api/v1/profiles/categories"))
                .andExpect(status().isOk());
    }

    @Test
    void getProfilesByCategoryReturnsOk() throws Exception {
        mockMvc.perform(get("/api/v1/profiles/by-category/artistas"))
                .andExpect(status().isOk());
    }

    @Test
    void getProfileByIdReturns404WhenNotFound() throws Exception {
        UUID nonExistentId = UUID.randomUUID();
        mockMvc.perform(get("/api/v1/profiles/" + nonExistentId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    void importProfilesReturnsSuccess() throws Exception {
        String csvContent = "nombre,descripcion,foto,categoria\n" +
                "Test Profile,Description,https://example.com/test.jpg,artistas";
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "profiles.csv",
                "text/csv",
                csvContent.getBytes()
        );

        mockMvc.perform(multipart("/api/v1/profiles/import").file(file))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Successfully imported")));
    }

    @Test
    void importProfilesReturnsErrorWhenFileIsEmpty() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "empty.csv",
                "text/csv",
                new byte[0]
        );

        mockMvc.perform(multipart("/api/v1/profiles/import").file(file))
                .andExpect(status().isBadRequest());
    }
}
