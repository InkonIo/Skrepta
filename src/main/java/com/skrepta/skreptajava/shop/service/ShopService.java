package com.skrepta.skreptajava.shop.service;

import com.skrepta.skreptajava.auth.entity.User;
import com.skrepta.skreptajava.auth.exception.ResourceNotFoundException;
import com.skrepta.skreptajava.auth.repository.UserRepository;
import com.skrepta.skreptajava.category.entity.Category;
import com.skrepta.skreptajava.category.repository.CategoryRepository;
import com.skrepta.skreptajava.config.FileStorageService;
import com.skrepta.skreptajava.shop.dto.ShopRequest;
import com.skrepta.skreptajava.shop.dto.ShopResponse;
import com.skrepta.skreptajava.shop.entity.Shop;
import com.skrepta.skreptajava.shop.repository.ShopRepository;
import com.skrepta.skreptajava.item.repository.ItemRepository;
import com.skrepta.skreptajava.item.entity.Item;
import com.skrepta.skreptajava.smartsearch.service.IndexingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;

import java.io.IOException;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShopService {

    private final ShopRepository shopRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final FileStorageService fileStorageService;
    private final ItemRepository itemRepository;
    private final EntityManager entityManager;
    private final IndexingService indexingService; // ✅ ДОБАВЛЕНО

    @Transactional
    public ShopResponse createShop(ShopRequest request) throws IOException {
        User currentUser = getCurrentUser();

        if (currentUser.getRole() != User.Role.SHOP && currentUser.getRole() != User.Role.ADMIN) {
            throw new AccessDeniedException("Only users with role SHOP or ADMIN can create a shop.");
        }

        User owner = currentUser;
        if (currentUser.getRole() == User.Role.ADMIN && request.getOwnerId() != null) {
            owner = userRepository.findById(request.getOwnerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Owner not found with ID: " + request.getOwnerId()));
        }

        Set<Category> categories = getCategoriesByIds(request.getCategoryIds());

        String logoUrl = null;
        if (request.getLogoFile() != null && !request.getLogoFile().isEmpty()) {
            logoUrl = fileStorageService.uploadFile(request.getLogoFile());
        }

        Shop shop = Shop.builder()
                .owner(owner)
                .name(request.getName())
                .description(request.getDescription())
                .logoUrl(logoUrl)
                .phone(request.getPhone())
                .instagramLink(request.getInstagramLink())
                .city(request.getCity())
                .address(request.getAddress())
                .createdAt(Instant.now())
                .isApproved(currentUser.getRole() == User.Role.ADMIN)
                .categories(categories)
                .build();

        Shop savedShop = shopRepository.save(shop);
        
        // ✅ АВТОМАТИЧЕСКАЯ ИНДЕКСАЦИЯ
        try {
            indexingService.indexShop(savedShop);
            log.info("Shop {} automatically indexed for search", savedShop.getId());
        } catch (Exception e) {
            log.error("Failed to auto-index shop {}: {}", savedShop.getId(), e.getMessage());
        }

        return mapToResponse(savedShop);
    }

    @Transactional
    public ShopResponse updateShop(Long shopId, ShopRequest request) throws IOException {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ResourceNotFoundException("Shop not found with ID: " + shopId));

        User currentUser = getCurrentUser();
        checkPermission(shop, currentUser);

        Set<Category> categories = getCategoriesByIds(request.getCategoryIds());

        if (request.getLogoFile() != null && !request.getLogoFile().isEmpty()) {
            if (shop.getLogoUrl() != null) {
                fileStorageService.deleteFile(shop.getLogoUrl());
            }
            String newLogoUrl = fileStorageService.uploadFile(request.getLogoFile());
            shop.setLogoUrl(newLogoUrl);
        }

        shop.setName(request.getName());
        shop.setDescription(request.getDescription());
        shop.setPhone(request.getPhone());
        shop.setInstagramLink(request.getInstagramLink());
        shop.setCity(request.getCity());
        shop.setAddress(request.getAddress());
        shop.setCategories(categories);

        Shop updatedShop = shopRepository.save(shop);
        
        // ✅ ПЕРЕИНДЕКСАЦИЯ ПОСЛЕ ОБНОВЛЕНИЯ
        try {
            indexingService.indexShop(updatedShop);
            log.info("Shop {} re-indexed after update", updatedShop.getId());
        } catch (Exception e) {
            log.error("Failed to re-index shop {}: {}", updatedShop.getId(), e.getMessage());
        }

        return mapToResponse(updatedShop);
    }

    @Transactional
    public void deleteShop(Long shopId) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ResourceNotFoundException("Shop not found with ID: " + shopId));

        User currentUser = getCurrentUser();
        checkPermission(shop, currentUser);

        deleteShopWithItems(shop);
    }

    @Transactional
    public void deleteOwnShop() {
        User currentUser = getCurrentUser();
        List<Shop> shops = shopRepository.findByOwnerId(currentUser.getId());
        Shop shop = shops.stream().findFirst()
            .orElseThrow(() -> new ResourceNotFoundException("Shop not found for current user."));

        deleteShopWithItems(shop);
    }

    @Transactional
    public void adminDeleteShop(Long shopId) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ResourceNotFoundException("Shop not found with ID: " + shopId));

        deleteShopWithItems(shop);
    }

    private void deleteShopWithItems(Shop shop) {
        List<Item> items = itemRepository.findByShopId(shop.getId());
        
        for (Item item : items) {
            entityManager.createNativeQuery("DELETE FROM user_favorites WHERE item_id = :itemId")
                    .setParameter("itemId", item.getId())
                    .executeUpdate();
            
            if (item.getImages() != null) {
                for (String imageUrl : item.getImages()) {
                    fileStorageService.deleteFile(imageUrl);
                }
            }
            
            itemRepository.delete(item);
        }

        if (shop.getLogoUrl() != null) {
            fileStorageService.deleteFile(shop.getLogoUrl());
        }

        shopRepository.delete(shop);
    }

    @Transactional(readOnly = true)
    public ShopResponse getShopById(Long shopId) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ResourceNotFoundException("Shop not found with ID: " + shopId));
        return mapToResponse(shop);
    }

    @Transactional(readOnly = true)
    public ShopResponse getMyShop() {
        User currentUser = getCurrentUser();
        List<Shop> shops = shopRepository.findByOwnerId(currentUser.getId());
        Shop shop = shops.stream().findFirst()
            .orElseThrow(() -> new ResourceNotFoundException("Shop not found for current user."));
        return mapToResponse(shop);
    }

    @Transactional(readOnly = true)
    public List<ShopResponse> getAllApprovedShops() {
        return shopRepository.findAll().stream()
                .filter(Shop::isApproved)
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ShopResponse> getAllShops() {
        return shopRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ShopResponse> getPendingShops() {
        return shopRepository.findAll().stream()
                .filter(shop -> !shop.isApproved())
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ShopResponse approveShop(Long shopId) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ResourceNotFoundException("Shop not found with ID: " + shopId));
        
        shop.setApproved(true);
        Shop approvedShop = shopRepository.save(shop);
        
        // ✅ ПЕРЕИНДЕКСАЦИЯ ПОСЛЕ ОДОБРЕНИЯ
        try {
            indexingService.indexShop(approvedShop);
            log.info("Approved shop {} re-indexed", approvedShop.getId());
        } catch (Exception e) {
            log.error("Failed to re-index shop {}: {}", approvedShop.getId(), e.getMessage());
        }
        
        return mapToResponse(approvedShop);
    }

    @Transactional
    public ShopResponse rejectShop(Long shopId) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new ResourceNotFoundException("Shop not found with ID: " + shopId));
        
        shop.setApproved(false);
        return mapToResponse(shopRepository.save(shop));
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found."));
    }

    private void checkPermission(Shop shop, User currentUser) {
        if (!shop.getOwner().getId().equals(currentUser.getId()) && currentUser.getRole() != User.Role.ADMIN) {
            throw new AccessDeniedException("You do not have permission to modify this shop.");
        }
    }

    private Set<Category> getCategoriesByIds(Set<Long> categoryIds) {
        Set<Category> categories = new HashSet<>(categoryRepository.findAllById(categoryIds));
        if (categories.size() != categoryIds.size()) {
            throw new ResourceNotFoundException("One or more categories not found.");
        }
        return categories;
    }

    private ShopResponse mapToResponse(Shop shop) {
        return ShopResponse.builder()
                .id(shop.getId())
                .owner(mapToUserResponse(shop.getOwner()))
                .name(shop.getName())
                .description(shop.getDescription())
                .logoUrl(shop.getLogoUrl())
                .phone(shop.getPhone())
                .instagramLink(shop.getInstagramLink())
                .city(shop.getCity())
                .address(shop.getAddress())
                .rating(shop.getRating())
                .isApproved(shop.isApproved())
                .createdAt(shop.getCreatedAt())
                .favoritesCount(shop.getFavoritesCount())
                .categories(shop.getCategories().stream()
                        .map(c -> com.skrepta.skreptajava.category.dto.CategoryResponse.builder()
                                .id(c.getId())
                                .name(c.getName())
                                .slug(c.getSlug())
                                .build())
                        .collect(Collectors.toSet()))
                .build();
    }

    private com.skrepta.skreptajava.auth.dto.UserResponse mapToUserResponse(User user) {
        return com.skrepta.skreptajava.auth.dto.UserResponse.builder()
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