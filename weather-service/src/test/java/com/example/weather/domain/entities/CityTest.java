package com.example.weather.domain.entities;

import com.example.weather.domain.value_objects.CityCode;
import com.example.weather.domain.value_objects.Rainfall;
import com.example.weather.domain.value_objects.Temperature;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class CityTest {

    @Test
    void constructor_withValidParameters_shouldCreateCity() {
        CityCode code = CityCode.of("TPE");
        String name = "台北";
        Temperature baseTemp = Temperature.of(BigDecimal.valueOf(25.0));
        Rainfall baseRainfall = Rainfall.of(BigDecimal.valueOf(15.0));

        City city = new City(code, name, baseTemp, baseRainfall);

        assertNotNull(city);
        assertEquals(code, city.code());
        assertEquals(name, city.name());
        assertEquals(baseTemp, city.baseTemperature());
        assertEquals(baseRainfall, city.baseRainfall());
    }

    @Test
    void constructor_withNullCode_shouldThrowException() {
        assertThrows(NullPointerException.class, () ->
            new City(null, "台北", Temperature.of(BigDecimal.valueOf(25.0)), Rainfall.of(BigDecimal.valueOf(15.0)))
        );
    }

    @Test
    void constructor_withNullName_shouldThrowException() {
        assertThrows(NullPointerException.class, () ->
            new City(CityCode.of("TPE"), null, Temperature.of(BigDecimal.valueOf(25.0)), Rainfall.of(BigDecimal.valueOf(15.0)))
        );
    }

    @Test
    void constructor_withEmptyName_shouldThrowException() {
        assertThrows(IllegalArgumentException.class, () ->
            new City(CityCode.of("TPE"), "", Temperature.of(BigDecimal.valueOf(25.0)), Rainfall.of(BigDecimal.valueOf(15.0)))
        );
    }

    @Test
    void equals_withSameCode_shouldBeEqual() {
        City city1 = new City(CityCode.of("TPE"), "台北", Temperature.of(BigDecimal.valueOf(25.0)), Rainfall.of(BigDecimal.valueOf(15.0)));
        City city2 = new City(CityCode.of("TPE"), "台北", Temperature.of(BigDecimal.valueOf(25.0)), Rainfall.of(BigDecimal.valueOf(15.0)));

        assertEquals(city1, city2);
    }

    @Test
    void equals_withDifferentCode_shouldNotBeEqual() {
        City tpe = new City(CityCode.of("TPE"), "台北", Temperature.of(BigDecimal.valueOf(25.0)), Rainfall.of(BigDecimal.valueOf(15.0)));
        City khh = new City(CityCode.of("KHH"), "高雄", Temperature.of(BigDecimal.valueOf(29.0)), Rainfall.of(BigDecimal.valueOf(8.0)));

        assertNotEquals(tpe, khh);
    }
}
