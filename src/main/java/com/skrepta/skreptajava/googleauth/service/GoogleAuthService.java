package com.skrepta.skreptajava.googleauth.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skrepta.skreptajava.auth.dto.AuthResponse;
import com.skrepta.skreptajava.auth.dto.UserResponse;
import com.skrepta.skreptajava.auth.entity.User;
import com.skrepta.skreptajava.auth.repository.UserRepository;
import com.skrepta.skreptajava.auth.service.JwtService;
import com.skrepta.skreptajava.location.entity.City;
import com.skrepta.skreptajava.location.repository.CityRepository;
import com.skrepta.skreptajava.location.service.LocationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleAuthService {

    private static final String GOOGLE_USERINFO_URL = "https://www.googleapis.com/oauth2/v3/userinfo";

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final CityRepository cityRepository;
    private final LocationService locationService;
    private final RestClient restClient = RestClient.create();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AuthResponse authenticateWithGoogle(String accessToken) {
        try {
            String body = restClient.get()
                    .uri(GOOGLE_USERINFO_URL)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(String.class);

            JsonNode userInfo = objectMapper.readTree(body);

            String email = userInfo.path("email").asText(null);
            if (email == null || email.isBlank()) {
                throw new RuntimeException("Google не вернул email");
            }

            String name = userInfo.path("name").asText(email);
            String pictureUrl = userInfo.path("picture").asText(null);

            User user = userRepository.findByEmail(email).orElseGet(() -> {
                User newUser = User.builder()
                        .email(email)
                        .fio(name)
                        .password("")
                        .role(User.Role.USER)
                        .avatarUrl(pictureUrl)
                        .city(resolveDefaultCity())
                        .phoneNumber(null)
                        .createdAt(Instant.now())
                        .build();
                return userRepository.save(newUser);
            });

            String jwtAccessToken = jwtService.generateToken(user);
            String jwtRefreshToken = jwtService.generateRefreshToken(user);

            return AuthResponse.builder()
                    .accessToken(jwtAccessToken)
                    .refreshToken(jwtRefreshToken)
                    .user(mapToUserResponse(user))
                    .build();

        } catch (RestClientResponseException e) {
            log.error("Google userinfo вернул ошибку: {} {}", e.getStatusCode(), e.getResponseBodyAsString());
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                throw new RuntimeException("Google access_token недействителен или истёк");
            }
            throw new RuntimeException("Ошибка авторизации через Google: " + e.getMessage());
        } catch (Exception e) {
            log.error("Google auth error: {}", e.getMessage());
            throw new RuntimeException("Ошибка авторизации через Google: " + e.getMessage());
        }
    }

    /**
     * При регистрации через Google город неизвестен (Google его не отдаёт),
     * поэтому подставляем Алматы по умолчанию. Пользователь сможет
     * поменять город позже через PUT /api/auth/me.
     */
    private City resolveDefaultCity() {
        return cityRepository.findAll().stream()
                .filter(c -> c.getName() != null && c.getName().equalsIgnoreCase("Алматы"))
                .findFirst()
                .or(() -> cityRepository.findAll().stream().findFirst())
                .orElse(null);
    }

    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fio(user.getFio())
                .phoneNumber(user.getPhoneNumber())
                .city(user.getCity() != null ? locationService.toCityResponse(user.getCity()) : null)
                .role(user.getRole())
                .avatarUrl(user.getAvatarUrl())
                .createdAt(user.getCreatedAt())
                .build();
    }
}