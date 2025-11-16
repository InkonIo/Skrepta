package com.skrepta.skreptajava.item.controller;

import com.skrepta.skreptajava.item.dto.ItemRequest;
import com.skrepta.skreptajava.item.dto.ItemResponse;
import com.skrepta.skreptajava.item.service.ItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;

    // Public endpoint: Get all active items (The main feed)
    @GetMapping("/items")
    public ResponseEntity<List<ItemResponse>> getAllItems() {
        return ResponseEntity.ok(itemService.getAllActiveItems());
    }

    // Public endpoint: Get item by ID
    @GetMapping("/items/{id}")
    public ResponseEntity<ItemResponse> getItemById(@PathVariable Long id) {
        return ResponseEntity.ok(itemService.getItemById(id));
    }

    // ✅ NEW: Public endpoint: Get all items for a specific shop
    @GetMapping("/shops/{shopId}/items")
    public ResponseEntity<List<ItemResponse>> getItemsByShop(@PathVariable Long shopId) {
        return ResponseEntity.ok(itemService.getItemsByShopId(shopId));
    }

    // SHOP/ADMIN endpoint: Create a new item for a specific shop
    @PostMapping("/shops/{shopId}/items")
    @PreAuthorize("hasRole('SHOP') or hasRole('ADMIN')")
    public ResponseEntity<ItemResponse> createItem(
            @PathVariable Long shopId,
            @Valid @ModelAttribute ItemRequest request
    ) throws IOException {
        return new ResponseEntity<>(itemService.createItem(shopId, request), HttpStatus.CREATED);
    }

    // SHOP/ADMIN endpoint: Update an existing item
    @PutMapping("/items/{id}")
    @PreAuthorize("hasRole('SHOP') or hasRole('ADMIN')")
    public ResponseEntity<ItemResponse> updateItem(
            @PathVariable Long id,
            @Valid @ModelAttribute ItemRequest request
    ) throws IOException {
        return ResponseEntity.ok(itemService.updateItem(id, request));
    }

    // SHOP/ADMIN endpoint: Delete an item
    @DeleteMapping("/items/{id}")
    @PreAuthorize("hasRole('SHOP') or hasRole('ADMIN')")
    public ResponseEntity<Void> deleteItem(@PathVariable Long id) {
        itemService.deleteItem(id);
        return ResponseEntity.noContent().build();
    }

    // Public endpoint: Increment item view count
    @PostMapping("/items/{id}/view")
    public ResponseEntity<Map<String, String>> incrementItemView(@PathVariable Long id) {
        itemService.incrementItemView(id);
        return ResponseEntity.ok(Map.of("message", "View incremented successfully"));
    }
}