package com.example.weather.infrastructure.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "otel.sdk.disabled=true")
@AutoConfigureMockMvc
class WeatherControllerContractTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getWeather_withValidCityCode_shouldReturn200WithWeatherData() throws Exception {
        mockMvc.perform(get("/weather/TPE")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.cityCode").value("TPE"))
            .andExpect(jsonPath("$.data.cityName").value("台北"))
            .andExpect(jsonPath("$.data.temperature").isNumber())
            .andExpect(jsonPath("$.data.rainfall").isNumber())
            .andExpect(jsonPath("$.data.updatedAt").exists())
            .andExpect(jsonPath("$.traceInfo.traceId").exists())
            .andExpect(jsonPath("$.traceInfo.spanId").exists())
            .andExpect(jsonPath("$.traceInfo.duration").isNumber());
    }

    @Test
    void getWeather_temperatureShouldBeWithinRange() throws Exception {
        mockMvc.perform(get("/weather/TPE"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.temperature", greaterThanOrEqualTo(15.0)))
            .andExpect(jsonPath("$.data.temperature", lessThanOrEqualTo(35.0)));
    }

    @Test
    void getWeather_rainfallShouldBeWithinRange() throws Exception {
        mockMvc.perform(get("/weather/TPE"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.rainfall", greaterThanOrEqualTo(0.0)))
            .andExpect(jsonPath("$.data.rainfall", lessThanOrEqualTo(50.0)));
    }

    @Test
    void getWeather_shouldIncludeTraceHeaders() throws Exception {
        mockMvc.perform(get("/weather/TPE"))
            .andExpect(status().isOk())
            .andExpect(header().exists("X-Trace-Id"))
            .andExpect(header().exists("X-Span-Id"))
            .andExpect(header().exists("X-Request-Duration"));
    }

    @Test
    void getWeather_traceIdShouldBe32HexCharacters() throws Exception {
        mockMvc.perform(get("/weather/TPE"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.traceInfo.traceId", matchesPattern("^[a-f0-9]{32}$")));
    }

    @Test
    void getWeather_spanIdShouldBe16HexCharacters() throws Exception {
        mockMvc.perform(get("/weather/TPE"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.traceInfo.spanId", matchesPattern("^[a-f0-9]{16}$")));
    }

    @Test
    void getWeather_withAllValidCities_shouldReturnCorrectCityNames() throws Exception {
        // Test TPE
        mockMvc.perform(get("/weather/TPE"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.cityName").value("台北"));

        // Test TXG
        mockMvc.perform(get("/weather/TXG"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.cityName").value("台中"));

        // Test KHH
        mockMvc.perform(get("/weather/KHH"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.cityName").value("高雄"));
    }

    @Test
    void getWeather_withInvalidCityCode_shouldReturn400() throws Exception {
        mockMvc.perform(get("/weather/XXX")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("INVALID_CITY_CODE"))
            .andExpect(jsonPath("$.traceInfo.traceId").exists());
    }
}
