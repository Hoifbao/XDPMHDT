package com.smartheritage.content;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartheritage.common.web.ApiException;
import com.smartheritage.content.client.SiteClient;
import com.smartheritage.content.repository.ArtifactContentRepository;
import com.smartheritage.content.repository.ArtifactRepository;
import com.smartheritage.content.repository.MediaResourceRepository;
import com.smartheritage.content.repository.LanguageRepository;
import com.smartheritage.content.entity.Language;
import com.smartheritage.content.events.ArtifactEventOutboxRepository;
import com.smartheritage.content.events.ArtifactEventTransport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test API hiện vật trên H2 (chế độ MySQL), site-service được giả lập.
 * Kịch bản gốc: Xuân Anh (nhánh XA-task3v4), chuyển sang module services/content-service.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:content;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.events.retry-delay-ms=3600000"
})
@AutoConfigureMockMvc
class ArtifactApiTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired ArtifactRepository artifacts;
    @Autowired ArtifactContentRepository contents;
    @Autowired MediaResourceRepository media;
    @MockitoBean SiteClient sites;
    @Autowired LanguageRepository languages;
    @Autowired ArtifactEventOutboxRepository outbox;
    @MockitoBean ArtifactEventTransport transport;
    @TempDir static Path uploads;
    @DynamicPropertySource
    static void uploadDirectory(DynamicPropertyRegistry registry) {
        registry.add("app.media.directory", () -> uploads.toString());
    }

    static final String VI = """
            {"siteId":1,"name":"Trong dong","contents":[{"languageCode":"vi","title":"Trong dong","description":"Mo ta","imageUrls":"[\\"https://example.com/a.jpg\\"]"}],
             "media":[{"mediaType":"AUDIO","fileUrl":"https://example.com/a.mp3","caption":"Audio"}]}
            """;

    @BeforeEach
    void clean() {
        reset(transport);
        outbox.deleteAll();
        languages.findAll().stream().filter(l -> !l.getCode().equals("vi") && !l.getCode().equals("en"))
                .forEach(languages::delete);
        languages.save(new Language("en", "English", true));
        media.deleteAll();
        contents.deleteAll();
        artifacts.deleteAll();
        reset(sites);
        when(sites.requireSite(1L)).thenReturn(new SiteClient.Site(1L, "Museum"));
        when(sites.findName(anyLong())).thenReturn(Optional.of("Museum"));
    }

    @Test
    void brokerFailureDoesNotFailApiAndCommittedChangesStayPending() throws Exception {
        doThrow(new IllegalStateException("broker unavailable in API test"))
                .when(transport).send(anyString(), any(), any());
        long id = create(VI);
        var entries = outbox.findAll();
        assertEquals(1, entries.size());
        org.junit.jupiter.api.Assertions.assertNull(entries.get(0).getPublishedAt());
        assertEquals(id, json.readTree(entries.get(0).getPayload()).path("artifactId").asLong());
        assertEquals(1L, artifacts.count());
    }

    @Test
    void managesLanguagesAndUsesVietnameseWhenDisabled() throws Exception {
        mvc.perform(post("/api/admin/languages").contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"fr\",\"name\":\"Français\",\"active\":true}"))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/admin/languages").contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"fr\",\"name\":\"Duplicate\",\"active\":true}"))
                .andExpect(status().isBadRequest());
        long id = create(VI);
        mvc.perform(post("/api/artifacts/{id}/contents", id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"languageCode\":\"fr\",\"title\":\"Tambour\",\"description\":\"Description française\"}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/artifacts/{id}?lang=fr", id))
                .andExpect(jsonPath("$.data.content.title").value("Tambour"))
                .andExpect(jsonPath("$.data.fallbackUsed").value(false));
        mvc.perform(put("/api/admin/languages/fr").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"French\",\"active\":false}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/languages")).andExpect(jsonPath("$.data.length()").value(2));
        mvc.perform(get("/api/admin/languages")).andExpect(jsonPath("$.data.length()").value(3));
        mvc.perform(get("/api/artifacts/{id}?lang=fr", id))
                .andExpect(jsonPath("$.data.content.languageCode").value("vi"))
                .andExpect(jsonPath("$.data.fallbackUsed").value(true));
        mvc.perform(put("/api/admin/languages/vi").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Vietnamese\",\"active\":false}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/admin/languages").contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"../bad\",\"name\":\"Invalid\",\"active\":true}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void uploadsImageAndAudioThroughGatewayOriginAndSupportsRanges() throws Exception {
        byte[] png = java.util.Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+aKxkAAAAASUVORK5CYII=");
        var result = mvc.perform(multipart("/api/media/upload")
                .file(new MockMultipartFile("file", "photo.png", "image/png", png))
                .header("X-Forwarded-Host", "museum.example").header("X-Forwarded-Proto", "https"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.data.mediaType").value("IMAGE"))
                .andReturn();
        String url = json.readTree(result.getResponse().getContentAsString()).at("/data/fileUrl").asText();
        org.junit.jupiter.api.Assertions.assertTrue(url.startsWith("https://museum.example/api/media/files/"));
        mvc.perform(get(java.net.URI.create(url).getPath())).andExpect(status().isOk())
                .andExpect(content().bytes(png)).andExpect(header().string("Content-Type", "image/png"));
        byte[] mp3 = new byte[32]; mp3[0] = 'I'; mp3[1] = 'D'; mp3[2] = '3';
        var audio = mvc.perform(multipart("/api/media/upload")
                .file(new MockMultipartFile("file", "audio.mp3", "audio/mpeg", mp3)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.data.mediaType").value("AUDIO")).andReturn();
        String audioPath = java.net.URI.create(json.readTree(audio.getResponse().getContentAsString()).at("/data/fileUrl").asText()).getPath();
        mvc.perform(get(audioPath).header("Range", "bytes=0-9")).andExpect(status().isPartialContent())
                .andExpect(header().string("Content-Range", "bytes 0-9/32"));
        mvc.perform(multipart("/api/media/upload").file(new MockMultipartFile("file", "fake.png", "image/png", mp3)))
                .andExpect(status().isBadRequest());
        mvc.perform(multipart("/api/media/upload").file(new MockMultipartFile("file", "bad.svg", "image/svg+xml", png)))
                .andExpect(status().isBadRequest());
        mvc.perform(multipart("/api/media/upload").file(new MockMultipartFile("file", "empty.mp3", "audio/mpeg", new byte[0])))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/media/files/missing.png")).andExpect(status().isNotFound());
    }

    long create(String body) throws Exception {
        var result = mvc.perform(post("/api/artifacts").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.media[0].artifactId").isNumber())
                .andExpect(jsonPath("$.data.locationName").value("Museum"))
                .andReturn();
        return json.readTree(result.getResponse().getContentAsString()).path("data").path("id").asLong();
    }

    @Test
    void fallbackAndBeaconUseSameContract() throws Exception {
        long id = create(VI);
        mvc.perform(get("/api/artifacts/{id}?lang=en", id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.requestedLanguage").value("en"))
                .andExpect(jsonPath("$.data.languageCode").value("vi"))
                .andExpect(jsonPath("$.data.fallback").value(true))
                .andExpect(jsonPath("$.data.content.title").value("Trong dong"));
        mvc.perform(put("/api/artifacts/{id}", id).contentType(MediaType.APPLICATION_JSON).content("{\"beaconUuid\":\"beacon-1\"}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/artifacts/beacon/beacon-1?lang=en")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(id)).andExpect(jsonPath("$.data.fallback").value(true));
        mvc.perform(get("/api/artifacts/{id}?lang=fr", id)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
        mvc.perform(get("/api/artifacts/beacon/missing")).andExpect(status().isNotFound());
        mvc.perform(get("/api/artifacts/999999")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
        mvc.perform(put("/api/artifacts/{id}", id).contentType(MediaType.APPLICATION_JSON).content("{\"beaconUuid\":\"\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.beaconUuid").doesNotExist());
    }

    @Test
    void englishAndMediaCanBeEditedAndDeleted() throws Exception {
        long id = create(VI);
        mvc.perform(post("/api/artifacts/{id}/contents", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"languageCode\":\"en\",\"title\":\"Bronze drum\",\"description\":\"English\"}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/artifacts/{id}?lang=en", id)).andExpect(jsonPath("$.data.name").value("Bronze drum"))
                .andExpect(jsonPath("$.data.fallback").value(false));
        String noMedia = VI.replace("\"media\":[{\"mediaType\":\"AUDIO\",\"fileUrl\":\"https://example.com/a.mp3\",\"caption\":\"Audio\"}]", "\"media\":[]");
        mvc.perform(put("/api/artifacts/{id}", id).contentType(MediaType.APPLICATION_JSON).content(noMedia))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.media").isEmpty())
                .andExpect(jsonPath("$.data.contents.length()").value(1));
        mvc.perform(delete("/api/artifacts/{id}", id)).andExpect(status().isOk());
        mvc.perform(get("/api/artifacts/{id}", id)).andExpect(status().isNotFound());
        assertEquals(0, contents.count());
        assertEquals(0, media.count());
    }

    @Test
    void rejectsMissingVietnameseAndInvalidMedia() throws Exception {
        mvc.perform(post("/api/artifacts").contentType(MediaType.APPLICATION_JSON).content(VI.replace("\"vi\"", "\"en\"")))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/artifacts").contentType(MediaType.APPLICATION_JSON)
                        .content(VI.replace("https://example.com/a.mp3", "javascript:alert(1)")))
                .andExpect(status().isBadRequest());
        assertEquals(0, artifacts.count());
    }

    @Test
    void siteMustExistAndOutageDoesNotWrite() throws Exception {
        when(sites.requireSite(1L)).thenThrow(ApiException.badRequest("Di tích không tồn tại"));
        mvc.perform(post("/api/artifacts").contentType(MediaType.APPLICATION_JSON).content(VI)).andExpect(status().isBadRequest());
        doThrow(ApiException.serviceUnavailable("site-service tắt")).when(sites).requireSite(1L);
        mvc.perform(post("/api/artifacts").contentType(MediaType.APPLICATION_JSON).content(VI))
                .andExpect(status().isServiceUnavailable());
        assertEquals(0, artifacts.count());
    }

    @Test
    void batchKeepsRequestedOrderAndSkipsMissing() throws Exception {
        long a = create(VI);
        long b = create(VI.replace("\"name\":\"Trong dong\"", "\"name\":\"Bia\""));
        mvc.perform(get("/api/artifacts/batch?ids=" + b + ",999999," + a + "," + b)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].id").value(b))
                .andExpect(jsonPath("$.data[0].name").value("Bia"))
                .andExpect(jsonPath("$.data[1].id").value(a))
                .andExpect(jsonPath("$.data[1].locationName").value("Museum"));
    }

    @Test
    void mediaApiAndHealthEndpoint() throws Exception {
        long id = create(VI);
        mvc.perform(post("/api/artifacts/{id}/media", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mediaType\":\"video\",\"fileUrl\":\"https://example.com/v.mp4\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.data.mediaType").value("VIDEO"));
        mvc.perform(get("/api/artifacts/{id}/media", id)).andExpect(jsonPath("$.data.length()").value(2));
        mvc.perform(delete("/api/media/999999")).andExpect(status().isNotFound());
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }
}
