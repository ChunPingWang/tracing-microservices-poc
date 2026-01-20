package com.example.weather.domain.value_objects;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Value object representing temperature in Celsius.
 * Immutable with automatic clamping to valid range [15.0, 35.0].
 */
public record Temperature(BigDecimal value) {

    public static final BigDecimal MIN_VALUE = BigDecimal.valueOf(15.0);
    public static final BigDecimal MAX_VALUE = BigDecimal.valueOf(35.0);

    public Temperature {
        if (value == null) {
            throw new IllegalArgumentException("Temperature value cannot be null");
        }
        // Clamp to valid range
        value = clamp(value);
    }

    /**
     * Factory method to create a Temperature with clamping.
     */
    public static Temperature of(BigDecimal value) {
        return new Temperature(value);
    }

    /**
     * Add variation and return new Temperature (clamped).
     */
    public Temperature add(BigDecimal variation) {
        return new Temperature(this.value.add(variation));
    }

    /**
     * Clamp value to valid range.
     */
    private static BigDecimal clamp(BigDecimal value) {
        if (value.compareTo(MIN_VALUE) < 0) {
            return MIN_VALUE;
        }
        if (value.compareTo(MAX_VALUE) > 0) {
            return MAX_VALUE;
        }
        return value.setScale(1, RoundingMode.HALF_UP);
    }
}
