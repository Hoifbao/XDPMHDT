package com.smartheritage.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.GatewayFilterSpec;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.cloud.gateway.route.builder.UriSpec;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Bảng định tuyến (task_w4_chi_tiet.docx mục III.2). Thứ tự = order, số nhỏ khớp trước.
 * Route feedbacks PHẢI đứng trước route /api/artifacts/**.
 */
@Configuration
public class GatewayRoutesConfig {

    /** Header CORS mà Gateway và service phía sau có thể cùng gắn: chỉ giữ giá trị của Gateway. */
    private static final String CORS_HEADERS = "Access-Control-Allow-Origin Access-Control-Allow-Credentials "
            + "Access-Control-Allow-Methods Access-Control-Allow-Headers Access-Control-Max-Age "
            + "Access-Control-Expose-Headers";

    @Bean
    public RouteLocator smartHeritageRoutes(RouteLocatorBuilder builder, RouteTargets t) {
        return builder.routes()
                .route("auth-service", r -> r.order(1)
                        .path("/api/auth/**")
                        .filters(GatewayRoutesConfig::common).uri(t.auth()))
                .route("site-service", r -> r.order(2)
                        .path("/api/heritage-sites/**")
                        .filters(GatewayRoutesConfig::common).uri(t.site()))
                // Tuần 5: feedback-service 8086. Phải đặt TRƯỚC /api/artifacts/**
                .route("feedback-service", r -> r.order(3)
                        .path("/api/artifacts/*/feedbacks", "/api/artifacts/*/feedbacks/**", "/api/feedbacks/**")
                        .filters(GatewayRoutesConfig::common).uri(t.feedback()))
                .route("content-service", r -> r.order(4)
                        .path("/api/artifacts/**", "/api/media/**")
                        .filters(GatewayRoutesConfig::common).uri(t.content()))
                .route("beacon-service", r -> r.order(5)
                        .path("/api/ibeacons/**")
                        .filters(GatewayRoutesConfig::common).uri(t.beacon()))
                // Tuần 5: personalization-service 8085 (/api/users/** là API cũ của Phú)
                .route("personalization-service", r -> r.order(6)
                        .path("/api/me/**", "/api/admin/visits/**", "/api/users/**")
                        .filters(GatewayRoutesConfig::common).uri(t.personalization()))
                // Tuần 5: analytics-service 8087
                .route("analytics-service", r -> r.order(7)
                        .path("/api/statistics/**")
                        .filters(GatewayRoutesConfig::common).uri(t.analytics()))
                // Còn lại (kể cả /api/demo/**, Swagger của Backend cũ) -> Backend cũ 8090
                .route("legacy-backend", r -> r.order(100)
                        .path("/**")
                        .filters(GatewayRoutesConfig::common).uri(t.legacy()))
                .build();
    }

    private static UriSpec common(GatewayFilterSpec f) {
        return f.dedupeResponseHeader(CORS_HEADERS, "RETAIN_FIRST");
    }
}
