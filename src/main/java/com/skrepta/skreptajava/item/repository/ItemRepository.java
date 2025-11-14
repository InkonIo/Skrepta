package com.skrepta.skreptajava.item.repository;

import com.skrepta.skreptajava.item.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {
    // ✅ Получить все товары магазина
    List<Item> findByShopId(Long shopId);
    
    // ✅ Получить только активные товары магазина (альтернатива)
    List<Item> findByShopIdAndIsActive(Long shopId, boolean isActive);
}