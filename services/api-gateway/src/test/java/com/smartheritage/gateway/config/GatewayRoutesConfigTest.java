package com.smartheritage.gateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.http.HttpMethod;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Kiểm tra bảng định tuyến tuần 5 (task_w5 mục III.1): đường dẫn nào tới service nào.
 * Lấy route khớp đầu tiên theo order, giống cách Gateway chọn route khi chạy thật.
 * Chạy: mvn -pl services/api-gateway test
 */
@SpringBootTest
class GatewayRoutesConfigTest {

    @Test
    void languagesAndUploadsReachContentService() {
        for (String path : List.of("/api/languages", "/api/admin/languages", "/api/admin/languages/fr",
                "/api/media/upload", "/api/media/files/test.mp3")) {
            Route route = routeFor(HttpMethod.GET, path);
            assertEquals("content-service", route.getId(), path);
            assertEquals("http://localhost:8083", route.getUri().toString(), path);
        }
    }

    @Autowired
    private RouteLocator routeLocator;

    @Test
    void feedback_rating_admin_toi_feedbackService8086() {
        for (String path : List.of("/api/artifacts/1/feedbacks", "/api/artifacts/1/rating-summary",
                "/api/admin/feedbacks", "/api/admin/feedbacks/3/status")) {
            Route route = routeFor(HttpMethod.GET, path);
            assertEquals("feedback-service", route.getId(), path);
            assertEquals("http://localhost:8086", route.getUri().toString(), path);
        }
        assertEquals("feedback-service", routeFor(HttpMethod.POST, "/api/artifacts/2/feedbacks").getId());
        assertEquals("feedback-service", routeFor(HttpMethod.PATCH, "/api/admin/feedbacks/3/status").getId());
    }

    @Test
    void hienVatVanToiContentService_khongBiFeedbackBatNham() {
        assertEquals("content-service", routeFor(HttpMethod.GET, "/api/artifacts/1").getId());
        assertEquals("content-service", routeFor(HttpMethod.GET, "/api/artifacts/1/contents").getId());
        assertEquals("content-service", routeFor(HttpMethod.GET, "/api/artifacts").getId());
    }

    @Test
    void adminVisitsVanToiPersonalization() {
        assertEquals("personalization-service", routeFor(HttpMethod.GET, "/api/admin/visits").getId());
    }

    /** Khoa W5: module notification của beacon-service - /api/me/devices có trước /api/me/** chung. */
    @Test
    void fcmDevicesToiBeaconService8084() {
        for (String path : List.of("/api/me/devices", "/api/me/devices/test", "/api/me/devices/3")) {
            // URI không assert tuyệt đối: BEACON_SERVICE_URL không đặt trong test -> trả về legacy 8090
            assertEquals("beacon-service", routeFor(HttpMethod.POST, path).getId(), path);
        }
        // Các đường /api/me còn lại vẫn về personalization (Phú)
        assertEquals("personalization-service", routeFor(HttpMethod.POST, "/api/me/visits").getId());
        assertEquals("personalization-service", routeFor(HttpMethod.GET, "/api/me/favorites").getId());
    }

    /** Bảo W5: Swagger chung - mỗi /v3/api-docs/{tên} tới đúng service. */
    @Test
    void apiDocsToiDungService() {
        assertEquals("http://localhost:8081", docRoute("auth").getUri().toString());
        assertEquals("http://localhost:8082", docRoute("site").getUri().toString());
        assertEquals("http://localhost:8085", docRoute("personalization").getUri().toString());
        assertEquals("http://localhost:8086", docRoute("feedback").getUri().toString());
        assertEquals("http://localhost:8090", docRoute("legacy").getUri().toString());
        // content/beacon/analytics: địa chỉ giống route API của chúng (test không đặt *_SERVICE_URL -> 8090)
        for (String name : List.of("content", "beacon", "analytics")) {
            assertEquals(routeFor(HttpMethod.GET, apiPathOf(name)).getUri(), docRoute(name).getUri(), name);
        }
    }

    /** File tĩnh Swagger UI do Gateway phục vụ, không được chuyển sang Backend cũ. */
    @Test
    void swaggerUiKhongRoiVaoBackendCu() {
        for (String path : List.of("/swagger-ui/index.html", "/swagger-ui/swagger-initializer.js",
                "/webjars/swagger-ui/index.html")) {
            assertTrue(firstRoute(HttpMethod.GET, path).isEmpty(), path);
        }
        // Các đường khác vẫn về Backend cũ như trước
        assertEquals("legacy-backend", routeFor(HttpMethod.GET, "/api/demo/redis").getId());
        assertEquals("legacy-backend", routeFor(HttpMethod.GET, "/v3/api-docs/khong-co").getId());
    }

    private Route docRoute(String name) {
        Route route = routeFor(HttpMethod.GET, "/v3/api-docs/" + name);
        assertEquals("api-docs-" + name, route.getId());
        return route;
    }

    private static String apiPathOf(String name) {
        return switch (name) {
            case "content" -> "/api/artifacts/1";
            case "beacon" -> "/api/ibeacons/registry";
            case "analytics" -> "/api/statistics/overview";
            default -> throw new IllegalArgumentException(name);
        };
    }

    private Route routeFor(HttpMethod method, String path) {
        return firstRoute(method, path).orElseThrow(() -> new AssertionError("Không có route cho " + path));
    }

    private Optional<Route> firstRoute(HttpMethod method, String path) {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.method(method, path).build());
        List<Route> routes = routeLocator.getRoutes().collectList().block();
        return routes.stream()
                .sorted(Comparator.comparingInt(Route::getOrder))
                .filter(route -> Boolean.TRUE.equals(Mono.from(route.getPredicate().apply(exchange)).block()))
                .findFirst();
    }
}
