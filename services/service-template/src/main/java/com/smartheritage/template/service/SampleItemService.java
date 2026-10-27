package com.smartheritage.template.service;

import com.smartheritage.common.web.ApiException;
import com.smartheritage.common.web.CurrentUser;
import com.smartheritage.template.dto.SampleItemRequest;
import com.smartheritage.template.entity.SampleItem;
import com.smartheritage.template.repository.SampleItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SampleItemService {

    private final SampleItemRepository repository;

    public List<SampleItem> findAll() {
        return repository.findAll();
    }

    public SampleItem findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy mục id=" + id));
    }

    /** Ví dụ lấy người đang đăng nhập từ header Gateway gắn (thay cho SecurityUtils của Backend cũ). */
    @Transactional
    public SampleItem create(SampleItemRequest req) {
        Long userId = CurrentUser.requireUserId();
        return repository.save(SampleItem.builder().name(req.name().trim()).createdBy(userId).build());
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(findById(id));
    }
}
