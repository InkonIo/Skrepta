package com.skrepta.skreptajava.category.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CategoryRequest {
    @NotBlank(message = "Category name is required")
    private String name;

    private Long parentId;

    private String icon;

    @NotNull(message = "Position is required")
    private Integer position;

    private Boolean isActive = true;
}
