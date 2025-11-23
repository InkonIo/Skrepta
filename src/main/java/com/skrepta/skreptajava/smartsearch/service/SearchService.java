package com.skrepta.skreptajava.smartsearch.service;

import com.pgvector.PGvector;
import com.skrepta.skreptajava.category.service.CategoryService;
import com.skrepta.skreptajava.item.service.ItemService;
import com.skrepta.skreptajava.shop.service.ShopService;
import com.skrepta.skreptajava.smartsearch.dto.SearchRequest;
import com.skrepta.skreptajava.smartsearch.dto.SearchResponse;
import com.skrepta.skreptajava.smartsearch.dto.SearchResultItem;
import com.skrepta.skreptajava.smartsearch.repository.SearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchService {

    private final EmbeddingService embeddingService;
    private final SearchRepository searchRepository;
    private final ItemService itemService;
    private final ShopService shopService;
    private final CategoryService categoryService;

    private static final double MIN_SCORE_THRESHOLD = 0.7; // Минимальная релевантность

    /**
     * Выполняет семантический поиск по всем типам объектов
     */
    @Transactional(readOnly = true)
    public SearchResponse search(SearchRequest request) {
        log.info("Searching for: '{}' (type: {}, limit: {})", 
                request.getQuery(), request.getType(), request.getLimit());

        try {
            // 1. Генерируем вектор для поискового запроса
            PGvector queryEmbedding = embeddingService.generateEmbedding(request.getQuery());
            if (queryEmbedding == null) {
                log.error("Failed to generate embedding for query: {}", request.getQuery());
                return SearchResponse.builder()
                        .query(request.getQuery())
                        .totalResults(0)
                        .results(List.of())
                        .build();
            }

        List<SearchResultItem> allResults = new ArrayList<>();

        // 2. Ищем по каждому типу объектов
        if (request.getType() == null || "ITEM".equals(request.getType())) {
            allResults.addAll(searchItemsInternal(queryEmbedding, request.getLimit()));
        }

        if (request.getType() == null || "SHOP".equals(request.getType())) {
            allResults.addAll(searchShopsInternal(queryEmbedding, request.getLimit()));
        }

        if (request.getType() == null || "CATEGORY".equals(request.getType())) {
            allResults.addAll(searchCategoriesInternal(queryEmbedding, request.getLimit()));
        }

        // 3. Сортируем по релевантности и фильтруем по минимальному порогу
        List<SearchResultItem> filteredResults = allResults.stream()
                .filter(item -> item.getScore() >= MIN_SCORE_THRESHOLD)
                .sorted(Comparator.comparing(SearchResultItem::getScore).reversed())
                .limit(request.getLimit())
                .toList();

            log.info("Found {} results for query: '{}'", filteredResults.size(), request.getQuery());

            return SearchResponse.builder()
                    .query(request.getQuery())
                    .totalResults(filteredResults.size())
                    .results(filteredResults)
                    .build();
        } catch (Exception e) {
            log.error("Error during search for query '{}': {}", request.getQuery(), e.getMessage(), e);
            throw new RuntimeException("Search failed: " + e.getMessage(), e);
        }
    }

    /**
     * Внутренний метод поиска товаров
     */
    private List<SearchResultItem> searchItemsInternal(PGvector queryEmbedding, int limit) {
        try {
            List<Map<String, Object>> rawResults = searchRepository.searchItems(queryEmbedding, limit);
            
            return rawResults.stream()
                    .map(result -> {
                        Long id = (Long) result.get("id");
                        try {
                            return SearchResultItem.builder()
                                    .type("ITEM")
                                    .id(id)
                                    .title((String) result.get("title"))
                                    .score((Double) result.get("score"))
                                    .data(itemService.getItemById(id))
                                    .build();
                        } catch (Exception e) {
                            log.warn("Failed to load item {}: {}", id, e.getMessage());
                            return null;
                        }
                    })
                    .filter(item -> item != null)
                    .toList();
        } catch (Exception e) {
            log.error("Error searching items: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Внутренний метод поиска магазинов
     */
    private List<SearchResultItem> searchShopsInternal(PGvector queryEmbedding, int limit) {
        try {
            List<Map<String, Object>> rawResults = searchRepository.searchShops(queryEmbedding, limit);
            
            return rawResults.stream()
                    .map(result -> {
                        Long id = (Long) result.get("id");
                        try {
                            return SearchResultItem.builder()
                                    .type("SHOP")
                                    .id(id)
                                    .title((String) result.get("title"))
                                    .score((Double) result.get("score"))
                                    .data(shopService.getShopById(id))
                                    .build();
                        } catch (Exception e) {
                            log.warn("Failed to load shop {}: {}", id, e.getMessage());
                            return null;
                        }
                    })
                    .filter(shop -> shop != null)
                    .toList();
        } catch (Exception e) {
            log.error("Error searching shops: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Внутренний метод поиска категорий
     */
    private List<SearchResultItem> searchCategoriesInternal(PGvector queryEmbedding, int limit) {
        try {
            List<Map<String, Object>> rawResults = searchRepository.searchCategories(queryEmbedding, limit);
            
            return rawResults.stream()
                    .map(result -> {
                        Long id = (Long) result.get("id");
                        try {
                            return SearchResultItem.builder()
                                    .type("CATEGORY")
                                    .id(id)
                                    .title((String) result.get("title"))
                                    .score((Double) result.get("score"))
                                    .data(categoryService.getCategoryById(id))
                                    .build();
                        } catch (Exception e) {
                            log.warn("Failed to load category {}: {}", id, e.getMessage());
                            return null;
                        }
                    })
                    .filter(category -> category != null)
                    .toList();
        } catch (Exception e) {
            log.error("Error searching categories: {}", e.getMessage());
            return List.of();
        }
    }
}