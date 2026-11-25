package com.smartheritage.gateway.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.UnknownHostException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Bảo W8: Gateway trả 503 (không phải 500) khi service phía sau tắt.
 * Chạy (sau khi mvn -DskipTests install ở thư mục gốc): mvn -pl services/api-gateway test
 */
class ServiceUnavailableHandlerTest {

    private final ServiceUnavailableHandler handler = new ServiceUnavailableHandler(new ObjectMapper());

    private static MockServerWebExchange exchange() {
        return MockServerWebExchange.from(MockServerHttpRequest.get("/api/feedbacks").build());
    }

    @Test
    void connectionRefusedTra503KemJson() {
        MockServerWebExchange exchange = exchange();
        handler.handle(exchange, new ConnectException("Connection refused: feedback-service/172.18.0.9:8086")).block();

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exchange.getResponse().getStatusCode());
        String body = exchange.getResponse().getBodyAsString().block();
        assertTrue(body.contains("\"success\":false"), body);
        assertTrue(body.contains(ServiceUnavailableHandler.MSG_503), body);
    }

    @Test
    void containerDaStopKhongPhanGiaiDuocTenHostTra503() {
        MockServerWebExchange exchange = exchange();
        handler.handle(exchange, new IllegalStateException("wrapped", new UnknownHostException("feedback-service"))).block();

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exchange.getResponse().getStatusCode());
    }

    @Test
    void containerVuaStopConGiuIpCuNoRouteToHostTra503() {
        // Lỗi Minor 12: log thật 25/11 'AnnotatedNoRouteToHostException: feedback-service/172.19.0.16:8086'
        MockServerWebExchange exchange = exchange();
        handler.handle(exchange, new IllegalStateException("wrapped", new NoRouteToHostException("feedback-service/172.19.0.16:8086"))).block();

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exchange.getResponse().getStatusCode());
        String body = exchange.getResponse().getBodyAsString().block();
        assertTrue(body.contains("\"success\":false"), body);
    }

    @Test
    void loiKhacChuyenTiepChoBoXuLyMacDinh() {
        MockServerWebExchange exchange = exchange();
        IllegalArgumentException other = new IllegalArgumentException("loi khac");

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> handler.handle(exchange, other).block());
        assertSame(other, thrown);
        assertFalse(ServiceUnavailableHandler.isConnectionFailure(other));
    }
}
