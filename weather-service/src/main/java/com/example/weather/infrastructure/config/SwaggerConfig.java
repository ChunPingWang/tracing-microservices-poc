package com.example.weather.infrastructure.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Swagger/OpenAPI 配置
 *
 * 提供 API 文件自動生成功能，可透過 Swagger UI 互動式測試 API。
 *
 * 存取方式：
 * - Swagger UI: http://localhost:8081/swagger-ui.html
 * - OpenAPI JSON: http://localhost:8081/v3/api-docs
 * - OpenAPI YAML: http://localhost:8081/v3/api-docs.yaml
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI weatherApiOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Weather Tracing PoC API")
                        .description("""
                                天氣查詢可觀測性展示系統 API

                                ## 功能特點

                                - **分散式追蹤**: 每個請求都會產生唯一的 Trace ID 和 Span ID
                                - **OpenTelemetry 整合**: 自動收集追蹤資料並匯出至 Jaeger
                                - **Prometheus 指標**: 提供即時效能監控指標

                                ## 追蹤資訊說明

                                ### Trace ID (追蹤識別碼)
                                - 32 個十六進位字元的唯一識別碼
                                - 代表一個完整的請求鏈路
                                - 從請求進入系統到回應返回的整個過程

                                ### Span ID (跨度識別碼)
                                - 16 個十六進位字元的唯一識別碼
                                - 代表請求鏈路中的一個單獨操作
                                - 一個 Trace 可包含多個 Spans

                                ## 可觀測性三大支柱

                                1. **Traces (追蹤)**: 請求在系統中的完整路徑
                                2. **Metrics (指標)**: 系統的數值化效能資料
                                3. **Logs (日誌)**: 詳細的事件記錄
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Weather Tracing Team")
                                .email("team@weather-tracing.example.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .externalDocs(new ExternalDocumentation()
                        .description("Jaeger UI - 查看分散式追蹤")
                        .url("http://localhost:16686"))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8081")
                                .description("Weather Service (直接存取)"),
                        new Server()
                                .url("http://localhost:8080")
                                .description("API Gateway")))
                .tags(List.of(
                        new Tag()
                                .name("Weather")
                                .description("天氣查詢 API - 提供台灣主要城市天氣資訊"),
                        new Tag()
                                .name("Health")
                                .description("健康檢查端點 - 用於監控服務狀態")));
    }
}
