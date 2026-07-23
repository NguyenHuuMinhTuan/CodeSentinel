package com.codesentinel.user.controller;

import com.codesentinel.common.exception.ApiResponse;
import com.codesentinel.user.dto.request.LoginRequest;
import com.codesentinel.user.dto.request.RegisterRequest;
import com.codesentinel.user.dto.response.AuthResponse;
import com.codesentinel.user.dto.response.UserResponse;
import com.codesentinel.user.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(
            @Valid @RequestBody RegisterRequest request
    ) {


        UserResponse data =
                authService.register(request);


        return ResponseEntity.ok(

                ApiResponse.<UserResponse>builder()

                        .success(true)

                        .message("Register successfully")

                        .data(data)

                        .timestamp(
                                LocalDateTime.now()
                        )

                        .build()
        );
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {

        AuthResponse data = authService.login(request);

        ApiResponse<AuthResponse> response = ApiResponse.<AuthResponse>builder()
                .success(true)
                .message("Login successfully")
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.ok(response);
    }
}