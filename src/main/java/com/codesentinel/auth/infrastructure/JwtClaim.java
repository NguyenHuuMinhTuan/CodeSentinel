package com.codesentinel.auth.infrastructure;

/**
 * Nguồn duy nhất khai báo tên các claim JWT dùng trong hệ thống.
 * (Trước refactor tồn tại 2 class trùng tên ở 2 package khác nhau — đã gộp lại đây.)
 */
public final class JwtClaim {

    private JwtClaim() {
    }

    public static final String USER_ID = "user_id";

    public static final String USERNAME = "username";

    public static final String TOKEN_TYPE = "token_type";
}
