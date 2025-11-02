package com.skrepta.skreptajava.shop.controller;

import com.skrepta.skreptajava.shop.dto.ShopRequest;
import com.skrepta.skreptajava.shop.dto.ShopResponse;
import com.skrepta.skreptajava.shop.service.ShopService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/shops")
@RequiredArgsConstructor
public class ShopController {

    private final ShopService shopService;

    // Public endpoint: Get all approved shops
    @GetMapping
    public ResponseEntity<List<ShopResponse>> getAllShops() {
        return ResponseEntity.ok(shopService.getAllApprovedShops());
    }

    // Public endpoint: Get shop by ID
    @GetMapping("/{id}")
    public ResponseEntity<ShopResponse> getShopById(@PathVariable Long id) {
        return ResponseEntity.ok(shopService.getShopById(id));
    }

    // SHOP endpoint: Create a new shop
    @PostMapping
    @PreAuthorize("hasRole('SHOP') or hasRole('ADMIN')")
    public ResponseEntity<ShopResponse> createShop(
            @Valid @ModelAttribute ShopRequest request
    ) throws IOException {
        return new ResponseEntity<>(shopService.createShop(request), HttpStatus.CREATED);
    }

    // SHOP/ADMIN endpoint: Update an existing shop
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SHOP') or hasRole('ADMIN')")
    public ResponseEntity<ShopResponse> updateShop(
            @PathVariable Long id,
            @Valid @ModelAttribute ShopRequest request
    ) throws IOException {
        return ResponseEntity.ok(shopService.updateShop(id, request));
    }

    // SHOP/ADMIN endpoint: Delete a shop
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SHOP') or hasRole('ADMIN')")
    public ResponseEntity<Void> deleteShop(@PathVariable Long id) {
        shopService.deleteShop(id);
        return ResponseEntity.noContent().build();
    }
}
