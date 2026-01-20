package com.example.weather.application.use_cases;

import com.example.weather.application.dto.WeatherResponse;
import com.example.weather.application.mappers.WeatherMapper;
import com.example.weather.domain.entities.City;
import com.example.weather.domain.ports.WeatherDataRepository;
import com.example.weather.domain.services.WeatherCalculationService;
import com.example.weather.domain.services.WeatherData;
import com.example.weather.domain.value_objects.CityCode;
import com.example.weather.infrastructure.web.GlobalExceptionHandler.CityNotFoundException;
import com.example.weather.infrastructure.web.GlobalExceptionHandler.InvalidCityCodeException;
import org.springframework.stereotype.Service;

/**
 * Use case for getting weather data for a city.
 * Orchestrates domain services and repository access.
 */
@Service
public class GetWeatherUseCase {

    private final WeatherDataRepository repository;
    private final WeatherCalculationService calculationService;
    private final WeatherMapper mapper;

    public GetWeatherUseCase(
            WeatherDataRepository repository,
            WeatherCalculationService calculationService,
            WeatherMapper mapper) {
        this.repository = repository;
        this.calculationService = calculationService;
        this.mapper = mapper;
    }

    // Constructor for testing without mapper
    public GetWeatherUseCase(
            WeatherDataRepository repository,
            WeatherCalculationService calculationService) {
        this.repository = repository;
        this.calculationService = calculationService;
        this.mapper = new WeatherMapper();
    }

    /**
     * Execute the use case to get weather for a city.
     *
     * @param cityCodeStr the city code string
     * @return the weather response
     * @throws CityNotFoundException if city not found
     * @throws InvalidCityCodeException if city code is invalid
     */
    public WeatherResponse execute(String cityCodeStr) {
        // Validate city code
        if (!CityCode.isValid(cityCodeStr)) {
            throw new InvalidCityCodeException(cityCodeStr);
        }

        CityCode cityCode = CityCode.of(cityCodeStr);

        // Fetch city from repository
        City city = repository.findByCityCode(cityCode)
            .orElseThrow(() -> new CityNotFoundException(cityCodeStr));

        // Calculate current weather with random variation
        WeatherData weatherData = calculationService.calculateCurrentWeather(city);

        // Map to response DTO
        return mapper.toResponse(weatherData);
    }
}
