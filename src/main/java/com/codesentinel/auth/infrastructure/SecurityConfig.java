package com.codesentinel.auth.infrastructure;

import com.codesentinel.auth.application.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;

import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Đặt trong module {@code auth} (thay vì {@code shared}) vì cấu hình này giờ
 * phụ thuộc trực tiếp vào {@link JwtService} và {@link UserRepository} để gắn
 * {@link JwtAuthenticationFilter} — bản chất đây là quy tắc xác thực/phân quyền
 * của domain auth, không phải hạ tầng dùng chung.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtService jwtService;

    private final UserRepository userRepository;

    private final ObjectMapper objectMapper;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        log.info("Security filter chain initialized");

        JwtAuthenticationFilter jwtAuthenticationFilter =
                new JwtAuthenticationFilter(jwtService, userRepository);

        http
                .cors(cors -> {})
                .csrf(AbstractHttpConfigurer::disable)

                .authorizeHttpRequests(auth -> auth

                        .requestMatchers(
                                "/api/auth/**",
                                "/api/scans/**"
                        ).permitAll()

                        // Xem/tạo user trực tiếp là thao tác quản trị -> chỉ ADMIN.
                        // Đăng ký tài khoản thông thường vẫn đi qua /api/auth/register (permitAll ở trên).
                        .requestMatchers(HttpMethod.GET, "/api/users").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/users").hasRole("ADMIN")

                        .anyRequest()
                        .authenticated()
                )

                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

                // Trả lỗi 401/403 theo đúng format ApiResponse, thay vì trang lỗi mặc định
                // của Spring Security (vốn chạy ngoài GlobalExceptionHandler).
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new RestAuthenticationEntryPoint(objectMapper))
                        .accessDeniedHandler(new RestAccessDeniedHandler(objectMapper))
                )

                .oauth2Login(oauth -> {
                    // TODO: cấu hình successHandler để phát hành JWT nội bộ sau khi OAuth2 login thành công
                });

        return http.build();
    }
}
