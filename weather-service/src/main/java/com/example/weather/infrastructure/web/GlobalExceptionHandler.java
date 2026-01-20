package com.example.weather.infrastructure.web;

import com.example.weather.application.dto.ApiResponse;
import com.example.weather.application.dto.TraceInfo;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(CityNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleCityNotFound(CityNotFoundException ex) {
        logger.warn("City not found: {}", ex.getMessage());

        Span currentSpan = Span.current();
        currentSpan.setStatus(StatusCode.ERROR, ex.getMessage());

        TraceInfo traceInfo = TraceInfo.fromCurrentSpan();
        ApiResponse<Void> response = ApiResponse.error("CITY_NOT_FOUND", "找不到指定的城市", traceInfo);

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(InvalidCityCodeException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidCityCode(InvalidCityCodeException ex) {
        logger.warn("Invalid city code: {}", ex.getMessage());

        Span currentSpan = Span.current();
        currentSpan.setStatus(StatusCode.ERROR, ex.getMessage());

        TraceInfo traceInfo = TraceInfo.fromCurrentSpan();
        ApiResponse<Void> response = ApiResponse.error("INVALID_CITY_CODE", "城市代碼格式錯誤", traceInfo);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        logger.error("Unexpected error", ex);

        Span currentSpan = Span.current();
        currentSpan.setStatus(StatusCode.ERROR, ex.getMessage());
        currentSpan.recordException(ex);

        TraceInfo traceInfo = TraceInfo.fromCurrentSpan();
        ApiResponse<Void> response = ApiResponse.error("INTERNAL_ERROR", "內部錯誤，請稍後再試", traceInfo);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    // Custom exception classes
    public static class CityNotFoundException extends RuntimeException {
        public CityNotFoundException(String cityCode) {
            super("City not found: " + cityCode);
        }
    }

    public static class InvalidCityCodeException extends RuntimeException {
        public InvalidCityCodeException(String cityCode) {
            super("Invalid city code: " + cityCode);
        }
    }
}
