package com.smartheritage.gateway.config;

import com.smartheritage.gateway.docs.OpenApiDocRewriter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.GatewayFilterSpec;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.cloud.gateway.route.builder.UriSpec;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Bảng định tuyến (task_w4_chi_tiet.docx mục III.2). Thứ tự = order, số nhỏ khớp trước.
 * Route feedbacks PHẢI đứng trước route /api/artifacts/**.
 * Bảo W5: thêm route tài liệu /v3/api-docs/{service} cho Swagger UI chung tại Gateway.
 */
@Configuration
public class GatewayRoutesConfig {

    /** Header CORS mà Gateway và service phía sau có thể cùng gắn: chỉ giữ giá trị của Gateway. */
    private static final String CORS_HEADERS = "Access-Control-Allow-Origin Access-Control-Allow-Credentials "
            + "Access-Control-Allow-Methods Access-Control-Allow-Headers Access-Control-Max-Age "
            + "Access-Control-Expose-Headers";

    /** Tài nguyên của Swagger UI do chính Gateway phục vụ: KHÔNG được rơi vào route Backend cũ. */
    static final String[] SWAGGER_UI_ASSETS = {"/webjars/**", "/swagger-ui/**"};

    @Bean
    public RouteLocator smartHeritageRoutes(RouteLocatorBuilder builder, RouteTargets t, OpenApiDocRewriter docs) {
        RouteLocatorBuilder.Builder routes = builder.routes();

        // Bảo W5 (task_w5 mục III.1): /v3/api-docs/{service} -> /v3/api-docs của service đó.
        // Dùng đúng địa chỉ mà route API đang dùng, nên tài liệu luôn khớp nơi request thật được chuyển tới.
        // /v3/api-docs/swagger-config do springdoc của Gateway trả, không qua route.
        apiDocTargets(t).forEach((name, uri) -> routes.route("api-docs-" + name, r -> r.order(0)
                .path("/v3/api-docs/" + name)
                .filters(f -> f.setPath("/v3/api-docs")
                        .modifyResponseBody(String.class, String.class,
                                (exchange, body) -> Mono.justOrEmpty(docs.rewrite(body))))
                .uri(uri)));

        return routes
                .route("auth-service", r -> r.order(1)
                        .path("/api/auth/**")
                        .filters(GatewayRoutesConfig::common).uri(t.auth()))
                .route("site-service", r -> r.order(2)
                        .path("/api/heritage-sites/**")
                        .filters(GatewayRoutesConfig::common).uri(t.site()))
                // Tuần 5: feedback-service 8086 (task_w5 mục III.1). Phải đặt TRƯỚC /api/artifacts/** (content)
                // và /api/admin/** của personalization; quyền ADMIN cho /api/admin/feedbacks/** do AccessRules kiểm.
                .route("feedback-service", r -> r.order(3)
                        .path("/api/artifacts/*/feedbacks", "/api/artifacts/*/feedbacks/**",
                                "/api/artifacts/*/rating-summary",
                                "/api/admin/feedbacks", "/api/admin/feedbacks/**",
                                "/api/feedbacks/**")
                        .filters(GatewayRoutesConfig::common).uri(t.feedback()))
                .route("content-service", r -> r.order(4)
                        .path("/api/artifacts/**", "/api/media/**", "/api/languages", "/api/admin/languages", "/api/admin/languages/**")
                        .filters(GatewayRoutesConfig::common).uri(t.content()))
                .route("beacon-service", r -> r.order(5)
                        // Khoa W5: /api/me/devices/** = module notification của beacon-service
                        // (đăng ký FCM token). Route này PHẢI trước route personalization (order 6).
                        .path("/api/ibeacons/**", "/api/me/devices", "/api/me/devices/**")
                        .filters(GatewayRoutesConfig::common).uri(t.beacon()))
                // Tuần 5: personalization-service 8085 (/api/users/** là API cũ của Phú)
                .route("personalization-service", r -> r.order(6)
                        .path("/api/me/**", "/api/admin/visits/**", "/api/users/**")
                        .filters(GatewayRoutesConfig::common).uri(t.personalization()))
                // Tuần 5: analytics-service 8087
                .route("analytics-service", r -> r.order(7)
                        .path("/api/statistics/**")
                        .filters(GatewayRoutesConfig::common).uri(t.analytics()))
                // Còn lại (kể cả /api/demo/**) -> Backend cũ 8090.
                // Trừ file tĩnh của Swagger UI tại Gateway. Swagger Backend cũ: chọn "backend-cu (8090)"
                // trong Swagger chung, hoặc mở thẳng http://localhost:8090/swagger-ui/index.html
                .route("legacy-backend", r -> r.order(100)
                        .path("/**").and().not(p -> p.path(SWAGGER_UI_ASSETS))
                        .filters(GatewayRoutesConfig::common).uri(t.legacy()))
                .build();
    }

    /** Tên trong đường dẫn /v3/api-docs/{tên} -> địa chỉ service (thứ tự = thứ tự trong Swagger UI). */
    static Map<String, String> apiDocTargets(RouteTargets t) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("auth", t.auth());
        m.put("site", t.site());
        m.put("content", t.content());
        m.put("beacon", t.beacon());
        m.put("personalization", t.personalization());
        m.put("feedback", t.feedback());
        m.put("analytics", t.analytics());
        m.put("legacy", t.legacy());
        return m;
    }

    private static UriSpec common(GatewayFilterSpec f) {
        return f.dedupeResponseHeader(CORS_HEADERS, "RETAIN_FIRST");
    }
}
