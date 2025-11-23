// ============================================
// SearchResponse.java
// ============================================
package com.skrepta.skreptajava.smartsearch.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SearchResponse {
    private String query;
    private Integer totalResults;
    private List<SearchResultItem> results;
}