package com.skrepta.skreptajava.admin.controller;

import com.skrepta.skreptajava.admin.dto.UserRoleUpdateRequest;
import com.skrepta.skreptajava.admin.dto.UserUpdateRequest;
import com.skrepta.skreptajava.admin.service.AdminService;
import com.skrepta.skreptajava.auth.dto.UserResponse;
import com.skrepta.skreptajava.shop.dto.ShopResponse;
import com.skrepta.skreptajava.shop.service.ShopService;
	import com.skrepta.skreptajava.item.service.ItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')") // Все методы в этом контроллере доступны только ADMIN
public class AdminController {

    private final AdminService adminService;
    private final ShopService shopService;
	    private final ItemService itemService;

    // === Управление пользователями ===
    
    @GetMapping("/users")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(adminService.getAllUsers());
    }

    @PutMapping("/users/{userId}/role")
    public ResponseEntity<UserResponse> updateUserRole(
            @PathVariable Long userId,
            @Valid @RequestBody UserRoleUpdateRequest request
    ) {
        return ResponseEntity.ok(adminService.updateUserRole(userId, request.getNewRole()));
    }

    @PutMapping("/users/{userId}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long userId,
            @Valid @RequestBody UserUpdateRequest request
    ) {
        return ResponseEntity.ok(adminService.updateUser(userId, request));
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long userId) {
        adminService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }

    // === Управление магазинами ===
    
    @GetMapping("/shops")
    public ResponseEntity<List<ShopResponse>> getAllShops() {
        return ResponseEntity.ok(shopService.getAllShops());
    }

    @GetMapping("/shops/pending")
    public ResponseEntity<List<ShopResponse>> getPendingShops() {
        return ResponseEntity.ok(shopService.getPendingShops());
    }

    @PutMapping("/shops/{shopId}/approve")
    public ResponseEntity<ShopResponse> approveShop(@PathVariable Long shopId) {
        return ResponseEntity.ok(shopService.approveShop(shopId));
    }

    @PutMapping("/shops/{shopId}/reject")
	    public ResponseEntity<ShopResponse> rejectShop(@PathVariable Long shopId) {
	        return ResponseEntity.ok(shopService.rejectShop(shopId));
	    }
	
	    @DeleteMapping("/shops/{shopId}")
	    public ResponseEntity<Void> deleteShop(@PathVariable Long shopId) {
	        adminService.deleteShop(shopId);
	        return ResponseEntity.noContent().build();
	    }
	
	    // === Управление товарами ===
	
	    @DeleteMapping("/items/{itemId}")
	    public ResponseEntity<Void> deleteItem(@PathVariable Long itemId) {
	        adminService.deleteItem(itemId);
	        return ResponseEntity.noContent().build();
	    }
	}