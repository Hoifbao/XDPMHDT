package com.smartheritage.feedback.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.smartheritage.common.security.UserHeaders;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.Optional;

/**
 * Lấy tên hiển thị (fullName) của người đang gửi đánh giá từ auth-service GET /api/auth/me.
 * feedback-service không đọc được bảng users (nằm ở auth_db) nên chụp lại tên lúc gửi vào cột reviewer_name.
 * Chuyển tiếp nguyên 3 header X-User-* mà Gateway đã gắn, nên auth-service chỉ trả hồ sơ của chính người gửi.
 * Mọi lỗi (auth-service tắt, timeout, 401...) đều trả Optional.empty(): không chặn việc gửi đánh giá.
 */
@Component
public class UserProfileClient {

    private static final Logger log = LoggerFactory.getLogger(UserProfileClient.class);
    private static final int MAX_NAME_LENGTH = 100;
    private static final List<String> FORWARDED_HEADERS =
            List.of(UserHeaders.USER_ID, UserHeaders.USER_NAME, UserHeaders.USER_ROLE);

    private final RestClient restClient;

    public UserProfileClient(@Qualifier("authRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public Optional<String> findCurrentUserFullName() {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return Optional.empty();
        }
        try {
            JsonNode body = restClient.get()
                    .uri("/api/auth/me")
                    .headers(h -> FORWARDED_HEADERS.forEach(name -> {
                        String value = request.getHeader(name);
                        if (value != null) {
                            h.set(name, value);
                        }
                    }))
                    .retrieve()
                    .body(JsonNode.class);
            JsonNode fullName = body == null ? null : body.get("fullName");
            if (fullName == null || fullName.isNull()) {
                return Optional.empty();
            }
            String name = fullName.asText().trim();
            if (name.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(name.length() > MAX_NAME_LENGTH ? name.substring(0, MAX_NAME_LENGTH) : name);
        } catch (RestClientException e) {
            log.warn("Không lấy được tên người dùng từ auth-service, dùng tên mặc định: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private static HttpServletRequest currentRequest() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        return attrs instanceof ServletRequestAttributes servletAttrs ? servletAttrs.getRequest() : null;
    }
}
