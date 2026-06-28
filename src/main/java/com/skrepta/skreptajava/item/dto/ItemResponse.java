package com.skrepta.skreptajava.item.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.skrepta.skreptajava.location.dto.CityResponse;
import com.skrepta.skreptajava.shop.dto.ShopResponse;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

import java.time.Instant;
import java.util.List;

@Data
@Builder
public class ItemResponse {
    private Long id;
    private ShopResponse shop;
    private String title;
    private String description;
    private List<String> images;
    private List<String> tags;
    private CityResponse city;
    private boolean isActive;
    private int views;
    private int favorites;
    private Instant createdAt;
    private Instant updatedAt;
    private BigDecimal price;
    private Long categoryId;
    private String categoryName;
    private JsonNode attributes;
}