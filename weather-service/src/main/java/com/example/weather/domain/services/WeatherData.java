package com.example.weather.domain.services;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Domain service output representing calculated weather data.
 */
public record WeatherData(
    String cityCode,
    String cityName,
    BigDecimal temperature,
    BigDecimal rainfall,
    LocalDateTime updatedAt
) {
}
