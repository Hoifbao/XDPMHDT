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

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Kiểm tra bảng định tuyến tuần 5 (task_w5 mục III.1): đường dẫn nào tới service nào.
 * Lấy route khớp đầu tiên theo order, giống cách Gateway chọn route khi chạy thật.
 * Chạy: mvn -pl services/api-gateway test
 */
@SpringBootTest
class GatewayRoutesConfigTest {

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

    private Route routeFor(HttpMethod method, String path) {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.method(method, path).build());
        List<Route> routes = routeLocator.getRoutes().collectList().block();
        return routes.stream()
                .sorted(Comparator.comparingInt(Route::getOrder))
                .filter(route -> Boolean.TRUE.equals(Mono.from(route.getPredicate().apply(exchange)).block()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Không có route cho " + path));
    }
}
