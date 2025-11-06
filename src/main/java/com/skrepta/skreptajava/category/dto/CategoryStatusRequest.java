package com.skrepta.skreptajava.category.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CategoryStatusRequest {
    @NotNull
    private Boolean isActive;
}
