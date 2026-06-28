package com.skrepta.skreptajava.shop.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.skrepta.skreptajava.auth.dto.UserResponse;
import com.skrepta.skreptajava.category.dto.CategoryResponse;
import com.skrepta.skreptajava.location.dto.CityResponse;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Set;

@Data
@Builder
public class ShopResponse {
    private Long id;
    private UserResponse owner;
    private String name;
    private String description;
    private String logoUrl;
    private String phone;
    private String instagramLink;
    private CityResponse city;
    private String address;
    private double rating;
    @JsonProperty("isApproved")
    private boolean isApproved;
    private Instant createdAt;
    private Set<CategoryResponse> categories;
    private Integer favoritesCount;
}