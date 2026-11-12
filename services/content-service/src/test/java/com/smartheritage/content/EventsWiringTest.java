package com.smartheritage.content;

import com.smartheritage.common.events.EventPublisher;
import com.smartheritage.common.events.EventsConfig;
import com.smartheritage.content.config.EventsImportConfig;
import com.smartheritage.content.events.ArtifactEventTransport;
import com.smartheritage.content.events.RabbitArtifactEventTransport;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

class EventsWiringTest {
    @Test
    void applicationYamlWiresConfirmedJsonPublisherOutsideApplication() {
        assertThat(EventsImportConfig.class.getAnnotation(Import.class).value()).contains(EventsConfig.class);
        assertThat(ContentServiceApplication.class.getAnnotation(Import.class).value()).doesNotContain(EventsConfig.class);
        new ApplicationContextRunner()
                .withInitializer(new ConfigDataApplicationContextInitializer())
                .withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class, RabbitAutoConfiguration.class))
                .withUserConfiguration(EventsImportConfig.class, RabbitArtifactEventTransport.class)
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(EventPublisher.class).hasSingleBean(ArtifactEventTransport.class)
                            .hasSingleBean(MessageConverter.class);
                    RabbitTemplate template = ctx.getBean(RabbitTemplate.class);
                    assertThat(template.getMessageConverter()).isInstanceOf(Jackson2JsonMessageConverter.class);
                    assertThat(template.isMandatoryFor(null)).isTrue();
                    CachingConnectionFactory factory = ctx.getBean(CachingConnectionFactory.class);
                    assertThat(factory.isPublisherConfirms()).isTrue();
                    assertThat(factory.isPublisherReturns()).isTrue();
                });
    }
}
