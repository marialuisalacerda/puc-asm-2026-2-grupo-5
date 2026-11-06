# notification-worker

Consome os eventos de reserva publicados pela API no RabbitMQ e gera notificações
(envio simulado em log). Tem **banco próprio** (`notifications`) e nunca acessa o banco da API.

Decisão: [ADR-004](../../docs/adr/ADR-004-mensageria.md) · Contratos:
[eventos](../../docs/api/eventos.md), [integração](../../docs/architecture/contratos-integracao.md) ·
Operação do broker: [infrastructure/messaging](../../infrastructure/messaging/README.md).

Java 21 · Spring Boot 3.3 · Spring AMQP · Spring Data JPA · Flyway · PostgreSQL. Porta **8081**.

## Fluxo

```text
booking.events (topic) ──booking.*──▶ notification.booking-events
        │
        ▼
BookingEventListener ── BookingEventParser (JSON + Bean Validation, version=1)
        │
        ▼
NotificationProcessingService ── uma transação no banco do worker:
        │   1. processed_events contém eventId? → duplicata: ack e ignora
        │   2. BookingEventHandler (Strategy por eventType) decide destinatário e texto
        │   3. INSERT processed_events + INSERT notifications
        │   4. NotificationSender.send(...)  → log "Notification sent channel=LOG recipient=…"
        ▼
commit → ack  |  falha → retry (1s, 2s) → DLQ  |  inválida → DLQ direto
```

| `eventType` | Handler | Destinatário |
|---|---|---|
| `BookingCreated` | `BookingCreatedHandler` | cuidador (`caregiverId`) |
| `BookingAccepted` | `BookingAcceptedHandler` | tutor (`tutorId`) |
| `BookingRejected` | `BookingRejectedHandler` | tutor (`tutorId`), com `reason` |
| `BookingCancelled` | `BookingCancelledHandler` | cuidador (`caregiverId`), com `reason` |

Novo evento = nova implementação de `BookingEventHandler` (`@Component`); nada mais muda.
Novo canal (e-mail, push) = nova implementação de `NotificationSender`.

## Estrutura

```text
src/main/java/br/pucminas/plantaopet/notification/
├── config/       topologia (Declarables), container do listener com retry, propriedades, Clock
├── controller/   GET /notifications (consulta read-only)
├── domain/       Notification, ProcessedEvent, RecipientType, NotificationChannel
├── messaging/    envelope, parser, listener, InvalidEventException
├── repository/   Spring Data JPA
└── service/      processamento idempotente, métricas, handler/ (Strategy), sender/ (porta de saída)
src/main/resources/db/migration/V1__create_notification_tables.sql
```

## Topologia declarada

Declarada pelo próprio worker ao conectar (`RabbitTopologyConfig`):

- exchange `booking.events` (`topic`, durável) — também declarada pela API, com os mesmos atributos;
- fila `notification.booking-events` (durável) com `x-dead-letter-exchange=booking.events.dlx`, binding `booking.*`;
- DLX `booking.events.dlx` (`fanout`, durável) → DLQ `notification.booking-events.dlq`.

## Idempotência

- Chave: `eventId` do envelope. Tabela `processed_events (event_id PK, event_type, processed_at)`.
- Verificação, gravação do `eventId`, gravação da notificação e envio ficam **na mesma transação**:
  qualquer falha desfaz tudo e a mensagem é retentada, sem deixar o evento marcado como processado.
- Evento repetido: confirmado (ack), ignorado, log `Duplicate event ignored` e métrica `outcome=duplicate`.
- Entregas concorrentes do mesmo evento: a PK de `processed_events` (e o `UNIQUE` em
  `notifications.event_id`) faz uma delas falhar; na retentativa ela é tratada como duplicata.
- Resultado: exatamente **uma** notificação gravada por `eventId`. O envio é *at-least-once*: se o
  commit falhar depois do envio, a retentativa pode reenviar (aceitável para o canal simulado).

## Retry e DLQ

