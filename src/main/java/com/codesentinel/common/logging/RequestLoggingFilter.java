package com.codesentinel.common.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    private final StandardServletMultipartResolver multipartResolver =
            new StandardServletMultipartResolver();

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        ContentCachingRequestWrapper wrappedRequest =
                new ContentCachingRequestWrapper(request);

        try {

            log.info("========== REQUEST START ==========");
            log.info("TIME: {}", LocalDateTime.now());
            log.info("METHOD: {}", request.getMethod());
            log.info("URI: {}", request.getRequestURI());
            log.info("IP: {}", request.getRemoteAddr());
            log.info("CONTENT-TYPE: {}", request.getContentType());

            // Log query params
            if (!request.getParameterMap().isEmpty()) {

                log.info("PARAMS:");

                request.getParameterMap().forEach((key, value) -> {
                    log.info("{} = {}", key, String.join(",", value));
                });
            }

            // Multipart request
            if (multipartResolver.isMultipart(request)) {

                MultipartHttpServletRequest multipartRequest =
                        multipartResolver.resolveMultipart(request);

                for (Map.Entry<String, MultipartFile> entry :
                        multipartRequest.getFileMap().entrySet()) {

                    MultipartFile file = entry.getValue();

                    log.info("========== FILE ==========");
                    log.info("KEY: {}", entry.getKey());
                    log.info("FILE NAME: {}", file.getOriginalFilename());
                    log.info("SIZE: {} bytes", file.getSize());
                    log.info("CONTENT TYPE: {}", file.getContentType());
                }
            }

            filterChain.doFilter(wrappedRequest, response);

            // Body JSON/Text
            String body = new String(
                    wrappedRequest.getContentAsByteArray(),
                    StandardCharsets.UTF_8
            );

            if (StringUtils.hasText(body)) {
                log.info("BODY: {}", body);
            }

            log.info("STATUS: {}", response.getStatus());
            log.info("========== REQUEST END ==========");

        } catch (Exception e) {

            log.error("REQUEST LOGGING ERROR", e);

            throw e;
        }
    }
}