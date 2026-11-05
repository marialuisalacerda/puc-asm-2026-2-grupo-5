# Observabilidade

Decisão registrada no [ADR-005](../../docs/adr/ADR-005-observabilidade.md).

## Fluxo

```text
API / notification-worker (OpenTelemetry Java Agent)
        |
        | OTLP (traces, métricas, logs)
        v
OpenTelemetry Collector
   |            |             |
   v            v             v
Jaeger     Prometheus       Loki
(traces)   (métricas)      (logs)
   \            |             /
    +------- Grafana --------+

Prometheus também coleta: RabbitMQ (:15692, agregado e por fila) e PostgreSQL (postgres-exporter)
```

## Estrutura

```text
infrastructure/observability/
├── otel-collector/config.yaml        # receivers OTLP e roteamento por sinal
├── prometheus/prometheus.yml         # alvos de scrape
├── prometheus/alerts.yml             # regras de alerta
├── loki/config.yaml                  # armazenamento de logs (OTLP nativo)
└── grafana/
    ├── provisioning/                 # datasources e provider de dashboards
    └── dashboards/plantao-pet-overview.json
```

## Como usar

```bash
docker compose up -d
```

- Grafana: http://localhost:3000 → dashboard **Plantão de Pet · Visão geral** (página inicial)
- Prometheus: http://localhost:9090/targets — todos os alvos devem estar `UP`
- Jaeger: http://localhost:16686 — serviços `api` e `notification-worker`
- Logs: Grafana → Explore → Loki → `{service_name="api"}`

## Dashboard

| Seção | Painéis |
|---|---|
| Visão geral | req/s, taxa de erro 5xx, reservas criadas, alvos ativos |
| API · HTTP | req/s por rota, p50/p95/p99, respostas por status, eventos de reserva |
| Mensageria | publicadas/entregues, mensagens por fila, DLQ, processadas/duplicadas/erros no worker, eventos por tipo |
| Infraestrutura | heap JVM, conexões PostgreSQL, disponibilidade |

Painéis da API e do worker mostram "sem dados" até os serviços rodarem (`--profile app`).

## Contrato para o backend

Nada é obrigatório para ter traces, métricas HTTP/JVM e logs: o agente instrumenta
Spring MVC, JDBC, Spring AMQP e Logback automaticamente, e propaga o trace da API
para o worker pelos headers da mensagem.

Métricas de negócio (contadores) devem usar os nomes abaixo para aparecerem no dashboard:

```java
// API — ex.: BookingService, após persistir a reserva
meterRegistry.counter("booking.created").increment();   // booking_created_total
// também: booking.accepted, booking.rejected, booking.cancelled

// notification-worker — no consumer
meterRegistry.counter("message.processing", "event_type", type, "outcome", "processed").increment(); // message_processing_total
meterRegistry.counter("message.processing.errors", "event_type", type).increment();                // message_processing_errors_total
```

`MeterRegistry` é injetado pelo Spring (requer `spring-boot-starter-actuator`) e pode ser
substituído por `SimpleMeterRegistry` nos testes. O agente só exporta métricas do Micrometer
com `OTEL_INSTRUMENTATION_MICROMETER_ENABLED=true`, já definido no `docker-compose.yml`.

Para rodar um serviço fora do Docker com instrumentação:

```bash
java -javaagent:opentelemetry-javaagent.jar \
  -Dotel.service.name=api \
  -Dotel.exporter.otlp.endpoint=http://localhost:4318 \
  -jar target/app.jar
```

## Alertas

Definidos em `prometheus/alerts.yml` e visíveis em http://localhost:9090/alerts:
`TargetDown`, `ApiHighErrorRate`, `ApiHighLatencyP95`, `MessageProcessingErrors`, `RabbitMqQueueBacklog`, `DeadLetterQueueNotEmpty`.
