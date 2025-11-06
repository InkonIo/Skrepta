package com.skrepta.skreptajava.shop.repository;

import com.skrepta.skreptajava.shop.entity.Shop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShopRepository extends JpaRepository<Shop, Long> {
    List<Shop> findByOwnerId(Long ownerId);
}
