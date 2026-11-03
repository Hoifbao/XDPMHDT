package com.smartheritage.content.service;

import com.smartheritage.common.web.ApiException;
import com.smartheritage.content.entity.Language;
import com.smartheritage.content.repository.LanguageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional
public class LanguageService implements ApplicationRunner {
    private final LanguageRepository repository;

    @Override
    public void run(ApplicationArguments args) {
        if (!repository.existsById("vi")) repository.save(new Language("vi", "Tiếng Việt", true));
        if (!repository.existsById("en")) repository.save(new Language("en", "English", true));
    }

    public List<Language> list(boolean all) {
        return all ? repository.findAllByOrderByCodeAsc() : repository.findByActiveTrueOrderByCodeAsc();
    }

    public Language save(String code, String name, Boolean active, boolean create) {
        code = normalize(code);
        if (name == null || name.isBlank() || name.trim().length() > 100 || active == null)
            throw ApiException.badRequest("Cần name (tối đa 100 ký tự) và active");
        if (create && repository.existsById(code)) throw ApiException.badRequest("Mã ngôn ngữ đã tồn tại");
        if (!create && !repository.existsById(code)) throw ApiException.notFound("Không có ngôn ngữ: " + code);
        if ("vi".equals(code) && !active) throw ApiException.badRequest("Tiếng Việt phải luôn hoạt động để fallback");
        return repository.save(new Language(code, name.trim(), active));
    }

    public Language require(String code) {
        return repository.findById(normalize(code))
                .orElseThrow(() -> ApiException.badRequest("Ngôn ngữ chưa được hỗ trợ"));
    }

    public static String normalize(String code) {
        String value = code == null ? "vi" : code.trim().toLowerCase(Locale.ROOT);
        if (!value.matches("[a-z]{2,3}(-[a-z0-9]{2,6})?" ) || value.length() > 10)
            throw ApiException.badRequest("Mã ngôn ngữ không hợp lệ");
        return value;
    }
}
