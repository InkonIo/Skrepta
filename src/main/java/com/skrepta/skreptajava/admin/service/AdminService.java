package com.skrepta.skreptajava.admin.service;

import com.skrepta.skreptajava.auth.dto.UserResponse;
import com.skrepta.skreptajava.auth.entity.User;
import com.skrepta.skreptajava.auth.exception.ResourceNotFoundException;
import com.skrepta.skreptajava.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;

    /**
     * Retrieves all users and maps them to UserResponse DTOs.
     * @return a list of all users
     */
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::mapToUserResponse)
                .collect(Collectors.toList());
    }

    /**
     * Updates the role of a specific user.
     * @param userId the ID of the user to update
     * @param newRole the new role to assign
     * @return the updated user as a UserResponse DTO
     */
    @Transactional
    public UserResponse updateUserRole(Long userId, User.Role newRole) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        user.setRole(newRole);
        userRepository.save(user);

        return mapToUserResponse(user);
    }

    /**
     * Updates a user's details by ID.
     * @param userId the ID of the user to update
     * @param request the update request DTO
     * @return the updated user as a UserResponse DTO
     */
    @Transactional
    public UserResponse updateUser(Long userId, com.skrepta.skreptajava.admin.dto.UserUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        // Проверка на уникальность email, если он изменился
        if (!user.getEmail().equals(request.getEmail()) && userRepository.existsByEmail(request.getEmail())) {
            throw new com.skrepta.skreptajava.auth.exception.UserAlreadyExistsException("User with email " + request.getEmail() + " already exists.");
        }

        user.setEmail(request.getEmail());
        user.setFio(request.getFio());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setCity(request.getCity());
        user.setRole(request.getRole());
        user.setAvatarUrl(request.getAvatarUrl());

        userRepository.save(user);

        return mapToUserResponse(user);
    }

    /**
     * Deletes a user by ID.
     * @param userId the ID of the user to delete
     */
    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        
        // TODO: Добавить логику для удаления связанных данных (магазины, товары и т.д.)
        // Временно просто удаляем пользователя.
        userRepository.delete(user);
    }

    /**
     * Maps a User entity to a UserResponse DTO.
     */
    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fio(user.getFio())
                .phoneNumber(user.getPhoneNumber())
                .city(user.getCity())
                .role(user.getRole())
                .avatarUrl(user.getAvatarUrl())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
