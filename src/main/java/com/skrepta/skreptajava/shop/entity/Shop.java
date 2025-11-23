package com.skrepta.skreptajava.shop.entity;

import com.skrepta.skreptajava.auth.entity.User;
import com.skrepta.skreptajava.category.entity.Category;
import com.skrepta.skreptajava.config.VectorType;
import com.skrepta.skreptajava.item.entity.Item;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.pgvector.PGvector;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.hibernate.annotations.Type;
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

    @OneToMany(mappedBy = "shop", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Item> items;

    @ManyToMany
    @JoinTable(
        name = "shop_categories",
        joinColumns = @JoinColumn(name = "shop_id"),
        inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    private Set<Category> categories = new HashSet<>();

    @Column(name = "favorites_count")
    private Integer favoritesCount = 0;
    
    // ✅ НОВОЕ: Поле для хранения вектора (embedding)
    @Type(VectorType.class)
@Column(columnDefinition = "vector(1536)")
private PGvector embedding;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Shop)) return false;
        Shop shop = (Shop) o;
        return id != null && Objects.equals(id, shop.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}