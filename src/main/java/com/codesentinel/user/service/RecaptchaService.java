package com.codesentinel.user.service;

import com.codesentinel.user.dto.response.RecaptchaResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class RecaptchaService {

    @Value("${google.recaptcha.secret}")
    private String recaptchaSecret;

    @Value("${google.recaptcha.url}")
    private String recaptchaUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    public boolean verifyToken(String token) {
        try {
            // Chuẩn bị dữ liệu form-urlencoded theo yêu cầu của Google
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("secret", recaptchaSecret);
            params.add("response", token);

            // Gửi request POST sang Google server
            RecaptchaResponse response = restTemplate.postForObject(recaptchaUrl, params, RecaptchaResponse.class);

            // Kiểm tra: Phải thành công và điểm tin cậy > 0.5 (Tránh bot)
            return response != null && response.isSuccess() && response.getScore() >= 0.5;
        } catch (Exception e) {
            // Nếu lỗi đường truyền, tạm thời cho false để bảo mật
            return false;
        }
    }
}