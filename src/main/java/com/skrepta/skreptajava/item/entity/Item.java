package com.skrepta.skreptajava.item.entity;

import com.skrepta.skreptajava.category.entity.Category;
import com.skrepta.skreptajava.config.VectorType;
import com.skrepta.skreptajava.shop.entity.Shop;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.fasterxml.jackson.databind.JsonNode;
import com.pgvector.PGvector;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.hibernate.annotations.Type;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "items")
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", nullable = false)
    private Shop shop;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private java.math.BigDecimal price;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @ElementCollection
    @CollectionTable(name = "item_images", joinColumns = @JoinColumn(name = "item_id"))
    @Column(name = "image_url")
    private List<String> images;

    @ElementCollection
    @CollectionTable(name = "item_tags", joinColumns = @JoinColumn(name = "item_id"))
    @Column(name = "tag")
    private List<String> tags;

    private String city;
    private boolean isActive = true;
    private int views = 0;
    private int favorites = 0;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant updatedAt;

    @Type(JsonType.class)
@Column(name = "attributes", columnDefinition = "jsonb")
private JsonNode attributes;

    @Type(VectorType.class)
    @Column(columnDefinition = "vector(1536)")
    private PGvector embedding;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Item)) return false;
        Item item = (Item) o;
        return id != null && Objects.equals(id, item.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}