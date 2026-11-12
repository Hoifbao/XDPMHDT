package com.smartheritage.content.config;

import com.smartheritage.common.events.EventsConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/** Keep RabbitMQ wiring outside Application so MVC test slices do not import it. */
@Configuration
@Import(EventsConfig.class)
public class EventsImportConfig {
}
