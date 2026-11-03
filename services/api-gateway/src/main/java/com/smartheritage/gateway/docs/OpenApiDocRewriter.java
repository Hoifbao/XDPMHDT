package com.smartheritage.gateway.docs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Bảo W5: sửa tài liệu OpenAPI của từng service trước khi trả cho Swagger UI tại Gateway.
 * <ol>
 *   <li>{@code servers} = "/" (chính Gateway). Nếu không, nút "Try it out" gọi thẳng cổng service
 *       (vd 8083), bỏ qua kiểm tra JWT/phân quyền và header X-User-* của Gateway.</li>
 *   <li>Thêm nút Authorize (Bearer JWT) cho service chưa khai báo (auth, site, content, ...).
 *       Service đã có scheme bearer (beacon, feedback, Backend cũ) thì dùng lại tên của service.</li>
 * </ol>
 * Body không phải JSON OpenAPI (trang lỗi, service trả 5xx) -> trả nguyên văn để Swagger UI tự báo lỗi.
 * Không sửa code của service nào.
 */
@Component
public class OpenApiDocRewriter {

    static final String BEARER = "bearerAuth";
    static final String GATEWAY_SERVER = "/";

    private final ObjectMapper mapper;

    public OpenApiDocRewriter(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public String rewrite(String body) {
        if (body == null || body.isBlank()) {
            return body;
        }
        JsonNode root;
        try {
            root = mapper.readTree(body);
        } catch (JsonProcessingException e) {
            return body;
        }
        if (!(root instanceof ObjectNode doc) || !doc.has("openapi")) {
            return body;
        }

        // 1. Mọi request thử từ Swagger đi qua Gateway (đường dẫn tương đối: chạy được cả localhost lẫn IP LAN)
        ArrayNode servers = doc.putArray("servers");
        servers.addObject()
                .put("url", GATEWAY_SERVER)
                .put("description", "API Gateway (8080): kiểm tra JWT và phân quyền như Web/Mobile");

        // 2. Nút Authorize
        ObjectNode schemes = child(child(doc, "components"), "securitySchemes");
        String schemeName = findBearerScheme(schemes);
        if (schemeName == null) {
            schemeName = BEARER;
            schemes.putObject(BEARER)
                    .put("type", "http")
                    .put("scheme", "bearer")
                    .put("bearerFormat", "JWT")
                    .put("description", "Dán accessToken lấy từ POST /api/auth/login (không gõ chữ Bearer)");
        }
        JsonNode security = doc.get("security");
        if (!(security instanceof ArrayNode list) || list.isEmpty()) {
            // Gửi token cho mọi API; API công khai bỏ qua token (giống OpenApiConfig của Backend cũ)
            doc.putArray("security").addObject().putArray(schemeName);
        }

        try {
            return mapper.writeValueAsString(doc);
        } catch (JsonProcessingException e) {
            return body;
        }
    }

    /** Tên scheme kiểu http bearer đã có, hoặc null. */
    private static String findBearerScheme(ObjectNode schemes) {
        for (Map.Entry<String, JsonNode> e : schemes.properties()) {
            JsonNode s = e.getValue();
            if ("http".equalsIgnoreCase(s.path("type").asText())
                    && "bearer".equalsIgnoreCase(s.path("scheme").asText())) {
                return e.getKey();
            }
        }
        return null;
    }

    /** Lấy object con, chưa có (hoặc sai kiểu) thì tạo mới. */
    private static ObjectNode child(ObjectNode parent, String name) {
        JsonNode n = parent.get(name);
        if (n instanceof ObjectNode o) {
            return o;
        }
        return parent.putObject(name);
    }
}
