package com.example.weather.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for WeatherDataEntity.
 */
@Repository
public interface JpaWeatherDataRepository extends JpaRepository<WeatherDataEntity, Long> {

    Optional<WeatherDataEntity> findByCityCode(String cityCode);
}
