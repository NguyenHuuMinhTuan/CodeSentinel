package com.codesentinel.user.service;

import com.codesentinel.user.dto.request.LoginRequest;
import com.codesentinel.user.dto.request.RegisterRequest;
import com.codesentinel.user.dto.response.AuthResponse;
import com.codesentinel.user.dto.response.UserResponse;

public interface AuthService {

    UserResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);
}