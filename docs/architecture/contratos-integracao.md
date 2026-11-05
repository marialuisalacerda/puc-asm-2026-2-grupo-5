# Contratos de integração

Referência única de nomes, portas, variáveis e regras compartilhadas entre os componentes.
Contratos detalhados: [OpenAPI](../api/openapi.yaml) · [Eventos](../api/eventos.md).

## Nomes

| Item | Valor |
|---|---|
| Produto | Plantão de Pet |
| Identificador técnico | `plantaopet` (pacotes Java `br.pucminas.plantaopet.api` e `br.pucminas.plantaopet.notification`) |
| Projeto Compose | `plantao-pet` |
| Java / Spring Boot | Java 21 · Spring Boot 3.3.x · Maven |
| Mobile | Expo (React Native) · TypeScript |

## Componentes e portas

| Componente | Pasta | Porta | Banco | Health |
|---|---|---|---|---|
| API | `services/api` | 8080 | `postgres` / database `plantao_pet` (dono: API) | `/actuator/health` |
| notification-worker | `services/notification-worker` | 8081 no container · **8082** no host (`WORKER_PORT`) | `notification-db` / database `notifications` (dono: worker) | `/actuator/health` |
| Mobile | `apps/mobile` | Expo/Metro no host (8081), fora do Compose | — | — |

## Endpoints do worker

O `notification-worker` expõe apenas leitura, para demonstração e suporte (não é usado pelo mobile):

| Método | Caminho | Resposta |
|---|---|---|
| GET | `/notifications?bookingId={uuid}` | lista de notificações geradas para a reserva (`id`, `eventId`, `bookingId`, `recipientType`, `recipientId`, `channel`, `message`, `createdAt`) |
| GET | `/actuator/health` | saúde do serviço |

## Ownership dos dados

- **API** é a única dona de `tutors`, `pets`, `caregivers`, `bookings` (ADR-003). Schema só via Flyway em `services/api/src/main/resources/db/migration/`.
- **notification-worker** é dono de `processed_events` e `notifications`, num banco separado, com Flyway próprio em `services/notification-worker/src/main/resources/db/migration/`.
- Nenhum componente lê o banco do outro. O worker recebe tudo o que precisa pelo evento.

## Variáveis de ambiente

| Variável | API | Worker | Observação |
|---|---|---|---|
| `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD` | sim | sim | cada serviço aponta para o próprio banco |
| `SPRING_RABBITMQ_HOST`, `SPRING_RABBITMQ_PORT`, `SPRING_RABBITMQ_USERNAME`, `SPRING_RABBITMQ_PASSWORD` | sim | sim | |
| `SERVER_PORT` | 8080 | 8081 | |
| `OTEL_*` | sim | sim | definidas no `docker-compose.yml` (ADR-005) |
| `EXPO_PUBLIC_API_URL` | — | — | mobile; padrão `http://localhost:8080` |
| `EXPO_PUBLIC_USE_MOCKS` | — | — | mobile; `true` usa dados simulados |

Defaults locais nos `application.yml`: `localhost`, usuário/senha `plantao_pet` / `plantao_pet_local` (API) e `notifications` / `notifications_local` (worker). Nada de credencial real.

## Regras de negócio da reserva

1. `scheduledEnd` > `scheduledStart` (400) e `scheduledStart` no futuro (422).
2. O pet deve pertencer ao tutor (422). O cuidador deve existir (404) e estar ativo (422).
3. O cuidador não pode ter outra reserva `REQUESTED` ou `ACCEPTED` sobreposta no período (409).
4. Transições permitidas (outras → 409):

```text
REQUESTED ──accept──▶ ACCEPTED ──cancel──▶ CANCELLED
    │  └──reject──▶ REJECTED
    └──cancel──▶ CANCELLED
```

5. Cada criação ou transição publica o evento correspondente após o commit.

## Preço (Strategy por tipo de serviço)

Valores em centavos de BRL, calculados pela API na criação e gravados em `bookings.price_cents`.

| `serviceType` | Estratégia | Regra |
|---|---|---|
| `WALK` | `WalkPriceStrategy` | R$ 30,00 por hora iniciada (mínimo 1h) |
| `COMPANIONSHIP` | `CompanionshipPriceStrategy` | R$ 25,00 por hora iniciada (mínimo 1h) |
| `HOME_VISIT` | `HomeVisitPriceStrategy` | R$ 45,00 por dia de calendário (UTC) coberto pelo período |
| `HOSTING` | `HostingPriceStrategy` | R$ 120,00 por diária (24h iniciadas, mínimo 1) |
| `TEMPORARY_CARE` | `TemporaryCarePriceStrategy` | R$ 20,00 por hora iniciada (mínimo 1h) |

Exemplos: `WALK` de 13:00 a 15:00 → 6000; `WALK` de 13:00 a 14:30 → 6000; `HOSTING` de 30h → 24000.

## Dados de demonstração (seed `V4__seed_demo_data.sql`)

IDs fixos, compartilhados com os mocks do mobile. E-mails fictícios em `example.com`.

| Tipo | ID | Nome | Detalhe |
|---|---|---|---|
| Tutor | `11111111-1111-1111-1111-111111111111` | Ana Tutora | ana.tutora@example.com |
| Tutor | `11111111-1111-1111-1111-111111111112` | Bruno Tutor | bruno.tutor@example.com |
| Pet | `22222222-2222-2222-2222-222222222221` | Thor | DOG, LARGE, tutor Ana |
| Pet | `22222222-2222-2222-2222-222222222222` | Mel | DOG, SMALL, tutor Ana |
| Pet | `22222222-2222-2222-2222-222222222223` | Bidu | DOG, MEDIUM, tutor Bruno |
| Cuidador | `33333333-3333-3333-3333-333333333331` | Carla Cuidadora | ativo |
| Cuidador | `33333333-3333-3333-3333-333333333332` | Diego Passeador | ativo |
| Cuidador | `33333333-3333-3333-3333-333333333333` | Elisa Anfitriã | ativo |
| Cuidador | `33333333-3333-3333-3333-333333333334` | Fábio Inativo | inativo |

## Observabilidade

Métricas de negócio (Micrometer `MeterRegistry` injetado, ADR-005):

| Serviço | Contador | Tags |
|---|---|---|
| API | `booking.created`, `booking.accepted`, `booking.rejected`, `booking.cancelled` | `service_type` |
| Worker | `message.processing` | `event_type`, `outcome` = `processed` \| `duplicate` |
| Worker | `message.processing.errors` | `event_type` |

Logs: SLF4J, uma linha por decisão relevante (ex.: `Booking created bookingId=… status=REQUESTED`). O agente OTel anexa `trace_id`.
