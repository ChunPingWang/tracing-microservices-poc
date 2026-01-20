# Weather Service Dockerfile
# 多階段建置，優化映像大小

# ===== 建置階段 =====
FROM gradle:8.5-jdk17 AS builder

WORKDIR /app

# 複製 Gradle 設定
COPY build.gradle settings.gradle* ./
COPY gradle ./gradle

# 下載依賴（利用 Docker 快取）
RUN gradle dependencies --no-daemon || true

# 複製原始碼
COPY src ./src

# 建置 JAR
RUN gradle bootJar --no-daemon -x test

# ===== 執行階段 =====
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# 建立非 root 使用者
RUN addgroup -g 1001 appgroup && \
    adduser -u 1001 -G appgroup -D appuser

# 複製 JAR
COPY --from=builder /app/build/libs/*.jar app.jar

# 設定權限
RUN chown -R appuser:appgroup /app

USER appuser

# 健康檢查
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8081/actuator/health || exit 1

# 環境變數
ENV JAVA_OPTS="-Xms256m -Xmx512m"
ENV SERVER_PORT=8081

EXPOSE 8081

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
