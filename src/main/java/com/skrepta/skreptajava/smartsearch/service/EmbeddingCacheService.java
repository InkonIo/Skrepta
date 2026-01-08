package com.skrepta.skreptajava.smartsearch.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.pgvector.PGvector;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;

@Slf4j
@Service
public class EmbeddingCacheService {

    // Кэш для embeddings (максимум 10,000 записей, живут 24 часа)
    private final Cache<String, PGvector> embeddingCache = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(Duration.ofHours(24))
            .recordStats() // Для мониторинга
            .build();

    /**
     * Получить embedding из кэша или сгенерировать новый
     */
    public PGvector getOrCompute(String text, EmbeddingGenerator generator) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }

        String cacheKey = generateCacheKey(text.trim().toLowerCase());
        
        PGvector cached = embeddingCache.getIfPresent(cacheKey);
        if (cached != null) {
            log.debug("Cache HIT for text: {}", text.substring(0, Math.min(50, text.length())));
            return cached;
        }

        log.debug("Cache MISS for text: {}", text.substring(0, Math.min(50, text.length())));
        PGvector embedding = generator.generate();
        
        if (embedding != null) {
            embeddingCache.put(cacheKey, embedding);
        }
        
        return embedding;
    }

    /**
     * Очистить кэш (для админа)
     */
    public void clearCache() {
        embeddingCache.invalidateAll();
        log.info("Embedding cache cleared");
    }

    /**
     * Получить статистику кэша
     */
    public CacheStats getStats() {
        var stats = embeddingCache.stats();
        return new CacheStats(
                embeddingCache.estimatedSize(),
                stats.hitRate(),
                stats.hitCount(),
                stats.missCount()
        );
    }

    /**
     * Генерируем MD5 хэш для ключа кэша
     */
    private String generateCacheKey(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            // Fallback на простой hashCode если MD5 недоступен
            return String.valueOf(text.hashCode());
        }
    }

    @FunctionalInterface
    public interface EmbeddingGenerator {
        PGvector generate();
    }

    public record CacheStats(
            long size,
            double hitRate,
            long hitCount,
            long missCount
    ) {}
}