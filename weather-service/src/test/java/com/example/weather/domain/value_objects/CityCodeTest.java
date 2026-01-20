package com.example.weather.domain.value_objects;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class CityCodeTest {

    @ParameterizedTest
    @ValueSource(strings = {"TPE", "TXG", "KHH"})
    void of_withValidCityCode_shouldCreateCityCode(String code) {
        CityCode cityCode = CityCode.of(code);

        assertNotNull(cityCode);
        assertEquals(code, cityCode.value());
    }

    @ParameterizedTest
    @ValueSource(strings = {"XXX", "ABC", "TAIPEI", "tpe", ""})
    void of_withInvalidCityCode_shouldThrowException(String code) {
        assertThrows(IllegalArgumentException.class, () -> CityCode.of(code));
    }

    @Test
    void of_withNullCode_shouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> CityCode.of(null));
    }

    @Test
    void equals_withSameCode_shouldBeEqual() {
        CityCode code1 = CityCode.of("TPE");
        CityCode code2 = CityCode.of("TPE");

        assertEquals(code1, code2);
        assertEquals(code1.hashCode(), code2.hashCode());
    }

    @Test
    void equals_withDifferentCode_shouldNotBeEqual() {
        CityCode tpe = CityCode.of("TPE");
        CityCode khh = CityCode.of("KHH");

        assertNotEquals(tpe, khh);
    }

    @Test
    void isValid_withValidCode_shouldReturnTrue() {
        assertTrue(CityCode.isValid("TPE"));
        assertTrue(CityCode.isValid("TXG"));
        assertTrue(CityCode.isValid("KHH"));
    }

    @Test
    void isValid_withInvalidCode_shouldReturnFalse() {
        assertFalse(CityCode.isValid("XXX"));
        assertFalse(CityCode.isValid(null));
        assertFalse(CityCode.isValid(""));
    }
}
