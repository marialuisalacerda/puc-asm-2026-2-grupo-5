# C4 · Nível 2 — Containers

> Índice do C4: [README](README.md) · Anterior: [C1 — Contexto](C1-contexto.md) · Próximos: [C3 — API](C3-componentes-api.md), [C3 — Worker](C3-componentes-worker.md)

## Objetivo

Abrir o sistema Plantão de Pet nas **unidades executáveis e de dados** (containers no sentido do C4), com a tecnologia de cada uma e o protocolo de cada comunicação. Os containers do backend correspondem 1:1 aos serviços do [`docker-compose.yml`](../../docker-compose.yml) (ADR-006); a implantação física está no [diagrama de deployment](../diagrams/deployment.md).

## Diagrama

```mermaid
flowchart TB
    tutor["Tutor<br/><i>[Pessoa]</i>"]
    cuidador["Cuidador<br/><i>[Pessoa]</i>"]

    subgraph sistema["Plantão de Pet"]
        direction TB
        mobile["<b>App mobile</b><br/><i>[Container: React Native · Expo · TypeScript]</i><br/>Telas de pets, cuidadores e reservas.<br/>PlantaoPetApi com implementação HTTP ou Mock"]
        api["<b>API</b><br/><i>[Container: Java 21 · Spring Boot 3.3 · porta 8080]</i><br/>REST, regras de reserva, preço (Strategy),<br/>persistência e publicação de eventos"]
        apidb[("<b>PostgreSQL da API</b><br/><i>[Container: PostgreSQL 16 · db plantao_pet]</i><br/>tutors, pets, caregivers, bookings<br/>schema via Flyway")]
        broker["<b>RabbitMQ</b><br/><i>[Container: RabbitMQ 3.13 · AMQP 0-9-1]</i><br/>exchange booking.events (topic),<br/>fila notification.booking-events, DLX/DLQ"]
        worker["<b>notification-worker</b><br/><i>[Container: Java 21 · Spring Boot 3.3 · porta 8081]</i><br/>Consome eventos de reserva de forma idempotente<br/>e registra notificações"]
        workerdb[("<b>PostgreSQL de notificações</b><br/><i>[Container: PostgreSQL 16 · db notifications]</i><br/>processed_events, notifications<br/>schema via Flyway próprio")]
    end

    subgraph obs["Stack de observabilidade (ADR-005)"]
        direction TB
        otel["<b>OTel Collector</b><br/><i>[Container: otelcol-contrib · 4317/4318]</i><br/>Recebe OTLP e roteia por sinal"]
        prom["<b>Prometheus</b><br/><i>[Container: Prometheus 2.54]</i><br/>Métricas e alertas"]
        exporter["<b>postgres-exporter</b><br/><i>[Container]</i><br/>Métricas do PostgreSQL da API"]
        jaeger["<b>Jaeger</b><br/><i>[Container: all-in-one 1.60]</i><br/>Traces distribuídos"]
        loki["<b>Loki</b><br/><i>[Container: Loki 3.1]</i><br/>Logs"]
        grafana["<b>Grafana</b><br/><i>[Container: Grafana 11.2]</i><br/>Dashboards e correlação"]
    end

    tutor -->|"Usa"| mobile
    cuidador -->|"Usa"| mobile
    mobile -->|"HTTP/JSON REST<br/>OpenAPI, erros RFC 7807"| api
    mobile -.->|"HTTP/JSON GET /notifications<br/>(consulta das notificações registradas)"| worker
    api -->|"JDBC · Spring Data JPA<br/>leitura e escrita"| apidb
    api -->|"AMQP · Spring AMQP<br/>publica Booking* após o commit"| broker
    broker -->|"AMQP · entrega da fila<br/>notification.booking-events"| worker
    worker -->|"JDBC · Spring Data JPA<br/>leitura e escrita"| workerdb

    api -->|"OTLP HTTP/protobuf :4318<br/>traces, métricas, logs"| otel
    worker -->|"OTLP HTTP/protobuf :4318<br/>traces, métricas, logs"| otel
    otel -->|"OTLP gRPC"| jaeger
    otel -->|"OTLP HTTP"| loki
    prom -->|"HTTP scrape :8889"| otel
    prom -->|"HTTP scrape :15692"| broker
    prom -->|"HTTP scrape :9187"| exporter
    exporter -->|"SQL"| apidb
    grafana -->|"PromQL"| prom
    grafana -->|"API Jaeger"| jaeger
    grafana -->|"LogQL"| loki

    classDef person fill:#08427b,stroke:#052e56,color:#ffffff
    classDef container fill:#1168bd,stroke:#0b4884,color:#ffffff
    classDef db fill:#2f7d4f,stroke:#1f5535,color:#ffffff
    classDef infra fill:#6b7b8c,stroke:#46525e,color:#ffffff
    class tutor,cuidador person
    class mobile,api,worker,broker container
    class apidb,workerdb db
    class otel,prom,exporter,jaeger,loki,grafana infra
```

