package com.smartheritage.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Địa chỉ các service phía sau Gateway (app.routes.* trong application.yml).
 * Service nào chưa tách thì để trỏ về Backend cũ (legacy, cổng 8090): demo không bị gãy.
 * Khi tách xong: chỉ đổi địa chỉ trong application.yml (hoặc biến môi trường), không sửa code.
 */
@ConfigurationProperties(prefix = "app.routes")
public record RouteTargets(
        String auth,
        String site,
        String content,
        String beacon,
        String personalization,
        String feedback,
        String analytics,
        String legacy) {
}
