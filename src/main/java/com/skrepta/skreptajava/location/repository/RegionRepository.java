package com.skrepta.skreptajava.location.repository;

import com.skrepta.skreptajava.location.entity.Region;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RegionRepository extends JpaRepository<Region, Long> {
    List<Region> findByCountryId(Long countryId);
}