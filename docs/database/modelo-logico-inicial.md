# Modelo lógico inicial do banco

## Status

**Proposta para revisão.**

Este documento detalha uma primeira versão do modelo lógico derivado do domínio atualmente documentado no projeto. Como os contratos definitivos da API ainda não estão implementados no repositório, os atributos abaixo devem ser revisados em conjunto com o responsável pelo backend antes de o modelo ser tratado como contrato definitivo.

## Premissas propostas

- PostgreSQL é o banco relacional principal.
- Identificadores são `UUID` e são gerados pela aplicação.
- Datas de auditoria usam `TIMESTAMPTZ`.
- Nomes físicos usam `snake_case`.
- As tabelas usam nomes no plural.
- Valores de status e tipo de serviço são protegidos no banco por `CHECK`, sem utilizar `ENUM` nativo do PostgreSQL, para facilitar evolução por migrations.
- O banco protege invariantes estruturais; regras de negócio continuam no domínio/aplicação.

## Entidades

### `tutors`

| Coluna | Tipo | Regras |
|---|---|---|
| `id` | `UUID` | PK, NOT NULL |
| `name` | `VARCHAR(120)` | NOT NULL |
| `email` | `VARCHAR(254)` | NOT NULL, UNIQUE |
| `created_at` | `TIMESTAMPTZ` | NOT NULL, default `CURRENT_TIMESTAMP` |
| `updated_at` | `TIMESTAMPTZ` | NOT NULL, default `CURRENT_TIMESTAMP` |

### `pets`

| Coluna | Tipo | Regras |
|---|---|---|
| `id` | `UUID` | PK, NOT NULL |
| `tutor_id` | `UUID` | NOT NULL, FK → `tutors(id)` |
| `name` | `VARCHAR(80)` | NOT NULL |
| `species` | `VARCHAR(40)` | NOT NULL |
| `size` | `VARCHAR(20)` | NULL; quando preenchido: `SMALL`, `MEDIUM`, `LARGE` |
| `notes` | `VARCHAR(500)` | NULL |
| `created_at` | `TIMESTAMPTZ` | NOT NULL, default `CURRENT_TIMESTAMP` |
| `updated_at` | `TIMESTAMPTZ` | NOT NULL, default `CURRENT_TIMESTAMP` |

### `caregivers`

| Coluna | Tipo | Regras |
|---|---|---|
| `id` | `UUID` | PK, NOT NULL |
| `name` | `VARCHAR(120)` | NOT NULL |
| `email` | `VARCHAR(254)` | NOT NULL, UNIQUE |
| `active` | `BOOLEAN` | NOT NULL, default `TRUE` |
| `created_at` | `TIMESTAMPTZ` | NOT NULL, default `CURRENT_TIMESTAMP` |
| `updated_at` | `TIMESTAMPTZ` | NOT NULL, default `CURRENT_TIMESTAMP` |

### `bookings`

| Coluna | Tipo | Regras |
|---|---|---|
| `id` | `UUID` | PK, NOT NULL |
| `tutor_id` | `UUID` | NOT NULL, FK → `tutors(id)` |
| `pet_id` | `UUID` | NOT NULL, FK → `pets(id)` |
| `caregiver_id` | `UUID` | NOT NULL, FK → `caregivers(id)` |
| `service_type` | `VARCHAR(30)` | NOT NULL, CHECK |
| `status` | `VARCHAR(30)` | NOT NULL, CHECK |
| `scheduled_start` | `TIMESTAMPTZ` | NOT NULL |
| `scheduled_end` | `TIMESTAMPTZ` | NOT NULL |
| `created_at` | `TIMESTAMPTZ` | NOT NULL, default `CURRENT_TIMESTAMP` |
| `updated_at` | `TIMESTAMPTZ` | NOT NULL, default `CURRENT_TIMESTAMP` |

## Valores inicialmente aceitos

### `bookings.service_type`

Os valores derivam dos serviços atualmente descritos no README:

- `WALK`
- `COMPANIONSHIP`
- `HOME_VISIT`
- `HOSTING`
- `TEMPORARY_CARE`

### `bookings.status`

Os estados abaixo seguem o fluxo mais completo atualmente descrito no ADR-001:

- `REQUESTED`
- `ACCEPTED`
- `PAID`
- `IN_PROGRESS`
- `COMPLETED`
- `REJECTED`
- `CANCELLED`
- `EXPIRED`

> A presença de `PAID`, `IN_PROGRESS`, `COMPLETED` e `EXPIRED` precisa ser confirmada com o backend antes de os contratos da API serem considerados estáveis.

## Relacionamentos

- Um `Tutor` possui zero ou muitos `Pet`.
- Um `Pet` pertence a exatamente um `Tutor`.
- Um `Tutor` possui zero ou muitas `Booking`.
- Uma `Booking` pertence a exatamente um `Tutor`.
- Uma `Booking` referencia exatamente um `Pet`.
- Uma `Booking` referencia exatamente um `Caregiver`.
- Um `Caregiver` pode participar de zero ou muitas `Booking`.

## Integridade

A migration inicial protege:

- PKs das quatro entidades;
- unicidade de e-mail em tutor e cuidador;
- FKs obrigatórias;
- período de reserva com `scheduled_end > scheduled_start`;
- valores válidos de `service_type`;
- valores válidos de `status`;
- valores válidos de `pets.size`.

Não foi criada uma constraint que garanta diretamente que `bookings.tutor_id` seja o mesmo tutor dono de `bookings.pet_id`. Essa é uma decisão que merece revisão: pode ser protegida por modelagem adicional/constraint composta ou tratada pela camada de domínio.

## Índices iniciais

PostgreSQL não cria automaticamente índices nas colunas que apenas referenciam FKs. Por isso a segunda migration adiciona índices somente para os relacionamentos:

- `pets(tutor_id)`
- `bookings(tutor_id)`
- `bookings(pet_id)`
- `bookings(caregiver_id)`

Índices adicionais por status, período ou ordenação devem ser adicionados somente quando os contratos de consulta estiverem definidos.

## Pontos obrigatórios de revisão antes do merge

1. Confirmar UUID como estratégia de identificação.
2. Confirmar lista de status de Booking com o backend.
3. Confirmar lista de tipos de serviço.
4. Confirmar se `species` e `size` pertencem à primeira versão.
5. Decidir se o banco deve garantir que o pet da reserva pertence ao tutor da reserva.
6. Confirmar política de exclusão (`RESTRICT`, soft delete etc.).
7. Confirmar se e-mail é realmente identificador único no domínio.
