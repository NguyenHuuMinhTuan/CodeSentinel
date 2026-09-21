package com.codesentinel.auth.application;

import com.codesentinel.auth.dto.request.CreateUserRequest;
import com.codesentinel.auth.dto.response.UserResponse;

import java.util.List;

public interface UserService {

    UserResponse createUser(CreateUserRequest request);

    List<UserResponse> getAllUsers();
}
