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

    private static final double MIN_SCORE_THRESHOLD = 0.5; // Снизил до 50%

    /**
     * Выполняет семантический поиск по всем типам объектов
     * С FALLBACK на keyword search если AI недоступен
     */
    @Transactional(readOnly = true)
    public SearchResponse search(SearchRequest request) {
        log.info("Searching for: '{}' (type: {}, limit: {})", 
                request.getQuery(), request.getType(), request.getLimit());

        // Пытаемся использовать AI semantic search
        try {
            return semanticSearch(request);
            
        } catch (Exception e) {
            log.warn("⚠️ Semantic search failed (OpenAI unavailable?), falling back to keyword search: {}", 
                    e.getMessage());
            
            // FALLBACK: Простой текстовый поиск
            return keywordSearchFallback(request);
        }
    }

    /**
     * AI-powered semantic search (основной метод)
     */
    private SearchResponse semanticSearch(SearchRequest request) {
        // 1. Генерируем вектор для поискового запроса
        PGvector queryEmbedding = embeddingService.generateEmbedding(request.getQuery());
        if (queryEmbedding == null) {
            throw new RuntimeException("Failed to generate embedding");
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

        // 3. Сортируем по релевантности и фильтруем
        List<SearchResultItem> filteredResults = allResults.stream()
                .filter(item -> item.getScore() >= MIN_SCORE_THRESHOLD)
                .sorted(Comparator.comparing(SearchResultItem::getScore).reversed())
                .limit(request.getLimit())
                .toList();

        log.info("✅ Semantic search: found {} results for query: '{}'", 
                filteredResults.size(), request.getQuery());

        return SearchResponse.builder()
                .query(request.getQuery())
                .totalResults(filteredResults.size())
                .results(filteredResults)
                .isFallback(false) // AI search успешен
                .build();
    }

    /**
     * FALLBACK: Простой keyword search (когда OpenAI недоступен)
     */
    private SearchResponse keywordSearchFallback(SearchRequest request) {
        log.info("🔍 Using FALLBACK keyword search for: '{}'", request.getQuery());

        List<SearchResultItem> allResults = new ArrayList<>();

        try {
            // Keyword search по каждому типу
            if (request.getType() == null || "ITEM".equals(request.getType())) {
                allResults.addAll(keywordSearchItems(request.getQuery(), request.getLimit()));
            }

            if (request.getType() == null || "SHOP".equals(request.getType())) {
                allResults.addAll(keywordSearchShops(request.getQuery(), request.getLimit()));
            }

            if (request.getType() == null || "CATEGORY".equals(request.getType())) {
                allResults.addAll(keywordSearchCategories(request.getQuery(), request.getLimit()));
            }

            // Сортируем и обрезаем
            List<SearchResultItem> results = allResults.stream()
                    .sorted(Comparator.comparing(SearchResultItem::getScore).reversed())
                    .limit(request.getLimit())
                    .toList();

            log.info("⚠️ Fallback search: found {} results", results.size());

            return SearchResponse.builder()
                    .query(request.getQuery())
                    .totalResults(results.size())
                    .results(results)
                    .isFallback(true) // Это fallback!
                    .message("AI search temporarily unavailable. Showing keyword-based results.")
                    .build();

        } catch (Exception e) {
            log.error("❌ Even fallback search failed: {}", e.getMessage());
            
            // Возвращаем пустой результат
            return SearchResponse.builder()
                    .query(request.getQuery())
                    .totalResults(0)
                    .results(List.of())
                    .isFallback(true)
                    .message("Search temporarily unavailable. Please try again later.")
                    .build();
        }
    }

    // ============================================
    // SEMANTIC SEARCH - внутренние методы
    // ============================================

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

    // ============================================
    // FALLBACK: KEYWORD SEARCH - внутренние методы
    // ============================================

    private List<SearchResultItem> keywordSearchItems(String query, int limit) {
        try {
            List<Map<String, Object>> rawResults = 
                searchRepository.keywordSearchItems(query, limit);
            
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
                            return null;
                        }
                    })
                    .filter(item -> item != null)
                    .toList();
        } catch (Exception e) {
            log.error("Keyword search items failed: {}", e.getMessage());
            return List.of();
        }
    }

    private List<SearchResultItem> keywordSearchShops(String query, int limit) {
        try {
            List<Map<String, Object>> rawResults = 
                searchRepository.keywordSearchShops(query, limit);
            
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
                            return null;
                        }
                    })
                    .filter(shop -> shop != null)
                    .toList();
        } catch (Exception e) {
            log.error("Keyword search shops failed: {}", e.getMessage());
            return List.of();
        }
    }

    private List<SearchResultItem> keywordSearchCategories(String query, int limit) {
        try {
            List<Map<String, Object>> rawResults = 
                searchRepository.keywordSearchCategories(query, limit);
            
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
                            return null;
                        }
                    })
                    .filter(category -> category != null)
                    .toList();
        } catch (Exception e) {
            log.error("Keyword search categories failed: {}", e.getMessage());
            return List.of();
        }
    }
}