package com.codesentinel.auth.infrastructure;

import com.codesentinel.auth.application.JwtService;
import com.codesentinel.auth.domain.Role;
import com.codesentinel.auth.domain.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Đọc access token từ header {@code Authorization: Bearer <token>}, verify và
 * nạp {@link User} tương ứng vào {@link SecurityContextHolder}.
 * <p>
 * Trước đây {@link JwtService#validateToken} tồn tại nhưng không có filter nào
 * gọi tới — mọi access token phát hành ra đều không được xác thực khi request
 * quay lại. Class này lấp khoảng trống đó.
 * <p>
 * Không đăng ký là {@code @Component}: filter này được khởi tạo và gắn thủ công
 * vào {@code SecurityFilterChain} trong {@code SecurityConfig} qua
 * {@code addFilterBefore(...)}. Nếu để Spring tự phát hiện như một bean
 * {@code Filter}, Boot sẽ tự đăng ký thêm 1 lần nữa ở servlet container,
 * khiến filter chạy 2 lần trên mỗi request.
 */
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String header = request.getHeader(AUTH_HEADER);

        if (header == null || !header.startsWith(BEARER_PREFIX)) {

            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(BEARER_PREFIX.length());

        try {

            if (jwtService.isAccessToken(token)
                    && SecurityContextHolder.getContext().getAuthentication() == null) {

                String username = jwtService.extractUsername(token);

                userRepository.findByUsername(username)
                        .filter(user -> jwtService.validateToken(token, user))
                        .ifPresent(this::authenticate);
            }

        } catch (Exception e) {

            // Token sai định dạng/hết hạn/chữ ký không hợp lệ -> coi như chưa đăng nhập,
            // để Spring Security tự quyết định 401/403 dựa trên rule của route.
            log.debug("Invalid JWT token: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private void authenticate(User user) {

        Role role = user.getRole() != null ? user.getRole() : Role.USER;

        List<SimpleGrantedAuthority> authorities =
                List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));

        var authToken = new UsernamePasswordAuthenticationToken(user, null, authorities);

        SecurityContextHolder.getContext().setAuthentication(authToken);
    }
}
