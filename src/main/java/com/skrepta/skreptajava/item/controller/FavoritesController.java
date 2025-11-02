package com.skrepta.skreptajava.item.controller;

import com.skrepta.skreptajava.item.dto.ItemResponse;
import com.skrepta.skreptajava.item.service.FavoritesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class FavoritesController {

    private final FavoritesService favoritesService;

    // USER endpoint: Get all favorite items
    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'SHOP', 'ADMIN')")
    public ResponseEntity<List<ItemResponse>> getFavorites() {
        return ResponseEntity.ok(favoritesService.getFavorites());
    }

    // USER endpoint: Add item to favorites
    @PostMapping("/{itemId}")
    @PreAuthorize("hasAnyRole('USER', 'SHOP', 'ADMIN')")
    public ResponseEntity<Void> addItemToFavorites(@PathVariable Long itemId) {
        favoritesService.addItemToFavorites(itemId);
        return ResponseEntity.ok().build();
    }

    // USER endpoint: Remove item from favorites
    @DeleteMapping("/{itemId}")
    @PreAuthorize("hasAnyRole('USER', 'SHOP', 'ADMIN')")
    public ResponseEntity<Void> removeItemFromFavorites(@PathVariable Long itemId) {
        favoritesService.removeItemFromFavorites(itemId);
        return ResponseEntity.noContent().build();
    }
}
