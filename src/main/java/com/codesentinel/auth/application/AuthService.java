package com.codesentinel.auth.application;

import com.codesentinel.auth.dto.request.LoginRequest;
import com.codesentinel.auth.dto.request.RefreshTokenRequest;
import com.codesentinel.auth.dto.request.RegisterRequest;
import com.codesentinel.auth.dto.response.AuthResponse;
import com.codesentinel.auth.dto.response.UserResponse;

public interface AuthService {

    UserResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    /**
     * Đổi refresh token còn hạn (chưa bị revoke) lấy cặp access/refresh token mới.
     * Refresh token cũ bị revoke ngay sau khi dùng (rotation) để chống replay.
     */
    AuthResponse refresh(RefreshTokenRequest request);

    /**
     * Thu hồi refresh token, chấm dứt phiên đăng nhập tương ứng.
     */
    void logout(RefreshTokenRequest request);
}
