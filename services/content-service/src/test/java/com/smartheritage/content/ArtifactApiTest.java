package com.smartheritage.content;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartheritage.common.web.ApiException;
import com.smartheritage.content.client.SiteClient;
import com.smartheritage.content.repository.ArtifactContentRepository;
import com.smartheritage.content.repository.ArtifactRepository;
import com.smartheritage.content.repository.MediaResourceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

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
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class ArtifactApiTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired ArtifactRepository artifacts;
    @Autowired ArtifactContentRepository contents;
    @Autowired MediaResourceRepository media;
    @MockitoBean SiteClient sites;

    static final String VI = """
            {"siteId":1,"name":"Trong dong","contents":[{"languageCode":"vi","title":"Trong dong","description":"Mo ta","imageUrls":"[\\"https://example.com/a.jpg\\"]"}],
             "media":[{"mediaType":"AUDIO","fileUrl":"https://example.com/a.mp3","caption":"Audio"}]}
            """;

    @BeforeEach
    void clean() {
        media.deleteAll();
        contents.deleteAll();
        artifacts.deleteAll();
        reset(sites);
        when(sites.requireSite(1L)).thenReturn(new SiteClient.Site(1L, "Museum"));
        when(sites.findName(anyLong())).thenReturn(Optional.of("Museum"));
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
