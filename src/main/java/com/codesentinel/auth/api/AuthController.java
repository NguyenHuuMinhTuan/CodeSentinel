package com.codesentinel.auth.api;

import com.codesentinel.auth.application.AuthService;
import com.codesentinel.auth.dto.request.LoginRequest;
import com.codesentinel.auth.dto.request.RegisterRequest;
import com.codesentinel.auth.dto.response.AuthResponse;
import com.codesentinel.auth.dto.response.UserResponse;
import com.codesentinel.shared.exception.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(
            @Valid @RequestBody RegisterRequest request
    ) {

        UserResponse data = authService.register(request);

        return ResponseEntity.ok(
                ApiResponse.success(data, "Register successfully")
        );
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request
    ) {

        AuthResponse data = authService.login(request);

        return ResponseEntity.ok(
                ApiResponse.success(data, "Login successfully")
        );
    }
}
