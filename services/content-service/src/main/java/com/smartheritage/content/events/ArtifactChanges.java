package com.smartheritage.content.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartheritage.content.entity.Artifact;
import com.smartheritage.content.repository.ArtifactContentRepository;
import com.smartheritage.content.repository.MediaResourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

/** Snapshots exclude technical IDs/timestamps; identical updates produce no event. */
@Component
@RequiredArgsConstructor
public class ArtifactChanges {
    private final ArtifactContentRepository contents;
    private final MediaResourceRepository media;
    private final ArtifactEventOutboxRepository outbox;
    private final ObjectMapper json;
    private final ApplicationEventPublisher events;

    public Map<String, Object> snapshot(Artifact a) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("siteId", a.getSiteId());
        fields.put("name", a.getName());
        fields.put("artifactCode", a.getArtifactCode());
        fields.put("description", a.getDescription());
        fields.put("thumbnailUrl", a.getThumbnailUrl());
        fields.put("period", a.getPeriod());
        fields.put("material", a.getMaterial());
        fields.put("dimensions", a.getDimensions());
        fields.put("beaconUuid", a.getBeaconUuid());
        fields.put("status", a.getStatus());
        fields.put("contents", contents.findByArtifactId(a.getId()).stream()
                .sorted(Comparator.comparing(c -> c.getLanguageCode()))
                .map(c -> Arrays.asList(c.getLanguageCode(), c.getTitle(), c.getDescription(),
                        c.getAudioUrl(), c.getVideoUrl(), c.getImageUrls(), c.getTranscript())).toList());
        fields.put("media", media.findByArtifactId(a.getId()).stream()
                .map(m -> Arrays.asList(m.getMediaType(), m.getFileUrl(), m.getCaption()))
                .sorted(Comparator.comparing(Object::toString)).toList());
        return fields;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void updated(Artifact a, Map<String, Object> before) {
        Map<String, Object> after = snapshot(a);
        record(a, after.keySet().stream().filter(key -> !Objects.equals(before.get(key), after.get(key))).toList());
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(Artifact a, List<String> changedFields) {
        if (changedFields.isEmpty()) return;
        Instant now = Instant.now();
        String id = UUID.randomUUID().toString();
        var data = new ArtifactUpdated(a.getId(), a.getSiteId(), changedFields, now);
        try {
            outbox.save(new ArtifactEventOutbox(id, now, json.writeValueAsString(data)));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Cannot serialize ArtifactUpdated", exception);
        }
        events.publishEvent(new ArtifactEventDispatcher.Pending(id));
    }
}
