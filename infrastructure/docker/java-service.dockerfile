# syntax=docker/dockerfile:1
# ============================================================
# Imagem única para os serviços Java/Spring Boot (api, notification-worker).
# O contexto de build é a pasta do serviço (services/<servico>), definido no
# docker-compose.yml. Instrumentação por OpenTelemetry Java Agent (ADR-005):
# nenhuma dependência de observabilidade é exigida no código do serviço.
# ============================================================

ARG JAVA_VERSION=21

# ---------- build ----------
FROM maven:3.9.9-eclipse-temurin-${JAVA_VERSION} AS build
WORKDIR /workspace

# Dependências em camada separada para aproveitar cache
COPY pom.xml .
RUN mvn -B -ntp dependency:go-offline

COPY src ./src
RUN mvn -B -ntp package -DskipTests \
    && cp "$(ls target/*.jar | grep -v -- '-plain.jar' | head -n 1)" /workspace/app.jar

# ---------- runtime ----------
FROM eclipse-temurin:${JAVA_VERSION}-jre-alpine
ARG OTEL_AGENT_VERSION=2.8.0

RUN addgroup -S app && adduser -S app -G app
WORKDIR /app

RUN mkdir -p /opt/otel
ADD --chmod=644 https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v${OTEL_AGENT_VERSION}/opentelemetry-javaagent.jar /opt/otel/opentelemetry-javaagent.jar
COPY --from=build --chown=app:app /workspace/app.jar /app/app.jar

USER app

# Desligar a instrumentação: OTEL_JAVAAGENT_ENABLED=false
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -javaagent:/opt/otel/opentelemetry-javaagent.jar -jar /app/app.jar"]
