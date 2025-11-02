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
}
