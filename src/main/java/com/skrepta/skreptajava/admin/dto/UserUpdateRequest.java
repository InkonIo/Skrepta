package com.skrepta.skreptajava.admin.dto;

import com.skrepta.skreptajava.auth.entity.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserUpdateRequest {
    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;

    @NotBlank(message = "FIO is required")
    private String fio;

    private String phoneNumber;

    private String city;

    @NotNull(message = "Role is required")
    private User.Role role;

    private String avatarUrl;
}
