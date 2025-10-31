package com.skrepta.skreptajava.auth.dto;

import com.skrepta.skreptajava.auth.entity.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "Email cannot be empty")
    @Email(message = "Email should be valid")
    private String email;

    @NotBlank(message = "Password cannot be empty")
    @Size(min = 6, message = "Password must be at least 6 characters long")
    private String password;

    @NotBlank(message = "Full name (FIO) cannot be empty")
    private String fio;

    @NotBlank(message = "Phone number cannot be empty")
    private String phoneNumber;

    @NotBlank(message = "City cannot be empty")
    private String city;

    private User.Role role; // Optional, defaults to USER in service layer
}
