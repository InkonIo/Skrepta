package com.skrepta.skreptajava.auth.service;

import com.skrepta.skreptajava.auth.dto.AuthResponse;
import com.skrepta.skreptajava.auth.dto.LoginRequest;
import com.skrepta.skreptajava.auth.dto.RegisterRequest;
import com.skrepta.skreptajava.auth.dto.ForgotPasswordRequest;
import com.skrepta.skreptajava.auth.dto.ResetPasswordRequest;
import com.skrepta.skreptajava.auth.dto.UserResponse;
import com.skrepta.skreptajava.auth.dto.UserUpdateRequest;
import com.skrepta.skreptajava.auth.entity.User;
import com.skrepta.skreptajava.auth.exception.InvalidCredentialsException;
import com.skrepta.skreptajava.auth.exception.ResourceNotFoundException;
import com.skrepta.skreptajava.auth.exception.UserAlreadyExistsException;
import com.skrepta.skreptajava.auth.repository.UserRepository;
import com.skrepta.skreptajava.location.entity.City;
import com.skrepta.skreptajava.location.repository.CityRepository;
import com.skrepta.skreptajava.location.service.LocationService;
import com.skrepta.skreptajava.shop.repository.ShopRepository;
import com.skrepta.skreptajava.shop.entity.Shop;
import com.skrepta.skreptajava.config.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final ShopRepository shopRepository;
    private final CityRepository cityRepository;
    private final LocationService locationService;
    private final FileStorageService fileStorageService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("User with email " + request.getEmail() + " already exists.");
        }

        User.Role role = request.getRole() != null ? request.getRole() : User.Role.USER;

        City city = cityRepository.findById(request.getCityId())
                .orElseThrow(() -> new ResourceNotFoundException("City not found with ID: " + request.getCityId()));

        var user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fio(request.getFio())
                .phoneNumber(request.getPhoneNumber())
                .city(city)
                .role(role)
                .createdAt(Instant.now())
                .build();

        userRepository.save(user);

        String email = user.getEmail();
        String fio = user.getFio();
        CompletableFuture.runAsync(() -> emailService.sendRegistrationConfirmationEmail(email, fio));

        var jwtToken = jwtService.generateToken(user);
        var refreshToken = jwtService.generateRefreshToken(user);

        return AuthResponse.builder()
                .accessToken(jwtToken)
                .refreshToken(refreshToken)
                .user(mapToUserResponse(user))
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );
        } catch (AuthenticationException e) {
            throw new InvalidCredentialsException("Invalid email or password.");
        }

        var user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("User not found after successful authentication attempt."));

        var jwtToken = jwtService.generateToken(user);
        var refreshToken = jwtService.generateRefreshToken(user);

        return AuthResponse.builder()
                .accessToken(jwtToken)
                .refreshToken(refreshToken)
                .user(mapToUserResponse(user))
                .build();
    }

    public void forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + request.getEmail()));

        String otp = String.format("%06d", new Random().nextInt(999999));

        user.setResetPasswordToken(otp);
        user.setResetPasswordTokenExpiry(Instant.now().plusSeconds(600));
        userRepository.save(user);

        String email = user.getEmail();
        CompletableFuture.runAsync(() -> emailService.sendPasswordResetCode(email, otp));
    }

    public void resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByResetPasswordToken(request.getToken())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid or expired password reset code."));

        if (user.getResetPasswordTokenExpiry() == null || user.getResetPasswordTokenExpiry().isBefore(Instant.now())) {
            user.setResetPasswordToken(null);
            user.setResetPasswordTokenExpiry(null);
            userRepository.save(user);
            throw new InvalidCredentialsException("Invalid or expired password reset code.");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setResetPasswordToken(null);
        user.setResetPasswordTokenExpiry(null);
        userRepository.save(user);
    }

    public AuthResponse refreshToken(String refreshToken) {
        final String userEmail = jwtService.extractUsername(refreshToken);

        if (userEmail != null) {
            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));

            if (jwtService.isTokenValid(refreshToken, user)) {
                String newAccessToken = jwtService.generateToken(user);
                return AuthResponse.builder()
                        .accessToken(newAccessToken)
                        .refreshToken(refreshToken)
                        .user(mapToUserResponse(user))
                        .build();
            }
        }
        throw new InvalidCredentialsException("Invalid or expired refresh token");
    }

    @Transactional
    public UserResponse updateMyProfile(String email, UserUpdateRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        if (request.getFio() != null && !request.getFio().isBlank()) {
            user.setFio(request.getFio());
        }

        if (request.getPhoneNumber() != null && !request.getPhoneNumber().isBlank()) {
            if (!request.getPhoneNumber().equals(user.getPhoneNumber())
                    && userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
                throw new UserAlreadyExistsException("Этот номер телефона уже используется");
            }
            user.setPhoneNumber(request.getPhoneNumber());
        }

        if (request.getCityId() != null) {
            City city = cityRepository.findById(request.getCityId())
                    .orElseThrow(() -> new ResourceNotFoundException("City not found with ID: " + request.getCityId()));
            user.setCity(city);
        }

        userRepository.save(user);

        return mapToUserResponse(user);
    }

    @Transactional
    public void deleteMyAccount(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        if (user.getRole() == User.Role.SHOP) {
            List<Shop> userShops = shopRepository.findByOwnerId(user.getId());

            for (Shop shop : userShops) {
                if (shop.getLogoUrl() != null) {
                    try {
                        fileStorageService.deleteFile(shop.getLogoUrl());
                    } catch (Exception e) {
                        System.err.println("Failed to delete shop logo: " + shop.getLogoUrl());
                    }
                }

                if (shop.getItems() != null) {
                    shop.getItems().forEach(item -> {
                        if (item.getImages() != null) {
                            item.getImages().forEach(imageUrl -> {
                                try {
                                    fileStorageService.deleteFile(imageUrl);
                                } catch (Exception e) {
                                    System.err.println("Failed to delete item image: " + imageUrl);
                                }
                            });
                        }
                    });
                }

                shopRepository.delete(shop);
            }
        }

        if (user.getAvatarUrl() != null) {
            try {
                fileStorageService.deleteFile(user.getAvatarUrl());
            } catch (Exception e) {
                System.err.println("Failed to delete user avatar: " + user.getAvatarUrl());
            }
        }

        user.getFavorites().clear();
        userRepository.save(user);
        userRepository.delete(user);
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