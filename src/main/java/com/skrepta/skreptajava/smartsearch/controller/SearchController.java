package com.skrepta.skreptajava.smartsearch.controller;

import com.pgvector.PGvector;
import com.skrepta.skreptajava.smartsearch.dto.SearchRequest;
import com.skrepta.skreptajava.smartsearch.dto.SearchResponse;
import com.skrepta.skreptajava.smartsearch.service.IndexingService;
import com.skrepta.skreptajava.smartsearch.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
@Tag(name = "Smart Search", description = "AI-powered semantic search API")
public class SearchController {

    private final SearchService searchService;
    private final IndexingService indexingService;

    /**
     * Основной эндпоинт для поиска
     * GET /api/search?query=что-то для кухни&type=ITEM&limit=20
     */
    @GetMapping
    @Operation(summary = "Semantic search", description = "Search for items, shops, and categories using AI")
    public ResponseEntity<SearchResponse> search(
            @RequestParam String query,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "20") Integer limit
    ) {
        log.info("Search request - query: '{}', type: {}, limit: {}", query, type, limit);

        if (query == null || query.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        SearchRequest request = new SearchRequest();
        request.setQuery(query.trim());
        request.setType(type);
        request.setLimit(Math.min(limit, 100)); // Максимум 100 результатов

        SearchResponse response = searchService.search(request);
        return ResponseEntity.ok(response);
    }

    /**
     * POST версия для более сложных запросов
     */
    @PostMapping
    @Operation(summary = "Semantic search (POST)", description = "Search using request body")
    public ResponseEntity<SearchResponse> searchPost(@RequestBody SearchRequest request) {
        log.info("Search POST request - query: '{}'", request.getQuery());

        if (request.getQuery() == null || request.getQuery().trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        SearchResponse response = searchService.search(request);
        return ResponseEntity.ok(response);
    }

    // ============================================
    // ADMIN ENDPOINTS - Управление индексацией
    // ============================================

    /**
     * Полная переиндексация всех данных (ADMIN only)
     * ВНИМАНИЕ: Это долгая операция!
     */
    @PostMapping("/admin/reindex-all")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reindex all data", description = "Regenerate embeddings for all items, shops, and categories (ADMIN only)")
    public ResponseEntity<String> reindexAll() {
        log.info("Admin triggered full reindexing");
        
        // Запускаем в отдельном потоке, чтобы не блокировать запрос
        new Thread(() -> {
            try {
                indexingService.indexAllData();
            } catch (Exception e) {
                log.error("Error during reindexing: {}", e.getMessage(), e);
            }
        }).start();

        return ResponseEntity.ok("Reindexing started in background");
    }

    /**
     * Переиндексация всех товаров (ADMIN only)
     */
    @PostMapping("/admin/reindex-items")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reindex items", description = "Regenerate embeddings for all items (ADMIN only)")
    public ResponseEntity<String> reindexItems() {
        log.info("Admin triggered items reindexing");
        
        new Thread(() -> {
            try {
                int count = indexingService.indexAllItems();
                log.info("Reindexed {} items", count);
            } catch (Exception e) {
                log.error("Error reindexing items: {}", e.getMessage(), e);
            }
        }).start();

        return ResponseEntity.ok("Items reindexing started");
    }

    /**
     * Переиндексация всех магазинов (ADMIN only)
     */
    @PostMapping("/admin/reindex-shops")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reindex shops", description = "Regenerate embeddings for all shops (ADMIN only)")
    public ResponseEntity<String> reindexShops() {
        log.info("Admin triggered shops reindexing");
        
        new Thread(() -> {
            try {
                int count = indexingService.indexAllShops();
                log.info("Reindexed {} shops", count);
            } catch (Exception e) {
                log.error("Error reindexing shops: {}", e.getMessage(), e);
            }
        }).start();

        return ResponseEntity.ok("Shops reindexing started");
    }

    /**
     * Переиндексация конкретного товара
     */
    @PostMapping("/admin/reindex-item/{itemId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SHOP')")
    @Operation(summary = "Reindex single item", description = "Regenerate embedding for a specific item")
    public ResponseEntity<String> reindexItem(@PathVariable Long itemId) {
        log.info("Reindexing item: {}", itemId);
        indexingService.indexItemById(itemId);
        return ResponseEntity.ok("Item reindexed successfully");
    }

    /**
     * Переиндексация конкретного магазина
     */
    @PostMapping("/admin/reindex-shop/{shopId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SHOP')")
    @Operation(summary = "Reindex single shop", description = "Regenerate embedding for a specific shop")
    public ResponseEntity<String> reindexShop(@PathVariable Long shopId) {
        log.info("Reindexing shop: {}", shopId);
        indexingService.indexShopById(shopId);
        return ResponseEntity.ok("Shop reindexed successfully");
    }


    @GetMapping("/test")
@Operation(summary = "Test search setup")
public ResponseEntity<Map<String, Object>> testSetup() {
    Map<String, Object> status = new HashMap<>();
    
    try {
        // Проверяем OpenAI через SearchService
        status.put("message", "Testing search setup...");
        
        // Простой тестовый поиск
        SearchRequest testRequest = new SearchRequest();
        testRequest.setQuery("test");
        testRequest.setLimit(1);
        
        SearchResponse response = searchService.search(testRequest);
        status.put("openai", "OK");
        status.put("search_works", true);
        status.put("results_count", response.getTotalResults());
        
    } catch (Exception e) {
        status.put("error", e.getMessage());
        status.put("search_works", false);
        log.error("Test failed", e);
    }
    
    return ResponseEntity.ok(status);
}
}