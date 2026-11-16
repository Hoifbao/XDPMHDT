package com.smartheritage.auth.controller;

import com.smartheritage.auth.entity.User;
import com.smartheritage.auth.repository.UserRepository;
import com.smartheritage.auth.service.RefreshTokenService;
import com.smartheritage.common.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Bảo W7 - quản lý người dùng: quyền (401/403), không tự khoá/tự hạ quyền, khoá thì thu hồi phiên,
 * tài khoản bị khoá không đăng nhập / làm mới token được.
 * Chạy: mvn -pl services/auth-service test
 */
@WebMvcTest({AdminUserController.class, AuthController.class})
class AdminUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserRepository userRepository;
    @MockitoBean
    private RefreshTokenService refreshTokenService;
    @MockitoBean
    private PasswordEncoder passwordEncoder;
    @MockitoBean
    private JwtService jwtService;

    private static User user(long id, String username, String role, boolean locked) {
        return User.builder().id(id).username(username).email(username + "@example.com")
                .passwordHash("$2a$10$hash").fullName("Người " + id).role(role).locked(locked).build();
    }

    // ---------- Quyền ----------

    @Test
    void danhSach_khongDangNhap401_visitor403() throws Exception {
        mockMvc.perform(get("/api/admin/users")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/admin/users").header("X-User-Id", "2").header("X-User-Role", "VISITOR"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(userRepository);
    }

    @Test
    void danhSach_admin200_coLocked_khongLoMatKhau() throws Exception {
        when(userRepository.search(eq("an"), isNull(), any()))
                .thenReturn(new PageImpl<>(List.of(user(2, "visitor_an", "VISITOR", true)), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/admin/users").param("q", " an ")
                        .header("X-User-Id", "1").header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].username").value("visitor_an"))
                .andExpect(jsonPath("$.content[0].locked").value(true))
                .andExpect(jsonPath("$.content[0].passwordHash").doesNotExist())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void danhSach_roleSai400_sizeQuaLonBiChan() throws Exception {
        mockMvc.perform(get("/api/admin/users").param("role", "BOSS")
                        .header("X-User-Id", "1").header("X-User-Role", "ADMIN"))
                .andExpect(status().isBadRequest());

        when(userRepository.search(isNull(), eq("ADMIN"), any())).thenAnswer(inv -> {
            PageRequest pr = inv.getArgument(2);
            assertEquals(AdminUserController.MAX_PAGE_SIZE, pr.getPageSize());
            return new PageImpl<>(List.of(), pr, 0);
        });
        mockMvc.perform(get("/api/admin/users").param("role", "admin").param("size", "5000")
                        .header("X-User-Id", "1").header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk());
    }

    // ---------- Khoá / mở khoá ----------

    @Test
    void khoaNguoiKhac_200_thuHoiPhien() throws Exception {
        User target = user(2, "visitor_an", "VISITOR", false);
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));

        mockMvc.perform(patch("/api/admin/users/2/status")
                        .header("X-User-Id", "1").header("X-User-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"locked\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.locked").value(true));
        assertTrue(target.isLocked());
        verify(userRepository).save(target);
        verify(refreshTokenService).revokeAllOf(2L);
    }

    @Test
    void moKhoa_200_khongThuHoiPhien() throws Exception {
        User target = user(2, "visitor_an", "VISITOR", true);
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));

        mockMvc.perform(patch("/api/admin/users/2/status")
                        .header("X-User-Id", "1").header("X-User-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"locked\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.locked").value(false));
        verify(refreshTokenService, never()).revokeAllOf(any());
    }

    @Test
    void tuKhoaChinhMinh_400_thieuLocked_400_khongCoNguoiDung_404() throws Exception {
        mockMvc.perform(patch("/api/admin/users/1/status")
                        .header("X-User-Id", "1").header("X-User-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"locked\":true}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(patch("/api/admin/users/2/status")
                        .header("X-User-Id", "1").header("X-User-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        mockMvc.perform(patch("/api/admin/users/99/status")
                        .header("X-User-Id", "1").header("X-User-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"locked\":true}"))
                .andExpect(status().isNotFound());
        verify(userRepository, never()).save(any());
    }

    // ---------- Đổi vai trò ----------

    @Test
    void doiVaiTro_200_roleSai400_tuDoi400() throws Exception {
        User target = user(2, "visitor_an", "VISITOR", false);
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));

        mockMvc.perform(patch("/api/admin/users/2/role")
                        .header("X-User-Id", "1").header("X-User-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"admin\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role").value("ADMIN"));
        assertEquals("ADMIN", target.getRole());

        mockMvc.perform(patch("/api/admin/users/2/role")
                        .header("X-User-Id", "1").header("X-User-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"BOSS\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(patch("/api/admin/users/1/role")
                        .header("X-User-Id", "1").header("X-User-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"VISITOR\"}"))
                .andExpect(status().isBadRequest());
    }

    // ---------- Tài khoản bị khoá ----------

    @Test
    void dangNhap_taiKhoanBiKhoa_403_khongCapToken() throws Exception {
        when(userRepository.findByUsername("visitor_an")).thenReturn(Optional.of(user(2, "visitor_an", "VISITOR", true)));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"visitor_an\",\"password\":\"Test@1234\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(AuthController.ACCOUNT_LOCKED))
                .andExpect(jsonPath("$.accessToken").doesNotExist());
        verify(refreshTokenService, never()).issue(any());
    }

    @Test
    void lamMoiToken_taiKhoanBiKhoa_403() throws Exception {
        when(refreshTokenService.consume("abc")).thenReturn(Optional.of(user(2, "visitor_an", "VISITOR", true)));

        mockMvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"abc\"}"))
                .andExpect(status().isForbidden());
        verify(refreshTokenService, never()).issue(any());
    }
}
