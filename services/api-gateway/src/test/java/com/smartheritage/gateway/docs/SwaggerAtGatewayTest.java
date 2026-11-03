package com.smartheritage.gateway.docs;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItems;

/**
 * Bảo W5: Swagger UI chung tại Gateway chạy được mà KHÔNG cần bật service nào
 * (trang và file tĩnh do Gateway tự phục vụ, không chuyển sang Backend cũ).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class SwaggerAtGatewayTest {

    @Autowired
    private WebTestClient web;

    @Test
    void swaggerConfig_coDuCacService() {
        web.get().uri("/v3/api-docs/swagger-config").exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.urls[*].name").value(hasItems("auth-service", "site-service", "content-service",
                        "beacon-service", "personalization-service", "feedback-service", "analytics-service",
                        "backend-cu (8090)"))
                .jsonPath("$.urls[*].url").value(hasItems("/v3/api-docs/auth", "/v3/api-docs/legacy"));
    }

    @Test
    void swaggerUiHtml_chuyenHuongVeTrangCuaGateway() {
        web.get().uri("/swagger-ui.html").exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().value("Location", containsString("/swagger-ui/index.html"));
    }

    @Test
    void trangSwaggerUi_doGatewayPhucVu() {
        // Backend cũ không chạy trong test: nếu rơi vào route legacy sẽ lỗi 5xx thay vì 200
        web.get().uri("/swagger-ui/index.html").exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(containsString("swagger-ui"));
    }
}
