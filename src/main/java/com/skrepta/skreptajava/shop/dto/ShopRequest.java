package com.skrepta.skreptajava.shop.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

@Data
public class ShopRequest {
    @NotBlank(message = "Shop name is required")
    private String name;

    private String description;

    private MultipartFile logoFile;

    @NotBlank(message = "Phone number is required")
    private String phone;

    private String instagram;

    @NotBlank(message = "City is required")
    private String city;

    private String address;

    @NotNull(message = "Category IDs are required")
    private Set<Long> categoryIds;

    // Опциональное поле для админа: указать владельца магазина
    private Long ownerId;
}