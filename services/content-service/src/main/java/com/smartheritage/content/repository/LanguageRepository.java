package com.smartheritage.content.repository;

import com.smartheritage.content.entity.Language;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LanguageRepository extends JpaRepository<Language, String> {
    List<Language> findAllByOrderByCodeAsc();
    List<Language> findByActiveTrueOrderByCodeAsc();
}
