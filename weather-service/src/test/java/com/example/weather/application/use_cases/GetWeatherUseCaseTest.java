package com.example.weather.application.use_cases;

import com.example.weather.application.dto.WeatherResponse;
import com.example.weather.application.mappers.WeatherMapper;
import com.example.weather.domain.entities.City;
import com.example.weather.domain.ports.WeatherDataRepository;
import com.example.weather.domain.services.WeatherCalculationService;
import com.example.weather.domain.services.WeatherData;
import com.example.weather.domain.value_objects.CityCode;
import com.example.weather.domain.value_objects.Rainfall;
import com.example.weather.domain.value_objects.Temperature;
import com.example.weather.infrastructure.web.GlobalExceptionHandler.CityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetWeatherUseCaseTest {

    @Mock
    private WeatherDataRepository repository;

    @Mock
    private WeatherCalculationService calculationService;

    private GetWeatherUseCase useCase;

    private City taipeiCity;

    @BeforeEach
    void setUp() {
        WeatherMapper mapper = new WeatherMapper();
        useCase = new GetWeatherUseCase(repository, calculationService, mapper);
        taipeiCity = new City(
            CityCode.of("TPE"),
            "台北",
            Temperature.of(BigDecimal.valueOf(25.0)),
            Rainfall.of(BigDecimal.valueOf(15.0))
        );
    }

    @Test
    void execute_withValidCityCode_shouldReturnWeatherResponse() {
        // Arrange
        when(repository.findByCityCode(any(CityCode.class))).thenReturn(Optional.of(taipeiCity));
        when(calculationService.calculateCurrentWeather(taipeiCity)).thenReturn(
            new WeatherData("TPE", "台北", BigDecimal.valueOf(26.5), BigDecimal.valueOf(12.3), LocalDateTime.now())
        );

        // Act
        WeatherResponse response = useCase.execute("TPE");

        // Assert
        assertNotNull(response);
        assertEquals("TPE", response.cityCode());
        assertEquals("台北", response.cityName());
        assertEquals(BigDecimal.valueOf(26.5), response.temperature());
        assertEquals(BigDecimal.valueOf(12.3), response.rainfall());

        verify(repository).findByCityCode(CityCode.of("TPE"));
        verify(calculationService).calculateCurrentWeather(taipeiCity);
    }

    @Test
    void execute_withInvalidCityCode_shouldThrowCityNotFoundException() {
        // Arrange
        when(repository.findByCityCode(any(CityCode.class))).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(CityNotFoundException.class, () -> useCase.execute("TPE"));
    }

    @Test
    void execute_shouldCallRepositoryAndCalculationService() {
        // Arrange
        when(repository.findByCityCode(any(CityCode.class))).thenReturn(Optional.of(taipeiCity));
        when(calculationService.calculateCurrentWeather(taipeiCity)).thenReturn(
            new WeatherData("TPE", "台北", BigDecimal.valueOf(25.0), BigDecimal.valueOf(15.0), LocalDateTime.now())
        );

        // Act
        useCase.execute("TPE");

        // Assert
        verify(repository, times(1)).findByCityCode(any(CityCode.class));
        verify(calculationService, times(1)).calculateCurrentWeather(taipeiCity);
    }
}
