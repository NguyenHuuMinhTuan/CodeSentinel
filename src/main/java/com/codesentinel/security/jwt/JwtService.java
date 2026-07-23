package com.codesentinel.security.jwt;

import com.codesentinel.user.entity.User;

import io.jsonwebtoken.Claims;

import java.util.function.Function;

public interface JwtService {


    /**
     * Tạo access token
     */
    String generateAccessToken(User user);


    /**
     * Tạo refresh token
     */
    String generateRefreshToken(User user);


    /**
     * Lấy username từ token
     */
    String extractUsername(String token);


    /**
     * Lấy claim bất kỳ
     */
    <T> T extractClaim(
            String token,
            Function<Claims, T> resolver
    );


    /**
     * Kiểm tra token hợp lệ
     */
    boolean validateToken(
            String token,
            User user
    );


    /**
     * Lấy toàn bộ claims
     */
    Claims extractAllClaims(String token);


    /**
     * Kiểm tra token hết hạn
     */
    boolean isTokenExpired(String token);

}