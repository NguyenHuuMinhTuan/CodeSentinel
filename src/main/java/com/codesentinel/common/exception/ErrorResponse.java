package com.codesentinel.common.exception;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ErrorResponse {
    private boolean success;

    private String message;

    private List<String> errors;

    private LocalDateTime timestamp;
}
