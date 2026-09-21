package com.codesentinel.auth.application;

import com.codesentinel.auth.dto.response.RecaptchaResponse;
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
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("secret", recaptchaSecret);
            params.add("response", token);

            RecaptchaResponse response = restTemplate.postForObject(recaptchaUrl, params, RecaptchaResponse.class);

            // Phải thành công và điểm tin cậy > 0.5 (tránh bot)
            return response != null && response.isSuccess() && response.getScore() >= 0.5;
        } catch (Exception e) {
            // Lỗi đường truyền -> coi như thất bại để đảm bảo an toàn
            return false;
        }
    }
}
