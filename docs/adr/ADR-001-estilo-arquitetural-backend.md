# ADR-001: Estilo arquitetural do backend

Status: Proposto

Data: 2026-10-24

Responsável: Amir Andrade (Arquitetura geral e C4)

Revisores: Amir Andrade (C4) + responsáveis por API e domínio, Banco de dados e Mensageria

## Contexto

Precisamos definir como o backend do PetAmparo é dividido e implantado. O PetAmparo é um marketplace mobile (Android e iOS) em que tutores de cães encontram, contratam e pagam cuidadores por período.

Requisitos que influenciam a decisão:

- **Reserva com estados**: solicitada, aceita, paga, em andamento, concluída, cancelada ou expirada. Reserva, agenda do cuidador e pagamento precisam mudar juntos, com consistência forte.
- **Pagamento no app com split e reembolso**, idempotente e conciliado com o gateway, sem cobrança duplicada.
- **Busca por serviço, período, porte e região** em até 2s no percentil de 95. Ela cruza perfil, agenda, avaliações e localização.
- **Chat** entre tutor e cuidador, com entrega em até 2s.
- **Notificações** por push, e-mail e SMS e **fotos** de até 10 MB redimensionadas, cargas que não devem bloquear o usuário.
- **Disponibilidade** de 99,5% e **500 usuários simultâneos**, crescendo sem mudar a arquitetura.
- **Fornecedores trocáveis** para pagamento, notificação, geolocalização e mídia; LGPD e auditoria das ações do suporte.

Restrições: o backend é mantido por um único time pequeno, com uma pessoa por área. O banco é PostgreSQL, acessado apenas pela API, e as notificações são enviadas por um worker que recebe eventos.

## Opções consideradas

1. **Monólito simples (em camadas).**&#32;
   1. **Prós: **menor custo inicial, um deploy, transações ACID (atomicidade, consistência, isolamento e durabilidade) e busca com uma única consulta.&#32;
   2. **Contras: **camadas técnicas não separam domínios e o acoplamento cresce; tudo escala junto; notificações e fotos ficam presas à requisição do usuário; extrair partes depois exige reescrita, o que conflita com a necessidade de suportar a estimativa de 10 mil usuário cadastrados e 500 simultâneos no lançamento.
2. **Monólito modular com workers assíncronos.**&#32;
   1. **Prós****: **mantém transações ACID (atomicidade, consistência, isolamento e durabilidade), um deploy da API e a busca simples; módulos por subdomínio com fronteiras explícitas; workers isolam notificações e fotos; cada módulo pode virar serviço sem reescrita.&#32;
   2. **Contras: **a separação entre módulos não é garantida pela tecnologia, só se mantém se a equipe respeitar as regras e o pipeline de CI verificar automaticamente que nenhum módulo usa o código interno de outro; como a API roda num único processo, não dá para escalar só uma parte (por exemplo, apenas o chat), e uma falha grave, como falta de memória, derruba todos os módulos ao mesmo tempo; a comunicação com os workers exige um broker de mensagens (serviço intermediário de filas) para operar e monitorar, além de contratos de evento, isto é, formatos de mensagem combinados entre quem publica e quem consome.
3. **Microsserviços.**&#32;
   1. **Prós: **escala, deploy e isolamento de falhas independentes por serviço; Possibilidade de tecnologias diferentes por conta de seus isolamento de serviços.
   2. **Contras: **reserva, agenda e pagamento viram transação distribuída (sagas e compensações); a busca passa a juntar dados de vários bancos, ameaçando os 2s do desempenho esperado; custo operacional alto (gateway, tracing, um pipeline por serviço) para um único time; contraria o banco único acessado só pela API.

## Decisão

Escolhemos o **monólito modular com workers assíncronos** porque ele garante consistência forte no fluxo reserva → agenda → pagamento e busca rápida em um só banco, isola as cargas assíncronas e mantém fronteiras de domínio prontas para extração, com custo operacional compatível com um único time.

A API é dividida nos módulos Contas, Perfis, Busca, Agenda e Reservas, Pagamentos, Chat, Avaliações e Suporte. Cada módulo expõe uma interface pública e não lê tabelas de outro módulo. Integrações externas ficam atrás de interfaces no módulo dono.

## Consequências

Positivas: transações ACID (atomicidade, consistência, isolamento e durabilidade) no fluxo de reserva e pagamento; busca resolvida em um banco; um deploy da API e poucos containers; falhas de fornecedores de notificação e o processamento de fotos não bloqueiam o usuário; módulos podem ser extraídos sem reescrita.

Negativas / riscos: a API escala e falha como um bloco; sem verificação automática das dependências (ex.: ArchUnit ou Spring Modulith), os módulos voltam a se acoplar; eventos exigem idempotência e versionamento. Um módulo só será extraído como serviço, em novo ADR, se surgir carga muito desigual (ex.: chat ou busca), falhas que derrubem a API inteira, um segundo time de backend ou exigência de conformidade em pagamentos.

## Impacto no C4

Nível afetado: C2 e C3

O que muda: no C2, o sistema PetAmparo é aberto nos containers App mobile, API, Notification worker, Media worker, Message broker e Banco PostgreSQL. Setas: App → API (HTTPS/JSON); API → Banco (leitura e escrita, única com acesso); API → Message broker (publica eventos); workers → Message broker (consomem eventos); Notification worker → Serviço de notificação; Media worker → Armazenamento de mídia. No C3, a API ganha os oito módulos como componentes. O C1 não muda.

## Referências

- BROWN, Simon. Modular monoliths. GOTO Berlin 2018. <https://gotopia.tech/sessions/515/modular-monoliths>
- FOWLER, Martin. Monolith first. 2015. <https://martinfowler.com/bliki/MonolithFirst.html>
- FOWLER, Martin. Microservice premium. 2015. <https://martinfowler.com/bliki/MicroservicePremium.html>
- LEWIS, James; FOWLER, Martin. Microservices. 2014. <https://martinfowler.com/articles/microservices.html>
- NEWMAN, Sam. Monolith to microservices. O'Reilly, 2019.
- NYGARD, Michael. Documenting architecture decisions. 2011. <https://cognitect.com/blog/2011/11/15/documenting-architecture-decisions>
- RICHARDS, Mark; FORD, Neal. Fundamentals of software architecture. O'Reilly, 2020.
- ROCKETSEAT. Monolito vs. microsserviços: como escolher sua arquitetura. <https://www.rocketseat.com.br/blog/artigos/post/monolito-vs-microservices-como-escolher-arquitetura>
- WESTEINDE, Kirsten. Deconstructing the monolith. Shopify Engineering, 2019. <https://shopify.engineering/deconstructing-monolith-designing-software-maximizes-developer-productivity>
