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
     * Deletes a user by ID.
     * @param userId the ID of the user to delete
     */
    public void deleteUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with ID: " + userId);
        }
        userRepository.deleteById(userId);
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
