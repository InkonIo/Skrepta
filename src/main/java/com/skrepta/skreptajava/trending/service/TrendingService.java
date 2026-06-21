package com.skrepta.skreptajava.trending.service;

import com.skrepta.skreptajava.item.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TrendingService {

    private final ItemRepository itemRepository;

    public List<String> getTrendingTags(int limit) {
        return itemRepository.findAll().stream()
            .filter(item -> item.isActive() && item.getCategory() != null)
            .sorted((a, b) -> Integer.compare(b.getViews(), a.getViews()))
            .map(item -> item.getCategory().getName())
            .distinct()
            .limit(limit)
            .collect(Collectors.toList());
    }
}