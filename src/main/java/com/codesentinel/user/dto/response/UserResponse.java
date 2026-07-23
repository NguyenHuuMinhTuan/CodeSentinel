package com.codesentinel.user.dto.response;

import lombok.Builder;
import lombok.Data;


@Data
@Builder
public class UserResponse {

    private Long id;

    private String fullName;

    private String username;

    private String email;

    private String avatarUrl;

}