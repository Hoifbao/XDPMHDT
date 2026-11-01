package com.smartheritage.beacon.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.smartheritage.common.web.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Hỏi content-service (tạm thời mặc định: Backend cũ 8090) thông tin hiện vật,
 * thay cho ArtifactRepository khi còn chung database. Mẫu theo SiteClient của Xuân Anh (content-service).
 * <ul>
 *   <li>{@link #requireArtifact}: dùng khi THÊM/SỬA/GÁN beacon. Dùng để kiểm tra hiện vật có tồn tại
 *       và hiện vật có cùng di tích với beacon (beacon_db không có FK sang content_db.artifacts).</li>
 *   <li>{@link #findName}: dùng khi ĐỌC (tên hiện vật hiển thị trên danh sách/registry). Lỗi -> trả rỗng,
 *       không làm hỏng API đọc. Có cache 5 phút.</li>
 * </ul>
 * Chấp nhận cả 2 dạng phản hồi: {id, name, siteId} (Backend cũ) hoặc {success, data: {...}} (service mới).
 */
@Component
public class ArtifactClient {

    private static final Logger log = LoggerFactory.getLogger(ArtifactClient.class);
    static final String UNAVAILABLE = "Không kết nối được dịch vụ hiện vật (content-service), vui lòng thử lại sau!";
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    private final RestClient restClient;
    private final Map<Long, CachedArtifact> cache = new ConcurrentHashMap<>();

    public ArtifactClient(@Qualifier("contentRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    /**
     * @param id     id hiện vật
     * @param name   tên hiện vật (có thể null)
     * @param siteId di tích của hiện vật; null = không xác minh được (không coi là khác di tích)
     */
    public record Artifact(Long id, String name, Long siteId) {
    }

    private record CachedArtifact(Artifact artifact, Instant expiresAt) {
    }

    /** Kiểm tra artifactId tồn tại; trả id, tên và di tích của hiện vật. */
    public Artifact requireArtifact(Long artifactId) {
        if (artifactId == null || artifactId <= 0) {
            throw ApiException.badRequest("artifactId không hợp lệ");
        }
        CachedArtifact cached = cache.get(artifactId);
        if (cached != null && cached.expiresAt().isAfter(Instant.now())) {
            return cached.artifact();
        }
        JsonNode body;
        try {
            body = restClient.get().uri("/api/artifacts/{id}", artifactId).retrieve().body(JsonNode.class);
        } catch (HttpClientErrorException.NotFound e) {
            throw ApiException.badRequest("Không tìm thấy hiện vật với ID: " + artifactId);
        } catch (RestClientException e) {
            log.warn("Gọi content-service lỗi khi kiểm tra hiện vật {}: {}", artifactId, e.getMessage());
            throw ApiException.serviceUnavailable(UNAVAILABLE);
        }
        JsonNode node = body != null && body.has("data") ? body.get("data") : body;
        if (node == null || !node.isObject() || node.path("id").asLong(-1) != artifactId) {
            log.warn("content-service trả phản hồi không hợp lệ cho hiện vật {}", artifactId);
            throw ApiException.serviceUnavailable(UNAVAILABLE);
        }
        String name = node.path("name").isMissingNode() || node.path("name").isNull() ? null : node.path("name").asText();
        Long siteId = node.path("siteId").isMissingNode() || node.path("siteId").isNull()
                ? null : node.path("siteId").asLong();
        Artifact artifact = new Artifact(artifactId, name, siteId);
        cache.put(artifactId, new CachedArtifact(artifact, Instant.now().plus(CACHE_TTL)));
        return artifact;
    }

    /** Tên hiện vật cho API đọc; content-service lỗi thì trả rỗng (artifactName = null). */
    public Optional<String> findName(Long artifactId) {
        if (artifactId == null) {
            return Optional.empty();
        }
        CachedArtifact cached = cache.get(artifactId);
        if (cached != null && cached.expiresAt().isAfter(Instant.now())) {
            return Optional.ofNullable(cached.artifact().name());
        }
        try {
            return Optional.ofNullable(requireArtifact(artifactId).name());
        } catch (ApiException e) {
            return Optional.empty();
        }
    }
}
