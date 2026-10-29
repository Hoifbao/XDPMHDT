package com.smartheritage.template.repository;

import com.smartheritage.template.entity.SampleItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SampleItemRepository extends JpaRepository<SampleItem, Long> {
}
