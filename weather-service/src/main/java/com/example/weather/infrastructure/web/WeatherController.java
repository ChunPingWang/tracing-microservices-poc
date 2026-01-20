package com.example.weather.infrastructure.web;

import com.example.weather.application.dto.ApiResponse;
import com.example.weather.application.dto.TraceInfo;
import com.example.weather.application.dto.WeatherResponse;
import com.example.weather.application.use_cases.GetWeatherUseCase;
import io.opentelemetry.api.trace.Span;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Weather", description = "天氣查詢 API - 提供台灣主要城市天氣資訊")
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
    @Operation(
            summary = "查詢城市天氣",
            description = """
                    根據城市代碼查詢即時天氣資訊。

                    **支援的城市代碼：**
                    - `TPE`: 台北市 (Taipei)
                    - `TXG`: 台中市 (Taichung)
                    - `KHH`: 高雄市 (Kaohsiung)

                    **回應標頭包含追蹤資訊：**
                    - `X-Trace-Id`: 分散式追蹤識別碼 (32字元)
                    - `X-Span-Id`: 當前 Span 識別碼 (16字元)
                    - `X-Request-Duration`: 請求處理時間 (毫秒)
                    """)
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "查詢成功",
                    headers = {
                            @Header(name = "X-Trace-Id", description = "追蹤識別碼", schema = @Schema(type = "string", example = "4bf92f3577b34da6a3ce929d0e0e4736")),
                            @Header(name = "X-Span-Id", description = "Span 識別碼", schema = @Schema(type = "string", example = "00f067aa0ba902b7")),
                            @Header(name = "X-Request-Duration", description = "處理時間(ms)", schema = @Schema(type = "integer", example = "45"))
                    },
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": true,
                                      "data": {
                                        "cityCode": "TPE",
                                        "cityName": "台北市",
                                        "temperature": 25.5,
                                        "rainfall": 2.3,
                                        "updatedAt": "2024-01-15T14:30:00"
                                      },
                                      "traceInfo": {
                                        "traceId": "4bf92f3577b34da6a3ce929d0e0e4736",
                                        "spanId": "00f067aa0ba902b7",
                                        "duration": 45
                                      }
                                    }
                                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "無效的城市代碼",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    {
                                      "success": false,
                                      "error": {
                                        "code": "INVALID_CITY_CODE",
                                        "message": "Invalid city code: ABC"
                                      }
                                    }
                                    """)))
    })
    @GetMapping("/{cityCode}")
    public ResponseEntity<ApiResponse<WeatherResponse>> getWeather(
            @Parameter(
                    description = "城市代碼 (TPE/TXG/KHH)",
                    required = true,
                    example = "TPE",
                    schema = @Schema(allowableValues = {"TPE", "TXG", "KHH"}))
            @PathVariable String cityCode) {
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
