package com.skrepta.skreptajava.location.repository;

import com.skrepta.skreptajava.location.entity.City;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CityRepository extends JpaRepository<City, Long> {
    List<City> findByCountryId(Long countryId);
    List<City> findByRegionId(Long regionId);
    Optional<City> findByCountryIdAndNameIgnoreCase(Long countryId, String name);
}