package com.skrepta.skreptajava.admin.service;

import com.skrepta.skreptajava.auth.dto.UserResponse;
import com.skrepta.skreptajava.auth.entity.User;
import com.skrepta.skreptajava.auth.exception.ResourceNotFoundException;
import com.skrepta.skreptajava.auth.repository.UserRepository;
import com.skrepta.skreptajava.location.entity.City;
import com.skrepta.skreptajava.location.repository.CityRepository;
import com.skrepta.skreptajava.location.service.LocationService;
import com.skrepta.skreptajava.shop.service.ShopService;
import com.skrepta.skreptajava.item.service.ItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final ShopService shopService;
    private final ItemService itemService;
    private final CityRepository cityRepository;
    private final LocationService locationService;

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::mapToUserResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public UserResponse updateUserRole(Long userId, User.Role newRole) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        user.setRole(newRole);
        userRepository.save(user);

        return mapToUserResponse(user);
    }

    @Transactional
    public UserResponse updateUser(Long userId, com.skrepta.skreptajava.admin.dto.UserUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        if (!user.getEmail().equals(request.getEmail()) && userRepository.existsByEmail(request.getEmail())) {
            throw new com.skrepta.skreptajava.auth.exception.UserAlreadyExistsException("User with email " + request.getEmail() + " already exists.");
        }

        City city = cityRepository.findById(request.getCityId())
                .orElseThrow(() -> new ResourceNotFoundException("City not found with ID: " + request.getCityId()));

        user.setEmail(request.getEmail());
        user.setFio(request.getFio());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setCity(city);
        user.setRole(request.getRole());
        user.setAvatarUrl(request.getAvatarUrl());

        userRepository.save(user);

        return mapToUserResponse(user);
    }

    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        // TODO: Добавить логику для удаления связанных данных (магазины, товары и т.д.)
        // Временно просто удаляем пользователя.
        userRepository.delete(user);
    }

    @Transactional
    public void deleteShop(Long shopId) {
        shopService.adminDeleteShop(shopId);
    }

    @Transactional
    public void deleteItem(Long itemId) {
        itemService.adminDeleteItem(itemId);
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