package com.smartheritage.backend.service;

import com.smartheritage.backend.controller.ArtifactController;
import com.smartheritage.backend.dto.*;
import com.smartheritage.backend.entity.*;
import com.smartheritage.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ArtifactLocalizationTest {
    ArtifactRepository artifacts = mock(ArtifactRepository.class);
    ArtifactContentRepository contents = mock(ArtifactContentRepository.class);
    HeritageSiteRepository sites = mock(HeritageSiteRepository.class);
    MediaResourceRepository media = mock(MediaResourceRepository.class);
    ArtifactService service = new ArtifactService(artifacts, contents, sites, media);
    MockMvc mvc;
    Artifact artifact;
    ArtifactContent vi;

    @BeforeEach void setup() {
        artifact = Artifact.builder().id(1L).name("Original")
            .site(HeritageSite.builder().id(2L).name("Site").build()).build();
        vi = ArtifactContent.builder().id(10L).artifact(artifact).languageCode("vi")
            .title("Viet").description("Mo ta").build();
        when(artifacts.findById(1L)).thenReturn(Optional.of(artifact));
        when(artifacts.findByBeaconUuid("uuid")).thenReturn(Optional.of(artifact));
        when(contents.findByArtifactId(1L)).thenReturn(List.of(vi));
        when(contents.findByArtifactIdAndLanguageCode(1L,"vi")).thenReturn(Optional.of(vi));
        mvc = MockMvcBuilders.standaloneSetup(new ArtifactController(service)).build();
    }
    @Test void englishTranslationSelected() throws Exception {
        var en = ArtifactContent.builder().id(11L).artifact(artifact).languageCode("en")
            .title("English").description("Description").build();
        when(contents.findByArtifactIdAndLanguageCode(1L,"en")).thenReturn(Optional.of(en));
        mvc.perform(get("/api/artifacts/1?lang=en")).andExpect(status().isOk())
            .andExpect(jsonPath("$.data.name").value("English"))
            .andExpect(jsonPath("$.data.languageCode").value("en"))
            .andExpect(jsonPath("$.data.fallback").value(false));
    }
    @Test void detailAndBeaconFallbackAndDefault() throws Exception {
        for (String path : List.of("/api/artifacts/1", "/api/artifacts/beacon/uuid")) {
            mvc.perform(get(path+"?lang=en")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.requestedLanguage").value("en"))
                .andExpect(jsonPath("$.data.languageCode").value("vi"))
                .andExpect(jsonPath("$.data.content.title").value("Viet"))
                .andExpect(jsonPath("$.data.fallback").value(true));
            mvc.perform(get(path)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.requestedLanguage").value("vi"))
                .andExpect(jsonPath("$.data.fallback").value(false));
            mvc.perform(get(path+"?lang=fr")).andExpect(status().isBadRequest());
        }
    }
    @Test void missingArtifactReturns404() throws Exception {
        mvc.perform(get("/api/artifacts/999?lang=en")).andExpect(status().isNotFound());
    }
    @Test void vietnameseRequiredAndProtected() {
        var request = new ArtifactRequest(); request.setName("New"); request.setSiteId(2L);
        assertThrows(IllegalArgumentException.class, () -> service.createArtifact(request));
        when(contents.findById(10L)).thenReturn(Optional.of(vi));
        assertThrows(IllegalArgumentException.class, () -> service.deleteContent(10L));
        verify(contents, never()).delete(any());
        verify(artifacts, never()).save(any());
    }
}
