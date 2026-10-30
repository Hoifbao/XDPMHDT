package com.smartheritage.content.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/** RestClient gọi site-service, timeout theo app.clients.timeout (mặc định 2 giây). */
@Configuration
public class RestClientConfig {

    @Bean
    public RestClient siteRestClient(RestClient.Builder builder,
                                     @Value("${app.clients.site-url}") String baseUrl,
                                     @Value("${app.clients.timeout:2s}") Duration timeout) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeout);
        factory.setReadTimeout(timeout);
        return builder.clone().baseUrl(baseUrl).requestFactory(factory).build();
    }
}
