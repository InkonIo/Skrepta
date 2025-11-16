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
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ItemService {

    private final ItemRepository itemRepository;
    private final ShopRepository shopRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final ShopService shopService; // Для маппинга ShopResponse

    /**
     * Creates a new item for a specific shop.
     * @param shopId The ID of the shop.
     * @param request DTO containing item details.
     * @return The created item as a DTO.
     */
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

        return mapToResponse(itemRepository.save(item));
    }

    /**
     * Updates an existing item. Only the shop owner or an ADMIN can update.
     * @param itemId The ID of the item to update.
     * @param request DTO containing updated item details.
     * @return The updated item as a DTO.
     */
    @Transactional
    public ItemResponse updateItem(Long itemId, ItemRequest request) throws IOException {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with ID: " + itemId));

        User currentUser = getCurrentUser();
        checkItemOwnership(item, currentUser);

        // Handle image update: delete old, upload new
        if (request.getImageFiles() != null && !request.getImageFiles().isEmpty()) {
            // Delete old images
            item.getImages().forEach(fileStorageService::deleteFile);
            List<String> newImageUrls = uploadImages(request.getImageFiles());
            item.setImages(newImageUrls);
        }

        item.setTitle(request.getTitle());
        item.setDescription(request.getDescription());
        item.setTags(request.getTags());
        item.setCity(request.getCity());
        item.setUpdatedAt(Instant.now());

        return mapToResponse(itemRepository.save(item));
    }

    /**
     * Deletes an item. Only the shop owner or an ADMIN can delete.
     * @param itemId The ID of the item to delete.
     */
    @Transactional
    public void deleteItem(Long itemId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with ID: " + itemId));

        User currentUser = getCurrentUser();
        checkItemOwnership(item, currentUser);

        // Delete images from storage
        item.getImages().forEach(fileStorageService::deleteFile);

        itemRepository.delete(item);
    }

    /**
     * Retrieves an item by ID.
     * @param itemId The ID of the item.
     * @return The item as a DTO.
     */
    @Transactional(readOnly = true)
    public ItemResponse getItemById(Long itemId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with ID: " + itemId));
        return mapToResponse(item);
    }

    /**
     * Retrieves all active items (feed).
     * @return A list of active items.
     */
    @Transactional(readOnly = true)
    public List<ItemResponse> getAllActiveItems() {
        return itemRepository.findAll().stream()
                .filter(Item::isActive)
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

        /**
	     * Deletes an item without ownership check (for ADMIN).
	     * @param itemId The ID of the item to delete.
	     */
	    @Transactional
	    public void adminDeleteItem(Long itemId) {
	        Item item = itemRepository.findById(itemId)
	                .orElseThrow(() -> new ResourceNotFoundException("Item not found with ID: " + itemId));
	
	        // Delete images from storage
	        item.getImages().forEach(fileStorageService::deleteFile);
	
	        itemRepository.delete(item);
	    }

        /**
            * Retrieves all active items for a specific shop.
            * @param shopId The ID of the shop.
            * @return A list of active items for the shop.
            */
        @Transactional(readOnly = true)
        public List<ItemResponse> getItemsByShopId(Long shopId) {
            Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ResourceNotFoundException("Shop not found with id: " + shopId));

            // Показываем только активные товары для публичного просмотра
            List<Item> items = itemRepository.findByShopId(shopId);

            return items.stream()
                .filter(Item::isActive) // ✅ Используем isActive() вместо getStatus()
                .map(this::mapToResponse) // ✅ Используем правильное имя метода
                .collect(Collectors.toList());
        }
	

        /**
     * Increases view count for an item.
     * @param itemId The ID of the item.
     */
    @Transactional
    public void incrementItemView(Long itemId) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with ID: " + itemId));
        
        item.setViews(item.getViews() + 1);
        itemRepository.save(item);
    }
	
    // --- Helper Methods ---
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

        public ItemResponse mapToResponse(Item item)
        {
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
