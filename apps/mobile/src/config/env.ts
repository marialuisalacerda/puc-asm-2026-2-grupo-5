/**
 * Configuração lida das variáveis públicas do Expo (docs/architecture/contratos-integracao.md).
 * O Expo só embute no bundle variáveis acessadas literalmente como process.env.EXPO_PUBLIC_*.
 */
export interface AppConfig {
  apiUrl: string;
  useMocks: boolean;
}

export const DEFAULT_API_URL = 'http://localhost:8080';

export function parseUseMocks(value: string | undefined): boolean {
  if (value === undefined || value.trim() === '') return true;
  return !['false', '0', 'no', 'nao', 'não'].includes(value.trim().toLowerCase());
}

export function readConfig(
  env: { EXPO_PUBLIC_API_URL?: string; EXPO_PUBLIC_USE_MOCKS?: string } = {
    EXPO_PUBLIC_API_URL: process.env.EXPO_PUBLIC_API_URL,
    EXPO_PUBLIC_USE_MOCKS: process.env.EXPO_PUBLIC_USE_MOCKS,
  },
): AppConfig {
  return {
    apiUrl: env.EXPO_PUBLIC_API_URL?.trim() || DEFAULT_API_URL,
    useMocks: parseUseMocks(env.EXPO_PUBLIC_USE_MOCKS),
  };
}
