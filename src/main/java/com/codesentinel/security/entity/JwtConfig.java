package com.codesentinel.security.entity;

import com.codesentinel.common.databuffer.DataBuffer;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "spring.security.jwt")
@Getter
@Setter
public class JwtConfig {

    private String secretKey;

    private long accessTokenExpiration;

    private long refreshTokenExpiration;

    private String issuer;


}
