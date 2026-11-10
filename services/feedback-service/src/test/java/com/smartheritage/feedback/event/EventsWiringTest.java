package com.smartheritage.feedback.event;

import com.smartheritage.common.events.EventPublisher;
import com.smartheritage.common.events.EventsConfig;
import com.smartheritage.feedback.config.EventsImportConfig;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Kiểm tra cách nối thư viện sự kiện vào feedback-service (không cần RabbitMQ chạy):
 * chỉ còn 1 MessageConverter (của EventsConfig) nên RabbitTemplate gửi JSON, không bị bỏ trống converter.
 */
class EventsWiringTest {

    private final ApplicationContextRunner context = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class, RabbitAutoConfiguration.class))
            .withUserConfiguration(EventsImportConfig.class, FeedbackEventPublisher.class)
            .withBean(MeterRegistry.class, SimpleMeterRegistry::new);

    @Test
    void eventsImportConfig_importEventsConfig() {
        Import imports = EventsImportConfig.class.getAnnotation(Import.class);

        assertThat(imports.value()).contains(EventsConfig.class);
    }

    @Test
    void chiCo1MessageConverter_vaRabbitTemplateDungJson() {
        context.run(ctx -> {
            assertThat(ctx).hasSingleBean(MessageConverter.class);
            assertThat(ctx).hasSingleBean(EventPublisher.class);
            assertThat(ctx).hasSingleBean(FeedbackEventPublisher.class);
            assertThat(ctx.getBean(RabbitTemplate.class).getMessageConverter())
                    .isInstanceOf(Jackson2JsonMessageConverter.class);
        });
    }
}
