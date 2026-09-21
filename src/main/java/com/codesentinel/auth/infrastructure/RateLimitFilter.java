package com.codesentinel.auth.infrastructure;

import com.codesentinel.shared.exception.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Chặn brute-force / spam trên các endpoint dễ bị lạm dụng nhất: đăng nhập,
 * đăng ký, làm mới token. Trước khi có filter này, 3 endpoint trên không có
 * bất kỳ giới hạn số lần gọi nào.
 * <p>
 * Giới hạn theo (IP + đường dẫn), lưu bucket trong memory của chính instance
 * bằng Bucket4j — đơn giản, không cần hạ tầng thêm (Redis...), nhưng KHÔNG
 * dùng chung giới hạn giữa nhiều instance nếu sau này scale ngang (mỗi
 * instance đếm riêng). Khi cần chia sẻ giới hạn giữa nhiều instance, thay
 * {@code buckets} bằng Bucket4j-Redis (hoặc tương đương) mà không cần đổi
 * phần logic còn lại.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Set<String> LIMITED_PATHS = Set.of(
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/refresh"
    );

    private static final int CAPACITY = 5;
    private static final Duration REFILL_PERIOD = Duration.ofMinutes(1);

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper;

    public RateLimitFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {

        return !LIMITED_PATHS.contains(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String key = request.getRemoteAddr() + ":" + request.getRequestURI();

        Bucket bucket = buckets.computeIfAbsent(key, k -> newBucket());

        if (bucket.tryConsume(1)) {

            filterChain.doFilter(request, response);
            return;
        }

        ApiResponse<Void> body = ApiResponse.<Void>builder()
                .success(false)
                .message("Too many requests, please try again later")
                .timestamp(LocalDateTime.now())
                .build();

        SecurityResponseWriter.write(response, HttpStatus.TOO_MANY_REQUESTS, body, objectMapper);
    }

    private Bucket newBucket() {

        Bandwidth limit = Bandwidth.builder()
                .capacity(CAPACITY)
                .refillGreedy(CAPACITY, REFILL_PERIOD)
                .build();

        return Bucket.builder().addLimit(limit).build();
    }
}
