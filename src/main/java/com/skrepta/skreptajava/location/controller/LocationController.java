package com.skrepta.skreptajava.location.controller;

import com.skrepta.skreptajava.location.dto.CityResponse;
import com.skrepta.skreptajava.location.dto.CountryResponse;
import com.skrepta.skreptajava.location.dto.RegionResponse;
import com.skrepta.skreptajava.location.service.LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/locations")
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;

    @GetMapping("/countries")
    public List<CountryResponse> getCountries() {
        return locationService.getCountries();
    }

    @GetMapping("/regions")
    public List<RegionResponse> getRegions(@RequestParam Long countryId) {
        return locationService.getRegions(countryId);
    }

    @GetMapping("/cities")
    public List<CityResponse> getCities(
            @RequestParam(required = false) Long countryId,
            @RequestParam(required = false) Long regionId
    ) {
        return locationService.getCities(countryId, regionId);
    }
}