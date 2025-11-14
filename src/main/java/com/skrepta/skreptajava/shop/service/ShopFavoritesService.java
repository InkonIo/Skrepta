package com.skrepta.skreptajava.shop.service;

import com.skrepta.skreptajava.auth.entity.User;
import com.skrepta.skreptajava.auth.exception.ResourceNotFoundException;
import com.skrepta.skreptajava.auth.repository.UserRepository;
import com.skrepta.skreptajava.shop.dto.ShopResponse;
import com.skrepta.skreptajava.shop.entity.Shop;
import com.skrepta.skreptajava.shop.repository.ShopRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ShopFavoritesService {

    private final UserRepository userRepository;
    private final ShopRepository shopRepository;
    private final ShopService shopService;

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found."));
    }

    /**
     * Получить все избранные магазины текущего пользователя
     */
    @Transactional(readOnly = true)
    public List<ShopResponse> getFavoriteShops() {
        User user = getCurrentUser();
        return user.getFavoriteShops().stream()
                .map(shop -> shopService.getShopById(shop.getId()))
                .collect(Collectors.toList());
    }

    /**
     * Добавить магазин в избранное
     */
    @Transactional
    public void addShopToFavorites(Long shopId) {
        User user = getCurrentUser();
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ResourceNotFoundException("Shop not found with ID: " + shopId));

        if (user.getFavoriteShops().add(shop)) {
            shop.setFavoritesCount(shop.getFavoritesCount() + 1);
            shopRepository.save(shop);
            userRepository.save(user);
        }
    }

    /**
     * Удалить магазин из избранного
     */
    @Transactional
    public void removeShopFromFavorites(Long shopId) {
        User user = getCurrentUser();
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ResourceNotFoundException("Shop not found with ID: " + shopId));

        if (user.getFavoriteShops().remove(shop)) {
            shop.setFavoritesCount(Math.max(0, shop.getFavoritesCount() - 1));
            shopRepository.save(shop);
            userRepository.save(user);
        }
    }

    /**
     * Проверить, находится ли магазин в избранном у пользователя
     */
    @Transactional(readOnly = true)
    public boolean isShopInFavorites(Long shopId) {
        User user = getCurrentUser();
        return user.getFavoriteShops().stream()
                .anyMatch(shop -> shop.getId().equals(shopId));
    }
}