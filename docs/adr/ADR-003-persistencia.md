# ADR-003 — Estratégia de persistência

## Status

Proposto

## Data

2026-10-23

## Responsável

Maria Luísa Lacerda — Banco de dados

## Revisores

Amir Andrade — Arquitetura e C4
Responsável pelo Backend

## Contexto

O PetAmparo precisa persistir dados relacionados a tutores, pets, cuidadores e reservas, mantendo integridade entre as entidades e permitindo a evolução controlada do modelo de dados.

A arquitetura do backend adota uma API Java com Spring Boot organizada como monólito modular. O fluxo principal de reservas exige consistência entre dados relacionados e a solução utiliza um único banco relacional acessado pela API.

A estratégia de persistência precisa garantir:

- integridade dos dados e dos relacionamentos;
- suporte a transações para operações que exigem consistência forte;
- evolução controlada e versionada do schema;
- reprodutibilidade do banco entre os ambientes dos integrantes;
- integração com a API Java/Spring Boot;
- separação clara de ownership dos dados;
- rastreabilidade das alterações estruturais;
- execução em ambiente local e containerizado;
- ausência de credenciais versionadas no repositório.

Também é necessário impedir que componentes independentes, como workers, dependam diretamente das tabelas internas da API.

## Critérios de decisão

As alternativas de banco de dados foram avaliadas considerando:

1. suporte a transações e consistência;
2. integridade referencial e constraints;
3. compatibilidade com Spring Boot e Spring Data JPA;
4. integração com migrations versionadas por Flyway;
5. execução reproduzível em containers;
6. adequação ao modelo relacional do domínio;
7. capacidade de evolução do modelo e das consultas;
8. custo operacional compatível com o escopo acadêmico e o tamanho da equipe.

## Opções consideradas

### Banco de dados

#### PostgreSQL

PostgreSQL atende aos requisitos atuais de persistência do PetAmparo, oferecendo suporte a transações, integridade referencial, constraints, índices e consultas relacionais.

Sua integração com Java, Spring Boot, Spring Data JPA e Flyway é madura e permite que o schema seja gerenciado de forma versionada e reproduzível.

Além do conjunto necessário para a primeira versão, PostgreSQL oferece margem para evolução do modelo por meio de recursos como estratégias adicionais de indexação, consultas SQL avançadas, suporte a dados semiestruturados e extensões especializadas.

Esses recursos adicionais não são requisitos obrigatórios da versão atual. Eles são considerados uma capacidade de evolução e não a justificativa exclusiva da escolha.

#### MySQL

MySQL também é uma alternativa tecnicamente viável.

Ele oferece banco relacional transacional, integridade referencial, índices e integração consolidada com Spring Boot, Spring Data JPA e Flyway. Portanto, os requisitos atuais do PetAmparo poderiam ser implementados utilizando MySQL sem alteração relevante na arquitetura da aplicação.

A alternativa não foi descartada por incapacidade técnica.

Para os requisitos atualmente identificados, não existe uma característica específica do MySQL que produza benefício arquitetural relevante sobre PostgreSQL. Como as duas alternativas possuem complexidade operacional semelhante neste projeto, a equipe prefere padronizar a persistência em PostgreSQL, que atende integralmente às necessidades atuais e oferece boa margem para evolução de consultas e estruturas de dados sem introduzir uma segunda tecnologia.

#### SQLite

SQLite possui baixo custo de configuração e é adequado a cenários simples ou aplicações com armazenamento local.

Entretanto, não representa adequadamente o ambiente previsto para o PetAmparo. A solução possui uma API backend executada como aplicação servidor, com requisições concorrentes, relacionamentos entre entidades e execução em containers. O banco também deverá funcionar como componente independente do processo da aplicação.

Por esses motivos, SQLite não foi selecionado como banco principal.

### Síntese da avaliação

| Critério | PostgreSQL | MySQL | SQLite |
|---|---|---|---|
| Modelo relacional | Atende | Atende | Atende |
| Transações | Atende | Atende | Atende, com limitações de cenário |
| Integridade referencial | Atende | Atende | Atende |
| Spring Data JPA | Integração madura | Integração madura | Possível |
| Flyway | Integração madura | Integração madura | Possível |
| Banco servidor independente | Sim | Sim | Não é seu principal cenário |
| Backend concorrente em containers | Adequado | Adequado | Menos adequado |
| Evolução de consultas/modelo | Alta | Alta | Mais limitada para o cenário pretendido |
| Adequação à arquitetura definida | Alta | Alta | Baixa |

PostgreSQL e MySQL foram considerados tecnicamente adequados. A decisão por PostgreSQL não decorre de uma incapacidade do MySQL, mas da opção de padronização em uma tecnologia que atende integralmente aos requisitos atuais e preserva boa margem de evolução.

### Evolução do schema

#### Flyway

Flyway permite manter migrations SQL versionadas junto ao código-fonte e aplicá-las em ordem controlada e reproduzível.

