package com.codesentinel.user.service;

import com.codesentinel.common.databuffer.DataBuffer;
import com.codesentinel.common.databuffer.DataBufferKey;
import com.codesentinel.security.entity.JwtConfig;
import com.codesentinel.security.jwt.JwtService;
import com.codesentinel.user.dto.request.LoginRequest;
import com.codesentinel.user.dto.request.RegisterRequest;
import com.codesentinel.user.dto.response.AuthResponse;
import com.codesentinel.user.dto.response.UserResponse;
import com.codesentinel.user.entity.RefreshToken;
import com.codesentinel.user.entity.User;
import com.codesentinel.user.repository.RefreshTokenRepository;
import com.codesentinel.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {


    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final JwtService jwtService;

    private final RefreshTokenRepository refreshTokenRepository;

    private final JwtConfig jwtConfig;


    @Override
    public UserResponse register(
            RegisterRequest request
    ) {


        if(userRepository.existsByUsername(
                request.getUsername()
        )){

            throw new RuntimeException(
                    "Username already exists"
            );
        }


        if(userRepository.existsByEmail(
                request.getEmail()
        )){

            throw new RuntimeException(
                    "Email already exists"
            );
        }



        User user = User.builder()

                .fullName(
                        request.getFullName()
                )

                .username(
                        request.getUsername()
                )

                .email(
                        request.getEmail()
                )

                .password(
                        passwordEncoder.encode(
                                request.getPassword()
                        )
                )

                .active(true)

                .createdAt(
                        LocalDateTime.now()
                )

                .build();



        User saved =
                userRepository.save(user);



        return UserResponse.builder()

                .id(saved.getId())

                .fullName(saved.getFullName())

                .username(saved.getUsername())

                .email(saved.getEmail())

                .avatarUrl(saved.getAvatarUrl())

                .build();

    }

    @Override
    public AuthResponse login(LoginRequest request) {


        User user =
                userRepository.findByEmail(
                                request.getEmail()
                        )
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Email hoặc password không đúng"
                                )
                        );


        // Check password

        if (!passwordEncoder.matches(
                request.getPassword(),
                user.getPassword()
        )) {

            throw new RuntimeException(
                    "Username hoặc password không đúng"
            );
        }


        // Generate access token

        String accessToken =
                jwtService.generateAccessToken(user);


        // Generate refresh token

        String refreshToken =
                jwtService.generateRefreshToken(user);


        // Lưu refresh token

        RefreshToken entity =
                RefreshToken.builder()
                        .token(refreshToken)
                        .user(user)
                        .expiryDate(
                                Instant.now()
                                        .plusMillis(
                                                jwtConfig.getRefreshTokenExpiration()
                                        )
                        )
                        .createdAt(
                                Instant.now()
                        )
                        .build();


        refreshTokenRepository.save(entity);


        return AuthResponse.builder()

                .accessToken(accessToken)

                .refreshToken(refreshToken)

                .tokenType("Bearer")

                .expiresIn(
                        jwtConfig.getAccessTokenExpiration()
                )

                .build();
    }
}