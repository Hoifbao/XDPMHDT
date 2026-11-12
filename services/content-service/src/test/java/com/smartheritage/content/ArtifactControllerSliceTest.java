package com.smartheritage.content;

import com.smartheritage.common.events.EventPublisher;
import com.smartheritage.content.controller.ArtifactController;
import com.smartheritage.content.service.ArtifactService;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ArtifactController.class)
class ArtifactControllerSliceTest {
    @Autowired MockMvc mvc;
    @Autowired ApplicationContext context;
    @MockitoBean ArtifactService artifacts;

    @Test
    void mvcSliceStartsWithoutRabbitDependencies() throws Exception {
        when(artifacts.getAllArtifacts()).thenReturn(List.of());
        mvc.perform(get("/api/artifacts")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
        assertThat(context.getBeansOfType(RabbitTemplate.class)).isEmpty();
        assertThat(context.getBeansOfType(EventPublisher.class)).isEmpty();
    }
}
