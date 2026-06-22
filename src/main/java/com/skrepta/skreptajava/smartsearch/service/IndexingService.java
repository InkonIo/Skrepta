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
import org.springframework.transaction.annotation.Propagation;
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

    @Transactional
    public int indexAllItems() {
        log.info("Indexing all items...");
        List<Item> items = itemRepository.findAll();
        AtomicInteger count = new AtomicInteger(0);
        items.forEach(item -> {
            try {
                // Batch-режим: каждый элемент в своей транзакции,
                // чтобы ошибка одного не валила весь reindex.
                indexItemNewTx(item);
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

    @Transactional
    public int indexAllShops() {
        log.info("Indexing all shops...");
        List<Shop> shops = shopRepository.findAll();
        AtomicInteger count = new AtomicInteger(0);
        shops.forEach(shop -> {
            try {
                indexShopNewTx(shop);
                count.incrementAndGet();
            } catch (Exception e) {
                log.error("Failed to index shop {}: {}", shop.getId(), e.getMessage());
            }
        });
        log.info("Successfully indexed {} shops", count.get());
        return count.get();
    }

    @Transactional
    public int indexAllCategories() {
        log.info("Indexing all categories...");
        List<Category> categories = categoryRepository.findAll();
        AtomicInteger count = new AtomicInteger(0);
        categories.forEach(category -> {
            try {
                indexCategoryNewTx(category);
                count.incrementAndGet();
            } catch (Exception e) {
                log.error("Failed to index category {}: {}", category.getId(), e.getMessage());
            }
        });
        log.info("Successfully indexed {} categories", count.get());
        return count.get();
    }

    // ──────────────────────────────────────────────────────────────────
    // ОДИНОЧНАЯ ИНДЕКСАЦИЯ — вызывается сразу после create/update в той
    // же бизнес-транзакции. БЕЗ REQUIRES_NEW.
    //
    // Почему важно: при GenerationType.IDENTITY save() в родительской
    // транзакции (createItem) уже делает физический INSERT, чтобы
    // получить ID, но эта транзакция ещё не закоммичена. Если здесь
    // открыть REQUIRES_NEW, новая транзакция работает на отдельном
    // соединении и не видит незакоммиченную строку — Hibernate решает,
    // что сущность новая, и при merge() с IDENTITY делает ВТОРОЙ INSERT
    // с новым ID. Результат — дубль в БД с одинаковым content/created_at.
    //
    // Без REQUIRES_NEW indexItem просто участвует в той же транзакции
    // (Propagation.REQUIRED по умолчанию), видит ту же Hibernate Session,
    // и save() корректно обновляет embedding на УЖЕ существующей строке.
    // ──────────────────────────────────────────────────────────────────

    @Transactional
    public void indexItem(Item item) {
        doIndexItem(item);
    }

    @Transactional
    public void indexShop(Shop shop) {
        doIndexShop(shop);
    }

    @Transactional
    public void indexCategory(Category category) {
        doIndexCategory(category);
    }

    // ──────────────────────────────────────────────────────────────────
    // BATCH-ИНДЕКСАЦИЯ — каждый элемент в своей собственной транзакции.
    // Тут REQUIRES_NEW уместен: цикл идёт по уже сохранённым сущностям
    // (никакого parallel-insert риска), и нужно, чтобы сбой одного
    // элемента не откатывал остальные.
    // ──────────────────────────────────────────────────────────────────

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void indexItemNewTx(Item item) {
        doIndexItem(item);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void indexShopNewTx(Shop shop) {
        doIndexShop(shop);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void indexCategoryNewTx(Category category) {
        doIndexCategory(category);
    }

    // ──────────────────────────────────────────────────────────────────
    // Общая логика, без своей транзакционной аннотации —
    // выполняется в транзакции метода-вызывающего.
    // ──────────────────────────────────────────────────────────────────

    private void doIndexItem(Item item) {
        if (item == null) return;
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

    private void doIndexShop(Shop shop) {
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

    private void doIndexCategory(Category category) {
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

    @Transactional
    public void indexItemById(Long itemId) {
        itemRepository.findById(itemId).ifPresent(this::indexItem);
    }

    @Transactional
    public void indexShopById(Long shopId) {
        shopRepository.findById(shopId).ifPresent(this::indexShop);
    }

    @Transactional
    public void indexCategoryById(Long categoryId) {
        categoryRepository.findById(categoryId).ifPresent(this::indexCategory);
    }
}