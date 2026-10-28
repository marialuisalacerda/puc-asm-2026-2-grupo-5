# ADR-007: Integração contínua e qualidade

Status: Proposto

Data: 2026-10-28

Responsável: Ana Carolina Magalhães (CI/CD e Qualidade)

Revisores: Amir Andrade (C4) + responsáveis por Backend, Mobile e Docker/Observabilidade

## Contexto

O projeto é desenvolvido por 7 integrantes em paralelo, cada um em uma área (mobile, API, banco, mensageria, infraestrutura). Todas as alterações chegam à `develop` por Pull Request, e a `main` só recebe a versão final validada.

Precisamos de uma verificação automática que:

- impeça que uma alteração quebre o build ou os testes de outra área sem que ninguém perceba;
- dê ao revisor do PR um sinal objetivo ("CI verde") antes da aprovação;
- funcione desde já, mesmo com o repositório contendo apenas documentação, e passe a validar cada parte assim que o código dela existir;
- seja gratuita e simples de manter por uma pessoa.

Restrições: o repositório está hospedado no GitHub e é público; a stack definida é Java/Spring Boot com Maven (API e worker), React Native/Expo com TypeScript (mobile) e Docker Compose (ambiente local). Os workflows devem seguir os nomes previstos no README: `backend-ci`, `mobile-ci` e `docker-ci`.

## Opções consideradas

1. **GitHub Actions.**
   1. **Prós:** integrado ao repositório e aos PRs, sem conta ou servidor extra; gratuito para repositório público; os checks aparecem no PR e podem ser exigidos pela branch protection; ampla oferta de actions oficiais (setup-java, setup-node, cache).
   2. **Contras:** acopla o pipeline ao GitHub; depuração depende de rodar o workflow (ou ferramentas como `act`).
2. **GitLab CI / CircleCI / Jenkins.**
   1. **Prós:** recursos equivalentes ou maiores (Jenkins é totalmente configurável).
   2. **Contras:** exigem espelhar o repositório, integrar contas ou manter um servidor (Jenkins); os status dos PRs dependem de integração adicional; custo operacional desproporcional para um projeto acadêmico.
3. **Sem CI, apenas revisão manual.**
   1. **Prós:** nenhum custo inicial.
   2. **Contras:** builds e testes quebrados só aparecem na máquina de outro integrante; o critério "pipeline verde" da Definition of Done fica impossível de verificar.

Sobre a organização dos workflows, também foram consideradas:

- **um workflow único** para tudo: mais simples, mas mistura falhas de áreas diferentes e dificulta saber quem deve corrigir;
- **workflows por área disparados por filtro de caminho (`paths`)**: economizam execuções, mas um check obrigatório que não roda fica "pendente" para sempre e trava o merge;
- **workflows por área que sempre rodam e pulam os passos quando o projeto ainda não existe**: um pouco mais de execução, mas os checks sempre reportam status.

## Decisão

Escolhemos **GitHub Actions** porque já está integrado ao repositório e aos Pull Requests, não tem custo e permite exigir os checks na branch protection.

Os workflows ficam em `.github/workflows/`, um por área:

| Workflow | Valida | Passos |
|---|---|---|
| `backend-ci` | `services/api/` e `services/notification-worker/` | `mvn -B verify` (build e testes), Java 21 |
| `mobile-ci` | `apps/mobile/` | `npm ci`, verificação de tipos, lint, testes, Node 20 |
| `docker-ci` | Dockerfiles e `docker-compose.yml` | `docker build` de cada Dockerfile, `docker compose config` |
| `docs-ci` | arquivos Markdown | verificação de links internos quebrados |

Regras dos workflows:

- disparam em `pull_request` e `push` para `develop` e `main`;
- **sempre rodam**; enquanto o projeto de uma área não existir (por exemplo, sem `pom.xml` ou `package.json`), os passos são pulados e o job termina com sucesso. Assim cada área passa a ser validada automaticamente no PR que criar seu projeto;
- usam permissão mínima (`contents: read`) e cancelam execuções antigas do mesmo PR;
- não usam credenciais; segredos, se necessários no futuro, ficam em GitHub Secrets.

Regras de branch protection para `develop` e `main`:

- exigir Pull Request, com pelo menos 1 aprovação;
- exigir verdes os checks `backend (api)`, `backend (notification-worker)`, `mobile`, `docker` e `docs-links` (nomes dos jobs dos workflows acima);
- exigir branch atualizada antes do merge;
- bloquear push direto e force push;
- `main`: além das anteriores, aceitar PR apenas a partir de `develop` (verificado na revisão) e exigir 2 aprovações.

Todo PR usa o template `.github/pull_request_template.md`, com os campos exigidos no AGENTS.md.

Versões assumidas: **Java 21** e **Node 20**. Se o backend ou o mobile escolherem outra versão, os workflows devem ser atualizados no mesmo PR que criar o projeto.

## Consequências

Positivas: falhas de build e de testes aparecem no próprio PR; o critério "pipeline verde" da Definition of Done passa a ser verificável; cada área ganha CI sem precisar configurar nada, bastando seguir a estrutura de pastas; checks separados indicam qual área quebrou.

Negativas / riscos: todos os workflows rodam em todo PR, inclusive de documentação (custo aceitável em repositório público); as versões de Java e Node precisam ser mantidas em sincronia com os projetos; a branch protection depende de um administrador do repositório; CD (deploy automático) não está no escopo deste ADR, pois não há ambiente de execução definido. Se for definido, será registrado em novo ADR.

Evoluções previstas, quando o código existir: verificação automática das fronteiras entre módulos da API (ArchUnit ou Spring Modulith, conforme o ADR-001), relatório de cobertura de testes e build/push das imagens Docker.

## Impacto no C4

Nível afetado: nenhum.

O que muda: CI/CD é ferramenta de desenvolvimento e não aparece como container ou componente do sistema. Se for adotado deploy automatizado, o diagrama de deployment em `docs/diagrams/` deverá ser atualizado.

## Referências

- GitHub Docs. Understanding GitHub Actions. <https://docs.github.com/en/actions/about-github-actions/understanding-github-actions>
- GitHub Docs. About protected branches. <https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/managing-protected-branches/about-protected-branches>
- FOWLER, Martin. Continuous Integration. 2024. <https://martinfowler.com/articles/continuousIntegration.html>
- HUMBLE, Jez; FARLEY, David. Continuous Delivery. Addison-Wesley, 2010.
