package com.skrepta.skreptajava.category.dto;

import com.skrepta.skreptajava.category.entity.Category;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.stream.Collectors;

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

    // ✅ этот метод ДОЛЖЕН быть внутри класса
    public static CategoryResponse fromEntity(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .parentId(category.getParent() != null ? category.getParent().getId() : null)
                .icon(category.getIcon())
                .position(category.getPosition())
                .isActive(category.getIsActive())
                .children(
                        category.getChildren() != null
                                ? category.getChildren().stream()
                                   .map(CategoryResponse::fromEntity)
                                   .collect(Collectors.toList())
                                : null
                )
                .build();
    }
}
