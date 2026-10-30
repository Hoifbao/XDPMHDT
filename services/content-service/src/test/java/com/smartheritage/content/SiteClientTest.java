package com.smartheritage.content;

import com.smartheritage.common.web.ApiException;
import com.smartheritage.content.client.SiteClient;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/** Kiểm tra SiteClient với 1 HTTP server giả (ý tưởng gốc: Xuân Anh). */
class SiteClientTest {

    @Test
    void validatesRemoteSiteAndMapsRemoteFailures() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/heritage-sites/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            int code;
            String body;
            if (path.endsWith("/1")) {          // Backend cũ: trả thẳng đối tượng
                code = 200; body = "{\"id\":1,\"name\":\"Museum\"}";
            } else if (path.endsWith("/4")) {   // service mới: bọc trong ApiResponse
                code = 200; body = "{\"success\":true,\"data\":{\"id\":4,\"name\":\"Hoang thanh\"}}";
            } else if (path.endsWith("/2")) {
                code = 404; body = "{\"message\":\"not found\"}";
            } else {
                code = 500; body = "{}";
            }
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(code, bytes.length);
            try (var stream = exchange.getResponseBody()) {
                stream.write(bytes);
            }
        });
        server.start();
        try {
            var client = new SiteClient(RestClient.create("http://127.0.0.1:" + server.getAddress().getPort()));
            assertEquals("Museum", client.requireSite(1L).name());
            assertEquals("Hoang thanh", client.requireSite(4L).name());
            assertEquals(400, assertThrows(ApiException.class, () -> client.requireSite(null)).getStatus().value());
            assertEquals(400, assertThrows(ApiException.class, () -> client.requireSite(2L)).getStatus().value());
            assertEquals(503, assertThrows(ApiException.class, () -> client.requireSite(3L)).getStatus().value());
            // API đọc không hỏng khi site-service lỗi
            assertTrue(client.findName(3L).isEmpty());
            assertEquals("Museum", client.findName(1L).orElseThrow());
        } finally {
            server.stop(0);
        }
    }
}
