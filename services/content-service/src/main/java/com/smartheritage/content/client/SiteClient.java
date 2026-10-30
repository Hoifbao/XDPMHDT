package com.smartheritage.content.client;

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
 * Hỏi site-service (tuần 4 mặc định: Backend cũ 8090) thông tin di tích, thay cho HeritageSiteRepository
 * khi còn chung database. Ý tưởng và test gốc: Xuân Anh (nhánh XA-task3v4).
 * <ul>
 *   <li>{@link #requireSite}: dùng khi THÊM/SỬA hiện vật. siteId không tồn tại -> 400; site-service lỗi -> 503
 *       (không ghi dữ liệu khi chưa kiểm tra được).</li>
 *   <li>{@link #findName}: dùng khi ĐỌC (tên di tích trong danh sách/chi tiết). Lỗi -> trả rỗng, không làm hỏng API đọc.
 *       Có cache 5 phút để danh sách không gọi site-service lặp lại.</li>
 * </ul>
 * Chấp nhận cả 2 dạng phản hồi: {id, name} (Backend cũ) hoặc {success, data: {id, name}} (service mới).
 */
@Component
public class SiteClient {

    private static final Logger log = LoggerFactory.getLogger(SiteClient.class);
    static final String UNAVAILABLE = "Không kết nối được dịch vụ di tích (site-service), vui lòng thử lại sau!";
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    private final RestClient restClient;
    private final Map<Long, CachedName> nameCache = new ConcurrentHashMap<>();

    public SiteClient(@Qualifier("siteRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public record Site(Long id, String name) {
    }

    private record CachedName(String name, Instant expiresAt) {
    }

    /** Kiểm tra siteId tồn tại; trả id và tên di tích. */
    public Site requireSite(Long siteId) {
        if (siteId == null || siteId <= 0) {
            throw ApiException.badRequest("siteId là bắt buộc");
        }
        JsonNode body;
        try {
            body = restClient.get().uri("/api/heritage-sites/{id}", siteId).retrieve().body(JsonNode.class);
        } catch (HttpClientErrorException.NotFound e) {
            throw ApiException.badRequest("Di tích không tồn tại: siteId=" + siteId);
        } catch (RestClientException e) {
            log.warn("Gọi site-service lỗi khi kiểm tra di tích {}: {}", siteId, e.getMessage());
            throw ApiException.serviceUnavailable(UNAVAILABLE);
        }
        JsonNode site = body != null && body.has("data") ? body.get("data") : body;
        if (site == null || !site.isObject() || site.path("id").asLong(-1) != siteId) {
            log.warn("site-service trả phản hồi không hợp lệ cho di tích {}", siteId);
            throw ApiException.serviceUnavailable(UNAVAILABLE);
        }
        String name = site.path("name").isMissingNode() || site.path("name").isNull() ? null : site.path("name").asText();
        nameCache.put(siteId, new CachedName(name, Instant.now().plus(CACHE_TTL)));
        return new Site(siteId, name);
    }

    /** Tên di tích cho API đọc; site-service lỗi thì trả rỗng (locationName = null). */
    public Optional<String> findName(Long siteId) {
        if (siteId == null) {
            return Optional.empty();
        }
        CachedName cached = nameCache.get(siteId);
        if (cached != null && cached.expiresAt().isAfter(Instant.now())) {
            return Optional.ofNullable(cached.name());
        }
        try {
            return Optional.ofNullable(requireSite(siteId).name());
        } catch (ApiException e) {
            return Optional.empty();
        }
    }
}
