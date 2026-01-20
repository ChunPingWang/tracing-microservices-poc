package com.example.weather.domain.value_objects;

import java.util.Set;

/**
 * Value object representing a valid city code.
 * Immutable and validated on creation.
 */
public record CityCode(String value) {

    private static final Set<String> VALID_CODES = Set.of("TPE", "TXG", "KHH");

    public CityCode {
        if (value == null || !VALID_CODES.contains(value)) {
            throw new IllegalArgumentException("Invalid city code: " + value + ". Must be one of: " + VALID_CODES);
        }
    }

    /**
     * Factory method to create a CityCode.
     */
    public static CityCode of(String code) {
        return new CityCode(code);
    }

    /**
     * Check if a code is valid without throwing exception.
     */
    public static boolean isValid(String code) {
        return code != null && VALID_CODES.contains(code);
    }

    /**
     * Returns all valid city codes.
     */
    public static Set<String> validCodes() {
        return VALID_CODES;
    }
}
