package com.codesentinel.security.jwt;

import com.codesentinel.common.databuffer.DataBuffer;
import com.codesentinel.common.databuffer.DataBufferKey;
import com.codesentinel.security.entity.JwtClaim;
import com.codesentinel.security.entity.JwtConfig;
import com.codesentinel.user.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.function.Function;


@Service
@RequiredArgsConstructor
public class JwtServiceImpl implements JwtService {


    private final JwtConfig jwtConfig;


    private SecretKey getSigningKey() {

        return Keys.hmacShaKeyFor(
                jwtConfig.getSecretKey()
                        .getBytes(StandardCharsets.UTF_8)
        );
    }


    @Override
    public String generateAccessToken(User user) {

        return Jwts.builder()
                .subject(user.getUsername())

                .claim(JwtClaim
                        .USER_ID,
                        user.getId()
                )

                .claim(
                        JwtClaim.USERNAME,
                        user.getUsername()
                )

                .claim(
                        JwtClaim.TOKEN_TYPE,
                        "ACCESS"
                )

                .issuedAt(new Date())

                .expiration(
                        new Date(
                                System.currentTimeMillis()
                                        + jwtConfig.getAccessTokenExpiration()
                        )
                )

                .signWith(getSigningKey())

                .compact();
    }



    @Override
    public String generateRefreshToken(User user) {

        return Jwts.builder()

                .subject(user.getUsername())

                .claim(
                        JwtClaim.USER_ID,
                        user.getId()
                )

                .claim(
                        JwtClaim.TOKEN_TYPE,
                        "REFRESH"
                )

                .issuedAt(new Date())

                .expiration(
                        new Date(
                                System.currentTimeMillis()
                                        + jwtConfig.getRefreshTokenExpiration()
                        )
                )

                .signWith(getSigningKey())

                .compact();
    }



    @Override
    public String extractUsername(String token) {

        return extractClaim(
                token,
                Claims::getSubject
        );
    }



    @Override
    public <T> T extractClaim(
            String token,
            Function<Claims,T> resolver
    ) {

        Claims claims =
                extractAllClaims(token);

        return resolver.apply(claims);
    }



    @Override
    public boolean validateToken(
            String token,
            User user
    ) {

        String username =
                extractUsername(token);


        return username.equals(
                user.getUsername()
        )
                &&
                !isTokenExpired(token);
    }



    @Override
    public Claims extractAllClaims(String token) {


        return Jwts.parser()

                .verifyWith(
                        getSigningKey()
                )

                .build()

                .parseSignedClaims(token)

                .getPayload();
    }



    @Override
    public boolean isTokenExpired(String token) {


        Date expiration =
                extractClaim(
                        token,
                        Claims::getExpiration
                );


        return expiration.before(
                new Date()
        );
    }
}