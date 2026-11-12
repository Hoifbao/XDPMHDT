package com.smartheritage.content.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ArtifactEventDelivery {
    private final ArtifactEventOutboxRepository outbox;
    private final ObjectProvider<ArtifactEventTransport> transport;
    private final ObjectMapper json;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deliver(String id) throws JsonProcessingException {
        var sender = transport.getIfAvailable();
        if (sender == null) return; // Keep pending if a deployment omits its transport.
        var entry = outbox.findForDelivery(id).orElse(null);
        if (entry == null || entry.getPublishedAt() != null) return;
        sender.send(entry.getEventId(), entry.getOccurredAt(),
                json.readValue(entry.getPayload(), ArtifactUpdated.class));
        entry.published();
    }
}
