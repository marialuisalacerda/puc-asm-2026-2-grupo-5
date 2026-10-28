# Revisão do pacote de banco

Este pacote foi preparado para ser extraído diretamente na raiz do repositório.

## Arquivos adicionados/atualizados

### Decisão e documentação

- `docs/adr/ADR-003-persistencia.md` — versão revisada do ADR-003.
- `docs/database/modelo-logico-inicial.md` — proposta de modelo lógico.
- `docs/diagrams/DER-inicial.md` — DER em Mermaid.

### Migrations

- `services/api/src/main/resources/db/migration/V1__create_initial_schema.sql`
- `services/api/src/main/resources/db/migration/V2__add_relationship_indexes.sql`

### Ambiente local de banco

- `infrastructure/database/docker-compose.yml`
- `infrastructure/database/.env.example`

### Testes

- `infrastructure/database/tests/` — suíte Maven/JUnit/Testcontainers/Flyway.

## Itens que precisam de revisão humana antes do merge

1. **ADR-003:** manter `Status: Proposto` até revisão da Arquitetura/C4 e Backend; depois alterar para `Aceito`.
2. **UUID:** confirmar se será a estratégia oficial de IDs.
3. **Booking.status:** confirmar a lista com o responsável pelo backend.
4. **Booking.service_type:** confirmar a lista com os contratos da API.
5. **Pet:** confirmar `species`, `size` e seus formatos.
6. **E-mail único:** confirmar se a unicidade pertence ao domínio de Tutor/Caregiver.
7. **Exclusão:** confirmar política `RESTRICT` versus soft delete.
8. **Tutor do pet na reserva:** decidir se o banco também deverá garantir que o `pet_id` pertence ao mesmo `tutor_id` da reserva.
9. **Compose global:** quando o `docker-compose.yml` raiz existir, integrar este serviço PostgreSQL a ele e evitar dois ambientes concorrentes.

## Como validar

### 1. Executar a suíte automatizada

Com Docker iniciado:

```bash
mvn -f infrastructure/database/tests/pom.xml test
```

### 2. Subir somente o PostgreSQL local

```bash
docker compose -f infrastructure/database/docker-compose.yml up -d
```

### 3. Derrubar o PostgreSQL local

```bash
docker compose -f infrastructure/database/docker-compose.yml down
```

Para remover também o volume de desenvolvimento:

```bash
docker compose -f infrastructure/database/docker-compose.yml down -v
```

## Sugestão de branch

```text
feat/<issue>-modelo-inicial-banco
```

## Sugestões de commits

Se preferir separar documentação, schema e testes:

```text
docs(database): finaliza ADR de persistência e documenta modelo inicial
feat(database): adiciona migrations do schema inicial
test(database): adiciona suíte de testes de persistência
infra(database): adiciona PostgreSQL para desenvolvimento local
```

O destino do Pull Request deve ser `develop`.
