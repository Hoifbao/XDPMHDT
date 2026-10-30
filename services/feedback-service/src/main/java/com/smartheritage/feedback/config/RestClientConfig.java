package com.smartheritage.feedback.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * RestClient gọi sang service khác, timeout kết nối và đọc theo app.clients.timeout (mặc định 2 giây).
 * Quá thời gian hoặc service kia tắt thì client ném lỗi, xem ArtifactClient / UserProfileClient.
 */
@Configuration
public class RestClientConfig {

    @Bean
    public RestClient contentRestClient(RestClient.Builder builder,
                                        @Value("${app.clients.content-url}") String baseUrl,
                                        @Value("${app.clients.timeout:2s}") Duration timeout) {
        return create(builder, baseUrl, timeout);
    }

    @Bean
    public RestClient authRestClient(RestClient.Builder builder,
                                     @Value("${app.clients.auth-url}") String baseUrl,
                                     @Value("${app.clients.timeout:2s}") Duration timeout) {
        return create(builder, baseUrl, timeout);
    }

    private static RestClient create(RestClient.Builder builder, String baseUrl, Duration timeout) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeout);
        factory.setReadTimeout(timeout);
        // clone(): mỗi client một builder riêng, không đè cấu hình của nhau
        return builder.clone().baseUrl(baseUrl).requestFactory(factory).build();
    }
}
