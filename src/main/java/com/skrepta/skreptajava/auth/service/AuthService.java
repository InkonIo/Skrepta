package com.skrepta.skreptajava.auth.service;

import com.skrepta.skreptajava.auth.dto.AuthResponse;
import com.skrepta.skreptajava.auth.dto.LoginRequest;
import com.skrepta.skreptajava.auth.dto.RegisterRequest;
import com.skrepta.skreptajava.auth.dto.ForgotPasswordRequest;
import com.skrepta.skreptajava.auth.dto.ResetPasswordRequest;
import com.skrepta.skreptajava.auth.dto.UserResponse;
import com.skrepta.skreptajava.auth.entity.User;
import com.skrepta.skreptajava.auth.exception.InvalidCredentialsException;
import com.skrepta.skreptajava.auth.exception.ResourceNotFoundException;
import com.skrepta.skreptajava.auth.exception.UserAlreadyExistsException;
import com.skrepta.skreptajava.auth.repository.UserRepository;
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

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final ShopRepository shopRepository;
    private final FileStorageService fileStorageService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;

    /**
     * Registers a new user.
     * @param request the registration request DTO
     * @return the authentication response with tokens and user details
     * @throws UserAlreadyExistsException if a user with the given email already exists
     */
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("User with email " + request.getEmail() + " already exists.");
        }

        // Default role to USER if not provided or invalid
        User.Role role = request.getRole() != null ? request.getRole() : User.Role.USER;

        var user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fio(request.getFio())
                .phoneNumber(request.getPhoneNumber())
                .city(request.getCity())
                .role(role)
                .createdAt(Instant.now())
                .build();

        userRepository.save(user);

        // Send registration confirmation email
        emailService.sendRegistrationConfirmationEmail(user.getEmail(), user.getFio());

        var jwtToken = jwtService.generateToken(user);
        var refreshToken = jwtService.generateRefreshToken(user);

        return AuthResponse.builder()
                .accessToken(jwtToken)
                .refreshToken(refreshToken)
                .user(mapToUserResponse(user))
                .build();
    }

    /**
     * Authenticates an existing user.
     * @param request the login request DTO
     * @return the authentication response with tokens and user details
     * @throws InvalidCredentialsException if authentication fails
     */
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

    /**
     * Initiates the password reset process by generating a 6-digit code and sending it via email.
     * @param request the forgot password request DTO
     */
    public void forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + request.getEmail()));

        // 1. Generate 6-digit code
        String otp = String.format("%06d", new Random().nextInt(999999));

        // 2. Save code and expiry to user
        user.setResetPasswordToken(otp);
        // Token expires in 10 minutes
        user.setResetPasswordTokenExpiry(Instant.now().plusSeconds(600));
        userRepository.save(user);

        // 3. Send email with the code
        emailService.sendPasswordResetCode(user.getEmail(), otp);
    }

    /**
     * Resets the user's password using the 6-digit code.
     * @param request the reset password request DTO
     */
    public void resetPassword(ResetPasswordRequest request) {
        // 1. Find user by token (code)
        User user = userRepository.findByResetPasswordToken(request.getToken())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid or expired password reset code."));

        // 2. Check if token is expired
        if (user.getResetPasswordTokenExpiry() == null || user.getResetPasswordTokenExpiry().isBefore(Instant.now())) {
            // Clear the token to prevent future attempts with the same expired code
            user.setResetPasswordToken(null);
            user.setResetPasswordTokenExpiry(null);
            userRepository.save(user);
            throw new InvalidCredentialsException("Invalid or expired password reset code.");
        }

        // 3. Update password and clear token fields
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setResetPasswordToken(null);
        user.setResetPasswordTokenExpiry(null);
        userRepository.save(user);
    }

    /**
     * Refreshes the access token using a valid refresh token.
     * @param refreshToken the refresh token
     * @return a new AuthResponse with a new access token and the same refresh token
     */
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

    /**
     * Deletes the user account and all associated data (shops, items, favorites, files).
     * @param email the email of the user to delete
     */
    @Transactional
    public void deleteMyAccount(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        // 1. Если пользователь - владелец магазина(ов), удаляем его магазины и связанные файлы
        if (user.getRole() == User.Role.SHOP) {
            List<Shop> userShops = shopRepository.findByOwnerId(user.getId());
            
            for (Shop shop : userShops) {
                // Удаляем логотип магазина
                if (shop.getLogoUrl() != null) {
                    try {
                        fileStorageService.deleteFile(shop.getLogoUrl());
                    } catch (Exception e) {
                        // Логируем ошибку, но продолжаем удаление
                        System.err.println("Failed to delete shop logo: " + shop.getLogoUrl());
                    }
                }
                
                // Удаляем изображения всех товаров магазина
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
                
                // Удаляем магазин (товары удалятся каскадно благодаря CascadeType.ALL)
                shopRepository.delete(shop);
            }
        }

        // 2. Удаляем аватар пользователя, если есть
        if (user.getAvatarUrl() != null) {
            try {
                fileStorageService.deleteFile(user.getAvatarUrl());
            } catch (Exception e) {
                System.err.println("Failed to delete user avatar: " + user.getAvatarUrl());
            }
        }

        // 3. Очищаем избранное (разрываем связь many-to-many)
        user.getFavorites().clear();
        userRepository.save(user);

        // 4. Удаляем пользователя
        userRepository.delete(user);
    }

    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fio(user.getFio())
                .phoneNumber(user.getPhoneNumber())
                .city(user.getCity())
                .role(user.getRole())
                .avatarUrl(user.getAvatarUrl())
                .build();
    }
}