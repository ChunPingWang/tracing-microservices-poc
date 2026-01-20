package com.example.weather.application.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for weather data.
 */
public record WeatherResponse(
    String cityCode,
    String cityName,
    BigDecimal temperature,
    BigDecimal rainfall,
    LocalDateTime updatedAt
) {
}
