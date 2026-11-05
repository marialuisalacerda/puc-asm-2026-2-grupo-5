import type { ServiceType } from '../api/types';

/** Predefinições de agenda, para não depender de date-picker nativo. */
export const DAY_OPTIONS = [
  { offset: 0, label: 'Hoje' },
  { offset: 1, label: 'Amanhã' },
  { offset: 2, label: 'Depois de amanhã' },
  { offset: 7, label: 'Em 1 semana' },
] as const;

export const TIME_OPTIONS = ['07:00', '09:00', '13:00', '15:00', '18:00', '20:00'] as const;

/** Durações em minutos sugeridas por tipo de serviço. */
export const DURATION_OPTIONS: Record<ServiceType, number[]> = {
  WALK: [30, 60, 90, 120],
  COMPANIONSHIP: [60, 120, 180, 240],
  HOME_VISIT: [60, 1440, 2880],
  HOSTING: [1440, 2880, 4320, 10080],
  TEMPORARY_CARE: [120, 240, 480, 720],
};

export interface Period {
  start: Date;
  end: Date;
}

/**
 * Monta o período a partir de um dia relativo (0 = hoje), horário "HH:mm" local e duração em minutos.
 */
export function buildPeriod(base: Date, dayOffset: number, time: string, durationMinutes: number): Period {
  const [hours, minutes] = time.split(':').map(Number);
  const start = new Date(base.getFullYear(), base.getMonth(), base.getDate() + dayOffset, hours, minutes, 0, 0);
  const end = new Date(start.getTime() + durationMinutes * 60_000);
  return { start, end };
}

export function isInFuture(date: Date, now: Date = new Date()): boolean {
  return date.getTime() > now.getTime();
}
