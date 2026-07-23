package com.codesentinel.common.init;

import com.codesentinel.common.databuffer.DataBuffer;
import com.codesentinel.security.entity.JwtConfig;
import com.codesentinel.security.entity.OAuthConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
public class ApplicationInit implements CommandLineRunner {


    private final JwtConfig jwtConfig;

    private final OAuthConfig oauthConfig;


    @Override
    public void run(String... args) throws Exception {


        DataBuffer.put("JWT_CONFIG", jwtConfig);

        DataBuffer.put("OAUTH_CONFIG", oauthConfig);

        System.out.println("Đã nạp thành công conf");

        System.out.println(jwtConfig.getSecretKey());

        System.out.println(jwtConfig.getAccessTokenExpiration());

        System.out.println(jwtConfig.getRefreshTokenExpiration());

        System.out.println(oauthConfig.getFacebook().getClientId());


    }
}