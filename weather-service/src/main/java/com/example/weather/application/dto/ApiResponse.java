package com.example.weather.application.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Generic API response wrapper.
 * All API responses follow this format for consistency.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
    boolean success,
    T data,
    ErrorInfo error,
    TraceInfo traceInfo
) {
    /**
     * Creates a successful response with data.
     */
    public static <T> ApiResponse<T> success(T data, TraceInfo traceInfo) {
        return new ApiResponse<>(true, data, null, traceInfo);
    }

    /**
     * Creates an error response.
     */
    public static <T> ApiResponse<T> error(String code, String message, TraceInfo traceInfo) {
        return new ApiResponse<>(false, null, new ErrorInfo(code, message), traceInfo);
    }

    /**
     * Error information record.
     */
    public record ErrorInfo(String code, String message) {}
}
