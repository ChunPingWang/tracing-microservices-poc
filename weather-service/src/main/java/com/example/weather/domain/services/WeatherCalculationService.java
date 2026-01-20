package com.example.weather.domain.services;

import com.example.weather.domain.entities.City;
import com.example.weather.domain.value_objects.Rainfall;
import com.example.weather.domain.value_objects.Temperature;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Domain service for calculating current weather with random variation.
 * Stateless service following DDD principles.
 */
@Service
public class WeatherCalculationService {

    private static final BigDecimal TEMP_VARIATION_RANGE = BigDecimal.valueOf(2.0);  // ±2°C
    private static final BigDecimal RAINFALL_VARIATION_RANGE = BigDecimal.valueOf(5.0);  // ±5mm

    private final SecureRandom random = new SecureRandom();

    /**
     * Calculate current weather for a city with random variation.
     * Per FR-010: Each query generates unique random variation.
     *
     * @param city the city to calculate weather for
     * @return weather data with applied variation
     */
    public WeatherData calculateCurrentWeather(City city) {
        Objects.requireNonNull(city, "City cannot be null");

        // Generate random variations
        BigDecimal tempVariation = generateVariation(TEMP_VARIATION_RANGE);
        BigDecimal rainfallVariation = generateVariation(RAINFALL_VARIATION_RANGE);

        // Apply variations (clamping is handled by value objects)
        Temperature currentTemp = city.baseTemperature().add(tempVariation);
        Rainfall currentRainfall = city.baseRainfall().add(rainfallVariation);

        return new WeatherData(
            city.code().value(),
            city.name(),
            currentTemp.value(),
            currentRainfall.value(),
            LocalDateTime.now()
        );
    }

    /**
     * Generate a random variation within ±range.
     */
    private BigDecimal generateVariation(BigDecimal range) {
        // Generate value between -range and +range
        double randomValue = (random.nextDouble() * 2 - 1) * range.doubleValue();
        return BigDecimal.valueOf(randomValue);
    }
}
