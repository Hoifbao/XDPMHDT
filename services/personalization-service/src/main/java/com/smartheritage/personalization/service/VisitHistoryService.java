package com.smartheritage.personalization.service;

import com.smartheritage.common.web.ApiException;
import com.smartheritage.personalization.client.ArtifactClient;
import com.smartheritage.personalization.client.ArtifactClient.ArtifactSnapshot;
import com.smartheritage.personalization.dto.RecordVisitRequest;
import com.smartheritage.personalization.dto.SyncVisitsResponse;
import com.smartheritage.personalization.dto.VisitHistoryResponse;
import com.smartheritage.personalization.entity.VisitHistory;
import com.smartheritage.personalization.repository.VisitHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class VisitHistoryService {
    private final VisitHistoryRepository visitHistoryRepository;
    private final ArtifactClient artifactClient;

    @Transactional(readOnly = true)
    public Page<VisitHistoryResponse> list(Long userId, Pageable pageable) {
        return enrich(visitHistoryRepository.findByUserIdOrderByDetectedAtDesc(userId, pageable));
    }

    @Transactional
    public boolean record(Long userId, RecordVisitRequest request) {
        validateRequest(request);
        if (visitHistoryRepository.existsByUserIdAndClientEventId(userId, request.getClientEventId())) {
            return false;
        }
        artifactClient.requireActive(request.getArtifactId());
        visitHistoryRepository.save(toEntity(userId, request));
        return true;
    }

    /** Đồng bộ offline all-or-nothing; clientEventId được giữ nguyên khi retry. */
    @Transactional
    public SyncVisitsResponse sync(Long userId, List<RecordVisitRequest> events) {
        if (events == null || events.isEmpty() || events.size() > 100) {
            throw ApiException.badRequest("Batch phải có từ 1 đến 100 sự kiện.");
        }

        List<RecordVisitRequest> toCreate = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        int duplicates = 0;

        for (RecordVisitRequest request : events) {
            validateRequest(request);
            if (!seen.add(request.getClientEventId())
                    || visitHistoryRepository.existsByUserIdAndClientEventId(userId, request.getClientEventId())) {
                duplicates++;
                continue;
            }
            toCreate.add(request);
        }

        // Kiểm tra mọi hiện vật trước khi ghi để batch không bị ghi dở dang.
        for (Long artifactId : toCreate.stream().map(RecordVisitRequest::getArtifactId).distinct().toList()) {
            artifactClient.requireActive(artifactId);
        }

        List<VisitHistory> rows = toCreate.stream().map(r -> toEntity(userId, r)).toList();
        visitHistoryRepository.saveAll(rows);
        visitHistoryRepository.flush();
        return new SyncVisitsResponse(events.size(), rows.size(), duplicates);
    }

    @Transactional(readOnly = true)
    public Page<VisitHistoryResponse> listForAdmin(LocalDateTime fromTime, LocalDateTime toTime,
                                                   Long siteId, Pageable pageable) {
        Page<VisitHistory> page;
        if (siteId == null) {
            page = visitHistoryRepository.findAdminHistory(fromTime, toTime, pageable);
        } else {
            List<Long> artifactIds = artifactClient.listBySite(siteId).stream()
                    .map(ArtifactSnapshot::id)
                    .filter(java.util.Objects::nonNull)
                    .toList();
            if (artifactIds.isEmpty()) return new PageImpl<>(List.of(), pageable, 0);
            page = visitHistoryRepository.findAdminHistoryByArtifacts(fromTime, toTime, artifactIds, pageable);
        }
        return enrich(page);
    }

    private Page<VisitHistoryResponse> enrich(Page<VisitHistory> page) {
        Map<Long, ArtifactSnapshot> artifacts = artifactClient.getMany(
                page.getContent().stream().map(VisitHistory::getArtifactId).toList());
        return page.map(v -> toResponse(v, artifacts.get(v.getArtifactId())));
    }

    private VisitHistoryResponse toResponse(VisitHistory row, ArtifactSnapshot artifact) {
        Long artifactId = row.getArtifactId();
        return new VisitHistoryResponse(
                row.getId(),
                artifactId,
                artifact == null || artifact.name() == null ? "Hiện vật #" + artifactId : artifact.name(),
                artifact == null ? null : artifact.thumbnailUrl(),
                artifact == null ? null : artifact.siteId(),
                artifact == null ? null : artifact.siteName(),
                row.getDetectedAt(),
                row.getDurationSeconds(),
                row.getSource(),
                row.getClientEventId());
    }

    private void validateRequest(RecordVisitRequest request) {
        if (request == null || request.getArtifactId() == null || request.getArtifactId() <= 0
                || request.getClientEventId() == null || request.getClientEventId().isBlank()) {
            throw ApiException.badRequest("Thiếu artifactId hoặc clientEventId.");
        }
        String source = request.getSource() == null ? "MANUAL" : request.getSource().trim().toUpperCase();
        if (!Set.of("BEACON", "MAP", "MANUAL").contains(source)) {
            throw ApiException.badRequest("source chỉ nhận BEACON, MAP hoặc MANUAL.");
        }
        Integer duration = request.getDurationSeconds();
        if (duration != null && (duration < 0 || duration > 86400)) {
            throw ApiException.badRequest("durationSeconds phải từ 0 đến 86400.");
        }
        request.setSource(source);
    }

    private VisitHistory toEntity(Long userId, RecordVisitRequest request) {
        return VisitHistory.builder()
                .userId(userId)
                .artifactId(request.getArtifactId())
                .durationSeconds(request.getDurationSeconds())
                .clientEventId(request.getClientEventId())
                .source(request.getSource() == null ? "MANUAL" : request.getSource().toUpperCase())
                .detectedAt(request.getDetectedAt() == null
                        ? LocalDateTime.now()
                        : request.getDetectedAt().atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime())
                .build();
    }
}
