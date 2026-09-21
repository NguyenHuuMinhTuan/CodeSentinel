package com.codesentinel.auth.application;

import com.codesentinel.auth.dto.request.LoginRequest;
import com.codesentinel.auth.dto.request.RegisterRequest;
import com.codesentinel.auth.dto.response.AuthResponse;
import com.codesentinel.auth.dto.response.UserResponse;

public interface AuthService {

    UserResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);
}
