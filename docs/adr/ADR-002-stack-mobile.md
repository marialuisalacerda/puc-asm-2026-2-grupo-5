# ADR-002: Stack do aplicativo mobile

Status: Proposto

Data: 2026-10-28

Responsável: [a definir] (Mobile)

Revisores: Amir Andrade (C4) + responsáveis por Backend e CI/CD e Qualidade

## Contexto

O Plantão de Pet precisa de um aplicativo para Android e iOS em que tutores cadastram seus cães, encontram cuidadores e solicitam reservas, e em que cuidadores aceitam ou recusam as solicitações. O app conversa apenas com a API (ADR-001) pelo contrato REST em `docs/api/openapi.yaml`.

Requisitos e restrições que influenciam a decisão:

- **Multiplataforma**: Android e iOS a partir de um único código, mantido por uma pessoa da equipe.
- **Desenvolvimento em paralelo**: o app deve evoluir antes da API ficar pronta, dependendo do contrato e não da implementação (AGENTS.md), com dados simulados que usem os mesmos IDs do seed da API.
- **Validação automática**: o workflow `mobile-ci` (ADR-007) executa `npm ci`, verificação de tipos, lint e testes em Node 20, sem emulador.
- **Demonstração simples**: os avaliadores e colegas devem conseguir rodar o app sem configurar SDKs nativos (Android Studio/Xcode).
- **Escopo acadêmico**: as funcionalidades são telas de formulário e listas; não há uso intenso de recursos nativos (câmera, Bluetooth, gráficos 3D).

## Opções consideradas

1. **React Native + Expo (TypeScript).**
   1. **Prós:** um código para Android, iOS e web; Expo Go e `npx expo start` dispensam Android Studio/Xcode para desenvolver e demonstrar; TypeScript permite espelhar os schemas do OpenAPI e detectar quebras de contrato no `tsc`; ecossistema npm (React Navigation, Jest, ESLint) roda no CI com Node 20; a mesma linguagem do restante do tooling web.
   2. **Contras:** dependência do ciclo de versões do Expo SDK (atualizações anuais com mudanças incompatíveis); módulos nativos fora do Expo Go exigem development build; desempenho inferior ao nativo em interfaces muito pesadas.
2. **Flutter (Dart).**
   1. **Prós:** bom desempenho e renderização consistente entre plataformas; tooling de testes integrado.
   2. **Contras:** exige SDK do Flutter e emuladores para rodar; Dart é uma linguagem a mais para a equipe; o CI precisaria de outra imagem e passos diferentes dos previstos no ADR-007.
3. **Nativo (Kotlin para Android e Swift para iOS).**
   1. **Prós:** melhor desempenho e acesso completo às APIs da plataforma.
   2. **Contras:** dois códigos para uma única pessoa manter; iOS exige macOS e Xcode; esforço desproporcional ao escopo.
4. **PWA (aplicação web progressiva).**
   1. **Prós:** sem instalação por loja; um código; roda em qualquer navegador.
   2. **Contras:** experiência e recursos limitados no iOS (notificações push e instalação restritas); não atende ao requisito de app mobile para Android e iOS; CORS obrigatório na API.

## Decisão

Escolhemos **React Native com Expo e TypeScript**, porque entrega Android e iOS (e web, para demonstração) com um único código, roda sem SDKs nativos e é validado pelo `mobile-ci` apenas com Node.

Versão: **Expo SDK 57** (React Native 0.86, React 19.2, TypeScript 6). O SDK 58, mais recente, foi descartado por exigir Node 22.13 ou superior (React Native 0.88/Metro), incompatível com o Node 20 definido no ADR-007; o SDK 57 aceita Node a partir de 20.19.4. A migração para o SDK 58 pode ser feita junto com a atualização do `mobile-ci` para Node 22. Pelo mesmo motivo, `@testing-library/react-native` fica na versão 13 e `eslint-plugin-expo` é fixado em 1.1.x via `overrides` no `package.json` (as versões seguintes exigem Node 22).

Diretrizes adotadas em `apps/mobile/`:

- **Camada de API por interface**: as telas dependem de `PlantaoPetApi` (`src/api/types.ts`), cujos tipos espelham os schemas do OpenAPI. Há duas implementações:
  - `HttpPlantaoPetApi`: `fetch` contra `EXPO_PUBLIC_API_URL` (padrão `http://localhost:8080`), convertendo respostas Problem (RFC 7807) em um `ApiError` tipado com mensagem amigável em português;
  - `MockPlantaoPetApi`: dados em memória com os IDs do seed `V4__seed_demo_data.sql`, as mesmas regras de negócio (sobreposição 409, transições, 422) e a mesma tabela de preços do contrato, com latência simulada.
- **Fábrica + injeção de dependência**: `createApi` escolhe a implementação por `EXPO_PUBLIC_USE_MOCKS` (padrão `true`) e o `ApiProvider` (React Context) a injeta; testes passam a implementação diretamente.
- **Estado**: React Context + hooks (sessão com o perfil escolhido, carregamento assíncrono). Sem Redux, pelo tamanho do app.
- **Navegação**: React Navigation (native-stack + bottom-tabs).
- **Qualidade**: ESLint com `eslint-config-expo`, Jest com `jest-expo` e Testing Library, `tsc --noEmit` limpo.

## Consequências

Positivas: o app é desenvolvido e demonstrado sem depender da API estar no ar; trocar mock por API real é só uma variável de ambiente; mudanças no contrato aparecem como erro de tipo; as regras de negócio do mock são testadas e servem como referência executável do contrato para o mobile; o CI valida o app sem emulador.

Negativas / riscos: as regras e preços existem em dois lugares (API e mock) e podem divergir; o mock deve ser atualizado no mesmo PR que alterar o contrato, e o teste de integração real continua sendo contra a API local. O Expo SDK precisa ser atualizado periodicamente. Variáveis `EXPO_PUBLIC_*` ficam embutidas no bundle e não podem conter segredos. Para a versão web, a API precisa liberar CORS.

## Impacto no C4

Nível afetado: C2

O que muda: o container **App mobile** (React Native/Expo, TypeScript) é detalhado com a tecnologia escolhida e se comunica apenas com o container **API** via HTTPS/JSON (contrato `docs/api/openapi.yaml`); em ambiente local, HTTP em `localhost:8080`. O app não acessa banco nem broker diretamente. Os níveis C1 e C3 não mudam.

## Referências

- Expo. Documentation. <https://docs.expo.dev/>
- React Native. Documentation. <https://reactnative.dev/docs/getting-started>
- React Navigation. Documentation. <https://reactnavigation.org/docs/getting-started>
- NOTTINGHAM, M.; WILDE, E.; DALAL, S. RFC 9457 (substitui a RFC 7807): Problem Details for HTTP APIs. <https://www.rfc-editor.org/rfc/rfc9457>
- Flutter. Documentation. <https://docs.flutter.dev/>
