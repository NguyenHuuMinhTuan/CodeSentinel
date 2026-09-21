package com.codesentinel.shared.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;

import org.springframework.security.web.SecurityFilterChain;

@Slf4j
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        log.info("Security filter chain initialized");

        http
                .cors(cors -> {})
                .csrf(AbstractHttpConfigurer::disable)

                .authorizeHttpRequests(auth -> auth

                        .requestMatchers(
                                "/api/auth/**",
                                "/api/scans/**"
                        ).permitAll()

                        .anyRequest()
                        .authenticated()
                )

                .oauth2Login(oauth -> {
                    // TODO: cấu hình successHandler để phát hành JWT nội bộ sau khi OAuth2 login thành công
                });

        return http.build();
    }
}
