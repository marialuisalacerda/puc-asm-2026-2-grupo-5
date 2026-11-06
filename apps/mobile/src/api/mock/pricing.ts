import type { ServiceType } from '../types';

/**
 * Espelho da tabela de preços do contrato (docs/architecture/contratos-integracao.md),
 * usado somente pelo MockPlantaoPetApi. Na API real o cálculo é feito no backend (Strategy).
 * Valores em centavos de BRL.
 */
const HOUR_MS = 60 * 60 * 1000;
const DAY_MS = 24 * HOUR_MS;

export interface PriceStrategy {
  calculate(start: Date, end: Date): number;
}

const perStartedHour = (centsPerHour: number): PriceStrategy => ({
  calculate: (start, end) => Math.max(1, Math.ceil((end.getTime() - start.getTime()) / HOUR_MS)) * centsPerHour,
});

/** Conta os dias de calendário UTC tocados pelo período [início, fim). */
export function utcCalendarDaysCovered(start: Date, end: Date): number {
  const startDay = Math.floor(start.getTime() / DAY_MS);
  // Período semiaberto: terminar exatamente à meia-noite UTC não cobra o dia seguinte.
  const endDay = Math.floor((end.getTime() - 1) / DAY_MS);
  return Math.max(1, endDay - startDay + 1);
}

export const PRICE_STRATEGIES: Record<ServiceType, PriceStrategy> = {
  WALK: perStartedHour(3000),
  COMPANIONSHIP: perStartedHour(2500),
  HOME_VISIT: { calculate: (start, end) => utcCalendarDaysCovered(start, end) * 4500 },
  HOSTING: {
    calculate: (start, end) => Math.max(1, Math.ceil((end.getTime() - start.getTime()) / DAY_MS)) * 12000,
  },
  TEMPORARY_CARE: perStartedHour(2000),
};

export function calculatePriceCents(serviceType: ServiceType, start: Date, end: Date): number {
  return PRICE_STRATEGIES[serviceType].calculate(start, end);
}
