package com.smartheritage.auth.service;

import com.smartheritage.auth.entity.RefreshToken;
import com.smartheritage.auth.entity.User;
import com.smartheritage.auth.repository.RefreshTokenRepository;
import com.smartheritage.common.security.JwtProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

/** Cấp, xoay vòng (rotation) và thu hồi refresh token. */
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final RefreshTokenRepository repository;
    private final JwtProperties props;

    /** Cấp refresh token mới cho người dùng; trả về chuỗi token gốc (chỉ trả 1 lần). */
    @Transactional
    public String issue(User user) {
        byte[] bytes = new byte[48];
        RANDOM.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        repository.save(RefreshToken.builder()
                .tokenHash(hash(raw))
                .user(user)
                .expiresAt(LocalDateTime.now().plusSeconds(props.getRefreshTokenTtlSeconds()))
                .build());
        return raw;
    }

    /**
     * Kiểm tra refresh token và thu hồi nó (mỗi token chỉ dùng được 1 lần).
     * Trả về người dùng sở hữu nếu hợp lệ.
     */
    @Transactional
    public Optional<User> consume(String raw) {
        if (raw == null || raw.isBlank()) return Optional.empty();
        Optional<RefreshToken> found = repository.findByTokenHash(hash(raw.trim()));
        if (found.isEmpty()) return Optional.empty();

        RefreshToken token = found.get();
        if (token.isRevoked()) {
            // Token đã dùng/đã thu hồi mà vẫn bị gửi lại: có thể bị đánh cắp -> thu hồi toàn bộ phiên của user
            repository.revokeAllByUserId(token.getUser().getId());
            return Optional.empty();
        }
        token.setRevoked(true);
        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            return Optional.empty();
        }
        User user = token.getUser();
        user.getRole(); // nạp User trước khi đóng transaction (LAZY)
        return Optional.of(user);
    }

    /** Đăng xuất: thu hồi refresh token. Token không tồn tại thì bỏ qua. */
    @Transactional
    public void revoke(String raw) {
        if (raw == null || raw.isBlank()) return;
        repository.findByTokenHash(hash(raw.trim())).ifPresent(t -> t.setRevoked(true));
    }

    public long getRefreshTtlSeconds() {
        return props.getRefreshTokenTtlSeconds();
    }

    private static String hash(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
