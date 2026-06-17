package com.skrepta.skreptajava.item.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;
import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;
import java.util.List;

@Data
public class ItemRequest {
    @NotBlank(message = "Item title is required")
    private String title;

    private String description;

    private List<MultipartFile> imageFiles; // Для загрузки нескольких файлов

    @NotNull(message = "Category ID is required")
    private Long categoryId;

    private List<String> tags;

    @NotBlank(message = "City is required")
    private String city;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Price must be non-negative")
    private BigDecimal price;
}
