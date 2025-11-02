package com.skrepta.skreptajava.category.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CategoryResponse {
    private Long id;
    private String name;
    private String slug;
    private Long parentId;
    private String icon;
    private Integer position;
    private Boolean isActive;
    private List<CategoryResponse> children;
}
