package com.example.weather.domain.value_objects;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class RainfallTest {

    @Test
    void of_withValidRainfall_shouldCreateRainfall() {
        Rainfall rainfall = Rainfall.of(BigDecimal.valueOf(15.0));

        assertNotNull(rainfall);
        assertEquals(BigDecimal.valueOf(15.0), rainfall.value());
    }

    @Test
    void of_withMinimumRainfall_shouldCreateRainfall() {
        Rainfall rainfall = Rainfall.of(BigDecimal.valueOf(0.0));

        assertEquals(0, BigDecimal.ZERO.compareTo(rainfall.value()));
    }

    @Test
    void of_withMaximumRainfall_shouldCreateRainfall() {
        Rainfall rainfall = Rainfall.of(BigDecimal.valueOf(50.0));

        assertEquals(0, BigDecimal.valueOf(50.0).compareTo(rainfall.value()));
    }

    @ParameterizedTest
    @CsvSource({
        "-5.0, 0.0",   // Below minimum, should clamp to 0
        "55.0, 50.0",  // Above maximum, should clamp to 50
        "-0.1, 0.0",   // Just below minimum
        "50.1, 50.0"   // Just above maximum
    })
    void of_withOutOfRangeRainfall_shouldClampToValidRange(double input, double expected) {
        Rainfall rainfall = Rainfall.of(BigDecimal.valueOf(input));

        assertEquals(0, BigDecimal.valueOf(expected).compareTo(rainfall.value()));
    }

    @Test
    void add_withPositiveVariation_shouldAddAndClamp() {
        Rainfall base = Rainfall.of(BigDecimal.valueOf(48.0));
        Rainfall result = base.add(BigDecimal.valueOf(5.0));

        // 48 + 5 = 53, but clamped to 50
        assertEquals(0, BigDecimal.valueOf(50.0).compareTo(result.value()));
    }

    @Test
    void add_withNegativeVariation_shouldSubtractAndClamp() {
        Rainfall base = Rainfall.of(BigDecimal.valueOf(3.0));
        Rainfall result = base.add(BigDecimal.valueOf(-5.0));

        // 3 - 5 = -2, but clamped to 0
        assertEquals(0, BigDecimal.ZERO.compareTo(result.value()));
    }

    @Test
    void add_withinRange_shouldReturnExactValue() {
        Rainfall base = Rainfall.of(BigDecimal.valueOf(15.0));
        Rainfall result = base.add(BigDecimal.valueOf(5.0));

        assertEquals(0, BigDecimal.valueOf(20.0).compareTo(result.value()));
    }

    @Test
    void equals_withSameValue_shouldBeEqual() {
        Rainfall rain1 = Rainfall.of(BigDecimal.valueOf(15.0));
        Rainfall rain2 = Rainfall.of(BigDecimal.valueOf(15.0));

        assertEquals(rain1, rain2);
    }
}
