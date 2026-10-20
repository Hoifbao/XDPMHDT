package com.smartheritage.backend.service;

import com.smartheritage.backend.dto.HeritageSiteRequest;
import com.smartheritage.backend.dto.HeritageSiteResponse;
import com.smartheritage.backend.entity.HeritageSite;
import com.smartheritage.backend.repository.HeritageSiteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HeritageSiteService {

    private final HeritageSiteRepository repository;

    /** Lấy toàn bộ danh sách di tích. */
    public List<HeritageSiteResponse> getAll() {
        return repository.findAll()
                .stream()
                .map(HeritageSiteResponse::from)
                .collect(Collectors.toList());
    }

    /** Lấy chi tiết một di tích theo id. */
    public HeritageSiteResponse getById(Long id) {
        HeritageSite site = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy di tích với id: " + id));
        return HeritageSiteResponse.from(site);
    }

    /** Tạo mới di tích. */
    public HeritageSiteResponse create(HeritageSiteRequest request) {
        if (request.getName() == null || request.getName().isBlank()) {
            throw new RuntimeException("Tên di tích không được để trống.");
        }
        if (repository.existsByName(request.getName())) {
            throw new RuntimeException("Di tích '" + request.getName() + "' đã tồn tại.");
        }

        HeritageSite site = HeritageSite.builder()
                .name(request.getName())
                .location(request.getLocation())
                .mapImageUrl(request.getMapImageUrl())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .build();

        return HeritageSiteResponse.from(repository.save(site));
    }

    /** Cập nhật thông tin di tích. */
    public HeritageSiteResponse update(Long id, HeritageSiteRequest request) {
        HeritageSite site = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy di tích với id: " + id));

        if (request.getName() != null && !request.getName().isBlank()) {
            // Kiểm tra tên trùng với di tích khác (không phải chính nó)
            boolean nameTaken = repository.findByNameContainingIgnoreCase(request.getName())
                    .stream()
                    .anyMatch(s -> !s.getId().equals(id));
            if (nameTaken) {
                throw new RuntimeException("Tên '" + request.getName() + "' đã được sử dụng bởi di tích khác.");
            }
            site.setName(request.getName());
        }

        if (request.getLocation() != null)    site.setLocation(request.getLocation());
        if (request.getMapImageUrl() != null) site.setMapImageUrl(request.getMapImageUrl());
        if (request.getLatitude() != null)    site.setLatitude(request.getLatitude());
        if (request.getLongitude() != null)   site.setLongitude(request.getLongitude());

        return HeritageSiteResponse.from(repository.save(site));
    }

    /** Xóa di tích theo id. */
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new RuntimeException("Không tìm thấy di tích với id: " + id);
        }
        repository.deleteById(id);
    }
}
