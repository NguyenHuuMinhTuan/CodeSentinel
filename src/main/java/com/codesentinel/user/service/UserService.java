package com.codesentinel.user.service;

import com.codesentinel.common.exception.BadRequestException;
import com.codesentinel.user.dto.request.CreateUserRequest;
import com.codesentinel.user.dto.response.UserResponse;
import com.codesentinel.user.entity.User;
import com.codesentinel.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public UserResponse createUser(CreateUserRequest request) {

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new BadRequestException("Email already exists");
        }

        User user = User.builder()
                .fullName(request.getUsername())
                .email(request.getEmail())
                .password(request.getPassword())
                .createdAt(LocalDateTime.now())
                .build();

        User savedUser = userRepository.save(user);

        return UserResponse.builder()
                .id(savedUser.getId())
                .username(savedUser.getFullName())
                .email(savedUser.getEmail())
                .build();
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
}
