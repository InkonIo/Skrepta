package com.skrepta.skreptajava.admin.dto;

import com.skrepta.skreptajava.auth.entity.User;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserRoleUpdateRequest {
    @NotNull(message = "Role cannot be null")
    private User.Role newRole;
}
