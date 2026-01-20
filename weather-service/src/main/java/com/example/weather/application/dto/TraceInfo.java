package com.example.weather.application.dto;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;

/**
 * Trace information DTO for API responses.
 * Contains OpenTelemetry trace and span identifiers.
 */
public record TraceInfo(
    String traceId,
    String spanId,
    long duration
) {
    /**
     * Creates TraceInfo from the current OpenTelemetry span.
     * Duration will be set to 0 initially (updated by controller).
     */
    public static TraceInfo fromCurrentSpan() {
        Span currentSpan = Span.current();
        SpanContext context = currentSpan.getSpanContext();

        return new TraceInfo(
            context.getTraceId(),
            context.getSpanId(),
            0L
        );
    }

    /**
     * Creates TraceInfo with a specific duration.
     */
    public TraceInfo withDuration(long durationMs) {
        return new TraceInfo(this.traceId, this.spanId, durationMs);
    }
}
