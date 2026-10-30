package com.smartheritage.feedback.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.smartheritage.common.web.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Hỏi content-service (tuần 4: Backend cũ 8090) trạng thái của một hiện vật,
 * thay cho ArtifactRepository khi còn chung database.
 * <ul>
 *   <li>GET /api/artifacts/{id} trả 200 -> đọc data.status (ACTIVE | INACTIVE)</li>
 *   <li>404 -> ApiException 404 "Không tìm thấy hiện vật"</li>
 *   <li>Không kết nối được, quá 2 giây, lỗi 5xx hoặc phản hồi lạ -> ApiException 503</li>
 * </ul>
 */
@Component
public class ArtifactClient {

    private static final Logger log = LoggerFactory.getLogger(ArtifactClient.class);
    static final String UNAVAILABLE = "Không kết nối được dịch vụ hiện vật (content-service), vui lòng thử lại sau!";

    private final RestClient restClient;

    public ArtifactClient(@Qualifier("contentRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    /** @return trạng thái của hiện vật (có thể null nếu content-service không trả trường status) */
    public String getArtifactStatus(Long artifactId) {
        JsonNode body;
        try {
            body = restClient.get()
                    .uri("/api/artifacts/{id}", artifactId)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (HttpClientErrorException.NotFound e) {
            throw ApiException.notFound("Không tìm thấy hiện vật với ID: " + artifactId);
        } catch (RestClientException e) {
            log.warn("Gọi content-service lỗi khi kiểm tra hiện vật {}: {}", artifactId, e.getMessage());
            throw ApiException.serviceUnavailable(UNAVAILABLE);
        }
        JsonNode data = body == null ? null : body.get("data");
        if (data == null || !data.isObject()) {
            log.warn("content-service trả phản hồi không có data cho hiện vật {}", artifactId);
            throw ApiException.serviceUnavailable(UNAVAILABLE);
        }
        JsonNode status = data.get("status");
        return status == null || status.isNull() ? null : status.asText();
    }
}
