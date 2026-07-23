package com.codesentinel.common.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL) // Trường nào null sẽ tự động biến mất khỏi JSON
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private Object errors; // Dùng Object hoặc List<String> để chứa lỗi Validation
    private LocalDateTime timestamp;
}
