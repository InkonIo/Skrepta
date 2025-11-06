package com.skrepta.skreptajava.shop.dto;

import com.skrepta.skreptajava.auth.dto.UserResponse;
import com.skrepta.skreptajava.category.dto.CategoryResponse;
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
    private String city;
    private String address;
    private double rating;
    private boolean isApproved;
    private Instant createdAt;
    private Set<CategoryResponse> categories;
}
