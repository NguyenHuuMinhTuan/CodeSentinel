package com.codesentinel.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
public class RecaptchaResponse {
    private boolean success;
    private double score; // reCAPTCHA v3 trả về điểm tin cậy, ví dụ 0.9
    private String action;
    @JsonProperty("error-codes")
    private List<String> errorCodes;
}
