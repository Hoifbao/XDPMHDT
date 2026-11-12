package com.smartheritage.beacon.service;

import com.smartheritage.beacon.entity.ProcessedEvent;
import com.smartheritage.beacon.repository.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Chống xử lý trùng sự kiện - Khoa, W6 (quy ước bắt buộc của nhóm).
 *
 * markIfNew: true nếu eventId chưa từng xử lý (ghi vào processed_events),
 * false nếu tin đã xử lý rồi -> bên nghe bỏ qua.
 * Hai tin trùng vào cùng lúc: tồn tại unique key uq_processed_events_event_id,
 * insert trùng ném DataIntegrityViolationException -> coi như đã xử lý.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessedEvents {

    private final ProcessedEventRepository repository;

    @Transactional
    public boolean markIfNew(UUID eventId, String eventType) {
        if (repository.existsByEventId(eventId)) {
            return false;
        }
        try {
            repository.save(ProcessedEvent.builder().eventId(eventId).eventType(eventType).build());
            return true;
        } catch (DataIntegrityViolationException e) {
            // Tin trùng vừa vào song song - đã có bản ghi lo rồi
            log.debug("[EVENTS] eventId {} đã được xử lý bởi luồng khác", eventId);
            return false;
        }
    }
}
