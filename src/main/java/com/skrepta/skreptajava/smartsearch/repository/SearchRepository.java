package com.skrepta.skreptajava.smartsearch.repository;

import com.pgvector.PGvector;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.jdbc.core.JdbcTemplate;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class SearchRepository {

    private final JdbcTemplate jdbcTemplate;

    // ============================================
    // SEMANTIC SEARCH (с векторами)
    // ============================================

    /**
     * Векторный поиск по товарам
     */
    public List<Map<String, Object>> searchItems(PGvector embedding, int limit,
        Long categoryId, String city, Map<String, String> attributes) {

    StringBuilder sql = new StringBuilder("""
        SELECT
            i.id,
            i.title,
            1 - (i.embedding <=> CAST(? AS vector)) AS score
        FROM items i
        WHERE i.embedding IS NOT NULL
          AND i.is_active = true
        """);

    List<Object> params = new java.util.ArrayList<>();
    String embeddingStr = embedding.toString();
    params.add(embeddingStr);

    if (categoryId != null) {
        sql.append(" AND i.category_id = ?");
        params.add(categoryId);
    }
    if (city != null && !city.isBlank()) {
        sql.append(" AND i.city = ?");
        params.add(city);
    }
    if (attributes != null && !attributes.isEmpty()) {
        sql.append(" AND i.attributes @> CAST(? AS jsonb)");
        params.add(new com.fasterxml.jackson.databind.ObjectMapper()
            .valueToTree(attributes).toString());
    }

    sql.append(" ORDER BY i.embedding <=> CAST(? AS vector) LIMIT ?");
    params.add(embeddingStr);
    params.add(limit);

    return jdbcTemplate.queryForList(sql.toString(), params.toArray());
}

    /**
     * Векторный поиск по магазинам
     */
    public List<Map<String, Object>> searchShops(PGvector embedding, int limit) {
        String sql = """
            SELECT
                s.id,
                s.name as title,
                1 - (s.embedding <=> CAST(? AS vector)) AS score
            FROM shops s
            WHERE s.embedding IS NOT NULL
              AND s.is_approved = true
            ORDER BY s.embedding <=> CAST(? AS vector)
            LIMIT ?
            """;
        
        String embeddingStr = embedding.toString();
        return jdbcTemplate.queryForList(sql, embeddingStr, embeddingStr, limit);
    }

    /**
     * Векторный поиск по категориям
     */
    public List<Map<String, Object>> searchCategories(PGvector embedding, int limit) {
        String sql = """
            SELECT
                c.id,
                c.name as title,
                1 - (c.embedding <=> CAST(? AS vector)) AS score
            FROM categories c
            WHERE c.embedding IS NOT NULL
              AND c.is_active = true
            ORDER BY c.embedding <=> CAST(? AS vector)
            LIMIT ?
            """;
        
        String embeddingStr = embedding.toString();
        return jdbcTemplate.queryForList(sql, embeddingStr, embeddingStr, limit);
    }

    // ============================================
    // FALLBACK: KEYWORD SEARCH (без векторов)
    // ============================================

    /**
     * Текстовый поиск по товарам (FALLBACK)
     * Использует простой ILIKE поиск
     */
    public List<Map<String, Object>> keywordSearchItems(String query, int limit,
        Long categoryId, String city, Map<String, String> attributes) {

    StringBuilder sql = new StringBuilder("""
        SELECT 
            i.id,
            i.title,
            0.6 as score,
            'KEYWORD_MATCH' as match_type
        FROM items i
        WHERE i.is_active = true
          AND (
              LOWER(i.title) LIKE LOWER(?)
              OR LOWER(i.description) LIKE LOWER(?)
          )
        """);

    String likePattern = "%" + query + "%";
    String startsWithPattern = query + "%";
    List<Object> params = new java.util.ArrayList<>();
    params.add(likePattern);
    params.add(likePattern);

    if (categoryId != null) {
        sql.append(" AND i.category_id = ?");
        params.add(categoryId);
    }
    if (city != null && !city.isBlank()) {
        sql.append(" AND i.city = ?");
        params.add(city);
    }
    if (attributes != null && !attributes.isEmpty()) {
        sql.append(" AND i.attributes @> CAST(? AS jsonb)");
        params.add(new com.fasterxml.jackson.databind.ObjectMapper()
            .valueToTree(attributes).toString());
    }

    sql.append("""
         ORDER BY 
            CASE 
                WHEN LOWER(i.title) = LOWER(?) THEN 1
                WHEN LOWER(i.title) LIKE LOWER(?) THEN 2
                ELSE 3 
            END,
            i.created_at DESC
        LIMIT ?
        """);
    params.add(query);
    params.add(startsWithPattern);
    params.add(limit);

    return jdbcTemplate.queryForList(sql.toString(), params.toArray());
}

    /**
     * Текстовый поиск по магазинам (FALLBACK)
     */
    public List<Map<String, Object>> keywordSearchShops(String query, int limit) {
        String sql = """
            SELECT 
                s.id,
                s.name as title,
                0.6 as score,
                'KEYWORD_MATCH' as match_type
            FROM shops s
            WHERE s.is_approved = true
              AND (
                  LOWER(s.name) LIKE LOWER(?) 
                  OR LOWER(s.description) LIKE LOWER(?)
              )
            ORDER BY 
                CASE 
                    WHEN LOWER(s.name) = LOWER(?) THEN 1
                    WHEN LOWER(s.name) LIKE LOWER(?) THEN 2
                    ELSE 3 
                END,
                s.created_at DESC
            LIMIT ?
            """;
        
        String likePattern = "%" + query + "%";
        String startsWithPattern = query + "%";
        
        return jdbcTemplate.queryForList(sql, 
            likePattern, likePattern, 
            query, startsWithPattern, 
            limit);
    }

    /**
     * Текстовый поиск по категориям (FALLBACK)
     */
    public List<Map<String, Object>> keywordSearchCategories(String query, int limit) {
        String sql = """
            SELECT 
                c.id,
                c.name as title,
                0.6 as score,
                'KEYWORD_MATCH' as match_type
            FROM categories c
            WHERE c.is_active = true
              AND LOWER(c.name) LIKE LOWER(?)
            ORDER BY 
                CASE 
                    WHEN LOWER(c.name) = LOWER(?) THEN 1
                    WHEN LOWER(c.name) LIKE LOWER(?) THEN 2
                    ELSE 3 
                END
            LIMIT ?
            """;
        
        String likePattern = "%" + query + "%";
        String startsWithPattern = query + "%";
        
        return jdbcTemplate.queryForList(sql, 
            likePattern, 
            query, startsWithPattern, 
            limit);
    }
}