package com.skrepta.skreptajava.location.service;

import com.skrepta.skreptajava.location.dto.CityResponse;
import com.skrepta.skreptajava.location.dto.CountryResponse;
import com.skrepta.skreptajava.location.dto.RegionResponse;
import com.skrepta.skreptajava.location.entity.City;
import com.skrepta.skreptajava.location.entity.Country;
import com.skrepta.skreptajava.location.entity.Region;
import com.skrepta.skreptajava.location.repository.CityRepository;
import com.skrepta.skreptajava.location.repository.CountryRepository;
import com.skrepta.skreptajava.location.repository.RegionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LocationService {

    private final CountryRepository countryRepository;
    private final RegionRepository regionRepository;
    private final CityRepository cityRepository;

    public List<CountryResponse> getCountries() {
        return countryRepository.findAll().stream()
                .map(this::toCountryResponse)
                .toList();
    }

    public List<RegionResponse> getRegions(Long countryId) {
        return regionRepository.findByCountryId(countryId).stream()
                .map(this::toRegionResponse)
                .toList();
    }

    public List<CityResponse> getCities(Long countryId, Long regionId) {
        List<City> cities;
        if (regionId != null) {
            cities = cityRepository.findByRegionId(regionId);
        } else if (countryId != null) {
            cities = cityRepository.findByCountryId(countryId);
        } else {
            cities = cityRepository.findAll();
        }
        return cities.stream().map(this::toCityResponse).toList();
    }

    private CountryResponse toCountryResponse(Country country) {
        return CountryResponse.builder()
                .id(country.getId())
                .code(country.getCode())
                .name(country.getName())
                .build();
    }

    private RegionResponse toRegionResponse(Region region) {
        return RegionResponse.builder()
                .id(region.getId())
                .countryId(region.getCountry().getId())
                .name(region.getName())
                .build();
    }

    public CityResponse toCityResponse(City city) {
        return CityResponse.builder()
                .id(city.getId())
                .countryId(city.getCountry().getId())
                .regionId(city.getRegion() != null ? city.getRegion().getId() : null)
                .name(city.getName())
                .build();
    }
}