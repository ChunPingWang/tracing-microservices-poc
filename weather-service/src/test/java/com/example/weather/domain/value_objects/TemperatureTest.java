package com.example.weather.domain.value_objects;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class TemperatureTest {

    @Test
    void of_withValidTemperature_shouldCreateTemperature() {
        Temperature temp = Temperature.of(BigDecimal.valueOf(25.0));

        assertNotNull(temp);
        assertEquals(BigDecimal.valueOf(25.0), temp.value());
    }

    @Test
    void of_withMinimumTemperature_shouldCreateTemperature() {
        Temperature temp = Temperature.of(BigDecimal.valueOf(15.0));

        assertEquals(BigDecimal.valueOf(15.0), temp.value());
    }

    @Test
    void of_withMaximumTemperature_shouldCreateTemperature() {
        Temperature temp = Temperature.of(BigDecimal.valueOf(35.0));

        assertEquals(BigDecimal.valueOf(35.0), temp.value());
    }

    @ParameterizedTest
    @CsvSource({
        "10.0, 15.0",  // Below minimum, should clamp to 15
        "40.0, 35.0",  // Above maximum, should clamp to 35
        "14.9, 15.0",  // Just below minimum
        "35.1, 35.0"   // Just above maximum
    })
    void of_withOutOfRangeTemperature_shouldClampToValidRange(double input, double expected) {
        Temperature temp = Temperature.of(BigDecimal.valueOf(input));

        assertEquals(0, BigDecimal.valueOf(expected).compareTo(temp.value()));
    }

    @Test
    void add_withPositiveVariation_shouldAddAndClamp() {
        Temperature base = Temperature.of(BigDecimal.valueOf(34.0));
        Temperature result = base.add(BigDecimal.valueOf(2.0));

        // 34 + 2 = 36, but clamped to 35
        assertEquals(0, BigDecimal.valueOf(35.0).compareTo(result.value()));
    }

    @Test
    void add_withNegativeVariation_shouldSubtractAndClamp() {
        Temperature base = Temperature.of(BigDecimal.valueOf(16.0));
        Temperature result = base.add(BigDecimal.valueOf(-2.0));

        // 16 - 2 = 14, but clamped to 15
        assertEquals(0, BigDecimal.valueOf(15.0).compareTo(result.value()));
    }

    @Test
    void add_withinRange_shouldReturnExactValue() {
        Temperature base = Temperature.of(BigDecimal.valueOf(25.0));
        Temperature result = base.add(BigDecimal.valueOf(2.0));

        assertEquals(0, BigDecimal.valueOf(27.0).compareTo(result.value()));
    }

    @Test
    void equals_withSameValue_shouldBeEqual() {
        Temperature temp1 = Temperature.of(BigDecimal.valueOf(25.0));
        Temperature temp2 = Temperature.of(BigDecimal.valueOf(25.0));

        assertEquals(temp1, temp2);
    }
}
