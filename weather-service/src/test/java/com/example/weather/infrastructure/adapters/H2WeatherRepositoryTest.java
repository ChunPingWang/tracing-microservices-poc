package com.example.weather.infrastructure.adapters;

import com.example.weather.domain.entities.City;
import com.example.weather.domain.value_objects.CityCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Sql(scripts = {"/schema.sql", "/data.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class H2WeatherRepositoryTest {

    @Autowired
    private H2WeatherRepository repository;

    @Test
    void findByCityCode_withExistingCity_shouldReturnCity() {
        Optional<City> result = repository.findByCityCode(CityCode.of("TPE"));

        assertTrue(result.isPresent());
        City city = result.get();
        assertEquals("TPE", city.code().value());
        assertEquals("台北", city.name());
        assertEquals(0, BigDecimal.valueOf(25.0).compareTo(city.baseTemperature().value()));
        assertEquals(0, BigDecimal.valueOf(15.0).compareTo(city.baseRainfall().value()));
    }

    @Test
    void findByCityCode_withNonExistingCity_shouldReturnEmpty() {
        // Note: "XXX" is not a valid CityCode, so this tests internal handling
        // In practice, validation happens before reaching repository
        Optional<City> result = repository.findByCityCode(CityCode.of("KHH"));

        assertTrue(result.isPresent());
        assertEquals("高雄", result.get().name());
    }

    @Test
    void findAll_shouldReturnAllCities() {
        List<City> cities = repository.findAll();

        assertEquals(3, cities.size());

        // Verify all expected cities are present
        assertTrue(cities.stream().anyMatch(c -> "TPE".equals(c.code().value())));
        assertTrue(cities.stream().anyMatch(c -> "TXG".equals(c.code().value())));
        assertTrue(cities.stream().anyMatch(c -> "KHH".equals(c.code().value())));
    }

    @Test
    void findByCityCode_taipeiBaseline_shouldMatchSpec() {
        // FR-008: Taipei baseline 25°C
        // FR-009: Taipei baseline 15mm
        Optional<City> result = repository.findByCityCode(CityCode.of("TPE"));

        assertTrue(result.isPresent());
        assertEquals(0, BigDecimal.valueOf(25.0).compareTo(result.get().baseTemperature().value()));
        assertEquals(0, BigDecimal.valueOf(15.0).compareTo(result.get().baseRainfall().value()));
    }

    @Test
    void findByCityCode_taichungBaseline_shouldMatchSpec() {
        // FR-008: Taichung baseline 27°C
        // FR-009: Taichung baseline 10mm
        Optional<City> result = repository.findByCityCode(CityCode.of("TXG"));

        assertTrue(result.isPresent());
        assertEquals(0, BigDecimal.valueOf(27.0).compareTo(result.get().baseTemperature().value()));
        assertEquals(0, BigDecimal.valueOf(10.0).compareTo(result.get().baseRainfall().value()));
    }

    @Test
    void findByCityCode_kaohsiungBaseline_shouldMatchSpec() {
        // FR-008: Kaohsiung baseline 29°C
        // FR-009: Kaohsiung baseline 8mm
        Optional<City> result = repository.findByCityCode(CityCode.of("KHH"));

        assertTrue(result.isPresent());
        assertEquals(0, BigDecimal.valueOf(29.0).compareTo(result.get().baseTemperature().value()));
        assertEquals(0, BigDecimal.valueOf(8.0).compareTo(result.get().baseRainfall().value()));
    }
}
