package com.codesentinel.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class RegisterRequest {


    @NotBlank(message = "Full name is required")
    private String fullName;


    @NotBlank(message = "Username is required")
    private String username;


    @Email(message = "Email invalid")
    @NotBlank(message = "Email is required")
    private String email;


    @NotBlank(message = "Password is required")
    private String password;

}