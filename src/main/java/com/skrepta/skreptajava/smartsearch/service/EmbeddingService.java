package com.skrepta.skreptajava.smartsearch.service;

import com.google.common.util.concurrent.RateLimiter;
import com.pgvector.PGvector;
import com.theokanning.openai.embedding.EmbeddingRequest;
import com.theokanning.openai.service.OpenAiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Slf4j
@Service
public class EmbeddingService {

    private final OpenAiService openAiService;
    private final EmbeddingCacheService cacheService; 
    private final RateLimiter rateLimiter; 
    
    private static final String MODEL = "text-embedding-3-large";
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 1000;

    
    public EmbeddingService(
            @Value("${openai.api.key}") String apiKey,
            EmbeddingCacheService cacheService
    ) {
        this.openAiService = new OpenAiService(apiKey, Duration.ofSeconds(30));
        this.cacheService = cacheService;
        
        this.rateLimiter = RateLimiter.create(50.0);
        
        log.info("EmbeddingService initialized with model: {} and rate limit: 50 req/s", MODEL);
    }

    
    public PGvector generateEmbedding(String text) {
        if (text == null || text.trim().isEmpty()) {
            log.warn("Attempted to generate embedding for empty text");
            return null;
        }

        return cacheService.getOrCompute(text, () -> generateEmbeddingInternal(text));
    }

    
    private PGvector generateEmbeddingInternal(String text) {
        String cleanText = text.trim().substring(0, Math.min(text.length(), 8000));

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                rateLimiter.acquire();
                
                EmbeddingRequest request = EmbeddingRequest.builder()
                        .model(MODEL)
                        .input(List.of(cleanText))
                        .build();

                List<Double> embedding = openAiService.createEmbeddings(request)
                        .getData()
                        .get(0)
                        .getEmbedding();

                float[] embeddingArray = new float[embedding.size()];
                for (int i = 0; i < embedding.size(); i++) {
                    embeddingArray[i] = embedding.get(i).floatValue();
                }

                log.debug("Successfully generated embedding for text (length: {})", cleanText.length());
                return new PGvector(embeddingArray);

            } catch (Exception e) {
                log.error("Error generating embedding (attempt {}/{}): {}", attempt, MAX_RETRIES, e.getMessage());
                
                if (attempt < MAX_RETRIES) {
                    try {
                        Thread.sleep(RETRY_DELAY_MS * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                } else {
                    log.error("Failed to generate embedding after {} attempts", MAX_RETRIES);
                    throw new RuntimeException("Failed to generate embedding: " + e.getMessage(), e);
                }
            }
        }

        return null;
    }

    
    public String generateItemText(String title, String description, List<String> tags, String categoryName) {
        StringBuilder sb = new StringBuilder();
        
        if (title != null) {
            sb.append(title).append(". ");
            sb.append(title).append(". "); 
        }
        
        if (categoryName != null) {
            sb.append("Категория: ").append(categoryName).append(". ");
        }
        
        if (tags != null && !tags.isEmpty()) {
            sb.append("Теги: ").append(String.join(", ", tags)).append(". ");
            tags.forEach(tag -> sb.append(tag).append(". "));
        }
        
        if (description != null) {
            // Обрезаем длинные описания
            String shortDesc = description.length() > 500 
                ? description.substring(0, 500) + "..." 
                : description;
            sb.append(shortDesc);
        }
        
        return sb.toString().trim();
    }

    
    public String generateShopText(String name, String description, String ownerName) {
        StringBuilder sb = new StringBuilder();
        
        if (name != null) {
            sb.append(name).append(". ");
            sb.append(name).append(". "); // Дубль для веса
        }
        
        if (description != null) sb.append(description).append(". ");
        if (ownerName != null) sb.append("Владелец: ").append(ownerName);
        
        return sb.toString().trim();
    }

    /**
     * Генерирует текстовый блок для категории
     */
    public String generateCategoryText(String name, String slug) {
        StringBuilder sb = new StringBuilder();
        
        if (name != null) {
            sb.append(name).append(". ");
            sb.append(name).append(". "); // Дубль для веса
        }
        if (slug != null) sb.append("Тип: ").append(slug);
        
        return sb.toString().trim();
    }
}