package com.example.weather.application.mappers;

import com.example.weather.application.dto.WeatherResponse;
import com.example.weather.domain.services.WeatherData;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Mapper for transforming between domain and application layer objects.
 */
@Component
public class WeatherMapper {

    /**
     * Maps domain WeatherData to application WeatherResponse DTO.
     *
     * @param weatherData the domain weather data
     * @return the response DTO
     */
    public WeatherResponse toResponse(WeatherData weatherData) {
        Objects.requireNonNull(weatherData, "WeatherData cannot be null");

        return new WeatherResponse(
            weatherData.cityCode(),
            weatherData.cityName(),
            weatherData.temperature(),
            weatherData.rainfall(),
            weatherData.updatedAt()
        );
    }
}