Legenda: azul = container da aplicação; verde = banco de dados; cinza = observabilidade. Seta tracejada = uso secundário.

## Containers

| Container | Tecnologia | Responsabilidade | Código / configuração |
|---|---|---|---|
| App mobile | React Native, Expo, TypeScript | Interface do tutor e do cuidador; alterna HTTP/Mock por `EXPO_PUBLIC_USE_MOCKS` | `apps/mobile/` |
| API | Java 21, Spring Boot 3.3, Spring Web, Spring Data JPA, Spring AMQP | Contrato REST ([OpenAPI](../api/openapi.yaml)), regras de negócio, preço, publicação de eventos | `services/api/` |
| PostgreSQL da API | PostgreSQL 16 | Dados do domínio de reservas | migrations em `services/api/src/main/resources/db/migration/` |
| RabbitMQ | RabbitMQ 3.13 (management + prometheus) | Transporte assíncrono dos eventos ([contrato de eventos](../api/eventos.md)) | `infrastructure/docker/rabbitmq/` |
| notification-worker | Java 21, Spring Boot 3.3, Spring AMQP, Spring Data JPA | Consumo idempotente, retry, DLQ, registro de notificações | `services/notification-worker/` |
| PostgreSQL de notificações | PostgreSQL 16 | Idempotência e notificações | migrations em `services/notification-worker/src/main/resources/db/migration/` |
| OTel Collector, Prometheus, Jaeger, Loki, Grafana, postgres-exporter | ver ADR-005 | Logs, métricas, traces, dashboards e alertas | [`infrastructure/observability/`](../../infrastructure/observability/README.md) |

## Ownership dos dados

```text
API ─────────────▶ PostgreSQL da API (plantao_pet)       dono: API
notification-worker ▶ PostgreSQL de notificações         dono: worker
API ──evento──▶ RabbitMQ ──▶ notification-worker         único caminho entre os dois
```

- Cada banco tem **um único dono** e um Flyway próprio. Nenhum container lê ou escreve no banco do outro ([ADR-003](../adr/ADR-003-persistencia.md), [contratos](../architecture/contratos-integracao.md#ownership-dos-dados)).
- O worker recebe **tudo** de que precisa no evento (`data` do envelope). Ele não chama a API nem lê `bookings`.
- Essa separação é a estratégia de "dados distribuídos / ownership" do projeto: os bancos são independentes e a consistência entre eles é **eventual**, via eventos.

## Decisões refletidas

| Decisão | ADR |
|---|---|
| Monolito modular (API) + worker assíncrono, sem microsserviços | [ADR-001](../adr/ADR-001-estilo-arquitetural-backend.md) |
| Stack mobile Expo/React Native | [ADR-002](../adr/ADR-002-stack-mobile.md) |
| PostgreSQL + JPA + Flyway, ownership por banco | [ADR-003](../adr/ADR-003-persistencia.md) |
| RabbitMQ, topic exchange, DLQ, idempotência | [ADR-004](../adr/ADR-004-mensageria.md) |
| OpenTelemetry → Collector → Prometheus/Jaeger/Loki → Grafana | [ADR-005](../adr/ADR-005-observabilidade.md) |
| Docker Compose, profile `app` | [ADR-006](../adr/ADR-006-containerizacao.md) |
