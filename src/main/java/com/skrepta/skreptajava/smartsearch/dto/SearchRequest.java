// ============================================
// SearchRequest.java
// ============================================
package com.skrepta.skreptajava.smartsearch.dto;

import java.util.Map;

import lombok.Data;

@Data
public class SearchRequest {
    private String query;
    private Integer limit = 20; // По умолчанию 20 результатов
    private String type; // "ITEM", "SHOP", "CATEGORY", или null для всех типов
    private Long categoryId;
private String city;
private Map<String, String> attributes;
}