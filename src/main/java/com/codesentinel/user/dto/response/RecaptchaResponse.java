package com.codesentinel.user.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
public class RecaptchaResponse {
    private boolean success;
    private double score; // Thường v3 sẽ trả về điểm số (Ví dụ: 0.9)
    private String action;
    @JsonProperty("error-codes")
    private List<String> errorCodes;
}