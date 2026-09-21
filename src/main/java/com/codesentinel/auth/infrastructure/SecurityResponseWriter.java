package com.codesentinel.auth.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.io.IOException;

/**
 * Ghi response JSON dùng chung cho {@link RestAuthenticationEntryPoint} và
 * {@link RestAccessDeniedHandler} — 2 nơi duy nhất cần tự tay serialize response
 * vì chúng chạy trong Spring Security filter chain, trước khi request chạm tới
 * {@code DispatcherServlet}/{@code GlobalExceptionHandler}.
 */
final class SecurityResponseWriter {

    private SecurityResponseWriter() {
    }

    static void write(
            HttpServletResponse response,
            HttpStatus status,
            Object body,
            ObjectMapper objectMapper
    ) throws IOException {

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
