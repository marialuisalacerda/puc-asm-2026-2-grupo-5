# API — Plantão de Pet

API REST (Java 21, Spring Boot 3.3, Maven) que é dona dos dados de tutores, pets, cuidadores e reservas
(ADR-003) e publica eventos de reserva no RabbitMQ (`booking.events`).

Contratos: [OpenAPI](../../docs/api/openapi.yaml) · [Eventos](../../docs/api/eventos.md) ·
[Contratos de integração](../../docs/architecture/contratos-integracao.md).

## Estrutura

```text
src/main/java/br/pucminas/plantaopet/api/
├── controller/   # REST + DTOs (controller/dto)
├── service/      # casos de uso (BookingService, TutorService, PetService, CaregiverService)
├── domain/       # entidades JPA, enums, regras de transição da reserva, evento de domínio
├── repository/   # Spring Data JPA (Repository Pattern)
├── pricing/      # PriceStrategy + uma estratégia por ServiceType + PriceCalculator (Strategy Pattern)
├── messaging/    # BookingEventPublisher (porta) + RabbitBookingEventPublisher + relay AFTER_COMMIT
├── config/       # Clock, IdGenerator, CORS, exchange
└── exception/    # ApiExceptionHandler (RFC 7807 / ProblemDetail)
src/main/resources/db/migration/  # Flyway V1..V5 (V3 preço, V4 seeds, V5 índice de sobreposição)
```

Fluxo de uma reserva: `Controller → BookingService → Booking (domínio) → BookingRepository`; o serviço emite um
`BookingDomainEvent` (Spring) e `BookingEventRelay` o publica no RabbitMQ **somente após o commit**
(`@TransactionalEventListener(AFTER_COMMIT)`).

## Como executar

Pré-requisitos: JDK 21, Maven 3.9+, PostgreSQL e RabbitMQ (pelo `docker compose` da raiz).

```bash
docker compose up -d postgres rabbitmq        # na raiz do repositório
mvn -f services/api/pom.xml spring-boot:run   # API em http://localhost:8080
```

Ou tudo em containers: `docker compose --profile app up -d` (imagem gerada por
`infrastructure/docker/java-service.dockerfile`).

### Variáveis de ambiente

| Variável | Padrão local |
|---|---|
| `DB_HOST` / `DB_PORT` / `DB_NAME` | `localhost` / `5432` / `plantao_pet` |
| `DB_USERNAME` / `DB_PASSWORD` | `plantao_pet` / `plantao_pet_local` |
| `SPRING_RABBITMQ_HOST` / `_PORT` / `_USERNAME` / `_PASSWORD` | `localhost` / `5672` / `plantao_pet` / `plantao_pet_local` |
| `SERVER_PORT` | `8080` |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:8081,http://localhost:19006` (Expo web) |

Os valores padrão são apenas para desenvolvimento local; não há credenciais reais no repositório.

## Endpoints

| Método | Caminho | Descrição |
|---|---|---|
| GET/POST | `/tutors` | lista / cadastra tutor (409 se e-mail já existe) |
| GET | `/tutors/{id}` · `/tutors/{id}/pets` | tutor · pets do tutor |
| POST | `/pets` · GET `/pets/{id}` | cadastra / consulta pet |
| GET | `/caregivers?active=true` · `/caregivers/{id}` | lista (filtra por `active`, padrão `true`) / consulta |
| GET | `/bookings/quote?serviceType=&scheduledStart=&scheduledEnd=` | preço calculado pela Strategy |
| POST | `/bookings` | cria reserva `REQUESTED` (publica `BookingCreated`) |
| GET | `/bookings?tutorId=&caregiverId=&status=` · `/bookings/{id}` | lista (mais recentes primeiro) / consulta |
| PATCH | `/bookings/{id}/accept` · `/reject` · `/cancel` | transições (publicam o evento correspondente) |

Operação: `/actuator/health`, `/actuator/info`, `/actuator/prometheus`; Swagger UI em `/swagger-ui.html`
(gerado pelo springdoc a partir do código; a fonte da verdade continua sendo `docs/api/openapi.yaml`).

Erros seguem RFC 7807 (`application/problem+json`): 400 (com `errors: [{field, message}]`), 404, 409, 422.

Exemplo:

```bash
curl -X POST http://localhost:8080/bookings -H 'Content-Type: application/json' -d '{
  "tutorId": "11111111-1111-1111-1111-111111111111",
  "petId": "22222222-2222-2222-2222-222222222221",
  "caregiverId": "33333333-3333-3333-3333-333333333332",
  "serviceType": "WALK",
  "scheduledStart": "2026-12-02T13:00:00Z",
  "scheduledEnd": "2026-12-02T15:00:00Z"
}'
```

## Métricas e logs

Contadores Micrometer `booking.created`, `booking.accepted`, `booking.rejected`, `booking.cancelled`
com a tag `service_type` (ADR-005). Logs SLF4J por decisão relevante, por exemplo
`Booking created bookingId=… status=REQUESTED` e `Booking event published eventId=…`.

## Testes

```bash
mvn -f services/api/pom.xml test     # unitários (Mockito) e @WebMvcTest
mvn -f services/api/pom.xml verify   # + integração (*IT) com Testcontainers: PostgreSQL + RabbitMQ
```

O `verify` exige Docker em execução. O Testcontainers foi fixado em 1.21.4 por compatibilidade com o
Docker Engine 29.

## Decisões e limitações

- **Publicação após o commit sem outbox:** se o RabbitMQ estiver fora no momento do envio, a reserva fica
  gravada e o evento é registrado em log de erro, mas não é reenviado. A evolução natural é um
  Transactional Outbox (precisa de ADR).
- **Concorrência:** a criação trava a linha do cuidador (`SELECT … FOR UPDATE`) para que duas reservas
  sobrepostas não passem juntas pela verificação de conflito; transições travam a linha da reserva.
- **Sobreposição:** períodos `[início, fim)`; reservas encostadas (fim = início) não conflitam.
- **HOME_VISIT:** conta os dias de calendário UTC de `[início, fim)`; terminar exatamente à meia-noite não
  cobra o dia seguinte.
