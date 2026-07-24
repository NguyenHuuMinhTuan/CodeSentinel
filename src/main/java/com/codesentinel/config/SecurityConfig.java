package com.codesentinel.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;

import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        System.out.println("SECURITY CONFIG LOADED");

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
            // Sau này sẽ cấu hình successHandler ở đây
        });

        return http.build();
    }
}