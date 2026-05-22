package com.codesentinel.user.controller;

import com.codesentinel.user.dto.request.CreateUserRequest;
import com.codesentinel.user.dto.response.UserResponse;
import com.codesentinel.user.entity.User;
import com.codesentinel.user.service.UserService;
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
    public UserResponse createUser(
            @Valid @RequestBody CreateUserRequest request
    ) {
        return userService.createUser(request);
    }

    @GetMapping
    public List<User> getUsers() {
        return userService.getAllUsers();
    }
}
