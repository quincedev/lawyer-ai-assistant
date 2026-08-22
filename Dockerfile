# ============================================================
# Stage 1: Build
# ============================================================

FROM maven:3.9.11-eclipse-temurin-21 AS builder

WORKDIR /workspace


# ------------------------------------------------------------
# 1. Copy Maven descriptor first
#
# 先复制 pom.xml，单独下载依赖。
#
# 这样只要 pom.xml 没变化，
# Docker 可以复用 dependency layer，
# 不需要每次重新下载全部 Maven dependencies。
# ------------------------------------------------------------

COPY pom.xml .

RUN mvn \
    -B \
    -ntp \
    dependency:go-offline


# ------------------------------------------------------------
# 2. Copy source
# ------------------------------------------------------------

COPY src ./src


# ------------------------------------------------------------
# 3. Build application
#
# Step 2 的目标是构建生产 Image。
#
# UT 已经在宿主机 / CI 中单独执行，
# Docker Build 不重复运行测试。
# ------------------------------------------------------------

RUN mvn \
    -B \
    -ntp \
    clean \
    package \
    -DskipTests


# ============================================================
# Stage 2: Runtime
# ============================================================

FROM eclipse-temurin:21-jre-jammy AS runtime

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

LABEL org.opencontainers.image.title="lawyer-ai-assistant"
LABEL org.opencontainers.image.description="Enterprise AI Agent and MCP legal knowledge service"
LABEL org.opencontainers.image.version="0.0.1-SNAPSHOT"

RUN groupadd \
        --system \
        appgroup \
    && useradd \
        --system \
        --gid appgroup \
        --create-home \
        --home-dir /app \
        appuser

WORKDIR /app

COPY --from=builder \
    /workspace/target/lawyer-ai-assistant-0.0.1-SNAPSHOT.jar \
    app.jar

RUN chown appuser:appgroup /app/app.jar

USER appuser

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

EXPOSE 8080
EXPOSE 8081

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]