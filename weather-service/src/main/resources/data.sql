-- Weather Tracing PoC - Seed Data
-- Baseline weather data for 3 Taiwan cities

-- Clear existing data (for re-initialization)
DELETE FROM weather_data;

-- Insert baseline data per FR-008 and FR-009
INSERT INTO weather_data (city_code, city_name, base_temperature, base_rainfall) VALUES
    ('TPE', '台北', 25.0, 15.0),   -- Taipei: 25°C, 15mm
    ('TXG', '台中', 27.0, 10.0),   -- Taichung: 27°C, 10mm
    ('KHH', '高雄', 29.0, 8.0);    -- Kaohsiung: 29°C, 8mm
