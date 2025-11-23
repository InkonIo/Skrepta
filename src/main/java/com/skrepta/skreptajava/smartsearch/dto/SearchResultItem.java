// ============================================
// SearchResultItem.java
// ============================================
package com.skrepta.skreptajava.smartsearch.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SearchResultItem {
    private String type; // "ITEM", "SHOP", "CATEGORY"
    private Long id;
    private String title;
    private Double score; // Оценка релевантности (0.0 - 1.0)
    private Object data; // Полные данные объекта (ItemResponse/ShopResponse/CategoryResponse)
}