package com.skrepta.skreptajava.shop.entity;

import com.skrepta.skreptajava.auth.entity.User;
import com.skrepta.skreptajava.category.entity.Category;
import com.skrepta.skreptajava.item.entity.Item;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "shops")
public class Shop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_uid", nullable = false)
    private User owner;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String logoUrl;
    private String phone;
    private String instagramLink;
    private String city;
    private String address;
    private double rating = 0.0;
    private boolean isApproved = false;

    @Column(nullable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "shop", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Item> items;

    @ManyToMany
    @JoinTable(
        name = "shop_categories",
        joinColumns = @JoinColumn(name = "shop_id"),
        inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    private Set<Category> categories = new HashSet<>();
}
