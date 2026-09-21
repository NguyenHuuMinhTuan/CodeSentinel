package com.codesentinel.auth.api;

import com.codesentinel.auth.application.UserService;
import com.codesentinel.auth.dto.request.CreateUserRequest;
import com.codesentinel.auth.dto.response.UserResponse;
import com.codesentinel.shared.exception.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    public ApiResponse<UserResponse> createUser(
            @Valid @RequestBody CreateUserRequest request
    ) {
        return ApiResponse.success(
                userService.createUser(request),
                "User created successfully"
        );
    }

    @GetMapping
    public ApiResponse<List<UserResponse>> getUsers() {
        return ApiResponse.success(
                userService.getAllUsers(),
                "Fetched users successfully"
        );
    }
}
