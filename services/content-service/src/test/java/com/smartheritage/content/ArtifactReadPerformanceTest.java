package com.smartheritage.content;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartheritage.content.client.SiteClient;
import com.smartheritage.content.dto.ArtifactRequest;
import com.smartheritage.content.events.ArtifactEventTransport;
import com.smartheritage.content.service.ArtifactService;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:readperf;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa",
        "spring.datasource.password=", "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.generate_statistics=true", "app.events.retry-delay-ms=3600000"
})
class ArtifactReadPerformanceTest {
    @Autowired ArtifactService service;
    @Autowired ObjectMapper json;
    @Autowired EntityManagerFactory entities;
    @MockitoBean SiteClient sites;
    @MockitoBean ArtifactEventTransport transport;

    @Test
    void detailUsesFourQueriesAndSiteCallDoesNotHoldDatabaseTransaction() throws Exception {
        when(sites.requireSite(1L)).thenReturn(new SiteClient.Site(1L, "Museum"));
        when(sites.findName(anyLong())).thenReturn(Optional.of("Museum"));
        long id = service.createArtifact(json.readValue(ArtifactApiTest.VI, ArtifactRequest.class)).getId();
        var stats = entities.unwrap(SessionFactory.class).getStatistics();
        stats.clear();
        when(sites.findName(1L)).thenAnswer(call -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            return Optional.of("Museum");
        });
        var result = service.localize(service.getArtifactById(id), "en");
        assertThat(result.isFallback()).isTrue();
        assertThat(stats.getPrepareStatementCount()).isEqualTo(4); // artifact + contents + media + language
        stats.clear();
        clearInvocations(sites);
        service.getBatch(java.util.List.of(id, id));
        assertThat(stats.getPrepareStatementCount()).isEqualTo(1);
        verify(sites, times(1)).findName(1L);
    }
}
