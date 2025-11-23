package com.skrepta.skreptajava.smartsearch.service;

import com.pgvector.PGvector;
import com.skrepta.skreptajava.category.entity.Category;
import com.skrepta.skreptajava.category.repository.CategoryRepository;
import com.skrepta.skreptajava.item.entity.Item;
import com.skrepta.skreptajava.item.repository.ItemRepository;
import com.skrepta.skreptajava.shop.entity.Shop;
import com.skrepta.skreptajava.shop.repository.ShopRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class IndexingService {

    private final EmbeddingService embeddingService;
    private final ItemRepository itemRepository;
    private final ShopRepository shopRepository;
    private final CategoryRepository categoryRepository;

    /**
     * Индексирует все существующие данные (товары, магазины, категории)
     * ВНИМАНИЕ: Это долгая операция! Используйте для первоначальной индексации
     */
    @Transactional
    public void indexAllData() {
        log.info("Starting full reindexing...");
        
        long startTime = System.currentTimeMillis();
        
        int itemsIndexed = indexAllItems();
        int shopsIndexed = indexAllShops();
        int categoriesIndexed = indexAllCategories();
        
        long duration = System.currentTimeMillis() - startTime;
        
        log.info("Reindexing completed in {}ms. Items: {}, Shops: {}, Categories: {}", 
                duration, itemsIndexed, shopsIndexed, categoriesIndexed);
    }

    /**
     * Индексирует все товары
     */
    @Transactional
    public int indexAllItems() {
        log.info("Indexing all items...");
        List<Item> items = itemRepository.findAll();
        AtomicInteger count = new AtomicInteger(0);
        
        items.forEach(item -> {
            try {
                indexItem(item);
                count.incrementAndGet();
                
                if (count.get() % 10 == 0) {
                    log.info("Indexed {}/{} items", count.get(), items.size());
                }
            } catch (Exception e) {
                log.error("Failed to index item {}: {}", item.getId(), e.getMessage());
            }
        });
        
        log.info("Successfully indexed {} items", count.get());
        return count.get();
    }

    /**
     * Индексирует все магазины
     */
    @Transactional
    public int indexAllShops() {
        log.info("Indexing all shops...");
        List<Shop> shops = shopRepository.findAll();
        AtomicInteger count = new AtomicInteger(0);
        
        shops.forEach(shop -> {
            try {
                indexShop(shop);
                count.incrementAndGet();
            } catch (Exception e) {
                log.error("Failed to index shop {}: {}", shop.getId(), e.getMessage());
            }
        });
        
        log.info("Successfully indexed {} shops", count.get());
        return count.get();
    }

    /**
     * Индексирует все категории
     */
    @Transactional
    public int indexAllCategories() {
        log.info("Indexing all categories...");
        List<Category> categories = categoryRepository.findAll();
        AtomicInteger count = new AtomicInteger(0);
        
        categories.forEach(category -> {
            try {
                indexCategory(category);
                count.incrementAndGet();
            } catch (Exception e) {
                log.error("Failed to index category {}: {}", category.getId(), e.getMessage());
            }
        });
        
        log.info("Successfully indexed {} categories", count.get());
        return count.get();
    }

    /**
     * Индексирует конкретный товар
     */
    @Transactional
    public void indexItem(Item item) {
        if (item == null) return;
        
        // Получаем название категории из магазина
        String categoryName = item.getShop() != null && 
                             item.getShop().getCategories() != null && 
                             !item.getShop().getCategories().isEmpty()
                ? item.getShop().getCategories().iterator().next().getName()
                : null;

        String text = embeddingService.generateItemText(
                item.getTitle(),
                item.getDescription(),
                item.getTags(),
                categoryName
        );

        PGvector embedding = embeddingService.generateEmbedding(text);
        if (embedding != null) {
            item.setEmbedding(embedding);
            itemRepository.save(item);
            log.debug("Indexed item: {} (ID: {})", item.getTitle(), item.getId());
        }
    }

    /**
     * Индексирует конкретный магазин
     */
    @Transactional
    public void indexShop(Shop shop) {
        if (shop == null) return;
        
        String ownerName = shop.getOwner() != null ? shop.getOwner().getFio() : null;
        
        String text = embeddingService.generateShopText(
                shop.getName(),
                shop.getDescription(),
                ownerName
        );

        PGvector embedding = embeddingService.generateEmbedding(text);
        if (embedding != null) {
            shop.setEmbedding(embedding);
            shopRepository.save(shop);
            log.debug("Indexed shop: {} (ID: {})", shop.getName(), shop.getId());
        }
    }

    /**
     * Индексирует конкретную категорию
     */
    @Transactional
    public void indexCategory(Category category) {
        if (category == null) return;
        
        String text = embeddingService.generateCategoryText(
                category.getName(),
                category.getSlug()
        );

        PGvector embedding = embeddingService.generateEmbedding(text);
        if (embedding != null) {
            category.setEmbedding(embedding);
            categoryRepository.save(category);
            log.debug("Indexed category: {} (ID: {})", category.getName(), category.getId());
        }
    }

    /**
     * Индексирует товар по ID
     */
    @Transactional
    public void indexItemById(Long itemId) {
        itemRepository.findById(itemId).ifPresent(this::indexItem);
    }

    /**
     * Индексирует магазин по ID
     */
    @Transactional
    public void indexShopById(Long shopId) {
        shopRepository.findById(shopId).ifPresent(this::indexShop);
    }

    /**
     * Индексирует категорию по ID
     */
    @Transactional
    public void indexCategoryById(Long categoryId) {
        categoryRepository.findById(categoryId).ifPresent(this::indexCategory);
    }
}