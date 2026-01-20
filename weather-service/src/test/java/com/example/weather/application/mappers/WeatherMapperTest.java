package com.example.weather.application.mappers;

import com.example.weather.application.dto.WeatherResponse;
import com.example.weather.domain.services.WeatherData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class WeatherMapperTest {

    private WeatherMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new WeatherMapper();
    }

    @Test
    void toResponse_withValidWeatherData_shouldMapCorrectly() {
        LocalDateTime now = LocalDateTime.now();
        WeatherData weatherData = new WeatherData(
            "TPE",
            "台北",
            BigDecimal.valueOf(26.5),
            BigDecimal.valueOf(12.3),
            now
        );

        WeatherResponse response = mapper.toResponse(weatherData);

        assertNotNull(response);
        assertEquals("TPE", response.cityCode());
        assertEquals("台北", response.cityName());
        assertEquals(BigDecimal.valueOf(26.5), response.temperature());
        assertEquals(BigDecimal.valueOf(12.3), response.rainfall());
        assertEquals(now, response.updatedAt());
    }

    @Test
    void toResponse_withNullWeatherData_shouldThrowException() {
        assertThrows(NullPointerException.class, () -> mapper.toResponse(null));
    }

    @Test
    void toResponse_shouldPreserveAllFields() {
        LocalDateTime timestamp = LocalDateTime.of(2026, 1, 21, 10, 30, 0);
        WeatherData data = new WeatherData(
            "KHH",
            "高雄",
            BigDecimal.valueOf(29.5),
            BigDecimal.valueOf(5.0),
            timestamp
        );

        WeatherResponse response = mapper.toResponse(data);

        assertEquals("KHH", response.cityCode());
        assertEquals("高雄", response.cityName());
        assertEquals(0, BigDecimal.valueOf(29.5).compareTo(response.temperature()));
        assertEquals(0, BigDecimal.valueOf(5.0).compareTo(response.rainfall()));
        assertEquals(timestamp, response.updatedAt());
    }
}
