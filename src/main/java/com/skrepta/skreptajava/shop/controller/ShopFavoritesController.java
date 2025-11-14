package com.skrepta.skreptajava.shop.controller;

import com.skrepta.skreptajava.shop.dto.ShopResponse;
import com.skrepta.skreptajava.shop.service.ShopFavoritesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shop-favorites")
@RequiredArgsConstructor
public class ShopFavoritesController {

    private final ShopFavoritesService shopFavoritesService;

    /**
     * Получить все избранные магазины
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'SHOP', 'ADMIN')")
    public ResponseEntity<List<ShopResponse>> getFavoriteShops() {
        return ResponseEntity.ok(shopFavoritesService.getFavoriteShops());
    }

    /**
     * Добавить магазин в избранное
     */
    @PostMapping("/{shopId}")
    @PreAuthorize("hasAnyRole('USER', 'SHOP', 'ADMIN')")
    public ResponseEntity<Void> addShopToFavorites(@PathVariable Long shopId) {
        shopFavoritesService.addShopToFavorites(shopId);
        return ResponseEntity.ok().build();
    }

    /**
     * Удалить магазин из избранного
     */
    @DeleteMapping("/{shopId}")
    @PreAuthorize("hasAnyRole('USER', 'SHOP', 'ADMIN')")
    public ResponseEntity<Void> removeShopFromFavorites(@PathVariable Long shopId) {
        shopFavoritesService.removeShopFromFavorites(shopId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Проверить, находится ли магазин в избранном
     */
    @GetMapping("/{shopId}/check")
    @PreAuthorize("hasAnyRole('USER', 'SHOP', 'ADMIN')")
    public ResponseEntity<Boolean> isShopInFavorites(@PathVariable Long shopId) {
        return ResponseEntity.ok(shopFavoritesService.isShopInFavorites(shopId));
    }
}