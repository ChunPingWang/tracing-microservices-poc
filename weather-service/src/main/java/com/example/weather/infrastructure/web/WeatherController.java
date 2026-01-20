package com.example.weather.infrastructure.web;

import com.example.weather.application.dto.ApiResponse;
import com.example.weather.application.dto.TraceInfo;
import com.example.weather.application.dto.WeatherResponse;
import com.example.weather.application.use_cases.GetWeatherUseCase;
import io.opentelemetry.api.trace.Span;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for weather endpoints.
 * Exposes GET /weather/{cityCode} endpoint per API contract.
 */
@RestController
@RequestMapping("/weather")
public class WeatherController {

    private static final Logger logger = LoggerFactory.getLogger(WeatherController.class);

    private final GetWeatherUseCase getWeatherUseCase;

    public WeatherController(GetWeatherUseCase getWeatherUseCase) {
        this.getWeatherUseCase = getWeatherUseCase;
    }

    /**
     * Get weather for a city.
     * FR-002: Returns weather data including city code, name, temperature, rainfall, timestamp.
     * FR-011-014: Includes trace information in response and headers.
     */
    @GetMapping("/{cityCode}")
    public ResponseEntity<ApiResponse<WeatherResponse>> getWeather(@PathVariable String cityCode) {
        long startTime = System.currentTimeMillis();

        logger.debug("Weather query received for city: {}", cityCode);

        // Execute use case
        WeatherResponse weatherResponse = getWeatherUseCase.execute(cityCode);

        // Calculate duration
        long duration = System.currentTimeMillis() - startTime;

        // Build trace info
        TraceInfo traceInfo = TraceInfo.fromCurrentSpan().withDuration(duration);

        // Add span attributes
        Span currentSpan = Span.current();
        currentSpan.setAttribute("cityCode", cityCode);
        currentSpan.setAttribute("duration_ms", duration);

        // Build API response
        ApiResponse<WeatherResponse> response = ApiResponse.success(weatherResponse, traceInfo);

        logger.debug("Weather query completed for city: {} in {}ms", cityCode, duration);

        // Return with trace headers
        return ResponseEntity.ok()
            .header("X-Trace-Id", traceInfo.traceId())
            .header("X-Span-Id", traceInfo.spanId())
            .header("X-Request-Duration", String.valueOf(duration))
            .body(response);
    }
}
