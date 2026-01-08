package com.skrepta.skreptajava.smartsearch.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResponse {
    
    /**
     * Оригинальный поисковый запрос
     */
    private String query;
    
    /**
     * Общее количество найденных результатов
     */
    private Integer totalResults;
    
    /**
     * Список результатов
     */
    private List<SearchResultItem> results;
    
    /**
     * Флаг: использовался ли fallback (keyword search)
     * true = OpenAI был недоступен, использовался простой поиск
     * false = использовался AI semantic search
     */
    @Builder.Default
    private Boolean isFallback = false;
    
    /**
     * Сообщение для пользователя (опционально)
     */
    private String message;
}