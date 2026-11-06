# Testes de banco

Esta suíte valida o schema PostgreSQL de forma isolada, sem depender de a API Spring Boot já estar implementada.

Ela utiliza:

- JUnit 5;
- Testcontainers;
- PostgreSQL 16;
- Flyway;
- driver JDBC PostgreSQL.

As migrations testadas são **as mesmas migrations da aplicação**, localizadas em:

```text
services/api/src/main/resources/db/migration/
```

Não existe cópia das migrations dentro da suíte.

## Pré-requisitos

- Java 17+
- Maven 3.9+
- Docker em execução

## Executar a partir da raiz do repositório

```bash
mvn -f infrastructure/database/tests/pom.xml test
```

## O que é validado

- aplicação completa das migrations;
- existência das tabelas iniciais;
- unicidade de e-mail;
- integridade de FKs;
- `CHECK` de porte do pet;
- `CHECK` de tipo de serviço;
- `CHECK` de status da reserva;
- `CHECK` do período da reserva;
- comportamento de exclusão com `RESTRICT`;
- existência dos índices de relacionamento.

## Observação

Quando o módulo `services/api` possuir seu próprio `pom.xml` e testes de integração, esta suíte poderá continuar como validação independente de schema ou ser incorporada ao pipeline do backend. A decisão deve ser feita sem duplicar testes desnecessariamente.
