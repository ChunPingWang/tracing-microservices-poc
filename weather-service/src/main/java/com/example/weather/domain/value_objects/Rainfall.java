package com.example.weather.domain.value_objects;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Value object representing rainfall in millimeters.
 * Immutable with automatic clamping to valid range [0.0, 50.0].
 */
public record Rainfall(BigDecimal value) {

    public static final BigDecimal MIN_VALUE = BigDecimal.ZERO;
    public static final BigDecimal MAX_VALUE = BigDecimal.valueOf(50.0);

    public Rainfall {
        if (value == null) {
            throw new IllegalArgumentException("Rainfall value cannot be null");
        }
        // Clamp to valid range
        value = clamp(value);
    }

    /**
     * Factory method to create a Rainfall with clamping.
     */
    public static Rainfall of(BigDecimal value) {
        return new Rainfall(value);
    }

    /**
     * Add variation and return new Rainfall (clamped).
     */
    public Rainfall add(BigDecimal variation) {
        return new Rainfall(this.value.add(variation));
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
