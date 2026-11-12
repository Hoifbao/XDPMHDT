package com.smartheritage.content.events;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ArtifactEventOutboxRepository extends JpaRepository<ArtifactEventOutbox, String> {
    List<ArtifactEventOutbox> findTop50ByPublishedAtIsNullOrderByOccurredAtAsc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from ArtifactEventOutbox e where e.eventId = :id")
    Optional<ArtifactEventOutbox> findForDelivery(String id);
}
