package com.skrepta.skreptajava.item.service;

import com.skrepta.skreptajava.auth.entity.User;
import com.skrepta.skreptajava.auth.exception.ResourceNotFoundException;
import com.skrepta.skreptajava.auth.repository.UserRepository;
import com.skrepta.skreptajava.item.dto.ItemResponse;
import com.skrepta.skreptajava.item.entity.Item;
import com.skrepta.skreptajava.item.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FavoritesService {

    private final UserRepository userRepository;
    private final ItemRepository itemRepository;
    private final ItemService itemService; // Для маппинга ItemResponse

    /**
     * Retrieves the current authenticated user.
     */
    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found."));
    }

    /**
     * Retrieves all favorite items for the current user.
     * @return A list of favorite items as DTOs.
     */
    @Transactional(readOnly = true)
    public List<ItemResponse> getFavorites() {
        User user = getCurrentUser();
        return user.getFavorites().stream()
                .map(itemService::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Adds an item to the current user's favorites.
     * @param itemId The ID of the item to add.
     */
    @Transactional
    public void addItemToFavorites(Long itemId) {
        User user = getCurrentUser();
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with ID: " + itemId));

        if (user.getFavorites().add(item)) {
            // Увеличиваем счетчик избранного у товара
            item.setFavorites(item.getFavorites() + 1);
            itemRepository.save(item);
            userRepository.save(user);
        }
    }

    /**
     * Removes an item from the current user's favorites.
     * @param itemId The ID of the item to remove.
     */
    @Transactional
    public void removeItemFromFavorites(Long itemId) {
        User user = getCurrentUser();
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with ID: " + itemId));

        if (user.getFavorites().remove(item)) {
            // Уменьшаем счетчик избранного у товара
            item.setFavorites(item.getFavorites() - 1);
            itemRepository.save(item);
            userRepository.save(user);
        }
    }
}
