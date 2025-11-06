package com.skrepta.skreptajava.auth.dto;

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
public class ResetPasswordRequest {

    @NotBlank(message = "Token cannot be empty")
    @Size(min = 6, max = 6, message = "Token must be 6 digits")
    private String token;

    @NotBlank(message = "New password cannot be empty")
    @Size(min = 6, message = "Password must be at least 6 characters long")
    private String newPassword;
}
