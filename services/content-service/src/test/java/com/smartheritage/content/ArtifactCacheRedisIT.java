package com.smartheritage.content;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartheritage.content.client.SiteClient;
import com.smartheritage.content.dto.*;
import com.smartheritage.content.events.*;
import com.smartheritage.content.service.*;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@EnabledIfEnvironmentVariable(named = "SM_RUN_REDIS_TESTS", matches = "true")
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:redis-it;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa",
        "spring.datasource.password=", "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.generate_statistics=true", "app.events.retry-delay-ms=3600000",
        "app.detail-cache.enabled=true", "app.detail-cache.namespace=xa:w7:redis-it"
})
class ArtifactCacheRedisIT {
    @Autowired ArtifactService service;
    @Autowired ArtifactDetailCache cache;
    @Autowired ArtifactReadLoader reads;
    @Autowired StringRedisTemplate redis;
    @Autowired ObjectMapper json;
    @Autowired EntityManagerFactory entities;
    @Autowired PlatformTransactionManager transactions;
    @MockitoBean ArtifactEventTransport transport;
    @MockitoBean SiteClient sites;
    private Long id;

    @BeforeEach
    void prepare() throws Exception {
        when(sites.requireSite(1L)).thenReturn(new SiteClient.Site(1L, "Museum"));
        when(sites.findName(anyLong())).thenReturn(Optional.of("Museum"));
        id = service.createArtifact(json.readValue(ArtifactApiTest.VI, ArtifactRequest.class)).getId();
    }
    @AfterEach
    void cleanup() {
        redis.delete(redis.keys("xa:w7:redis-it:*"));
    }

    @Test
    void realCacheHitAvoidsDetailQueriesAndCommittedChangeEvicts() throws Exception {
        var stats = entities.unwrap(SessionFactory.class).getStatistics();
        stats.clear();
        service.localize(service.getArtifactById(id), "en");
        assertThat(stats.getPrepareStatementCount()).isEqualTo(4);
        stats.clear();
        var hit = service.localize(service.getArtifactById(id), "vi");
        assertThat(hit.getRequestedLanguage()).isEqualTo("vi");
        assertThat(stats.getPrepareStatementCount()).isEqualTo(1);
        var edit = new ArtifactRequest(); edit.setName("After commit");
        service.updateArtifact(id, edit);
        assertThat(redis.hasKey(cache.dataKey(id))).isFalse();
        assertThat(service.getArtifactById(id).getName()).isEqualTo("After commit");
        service.upsertContent(id, json.readValue("""
                {"languageCode":"vi","title":"New translation","description":"Updated description"}
                """, ArtifactContentRequest.class));
        assertThat(service.localize(service.getArtifactById(id), "vi").getName()).isEqualTo("New translation");
        service.deleteArtifact(id);
        assertThatThrownBy(() -> service.getArtifactById(id)).hasMessageContaining("Không tìm thấy");
    }

    @Test
    void rollbackKeepsCacheAndStaleInFlightReadCannotRefillAfterInvalidation() {
        service.getArtifactById(id);
        String saved = redis.opsForValue().get(cache.dataKey(id));
        new TransactionTemplate(transactions).executeWithoutResult(tx -> {
            var edit = new ArtifactRequest(); edit.setName("Rolled back");
            service.updateArtifact(id, edit); tx.setRollbackOnly();
        });
        assertThat(redis.opsForValue().get(cache.dataKey(id))).isEqualTo(saved);
        cache.changed(new ArtifactChanges.Changed(id));
        cache.get(id, () -> {
            var old = reads.detail(id);
            cache.changed(new ArtifactChanges.Changed(id));
            return old;
        });
        assertThat(redis.hasKey(cache.dataKey(id))).isFalse();
    }
}
