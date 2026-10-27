# ADR-003 — Estratégia de persistência

## Status

Proposto

## Data

2026-10-23

## Responsável

_(integrante 4: Banco de dados)_

## Revisores

_(a definir)_

## Contexto

O PetAmparo conecta tutores de pets a cuidadores e precisa persistir dados de tutores, pets, cuidadores e reservas.

A API principal será implementada em Java com Spring Boot.

A solução precisa garantir:

- integridade dos dados e relacionamentos consistentes entre as entidades;
- evolução controlada e versionada do schema;
- reprodutibilidade do ambiente entre os integrantes;
- separação clara de ownership dos dados.

Também é necessário evitar que diferentes componentes mantenham ou alterem o mesmo schema de forma independente.

## Opções consideradas

### Banco de dados

- **PostgreSQL**: relacional, transacional, integração madura com Spring Boot.
- **MySQL**: relacional maduro e amplamente utilizado.
- **SQLite**: configuração simples e baixo custo para ambiente local, mas pouco adequado a backend em containers, relacionamentos transacionais e um ambiente próximo de uma aplicação distribuída.

> **Nota para o autor:** o motivo de rejeição do MySQL na versão original ("o projeto já definiu PostgreSQL como tecnologia principal") é circular, pois é justamente este ADR que define isso. Sugestão: trocar por critérios reais que diferenciem as opções, por exemplo:
> - suporte a busca geográfica (PostGIS) para encontrar cuidadores por região;
> - suporte a tipos JSON/JSONB para dados semiestruturados do perfil;
> - familiaridade da equipe com cada banco.
>
> Cabe a você avaliar quais desses critérios realmente se aplicam ao projeto.

### Evolução do schema

- **Flyway**: migrations versionadas em SQL, integradas ao Spring Boot.
- **Alterações manuais sem ferramenta de migration**: reduzem o trabalho inicial, mas dificultam rastreabilidade, reprodução do ambiente e integração entre integrantes.

## Decisão

1. Usar **PostgreSQL** como banco de dados relacional principal.
2. Acessar o banco pela API Java/Spring Boot com **Spring Data JPA**.
3. Versionar as alterações estruturais com **Flyway**. As migrations do repositório são a única fonte de evolução do schema.
4. **Ownership dos dados:** somente a API acessa o banco do domínio principal. Outros componentes, como o `notification-worker`, não acessam diretamente as tabelas da API e se comunicam com ela por contratos definidos (APIs ou eventos).

As convenções de uso (estrutura de pastas, regras de migrations, seeds, constraints, índices, configuração e testes) estão no [Guia de persistência](../database/README.md).

## Consequências

### Positivas

- schema versionado junto com o código e com histórico das alterações;
- maior reprodutibilidade entre ambientes e menor risco de divergência entre bancos locais;
- integração natural com Spring Boot;
- suporte a integridade referencial e transações;
- alterações de schema revisáveis por Pull Request;
- menor acoplamento entre componentes, já que ninguém além da API depende do schema.

### Negativas

- toda alteração estrutural exige uma nova migration;
- migrations já compartilhadas não podem ser alteradas;
- mudanças incompatíveis exigem planejamento de migração;
- testes de integração com PostgreSQL custam mais que testes puramente unitários;
- o `notification-worker` depende de eventos/contratos para obter dados, em vez de consultar o banco.

## Impacto no C4

- **C2 (Containers):** adicionar o container **Banco de dados (PostgreSQL)**.
- Apenas o container **API** tem relação com o banco ("Lê e grava dados do domínio, via JPA").
- O **notification-worker** não tem seta para o banco; ele recebe dados por eventos (ver ADR de mensageria).
- **C3 da API:** os repositories (Spring Data JPA) são os componentes que acessam o banco.

## Referências

- [Guia de persistência](../database/README.md)
- DER: `docs/diagrams/`
- [Documentação do Flyway](https://documentation.red-gate.com/flyway)
- [Spring Data JPA](https://spring.io/projects/spring-data-jpa)
- Michael Nygard, [Documenting Architecture Decisions](https://cognitect.com/blog/2011/11/15/documenting-architecture-decisions)
