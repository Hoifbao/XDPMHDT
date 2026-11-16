package com.smartheritage.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "full_name", length = 100)
    private String fullName;

    @Column(name = "preferred_language", length = 10)
    @Builder.Default
    private String preferredLanguage = "vi";

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String role = "VISITOR";

    /**
     * Bảo W7 (quản lý người dùng): ADMIN khoá tài khoản thì không đăng nhập, không làm mới token được.
     * DEFAULT 0 để khi ddl-auto=update tự thêm cột, các tài khoản có sẵn vẫn ở trạng thái mở.
     */
    @Column(name = "locked", nullable = false, columnDefinition = "TINYINT(1) NOT NULL DEFAULT 0")
    @Builder.Default
    private boolean locked = false;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
