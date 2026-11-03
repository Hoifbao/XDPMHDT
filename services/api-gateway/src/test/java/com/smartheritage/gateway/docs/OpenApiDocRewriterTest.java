package com.smartheritage.gateway.docs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Bảo W5: tài liệu OpenAPI của service sau khi đi qua Gateway. Chạy: mvn -pl services/api-gateway test */
class OpenApiDocRewriterTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final OpenApiDocRewriter rewriter = new OpenApiDocRewriter(mapper);

    @Test
    void serviceChuaCoAuthorize_themBearerVaServerGateway() throws Exception {
        String in = """
                {"openapi":"3.1.0","info":{"title":"content-service"},
                 "servers":[{"url":"http://localhost:8083","description":"Generated server url"}],
                 "paths":{"/api/artifacts":{"get":{}}}}""";
        JsonNode out = mapper.readTree(rewriter.rewrite(in));

        assertEquals(1, out.get("servers").size());
        assertEquals("/", out.get("servers").get(0).get("url").asText());
        JsonNode scheme = out.at("/components/securitySchemes/bearerAuth");
        assertEquals("http", scheme.get("type").asText());
        assertEquals("bearer", scheme.get("scheme").asText());
        assertTrue(out.get("security").get(0).has("bearerAuth"));
        // Phần còn lại giữ nguyên
        assertEquals("content-service", out.at("/info/title").asText());
        assertTrue(out.at("/paths/~1api~1artifacts/get").isObject());
    }

    @Test
    void serviceDaCoSchemeBearer_dungLaiTenCu_khongThemTrung() throws Exception {
        String in = """
                {"openapi":"3.0.1","info":{"title":"beacon-service"},
                 "components":{"schemas":{"X":{"type":"object"}},
                   "securitySchemes":{"jwt":{"type":"http","scheme":"bearer","bearerFormat":"JWT"}}}}""";
        JsonNode out = mapper.readTree(rewriter.rewrite(in));

        JsonNode schemes = out.at("/components/securitySchemes");
        assertEquals(1, schemes.size());
        assertTrue(schemes.has("jwt"));
        assertTrue(out.get("security").get(0).has("jwt"));
        assertTrue(out.at("/components/schemas/X").isObject());
    }

    @Test
    void securityDaKhaiBao_giuNguyen() throws Exception {
        String in = """
                {"openapi":"3.0.1",
                 "components":{"securitySchemes":{"bearerAuth":{"type":"http","scheme":"bearer"}}},
                 "security":[{"bearerAuth":[]}]}""";
        JsonNode out = mapper.readTree(rewriter.rewrite(in));
        assertEquals(1, out.get("security").size());
    }

    @Test
    void khongPhaiOpenApi_traNguyenVan() {
        String html = "<html><body>Whitelabel Error Page</body></html>";
        assertEquals(html, rewriter.rewrite(html));
        String json = "{\"status\":500,\"error\":\"Internal Server Error\"}";
        assertEquals(json, rewriter.rewrite(json));
        assertEquals("", rewriter.rewrite(""));
        assertNull(rewriter.rewrite(null));
        assertFalse(rewriter.rewrite("[1,2]").contains("servers"));
    }
}
