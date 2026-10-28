# DER inicial — PetAmparo

> Proposta para revisão junto ao responsável pelo backend antes de ser considerada contrato definitivo.

```mermaid
erDiagram
    TUTORS ||--o{ PETS : possui
    TUTORS ||--o{ BOOKINGS : solicita
    PETS ||--o{ BOOKINGS : participa
    CAREGIVERS ||--o{ BOOKINGS : atende

    TUTORS {
        uuid id PK
        varchar name
        varchar email UK
        timestamptz created_at
        timestamptz updated_at
    }

    PETS {
        uuid id PK
        uuid tutor_id FK
        varchar name
        varchar species
        varchar size
        varchar notes
        timestamptz created_at
        timestamptz updated_at
    }

    CAREGIVERS {
        uuid id PK
        varchar name
        varchar email UK
        boolean active
        timestamptz created_at
        timestamptz updated_at
    }

    BOOKINGS {
        uuid id PK
        uuid tutor_id FK
        uuid pet_id FK
        uuid caregiver_id FK
        varchar service_type
        varchar status
        timestamptz scheduled_start
        timestamptz scheduled_end
        timestamptz created_at
        timestamptz updated_at
    }
```

## Observações

- `Booking` mantém referências explícitas para tutor, pet e cuidador para refletir o fluxo atualmente documentado.
- A migration protege o período com `scheduled_end > scheduled_start`.
- A consistência entre `bookings.tutor_id` e o tutor proprietário de `bookings.pet_id` ainda está marcada como ponto de decisão arquitetural/modelagem.
- O diagrama representa o modelo lógico proposto; regras de negócio de transição entre status pertencem ao domínio da aplicação.
