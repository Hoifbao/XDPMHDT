package com.smartheritage.gateway.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.ConnectException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Bảo W8: service phía sau tắt / không kết nối được -> Gateway trả 503 thay vì 500.
 *  - ConnectException: container đang chạy nhưng service chưa mở cổng ("Connection refused"),
 *    gồm cả AnnotatedConnectException và ConnectTimeoutException của Netty (đều là lớp con).
 *  - UnknownHostException: container đã stop nên tên host trong Docker (vd. feedback-service) không còn.
 * Các lỗi khác chuyển tiếp cho bộ xử lý mặc định của Spring Boot (giữ nguyên hành vi cũ).
 * Order(-2): chạy trước DefaultErrorWebExceptionHandler (Order -1).
 */
@Component
@Order(-2)
public class ServiceUnavailableHandler implements ErrorWebExceptionHandler {

    static final String MSG_503 = "Dịch vụ tạm thời không khả dụng, vui lòng thử lại sau!";

    private final ObjectMapper objectMapper;

    public ServiceUnavailableHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();
        if (!isConnectionFailure(ex) || response.isCommitted()) {
            return Mono.error(ex);
        }
        response.setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
        response.getHeaders().setContentType(new MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8));
        byte[] body;
        try {
            // Cùng dạng JSON với lỗi 401/403 của JwtAuthGlobalFilter
            body = objectMapper.writeValueAsBytes(Map.of("success", false, "message", MSG_503));
        } catch (JsonProcessingException e) {
            body = "{\"success\":false,\"message\":\"Service Unavailable\"}".getBytes(StandardCharsets.UTF_8);
        }
        return response.writeWith(Mono.just(response.bufferFactory().wrap(body)));
    }

    /** Duyệt cả chuỗi cause vì Reactor/Netty có thể bọc lỗi kết nối trong lỗi khác. */
    static boolean isConnectionFailure(Throwable ex) {
        for (Throwable t = ex; t != null; t = (t.getCause() == t ? null : t.getCause())) {
            if (t instanceof ConnectException || t instanceof UnknownHostException) {
                return true;
            }
        }
        return false;
    }
}
