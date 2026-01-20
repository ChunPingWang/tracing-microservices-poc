package com.example.weather.domain.entities;

import com.example.weather.domain.value_objects.CityCode;
import com.example.weather.domain.value_objects.Rainfall;
import com.example.weather.domain.value_objects.Temperature;

import java.util.Objects;

/**
 * Domain entity representing a city with weather baseline data.
 * Identity is defined by CityCode.
 */
public record City(
    CityCode code,
    String name,
    Temperature baseTemperature,
    Rainfall baseRainfall
) {
    public City {
        Objects.requireNonNull(code, "City code cannot be null");
        Objects.requireNonNull(name, "City name cannot be null");
        Objects.requireNonNull(baseTemperature, "Base temperature cannot be null");
        Objects.requireNonNull(baseRainfall, "Base rainfall cannot be null");

        if (name.isBlank()) {
            throw new IllegalArgumentException("City name cannot be empty");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        City city = (City) o;
        return Objects.equals(code, city.code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code);
    }
}
