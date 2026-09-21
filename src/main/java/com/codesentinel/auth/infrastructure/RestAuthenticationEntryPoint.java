package com.codesentinel.auth.infrastructure;

import com.codesentinel.shared.exception.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Trả 401 theo đúng format {@link ApiResponse} thay vì trang lỗi/JSON mặc định
 * của Spring Security. Kích hoạt khi request KHÔNG có (hoặc có nhưng không hợp
 * lệ) danh tính, chạm vào route yêu cầu {@code authenticated()}.
 */
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {

        ApiResponse<Void> body = ApiResponse.<Void>builder()
                .success(false)
                .message("Authentication required")
                .timestamp(LocalDateTime.now())
                .build();

        SecurityResponseWriter.write(response, HttpStatus.UNAUTHORIZED, body, objectMapper);
    }
}
