package com.skrepta.skreptajava.auth.dto;

import com.skrepta.skreptajava.auth.entity.User;
import com.skrepta.skreptajava.location.dto.CityResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private String email;
    private String fio;
    private String phoneNumber;
    private CityResponse city;
    private User.Role role;
    private String avatarUrl;
    private java.time.Instant createdAt;
}