package com.skrepta.skreptajava.smartsearch.repository;

import com.pgvector.PGvector;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Repository
@RequiredArgsConstructor
public class SearchRepository {

    private final EntityManager entityManager;

    /**
     * Поиск товаров по векторному сходству
     */
    public List<Map<String, Object>> searchItems(PGvector queryEmbedding, int limit) {
        String sql = """
            SELECT 
                i.id,
                i.title,
                1 - (i.embedding <=> CAST(:embedding AS vector)) AS score
            FROM items i
            WHERE i.embedding IS NOT NULL
              AND i.is_active = true
            ORDER BY i.embedding <=> CAST(:embedding AS vector)
            LIMIT :limit
            """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("embedding", queryEmbedding.toString());
        query.setParameter("limit", limit);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();
        
        List<Map<String, Object>> mappedResults = new ArrayList<>();
        for (Object[] row : results) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", ((Number) row[0]).longValue());
            map.put("title", (String) row[1]);
            map.put("score", ((Number) row[2]).doubleValue());
            mappedResults.add(map);
        }
        
        return mappedResults;
    }

    /**
     * Поиск магазинов по векторному сходству
     */
    public List<Map<String, Object>> searchShops(PGvector queryEmbedding, int limit) {
        String sql = """
            SELECT 
                s.id,
                s.name,
                1 - (s.embedding <=> CAST(:embedding AS vector)) AS score
            FROM shops s
            WHERE s.embedding IS NOT NULL
              AND s.is_approved = true
            ORDER BY s.embedding <=> CAST(:embedding AS vector)
            LIMIT :limit
            """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("embedding", queryEmbedding.toString());
        query.setParameter("limit", limit);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();
        
        List<Map<String, Object>> mappedResults = new ArrayList<>();
        for (Object[] row : results) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", ((Number) row[0]).longValue());
            map.put("title", (String) row[1]);
            map.put("score", ((Number) row[2]).doubleValue());
            mappedResults.add(map);
        }
        
        return mappedResults;
    }

    /**
     * Поиск категорий по векторному сходству
     */
    public List<Map<String, Object>> searchCategories(PGvector queryEmbedding, int limit) {
        String sql = """
            SELECT 
                c.id,
                c.name,
                1 - (c.embedding <=> CAST(:embedding AS vector)) AS score
            FROM categories c
            WHERE c.embedding IS NOT NULL
              AND c.is_active = true
            ORDER BY c.embedding <=> CAST(:embedding AS vector)
            LIMIT :limit
            """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("embedding", queryEmbedding.toString());
        query.setParameter("limit", limit);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();
        
        List<Map<String, Object>> mappedResults = new ArrayList<>();
        for (Object[] row : results) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", ((Number) row[0]).longValue());
            map.put("title", (String) row[1]);
            map.put("score", ((Number) row[2]).doubleValue());
            mappedResults.add(map);
        }
        
        return mappedResults;
    }
}