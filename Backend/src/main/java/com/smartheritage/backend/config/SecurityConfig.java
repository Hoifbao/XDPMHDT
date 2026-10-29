package com.smartheritage.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartheritage.backend.security.AuthPrincipal;
import com.smartheritage.backend.security.JwtAuthenticationFilter;
import com.smartheritage.backend.security.Roles;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Bảo mật API (Bảo - Sprint 3): JWT stateless + phân quyền RBAC 2 vai trò VISITOR / ADMIN.
 *
 * Quy tắc (xét từ trên xuống, khớp quy tắc nào dùng quy tắc đó):
 *  1. Swagger, /api/auth/register|login|refresh|logout       -> công khai
 *  2. /api/auth/me, /api/auth/users/{id}                       -> phải đăng nhập (chỉ xem hồ sơ của mình, ADMIN xem được tất cả)
 *  3. /api/users/{userId}/** (yêu thích, lịch sử - Phú)       -> phải đăng nhập, đúng chủ tài khoản hoặc ADMIN
 *     /api/me/** (dữ liệu của người đang đăng nhập - Phú)      -> phải đăng nhập (VISITOR hoặc ADMIN)
 *     /api/admin/** (API quản trị)                              -> chỉ ADMIN
 *  4. POST /api/artifacts/{id}/feedbacks (gửi đánh giá - Tường) -> VISITOR hoặc ADMIN đã đăng nhập
 *  5. /api/statistics/** (Dashboard - Tuấn)                    -> chỉ ADMIN
 *  6. /api/demo/** (demo Redis - Phú)                          -> công khai, chỉ để demo
 *  7. Mọi API GET khác                                         -> công khai
 *  8. Mọi API POST/PUT/PATCH/DELETE khác (thêm/sửa/xóa)        -> chỉ ADMIN
 * Thiếu/sai/hết hạn token -> 401; đúng token nhưng không đủ quyền -> 403.
 */
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())               // API stateless dùng Bearer token, không dùng cookie
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .httpBasic(b -> b.disable())
            .formLogin(f -> f.disable())
            .authorizeHttpRequests(auth -> auth
                // Preflight CORS của trình duyệt
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // 1. Swagger + trang lỗi
                .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/error").permitAll()
                .requestMatchers(HttpMethod.POST,
                        "/api/auth/register", "/api/auth/login", "/api/auth/refresh", "/api/auth/logout").permitAll()
                // 2. Hồ sơ cá nhân
                .requestMatchers("/api/auth/me", "/api/auth/users/**").authenticated()
                // 3. Dữ liệu cá nhân theo userId trên URL
                .requestMatchers("/api/users/{userId}/**").access(ownerOrAdmin())
                .requestMatchers("/api/me/**").authenticated()
                .requestMatchers("/api/admin/**").hasRole(Roles.ADMIN)
                // 4. Du khách gửi đánh giá
                .requestMatchers(HttpMethod.POST, "/api/artifacts/*/feedbacks").hasAnyRole(Roles.VISITOR, Roles.ADMIN)
                // 5. Thống kê
                .requestMatchers("/api/statistics/**").hasRole(Roles.ADMIN)
                // 6. Demo Redis
                .requestMatchers("/api/demo/**").permitAll()
                // 7 + 8. Đọc công khai, ghi chỉ ADMIN
                .requestMatchers(HttpMethod.GET, "/api/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/**").hasRole(Roles.ADMIN)
                .requestMatchers(HttpMethod.PUT, "/api/**").hasRole(Roles.ADMIN)
                .requestMatchers(HttpMethod.PATCH, "/api/**").hasRole(Roles.ADMIN)
                .requestMatchers(HttpMethod.DELETE, "/api/**").hasRole(Roles.ADMIN)
                .anyRequest().permitAll()
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((req, res, e) ->
                        writeJson(res, HttpServletResponse.SC_UNAUTHORIZED,
                                "Bạn chưa đăng nhập hoặc phiên đăng nhập đã hết hạn!"))
                .accessDeniedHandler((req, res, e) ->
                        writeJson(res, HttpServletResponse.SC_FORBIDDEN,
                                "Bạn không có quyền thực hiện thao tác này!"))
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /**
     * JwtAuthenticationFilter là @Component nên Spring Boot sẽ tự đăng ký thêm 1 lần làm filter servlet.
     * Tắt đăng ký đó để filter chỉ chạy trong chuỗi Spring Security.
     */
    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> disableJwtFilterAutoRegistration(JwtAuthenticationFilter filter) {
        FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    /** Cho phép nếu {userId} trên URL trùng với người đang đăng nhập, hoặc người đó là ADMIN. */
    private static AuthorizationManager<RequestAuthorizationContext> ownerOrAdmin() {
        return (authentication, context) -> {
            var auth = authentication.get();
            if (auth == null || !(auth.getPrincipal() instanceof AuthPrincipal me)) {
                return new AuthorizationDecision(false);
            }
            if (me.isAdmin()) {
                return new AuthorizationDecision(true);
            }
            String userId = context.getVariables().get("userId");
            return new AuthorizationDecision(userId != null && userId.equals(String.valueOf(me.userId())));
        };
    }

    /** Web Admin mở bằng Live Server / file:// nên cho phép mọi origin (không dùng cookie nên an toàn). */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(false);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    private void writeJson(HttpServletResponse res, int status, String message) throws IOException {
        res.setStatus(status);
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(res.getOutputStream(), Map.of("success", false, "message", message));
    }
}
