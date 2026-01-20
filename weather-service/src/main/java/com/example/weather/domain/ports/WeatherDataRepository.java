package com.example.weather.domain.ports;

import com.example.weather.domain.entities.City;
import com.example.weather.domain.value_objects.CityCode;

import java.util.List;
import java.util.Optional;

/**
 * Port interface for weather data repository.
 * Implemented by infrastructure layer adapters.
 */
public interface WeatherDataRepository {

    /**
     * Find a city by its code.
     *
     * @param code the city code
     * @return the city if found, empty otherwise
     */
    Optional<City> findByCityCode(CityCode code);

    /**
     * Find all cities.
     *
     * @return list of all cities
     */
    List<City> findAll();
}
