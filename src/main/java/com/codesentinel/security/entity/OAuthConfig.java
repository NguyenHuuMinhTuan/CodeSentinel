package com.codesentinel.security.entity;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;


@Configuration
@ConfigurationProperties(prefix = "spring.security.oauth2.client.registration")
@Getter
@Setter
public class OAuthConfig {


    private Facebook facebook;


    @Getter
    @Setter
    public static class Facebook {

        private String clientId;

        private String clientSecret;

        private String redirectUri;
    }
}