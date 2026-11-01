package com.smartheritage.beacon.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * RestClient gọi site-service và content-service, timeout theo app.clients.timeout (mặc định 2 giây).
 * site-service còn thiếu (Ân) / khi cần quay về Backend cũ: đổi biến SITE_SERVICE_URL / CONTENT_SERVICE_URL.
 */
@Configuration
public class RestClientConfig {

    @Bean
    public RestClient siteRestClient(RestClient.Builder builder,
                                     @Value("${app.clients.site-url}") String baseUrl,
                                     @Value("${app.clients.timeout:2s}") Duration timeout) {
        return restClient(builder, baseUrl, timeout);
    }

    @Bean
    public RestClient contentRestClient(RestClient.Builder builder,
                                        @Value("${app.clients.content-url}") String baseUrl,
                                        @Value("${app.clients.timeout:2s}") Duration timeout) {
        return restClient(builder, baseUrl, timeout);
    }

    private static RestClient restClient(RestClient.Builder builder, String baseUrl, Duration timeout) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeout);
        factory.setReadTimeout(timeout);
        return builder.clone().baseUrl(baseUrl).requestFactory(factory).build();
    }
}
