package com.smartheritage.backend.service;

import com.smartheritage.backend.dto.RecordVisitRequest;
import com.smartheritage.backend.dto.SyncVisitsResponse;
import com.smartheritage.backend.dto.VisitHistoryResponse;
import com.smartheritage.backend.entity.Artifact;
import com.smartheritage.backend.entity.User;
import com.smartheritage.backend.entity.VisitHistory;
import com.smartheritage.backend.repository.ArtifactRepository;
import com.smartheritage.backend.repository.VisitHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class VisitHistoryService {
    private final VisitHistoryRepository visitHistoryRepository;
    private final ArtifactRepository artifactRepository;

    @Transactional(readOnly = true)
    public Page<VisitHistoryResponse> list(Long userId, Pageable pageable) {
        return visitHistoryRepository.findHistoryByUserId(userId, pageable);
    }

    @Transactional
    public boolean record(User user, RecordVisitRequest request) {
        validateRequest(request);
        if (visitHistoryRepository.existsByUser_IdAndClientEventId(user.getId(), request.getClientEventId())) {
            return false;
        }
        Artifact artifact = requireActiveArtifact(request.getArtifactId());
        visitHistoryRepository.save(toEntity(user, artifact, request));
        return true;
    }

    /**
     * All-or-nothing batch for offline events. A clientEventId is generated once
     * on mobile and reused on retry. DB uniqueness is the final duplicate guard.
     */
    @Transactional
    public SyncVisitsResponse sync(User user, List<RecordVisitRequest> events) {
        if (events == null || events.isEmpty() || events.size() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Batch phải có từ 1 đến 100 sự kiện.");
        }

        List<RecordVisitRequest> toCreate = new ArrayList<>();
        Set<String> seenInBatch = new HashSet<>();
        int duplicates = 0;

        for (RecordVisitRequest request : events) {
            validateRequest(request);
            if (!seenInBatch.add(request.getClientEventId())
                || visitHistoryRepository.existsByUser_IdAndClientEventId(user.getId(), request.getClientEventId())) {
                duplicates++;
                continue;
            }
            toCreate.add(request);
        }

        // Validate every new event before writing; transaction prevents partial batches.
        List<VisitHistory> rows = new ArrayList<>();
        for (RecordVisitRequest request : toCreate) {
            rows.add(toEntity(user, requireActiveArtifact(request.getArtifactId()), request));
        }
        try {
            visitHistoryRepository.saveAll(rows);
            visitHistoryRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            // A concurrent request may have inserted the same idempotency key.
            // Keep transaction safety: bubble up rather than return a false success.
            throw ex;
        }
        return new SyncVisitsResponse(events.size(), rows.size(), duplicates);
    }

    @Transactional(readOnly = true)
    public Page<VisitHistoryResponse> listForAdmin(LocalDateTime fromTime, LocalDateTime toTime,
                                                    Long siteId, Pageable pageable) {
        return visitHistoryRepository.findAdminHistory(fromTime, toTime, siteId, pageable);
    }

    private Artifact requireActiveArtifact(Long artifactId) {
        Artifact artifact = artifactRepository.findById(artifactId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy hiện vật."));
        if (!"ACTIVE".equalsIgnoreCase(artifact.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Không thể ghi lịch sử cho hiện vật không hoạt động.");
        }
        return artifact;
    }

    private void validateRequest(RecordVisitRequest request) {
        if (request == null || request.getArtifactId() == null || request.getArtifactId() <= 0
            || request.getClientEventId() == null || request.getClientEventId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thiếu artifactId hoặc clientEventId.");
        }
        String source = request.getSource() == null ? "MANUAL" : request.getSource().trim().toUpperCase();
        if (!Set.of("BEACON", "MAP", "MANUAL").contains(source)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "source chỉ nhận BEACON, MAP hoặc MANUAL.");
        }
        if (request.getDurationSeconds() != null && (request.getDurationSeconds() < 0 || request.getDurationSeconds() > 86400)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "durationSeconds phải từ 0 đến 86400.");
        }
        request.setSource(source);
    }

    private VisitHistory toEntity(User user, Artifact artifact, RecordVisitRequest request) {
        VisitHistory row = new VisitHistory();
        row.setUser(user);
        row.setArtifact(artifact);
        row.setClientEventId(request.getClientEventId());
        row.setSource(request.getSource() == null ? "MANUAL" : request.getSource().toUpperCase());
        row.setDurationSeconds(request.getDurationSeconds()); // null giữ nguyên là null
        row.setDetectedAt(request.getDetectedAt() == null
            ? LocalDateTime.now()
            : request.getDetectedAt().withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime());
        return row;
    }
}
