package com.example.weather.infrastructure.persistence;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * JPA entity for weather data persistence.
 * Maps to weather_data table in H2 database.
 */
@Entity
@Table(name = "weather_data")
public class WeatherDataEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "city_code", nullable = false, unique = true, length = 10)
    private String cityCode;

    @Column(name = "city_name", nullable = false, length = 50)
    private String cityName;

    @Column(name = "base_temperature", nullable = false, precision = 5, scale = 2)
    private BigDecimal baseTemperature;

    @Column(name = "base_rainfall", nullable = false, precision = 5, scale = 2)
    private BigDecimal baseRainfall;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCityCode() { return cityCode; }
    public void setCityCode(String cityCode) { this.cityCode = cityCode; }

    public String getCityName() { return cityName; }
    public void setCityName(String cityName) { this.cityName = cityName; }

    public BigDecimal getBaseTemperature() { return baseTemperature; }
    public void setBaseTemperature(BigDecimal baseTemperature) { this.baseTemperature = baseTemperature; }

    public BigDecimal getBaseRainfall() { return baseRainfall; }
    public void setBaseRainfall(BigDecimal baseRainfall) { this.baseRainfall = baseRainfall; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
