package com.smartheritage.content.service;

import com.smartheritage.common.web.ApiException;
import com.smartheritage.content.dto.*;
import com.smartheritage.content.entity.Artifact;
import com.smartheritage.content.entity.ArtifactContent;
import com.smartheritage.content.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Only DB work inside this transaction: no Redis, RabbitMQ or site-service calls. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ArtifactReadLoader {
    private final ArtifactRepository artifacts;
    private final ArtifactContentRepository contents;
    private final MediaResourceRepository media;

    public ArtifactResponse detail(Long id) {
        Artifact a = artifacts.findById(id).orElseThrow(() -> ApiException.notFound("Không tìm thấy hiện vật với ID: " + id));
        return ArtifactResponse.builder().id(a.getId()).siteId(a.getSiteId()).locationId(a.getSiteId())
                .name(a.getName()).artifactCode(a.getArtifactCode()).description(a.getDescription())
                .thumbnailUrl(a.getThumbnailUrl()).period(a.getPeriod()).material(a.getMaterial())
                .dimensions(a.getDimensions()).beaconUuid(a.getBeaconUuid()).status(a.getStatus())
                .createdAt(a.getCreatedAt()).updatedAt(a.getUpdatedAt())
                .contents(contents.findByArtifactId(id).stream().map(this::content).toList())
                .media(media.findByArtifactId(id)).build();
    }

    public Long idForBeacon(String uuid) {
        return artifacts.findByBeaconUuid(uuid).map(Artifact::getId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy hiện vật với Beacon UUID: " + uuid));
    }

    public List<ArtifactSummaryResponse> summaries(Long siteId, String keyword) {
        var rows = siteId != null ? artifacts.findBySiteId(siteId)
                : keyword != null ? artifacts.searchByName(keyword) : artifacts.findAll();
        return rows.stream().map(this::summary).toList();
    }

    public List<ArtifactSummaryResponse> batch(List<Long> ids) {
        Map<Long, Artifact> rows = artifacts.findAllById(ids).stream()
                .collect(Collectors.toMap(Artifact::getId, Function.identity()));
        return ids.stream().map(rows::get).filter(Objects::nonNull).map(this::summary).toList();
    }

    private ArtifactSummaryResponse summary(Artifact a) {
        return ArtifactSummaryResponse.builder().id(a.getId()).siteId(a.getSiteId()).locationId(a.getSiteId())
                .name(a.getName()).artifactCode(a.getArtifactCode()).thumbnailUrl(a.getThumbnailUrl())
                .period(a.getPeriod()).material(a.getMaterial()).beaconUuid(a.getBeaconUuid()).status(a.getStatus()).build();
    }

    private ArtifactContentResponse content(ArtifactContent c) {
        return ArtifactContentResponse.builder().id(c.getId()).artifactId(c.getArtifact().getId())
                .languageCode(c.getLanguageCode()).title(c.getTitle()).description(c.getDescription())
                .audioUrl(c.getAudioUrl()).videoUrl(c.getVideoUrl()).imageUrls(c.getImageUrls())
                .transcript(c.getTranscript()).createdAt(c.getCreatedAt()).updatedAt(c.getUpdatedAt()).build();
    }
}
