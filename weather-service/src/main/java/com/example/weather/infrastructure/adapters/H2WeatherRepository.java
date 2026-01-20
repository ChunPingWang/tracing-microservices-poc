package com.example.weather.infrastructure.adapters;

import com.example.weather.domain.entities.City;
import com.example.weather.domain.ports.WeatherDataRepository;
import com.example.weather.domain.value_objects.CityCode;
import com.example.weather.domain.value_objects.Rainfall;
import com.example.weather.domain.value_objects.Temperature;
import com.example.weather.infrastructure.persistence.JpaWeatherDataRepository;
import com.example.weather.infrastructure.persistence.WeatherDataEntity;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Adapter implementing WeatherDataRepository port using H2/JPA.
 * Maps between JPA entities and domain entities.
 */
@Repository
public class H2WeatherRepository implements WeatherDataRepository {

    private final JpaWeatherDataRepository jpaRepository;

    public H2WeatherRepository(JpaWeatherDataRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<City> findByCityCode(CityCode code) {
        return jpaRepository.findByCityCode(code.value())
            .map(this::toDomainEntity);
    }

    @Override
    public List<City> findAll() {
        return jpaRepository.findAll().stream()
            .map(this::toDomainEntity)
            .toList();
    }

    /**
     * Maps JPA entity to domain entity.
     */
    private City toDomainEntity(WeatherDataEntity entity) {
        return new City(
            CityCode.of(entity.getCityCode()),
            entity.getCityName(),
            Temperature.of(entity.getBaseTemperature()),
            Rainfall.of(entity.getBaseRainfall())
        );
    }
}
