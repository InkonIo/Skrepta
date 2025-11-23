package com.skrepta.skreptajava.item.service;

import com.skrepta.skreptajava.auth.entity.User;
import com.skrepta.skreptajava.auth.exception.ResourceNotFoundException;
import com.skrepta.skreptajava.auth.repository.UserRepository;
import com.skrepta.skreptajava.config.FileStorageService;
import com.skrepta.skreptajava.item.dto.ItemRequest;
import com.skrepta.skreptajava.item.dto.ItemResponse;
import com.skrepta.skreptajava.item.entity.Item;
import com.skrepta.skreptajava.item.repository.ItemRepository;
import com.skrepta.skreptajava.shop.entity.Shop;
import com.skrepta.skreptajava.shop.repository.ShopRepository;
import com.skrepta.skreptajava.shop.service.ShopService;
import com.skrepta.skreptajava.smartsearch.service.IndexingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ItemService {

    private final ItemRepository itemRepository;
    private final ShopRepository shopRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final ShopService shopService;
    private final IndexingService indexingService; // ✅ ДОБАВЛЕНО

    @Transactional
    public ItemResponse createItem(Long shopId, ItemRequest request) throws IOException {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ResourceNotFoundException("Shop not found with ID: " + shopId));

        User currentUser = getCurrentUser();
        checkShopOwnership(shop, currentUser);

        List<String> imageUrls = uploadImages(request.getImageFiles());

        Item item = Item.builder()
                .shop(shop)
                .title(request.getTitle())
                .description(request.getDescription())
                .images(imageUrls)
                .tags(request.getTags())
                .city(request.getCity())
                .isActive(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        Item savedItem = itemRepository.save(item);
        
        // ✅ АВТОМАТИЧЕСКАЯ ИНДЕКСАЦИЯ
        try {
            indexingService.indexItem(savedItem);
            log.info("Item {} automatically indexed for search", savedItem.getId());
        } catch (Exception e) {
            log.error("Failed to auto-index item {}: {}", savedItem.getId(), e.getMessage());
        }

        return mapToResponse(savedItem);
    }

    @Transactional
    public ItemResponse updateItem(Long itemId, ItemRequest request) throws IOException {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with ID: " + itemId));

        User currentUser = getCurrentUser();
        checkItemOwnership(item, currentUser);

        if (request.getImageFiles() != null && !request.getImageFiles().isEmpty()) {
            item.getImages().forEach(fileStorageService::deleteFile);
            List<String> newImageUrls = uploadImages(request.getImageFiles());
            item.setImages(newImageUrls);
        }

        item.setTitle(request.getTitle());
        item.setDescription(request.getDescription());
        item.setTags(request.getTags());
        item.setCity(request.getCity());
        item.setUpdatedAt(Instant.now());

        Item updatedItem = itemRepository.save(item);
        
        // ✅ ПЕРЕИНДЕКСАЦИЯ ПОСЛЕ ОБНОВЛЕНИЯ
        try {
            indexingService.indexItem(updatedItem);
            log.info("Item {} re-indexed after update", updatedItem.getId());
        } catch (Exception e) {
            log.error("Failed to re-index item {}: {}", updatedItem.getId(), e.getMessage());
        }

        return mapToResponse(updatedItem);
    }

    @Transactional
    public void deleteItem(Long itemId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with ID: " + itemId));

        User currentUser = getCurrentUser();
        checkItemOwnership(item, currentUser);

        item.getImages().forEach(fileStorageService::deleteFile);
        itemRepository.delete(item);
    }

    @Transactional(readOnly = true)
    public ItemResponse getItemById(Long itemId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with ID: " + itemId));
        return mapToResponse(item);
    }

    @Transactional(readOnly = true)
    public List<ItemResponse> getAllActiveItems() {
        return itemRepository.findAll().stream()
                .filter(Item::isActive)
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void adminDeleteItem(Long itemId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with ID: " + itemId));

        item.getImages().forEach(fileStorageService::deleteFile);
        itemRepository.delete(item);
    }

    @Transactional(readOnly = true)
    public List<ItemResponse> getItemsByShopId(Long shopId) {
        Shop shop = shopRepository.findById(shopId)
            .orElseThrow(() -> new ResourceNotFoundException("Shop not found with id: " + shopId));

        List<Item> items = itemRepository.findByShopId(shopId);

        return items.stream()
            .filter(Item::isActive)
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    @Transactional
    public void incrementItemView(Long itemId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with ID: " + itemId));
        
        item.setViews(item.getViews() + 1);
        itemRepository.save(item);
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found."));
    }

    private void checkShopOwnership(Shop shop, User currentUser) {
        if (!shop.getOwner().getId().equals(currentUser.getId()) && currentUser.getRole() != User.Role.ADMIN) {
            throw new AccessDeniedException("You do not have permission to create items for this shop.");
        }
    }

    private void checkItemOwnership(Item item, User currentUser) {
        if (!item.getShop().getOwner().getId().equals(currentUser.getId()) && currentUser.getRole() != User.Role.ADMIN) {
            throw new AccessDeniedException("You do not have permission to modify this item.");
        }
    }

    private List<String> uploadImages(List<MultipartFile> files) throws IOException {
        if (files == null || files.isEmpty()) {
            return List.of();
        }
        return files.stream()
                .map(file -> {
                    try {
                        return fileStorageService.uploadFile(file);
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to upload file to storage.", e);
                    }
                })
                .collect(Collectors.toList());
    }

    public ItemResponse mapToResponse(Item item) {
        return ItemResponse.builder()
                .id(item.getId())
                .shop(shopService.getShopById(item.getShop().getId()))
                .title(item.getTitle())
                .description(item.getDescription())
                .images(item.getImages())
                .tags(item.getTags())
                .city(item.getCity())
                .isActive(item.isActive())
                .views(item.getViews())
                .favorites(item.getFavorites())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .build();
    }
}