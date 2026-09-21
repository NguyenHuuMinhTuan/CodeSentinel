package com.codesentinel.auth.infrastructure;

import com.codesentinel.shared.exception.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Trả 403 theo đúng format {@link ApiResponse}. Kích hoạt khi request ĐÃ xác
 * thực thành công (có access token hợp lệ) nhưng thiếu role cần thiết — ví dụ
 * user thường gọi {@code GET /api/users} (yêu cầu ROLE_ADMIN).
 */
@RequiredArgsConstructor
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException {

        ApiResponse<Void> body = ApiResponse.<Void>builder()
                .success(false)
                .message("Access denied")
                .timestamp(LocalDateTime.now())
                .build();

        SecurityResponseWriter.write(response, HttpStatus.FORBIDDEN, body, objectMapper);
    }
}
