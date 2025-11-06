package com.skrepta.skreptajava.item.dto;

import com.skrepta.skreptajava.shop.dto.ShopResponse;
import lombok.Builder;
import lombok.Data;

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
    private String city;
    private boolean isActive;
    private int views;
    private int favorites;
    private Instant createdAt;
    private Instant updatedAt;
}
