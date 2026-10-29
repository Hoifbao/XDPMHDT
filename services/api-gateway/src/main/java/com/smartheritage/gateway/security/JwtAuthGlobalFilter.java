package com.smartheritage.gateway.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartheritage.common.security.AuthPrincipal;
import com.smartheritage.common.security.JwtService;
import com.smartheritage.common.security.UserHeaders;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Chạy cho MỌI request đi qua Gateway, trước khi chuyển tiếp:
 *  1. Đọc "Authorization: Bearer <accessToken>" và kiểm tra bằng JwtService (module common).
 *  2. Áp dụng AccessRules: thiếu quyền -> trả 401/403 ngay tại Gateway, không gọi xuống service.
 *  3. Xóa mọi header X-User-* do client tự gửi (chống giả mạo người dùng).
 *  4. Token hợp lệ -> gắn X-User-Id, X-User-Name, X-User-Role cho service phía sau đọc (CurrentUser).
 * Header Authorization vẫn được chuyển tiếp để Backend cũ (tự kiểm tra JWT) chạy như tuần 3.
 */
@Component
public class JwtAuthGlobalFilter implements GlobalFilter, Ordered {

    private static final String BEARER = "Bearer ";
    private static final String MSG_401 = "Bạn chưa đăng nhập hoặc phiên đăng nhập đã hết hạn!";
    private static final String MSG_403 = "Bạn không có quyền thực hiện thao tác này!";

    private final JwtService jwtService;
    private final AccessRules accessRules;
    private final ObjectMapper objectMapper;

    public JwtAuthGlobalFilter(JwtService jwtService, AccessRules accessRules, ObjectMapper objectMapper) {
        this.jwtService = jwtService;
        this.accessRules = accessRules;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().pathWithinApplication().value();
        Optional<AuthPrincipal> user = bearerToken(request).flatMap(jwtService::parseAccessToken);

        AccessRules.Decision decision = accessRules.check(request.getMethod(), path, user.orElse(null));
        if (decision == AccessRules.Decision.UNAUTHORIZED) {
            return writeError(exchange, HttpStatus.UNAUTHORIZED, MSG_401);
        }
        if (decision == AccessRules.Decision.FORBIDDEN) {
            return writeError(exchange, HttpStatus.FORBIDDEN, MSG_403);
        }

        ServerHttpRequest forwarded = request.mutate().headers(headers -> {
            // Xóa header X-User-* client tự gửi (không phân biệt hoa thường)
            List<String> spoofed = new ArrayList<>();
            for (String name : headers.keySet()) {
                if (name.toLowerCase(Locale.ROOT).startsWith(UserHeaders.PREFIX.toLowerCase(Locale.ROOT))) {
                    spoofed.add(name);
                }
            }
            spoofed.forEach(headers::remove);
            user.ifPresent(u -> {
                headers.set(UserHeaders.USER_ID, String.valueOf(u.userId()));
                headers.set(UserHeaders.USER_NAME, URLEncoder.encode(
                        u.username() == null ? "" : u.username(), StandardCharsets.UTF_8));
                headers.set(UserHeaders.USER_ROLE, u.role());
            });
        }).build();

        return chain.filter(exchange.mutate().request(forwarded).build());
    }

    /** Chạy sớm, trước các filter chuyển tiếp của Gateway. */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 100;
    }

    private static Optional<String> bearerToken(ServerHttpRequest request) {
        String header = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(BEARER)) {
            return Optional.of(header.substring(BEARER.length()).trim());
        }
        return Optional.empty();
    }

    /** Trả JSON giống Backend tuần 3: {"success": false, "message": "..."} */
    private Mono<Void> writeError(ServerWebExchange exchange, HttpStatus status, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(new MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8));
        byte[] body;
        try {
            body = objectMapper.writeValueAsBytes(Map.of("success", false, "message", message));
        } catch (JsonProcessingException e) {
            body = ("{\"success\":false,\"message\":\"" + status.getReasonPhrase() + "\"}")
                    .getBytes(StandardCharsets.UTF_8);
        }
        return response.writeWith(Mono.just(response.bufferFactory().wrap(body)));
    }
}
