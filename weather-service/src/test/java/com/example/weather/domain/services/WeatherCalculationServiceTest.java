package com.example.weather.domain.services;

import com.example.weather.domain.entities.City;
import com.example.weather.domain.value_objects.CityCode;
import com.example.weather.domain.value_objects.Rainfall;
import com.example.weather.domain.value_objects.Temperature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class WeatherCalculationServiceTest {

    private WeatherCalculationService service;
    private City taipeiCity;

    @BeforeEach
    void setUp() {
        service = new WeatherCalculationService();
        taipeiCity = new City(
            CityCode.of("TPE"),
            "台北",
            Temperature.of(BigDecimal.valueOf(25.0)),
            Rainfall.of(BigDecimal.valueOf(15.0))
        );
    }

    @Test
    void calculateCurrentWeather_shouldReturnWeatherData() {
        WeatherData result = service.calculateCurrentWeather(taipeiCity);

        assertNotNull(result);
        assertEquals("TPE", result.cityCode());
        assertEquals("台北", result.cityName());
        assertNotNull(result.temperature());
        assertNotNull(result.rainfall());
        assertNotNull(result.updatedAt());
    }

    @Test
    void calculateCurrentWeather_temperatureShouldBeWithinRange() {
        WeatherData result = service.calculateCurrentWeather(taipeiCity);

        // Temperature should be between 15 and 35
        assertTrue(result.temperature().compareTo(BigDecimal.valueOf(15.0)) >= 0);
        assertTrue(result.temperature().compareTo(BigDecimal.valueOf(35.0)) <= 0);
    }

    @Test
    void calculateCurrentWeather_rainfallShouldBeWithinRange() {
        WeatherData result = service.calculateCurrentWeather(taipeiCity);

        // Rainfall should be between 0 and 50
        assertTrue(result.rainfall().compareTo(BigDecimal.ZERO) >= 0);
        assertTrue(result.rainfall().compareTo(BigDecimal.valueOf(50.0)) <= 0);
    }

    @Test
    void calculateCurrentWeather_temperatureShouldBeNearBaseline() {
        WeatherData result = service.calculateCurrentWeather(taipeiCity);

        // Temperature should be within ±2 of baseline (25), so 23-27
        // But after clamping, still within 15-35
        BigDecimal temp = result.temperature();
        assertTrue(temp.compareTo(BigDecimal.valueOf(23.0)) >= 0);
        assertTrue(temp.compareTo(BigDecimal.valueOf(27.0)) <= 0);
    }

    @Test
    void calculateCurrentWeather_rainfallShouldBeNearBaseline() {
        WeatherData result = service.calculateCurrentWeather(taipeiCity);

        // Rainfall should be within ±5 of baseline (15), so 10-20
        BigDecimal rainfall = result.rainfall();
        assertTrue(rainfall.compareTo(BigDecimal.valueOf(10.0)) >= 0);
        assertTrue(rainfall.compareTo(BigDecimal.valueOf(20.0)) <= 0);
    }

    @RepeatedTest(10)
    void calculateCurrentWeather_shouldProduceDifferentValues() {
        // FR-010: Each query should produce different values
        Set<String> uniqueResults = new HashSet<>();

        for (int i = 0; i < 5; i++) {
            WeatherData result = service.calculateCurrentWeather(taipeiCity);
            String key = result.temperature() + "-" + result.rainfall();
            uniqueResults.add(key);
        }

        // At least some variation should occur (statistically very likely with random)
        // Allow for rare case of same value appearing twice
        assertTrue(uniqueResults.size() >= 2,
            "Expected at least 2 unique results from 5 queries, but got " + uniqueResults.size());
    }

    @Test
    void calculateCurrentWeather_withNullCity_shouldThrowException() {
        assertThrows(NullPointerException.class, () -> service.calculateCurrentWeather(null));
    }
}