As alterações de schema podem ser revisadas por Pull Request e executadas de maneira consistente nos diferentes ambientes.

#### Alterações manuais sem ferramenta de migration

Alterações manuais reduziriam o trabalho inicial, porém dificultariam:

- rastreabilidade;
- reprodução do ambiente;
- sincronização do schema entre integrantes;
- automação em CI/CD;
- identificação da versão atual do banco.

Por esses motivos, essa alternativa foi descartada.

## Decisão

1. Adotar **PostgreSQL** como banco de dados relacional principal.
2. Realizar o acesso à persistência pela **API Java/Spring Boot**, utilizando **Spring Data JPA**.
3. Versionar todas as alterações estruturais com **Flyway**.
4. Considerar as migrations versionadas no repositório como a **única fonte oficial de evolução do schema**.
5. Armazenar as migrations em:

   ```text
   services/api/src/main/resources/db/migration/
   ```

6. O banco do domínio principal pertence à **API**. Componentes externos, incluindo `notification-worker` e outros workers, não acessam diretamente suas tabelas. Esses componentes obtêm dados por contratos explícitos, como APIs ou eventos.
7. Dentro do monólito modular, o ownership dos dados deverá respeitar as fronteiras dos módulos. Um módulo não deverá depender diretamente das estruturas internas de persistência pertencentes a outro módulo.
8. Constraints de persistência deverão proteger invariantes estruturais por meio de `PRIMARY KEY`, `FOREIGN KEY`, `NOT NULL`, `UNIQUE` e `CHECK`, quando aplicável.
9. Regras exclusivamente de negócio permanecerão na camada de domínio/aplicação.
10. Índices serão criados quando houver padrão de acesso justificável, especialmente para relacionamentos e consultas efetivamente utilizadas. Índices não serão adicionados apenas por antecipação.
11. Credenciais e strings de conexão reais não serão armazenadas no repositório. Configurações sensíveis serão fornecidas por variáveis de ambiente.

As convenções operacionais estão documentadas no [Guia de persistência](../database/README.MD).

## Consequências

### Positivas

- schema versionado junto com o código;
- histórico rastreável das alterações estruturais;
- maior reprodutibilidade entre ambientes;
- redução do risco de divergência entre bancos locais;
- suporte a integridade referencial e transações;
- integração direta com Spring Boot, JPA e Flyway;
- migrations revisáveis por Pull Request;
- separação clara entre o banco da API e os workers;
- preservação das fronteiras internas do monólito modular;
- possibilidade de validar o schema automaticamente em testes e CI.

### Negativas e riscos

- toda alteração estrutural exige nova migration;
- migrations compartilhadas não deverão ser reescritas;
- mudanças incompatíveis exigem planejamento de migração;
- testes de integração com PostgreSQL custam mais que testes puramente unitários;
- workers dependem de APIs ou eventos para obter dados;
- um banco único exige disciplina para que módulos não violem ownership;
- índices e constraints inadequados podem prejudicar desempenho ou evolução do schema.

## Impacto no C4

### C2 — Containers

O modelo deverá representar o container **Banco de dados — PostgreSQL**.

Somente o container **API** terá relação direta com o PostgreSQL:

```text
API → PostgreSQL
Lê e grava dados do domínio por meio da camada de persistência.
```

Workers não terão relação direta com o banco.

A comunicação assíncrona seguirá o fluxo:

```text
API
 |
 v
Message Broker
 |
 v
Worker
```

### C3 — Componentes da API

Repositories implementados com Spring Data JPA serão os componentes responsáveis pelo acesso ao PostgreSQL.

As fronteiras de ownership dos dados deverão permanecer coerentes com os módulos definidos para o backend.

## Implementação decorrente desta decisão

Após a aprovação deste ADR, a implementação da persistência deverá contemplar:

1. modelagem conceitual e lógica das entidades iniciais;
2. DER documentado em `docs/diagrams/`;
3. configuração do PostgreSQL para desenvolvimento local;
4. integração do Flyway com a API;
5. criação da migration inicial do schema;
6. definição e validação das constraints;
7. definição de índices justificados;
8. configuração por variáveis de ambiente;
9. testes automatizados de migrations e integridade;
10. atualização da documentação conforme o modelo evoluir.

O modelo inicial deverá contemplar, no mínimo:

```text
Tutor
  |
  +---- Pet

Tutor
  |
  +---- Booking
          |
          +---- Caregiver
          |
          +---- Pet
```

Os atributos, cardinalidades, constraints e índices serão detalhados no modelo de dados e no DER.

## Referências

- [Guia de persistência](../database/README.MD)
- [`docs/diagrams/`] — DER e diagramas do modelo de dados
- ADR-001 — Estilo arquitetural do backend
- Documentação do Flyway
- Spring Data JPA
- Michael Nygard, *Documenting Architecture Decisions*
