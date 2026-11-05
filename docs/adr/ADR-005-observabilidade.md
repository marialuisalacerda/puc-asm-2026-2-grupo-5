# ADR-005: Estratégia de observabilidade

Status: Proposto
Data: 2026-10-28
Responsável: João Pedro Schlindwein (Docker e Observabilidade)
Revisores: Amir Gabriel Dantas Santos Andrade (Arquitetura e C4) + equipe

## Contexto
O Plantão de Pet é um monolito modular (ADR-001) com comunicação assíncrona via RabbitMQ
entre a API e o `notification-worker`. Uma reserva atravessa mobile → API → PostgreSQL →
RabbitMQ → worker, então falhas e lentidão precisam ser rastreáveis de ponta a ponta.

Requisitos que influenciam a decisão:

- demonstrar os três pilares: **logs, métricas e traces**;
- RNF de disponibilidade (99,5%) e de busca em até 2s exigem medir taxa de erro e latência;
- acompanhar métricas de negócio (reservas criadas/aceitas/rejeitadas) e de mensageria
  (mensagens processadas, falhas, fila acumulada);
- backend ainda em desenvolvimento por outro integrante: a observabilidade **não pode
  depender** de código pronto da API para ser montada e testada;
- tudo deve subir localmente com `docker compose up -d` (ADR-006).

## Opções consideradas
1. **Spring Boot Actuator + Micrometer + Prometheus (scrape direto), sem traces**
   - prós: simples, padrão do ecossistema Spring;
   - contras: não cobre traces distribuídos API → RabbitMQ → worker; exige dependências
     e configuração no código de cada serviço; logs ficariam só no `docker logs`.
2. **OpenTelemetry (Java Agent) → OTel Collector → Prometheus, Jaeger e Loki, visualizados no Grafana**
   - prós: padrão aberto e neutro de fornecedor; instrumentação automática de Spring MVC,
     JDBC, Spring AMQP/RabbitMQ e Logback sem alterar código; propagação de contexto
     de trace pela mensagem do RabbitMQ; o Collector desacopla a aplicação dos backends;
   - contras: mais containers (collector, jaeger, loki); agente adiciona alguns segundos
     no startup e um pouco de memória.
3. **SaaS (Datadog, New Relic, Grafana Cloud)**
   - prós: zero infraestrutura;
   - contras: custo, conta/credenciais por integrante, dependência externa e
     não reproduzível offline para a apresentação.

## Decisão
Escolhemos a **opção 2** porque cobre os três pilares com um padrão aberto e não acopla o
código de negócio à observabilidade.

- **Instrumentação:** OpenTelemetry Java Agent embutido na imagem Docker dos serviços Java
  (`infrastructure/docker/java-service.dockerfile`). Configuração via variáveis `OTEL_*`
  no `docker-compose.yml`. A API e o worker **não** precisam declarar dependências de
  observabilidade.
- **Coleta:** OpenTelemetry Collector recebe OTLP (4317/4318) e roteia:
  traces → **Jaeger**, métricas → endpoint **Prometheus**, logs → **Loki**.
- **Infraestrutura:** Prometheus também coleta RabbitMQ (plugin `rabbitmq_prometheus`)
  e PostgreSQL (`postgres-exporter`).
- **Visualização:** **Grafana** com datasources e dashboard provisionados como código;
  correlação trace ↔ logs pelo `trace_id`.
- **Alertas:** regras em `infrastructure/observability/prometheus/alerts.yml`
  (alvo fora, erro 5xx > 5%, p95 > 1s, falhas no worker, fila acumulada, mensagens na DLQ).

### Contrato de métricas de negócio
Métricas customizadas são registradas pelo código com Micrometer (`MeterRegistry`) ou com a
API do OpenTelemetry e chegam ao Prometheus pelo agente:

| Nome no código | Nome no Prometheus | Responsável |
|---|---|---|
| `booking.created` | `booking_created_total` | API |
| `booking.accepted` | `booking_accepted_total` | API |
| `booking.rejected` | `booking_rejected_total` | API |
| `booking.cancelled` | `booking_cancelled_total` | API |
| `message.processing` | `message_processing_total` | notification-worker |
| `message.processing.errors` | `message_processing_errors_total` | notification-worker |

Métricas HTTP (`http_server_request_duration_seconds`) e JVM (`jvm_*`) são automáticas.

## Consequências
Positivas:
- três pilares correlacionados e um trace único do fluxo de reserva entre API e worker;
- serviços continuam testáveis sem a stack (agente desligável com `OTEL_JAVAAGENT_ENABLED=false`);
- troca de backend (ex.: Jaeger → Tempo) afeta apenas a configuração do Collector;
- dashboards e alertas versionados e revisáveis por Pull Request.

Negativas / riscos:
- ambiente local mais pesado (~6 containers de observabilidade);
- nomes de métricas dependem das convenções semânticas do OpenTelemetry; mudanças de
  versão do agente podem exigir ajuste nas consultas do dashboard;
- stack não preparada para produção (sem persistência distribuída, sem autenticação forte).

## Impacto no C4
Nível afetado: C2
O que muda:
- adicionar o container **OpenTelemetry Collector**, que recebe telemetria (OTLP) da API e do `notification-worker`;
- adicionar **Prometheus**, **Jaeger**, **Loki** e **Grafana** (podem ser agrupados como "Stack de observabilidade");
- setas: API/worker → Collector ("envia traces, métricas e logs via OTLP"); Prometheus → RabbitMQ/PostgreSQL ("coleta métricas").

## Referências
- [Guia de observabilidade](../../infrastructure/observability/README.md)
- OpenTelemetry Java Agent: <https://opentelemetry.io/docs/zero-code/java/agent/>
- OpenTelemetry Collector: <https://opentelemetry.io/docs/collector/>
- Semantic conventions HTTP metrics: <https://opentelemetry.io/docs/specs/semconv/http/http-metrics/>
- RabbitMQ Prometheus: <https://www.rabbitmq.com/docs/prometheus>
