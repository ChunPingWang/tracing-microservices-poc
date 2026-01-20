-- Weather Tracing PoC - Database Schema
-- H2 Database (In-Memory)

CREATE TABLE IF NOT EXISTS weather_data (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    city_code VARCHAR(10) NOT NULL UNIQUE,
    city_name VARCHAR(50) NOT NULL,
    base_temperature DECIMAL(5,2) NOT NULL,
    base_rainfall DECIMAL(5,2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_weather_data_city_code ON weather_data(city_code);

-- Optional: Query history for trace demonstration
CREATE TABLE IF NOT EXISTS query_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    city_code VARCHAR(10) NOT NULL,
    trace_id VARCHAR(64),
    queried_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    response_time_ms BIGINT
);

CREATE INDEX IF NOT EXISTS idx_query_history_trace_id ON query_history(trace_id);
