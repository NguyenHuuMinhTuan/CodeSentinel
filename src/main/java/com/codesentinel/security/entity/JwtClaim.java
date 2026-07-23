package com.codesentinel.security.entity;


public final class JwtClaim {


    private JwtClaim() {
    }


    /**
     * User id
     */
    public static final String USER_ID = "user_id";


    /**
     * Username
     */
    public static final String USERNAME = "username";


    /**
     * Role
     */
    public static final String ROLE = "role";


    /**
     * Token type
     */
    public static final String TOKEN_TYPE = "token_type";


    /**
     * Access token
     */
    public static final String ACCESS_TOKEN = "access_token";


    /**
     * Refresh token
     */
    public static final String REFRESH_TOKEN = "refresh_token";

}