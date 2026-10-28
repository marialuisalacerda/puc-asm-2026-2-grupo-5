# ADR-006: Containerização e ambiente local

Status: Proposto
Data: 2026-10-28
Responsável: João Pedro Schlindwein (Docker e Observabilidade)
Revisores: Amir Gabriel Dantas Santos Andrade (Arquitetura e C4) + Ana Carolina Magalhães (CI/CD)

## Contexto
Sete integrantes desenvolvem partes diferentes (API, worker, banco, mensageria,
observabilidade) em máquinas distintas. Precisamos que:

- o ambiente seja reproduzível e suba com um único comando (`docker compose up -d`);
- cada integrante consiga trabalhar mesmo que os outros componentes ainda não existam;
- nenhuma credencial real seja versionada;
- o pipeline de CI (ADR-007) consiga validar imagens e o Compose;
- a apresentação gravada não dependa de instalações locais de Java, PostgreSQL ou RabbitMQ.

## Opções consideradas
1. **Instalação manual de cada dependência**: sem overhead de containers, mas cada máquina
   fica diferente e o onboarding é lento.
2. **Docker + Docker Compose**: um arquivo descreve todo o ambiente; padrão de mercado;
   suportado pelo GitHub Actions. Contra: consumo de memória e curva de aprendizado.
3. **Kubernetes local (kind/minikube)**: próximo de produção, mas complexidade
   desproporcional ao escopo acadêmico e ao monolito modular (ADR-001).

## Decisão
Escolhemos **Docker + Docker Compose** (opção 2).

- **`docker-compose.yml` na raiz** com todos os componentes do backend: API,
  `notification-worker`, PostgreSQL, RabbitMQ e a stack de observabilidade (ADR-005).
- **Profiles:** infraestrutura e observabilidade sobem por padrão; `api` e
  `notification-worker` ficam no profile `app` (`docker compose --profile app up -d`).
  Assim o ambiente funciona antes dos projetos Maven existirem.
- **Dockerfile único para serviços Java** (`infrastructure/docker/java-service.dockerfile`):
  multi-stage (Maven → JRE Alpine), cache de dependências, usuário não-root e
  OpenTelemetry Java Agent embutido. O contexto de build é a pasta de cada serviço.
- **Configuração por variáveis de ambiente:** `.env.example` versionado com valores
  fictícios; `.env` real é ignorado pelo Git. Nomes do banco seguem `docs/database/README.md`
  (`DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`).
- **Ownership de dados (ADR-003):** dois PostgreSQL separados no Compose — `postgres` (dono: API) e `notification-db` (dono: `notification-worker`); nenhum serviço recebe credenciais do banco do outro.
- **Healthchecks** em PostgreSQL e RabbitMQ; serviços Java só iniciam com as dependências saudáveis.
- **Versões de imagens fixadas** (sem `latest`) para reprodutibilidade.
- O mobile (Expo) **não** é containerizado: roda com o tooling do Expo apontando para a API exposta pelo Compose.

## Consequências
Positivas:
- onboarding em um comando; mesmo ambiente na máquina de todos e no CI;
- integrantes podem desenvolver contra serviços reais (banco, broker) sem instalá-los;
- imagens dos serviços Java padronizadas, já instrumentadas.

Negativas / riscos:
- consumo de memória da stack completa (~2–3 GB);
- conflitos de porta com outros projetos locais — mitigado por portas configuráveis no `.env`;
- portas do host escolhidas para não colidir com o Metro do Expo (8081): worker publicado na 8082;
- o Dockerfile compartilhado assume projetos Maven com `pom.xml` e `src/` na raiz do serviço;
  serviços com outra estrutura precisarão de Dockerfile próprio.

## Impacto no C4
Nível afetado: C2 (e diagrama de deployment em `docs/diagrams/`)
O que muda: os containers do C2 passam a corresponder 1:1 aos serviços do `docker-compose.yml`;
o diagrama de deployment representa o host Docker com a rede `backend`.

## Referências
- [Guia de Docker](../../infrastructure/docker/README.md)
- Docker Compose profiles: <https://docs.docker.com/compose/how-tos/profiles/>
- Dockerfile best practices: <https://docs.docker.com/build/building/best-practices/>
