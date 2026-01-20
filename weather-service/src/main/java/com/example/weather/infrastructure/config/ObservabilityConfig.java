package com.example.weather.infrastructure.config;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ObservabilityConfig {

    private static final Logger logger = LoggerFactory.getLogger(ObservabilityConfig.class);

    @Value("${otel.sdk.disabled:false}")
    private boolean otelDisabled;

    @Bean
    public OpenTelemetry openTelemetry() {
        if (otelDisabled) {
            logger.info("OpenTelemetry SDK is disabled");
            return OpenTelemetry.noop();
        }

        try {
            OpenTelemetry openTelemetry = AutoConfiguredOpenTelemetrySdk.builder()
                    .setResultAsGlobal()
                    .build()
                    .getOpenTelemetrySdk();
            logger.info("OpenTelemetry SDK initialized successfully");
            return openTelemetry;
        } catch (Exception e) {
            logger.warn("Failed to initialize OpenTelemetry SDK, using noop: {}", e.getMessage());
            return OpenTelemetry.noop();
        }
    }

    @Bean
    public Tracer tracer(OpenTelemetry openTelemetry) {
        return openTelemetry.getTracer("weather-service", "1.0.0");
    }
}
