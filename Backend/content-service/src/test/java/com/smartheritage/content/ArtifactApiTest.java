package com.smartheritage.content;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartheritage.content.repository.*;
import com.smartheritage.content.service.SiteClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:content;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
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
 @BeforeEach void clean() {
  media.deleteAll(); contents.deleteAll(); artifacts.deleteAll();
  when(sites.requireSite(1L)).thenReturn(new SiteClient.Site(1L,"Museum"));
 }
 long create() throws Exception {
  var result = mvc.perform(post("/api/artifacts").contentType(MediaType.APPLICATION_JSON).content(VI))
      .andExpect(status().isCreated()).andExpect(jsonPath("$.data.media[0].artifactId").isNumber()).andReturn();
  return json.readTree(result.getResponse().getContentAsString()).path("data").path("id").asLong();
 }
 @Test void fallbackAndBeaconUseSameContract() throws Exception {
  long id = create();
  mvc.perform(get("/api/artifacts/{id}?lang=en",id)).andExpect(status().isOk())
    .andExpect(jsonPath("$.data.requestedLanguage").value("en"))
    .andExpect(jsonPath("$.data.languageCode").value("vi"))
    .andExpect(jsonPath("$.data.fallback").value(true))
    .andExpect(jsonPath("$.data.content.title").value("Trong dong"));
  mvc.perform(put("/api/artifacts/{id}",id).contentType(MediaType.APPLICATION_JSON).content("{\"beaconUuid\":\"beacon-1\"}"))
    .andExpect(status().isOk());
  mvc.perform(get("/api/artifacts/beacon/beacon-1?lang=en")).andExpect(status().isOk())
    .andExpect(jsonPath("$.data.id").value(id)).andExpect(jsonPath("$.data.fallback").value(true));
  mvc.perform(get("/api/artifacts/{id}?lang=fr",id)).andExpect(status().isBadRequest());
  mvc.perform(get("/api/artifacts/beacon/missing")).andExpect(status().isNotFound());
  mvc.perform(put("/api/artifacts/{id}",id).contentType(MediaType.APPLICATION_JSON).content("{\"beaconUuid\":\"\"}"))
    .andExpect(status().isOk()).andExpect(jsonPath("$.data.beaconUuid").doesNotExist());
 }
 @Test void englishAndMediaCanBeEditedAndDeleted() throws Exception {
  long id = create();
  mvc.perform(post("/api/artifacts/{id}/contents",id).contentType(MediaType.APPLICATION_JSON)
    .content("{\"languageCode\":\"en\",\"title\":\"Bronze drum\",\"description\":\"English\"}"))
    .andExpect(status().isOk());
  mvc.perform(get("/api/artifacts/{id}?lang=en",id)).andExpect(jsonPath("$.data.name").value("Bronze drum"))
    .andExpect(jsonPath("$.data.fallback").value(false));
  mvc.perform(put("/api/artifacts/{id}",id).contentType(MediaType.APPLICATION_JSON).content(VI.replace("\"media\":[{\"mediaType\":\"AUDIO\",\"fileUrl\":\"https://example.com/a.mp3\",\"caption\":\"Audio\"}]", "\"media\":[]")))
    .andExpect(status().isOk()).andExpect(jsonPath("$.data.media").isEmpty()).andExpect(jsonPath("$.data.contents.length()").value(1));
  mvc.perform(delete("/api/artifacts/{id}",id)).andExpect(status().isOk());
  mvc.perform(get("/api/artifacts/{id}",id)).andExpect(status().isNotFound());
  org.junit.jupiter.api.Assertions.assertEquals(0, contents.count());
  org.junit.jupiter.api.Assertions.assertEquals(0, media.count());
 }
 @Test void rejectsMissingVietnameseAndInvalidMedia() throws Exception {
  mvc.perform(post("/api/artifacts").contentType(MediaType.APPLICATION_JSON).content(VI.replace("\"vi\"","\"en\""))).andExpect(status().isBadRequest());
  mvc.perform(post("/api/artifacts").contentType(MediaType.APPLICATION_JSON).content(VI.replace("https://example.com/a.mp3","javascript:alert(1)"))).andExpect(status().isBadRequest());
  org.junit.jupiter.api.Assertions.assertEquals(0, artifacts.count());
 }
 @Test void siteMustExistAndOutageDoesNotWrite() throws Exception {
  when(sites.requireSite(1L)).thenThrow(new IllegalArgumentException("Unknown siteId"));
  mvc.perform(post("/api/artifacts").contentType(MediaType.APPLICATION_JSON).content(VI)).andExpect(status().isBadRequest());
  doThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE)).when(sites).requireSite(1L);
  mvc.perform(post("/api/artifacts").contentType(MediaType.APPLICATION_JSON).content(VI)).andExpect(status().isServiceUnavailable());
  org.junit.jupiter.api.Assertions.assertEquals(0, artifacts.count());
 }
}