- `spring.rabbitmq.listener.simple.retry.*`: 3 tentativas no total, backoff exponencial 1s, 2s
  (`RetryInterceptor` stateless), `default-requeue-rejected=false`.
- Esgotadas as tentativas: `RejectAndDontRequeueRecoverer` → DLX → DLQ.
- `InvalidEventException` (subclasse de `AmqpRejectAndDontRequeueException`) para JSON malformado,
  campo obrigatório ausente, `version` diferente de 1 ou `eventType` desconhecido: **sem retry**, direto
  para a DLQ.
- Campos novos e opcionais no JSON são ignorados (leitor tolerante), conforme a regra de versionamento.

## Métricas e logs

| Métrica (Micrometer) | Tags | Quando |
|---|---|---|
| `message.processing` | `event_type`, `outcome=processed\|duplicate` | evento concluído |
| `message.processing.errors` | `event_type` (`unknown` se ilegível) | cada tentativa com falha ou rejeição |

Exportadas pelo agente OpenTelemetry (ADR-005). Logs: `Event processed …`, `Duplicate event ignored …`,
`Notification sent channel=… recipient=TIPO:id bookingId=… eventId=…`, `Event rejected to DLQ reason=…`,
`Event processing failed …`.

## Variáveis de ambiente

| Variável | Padrão local |
|---|---|
| `SERVER_PORT` | `8081` |
| `DB_HOST` / `DB_PORT` / `DB_NAME` | `localhost` / `5432` / `notifications` |
| `DB_USERNAME` / `DB_PASSWORD` | `notifications` / `notifications_local` |
| `SPRING_RABBITMQ_HOST` / `_PORT` | `localhost` / `5672` |
| `SPRING_RABBITMQ_USERNAME` / `_PASSWORD` | `guest` / `guest` (no Compose vêm de `RABBITMQ_USER` / `RABBITMQ_PASSWORD`) |

## Como rodar

Pelo Compose (a partir da raiz do repositório):

```bash
docker compose --profile app up -d --build notification-worker
docker compose logs -f notification-worker
```

Local (fora do Compose): o `notification-db` do Compose não publica porta, então suba um
PostgreSQL descartável para o worker e use o RabbitMQ do Compose (`docker compose up -d rabbitmq`):

```bash
docker run -d --name notifications-db-local -p 5433:5432 \
  -e POSTGRES_DB=notifications -e POSTGRES_USER=notifications -e POSTGRES_PASSWORD=notifications_local \
  postgres:16.4-alpine
cd services/notification-worker
DB_PORT=5433 SPRING_RABBITMQ_USERNAME=plantao_pet SPRING_RABBITMQ_PASSWORD=plantao_pet_local mvn spring-boot:run
```

Consultas (porta 8081 do container; no Compose o host publica em `WORKER_PORT`, padrão **8082**,
para não conflitar com o Metro do Expo; troque a porta abaixo conforme o caso):

```bash
curl http://localhost:8081/actuator/health
curl "http://localhost:8081/notifications?bookingId=<uuid>"   # sem bookingId: 50 mais recentes
curl http://localhost:8081/actuator/metrics/message.processing
```

## Como testar

```bash
mvn -B -ntp -f services/notification-worker/pom.xml verify
```

- Unitários (JUnit + Mockito): handlers e registry, serviço idempotente (novo, duplicado,
  eventType desconhecido, falha no envio), parser (válido, malformado, campo ausente, versão,
  UUID inválido), listener (métricas de erro) e controller (`@WebMvcTest`).
- Integração (`NotificationWorkerIntegrationTest`, Testcontainers PostgreSQL 16 + RabbitMQ 3.13):
  publica na exchange real e verifica notificação gravada e exposta em `GET /notifications`;
  mesmo `eventId` 2x → 1 registro; lixo → DLQ; `eventType` desconhecido → DLQ; falha transitória →
  3 tentativas, rollback e DLQ. Requer Docker; sem Docker os testes de integração são ignorados.
