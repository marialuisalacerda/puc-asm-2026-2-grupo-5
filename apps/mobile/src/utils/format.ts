import type { BookingStatus, PetSize, ServiceType } from '../api/types';

const currencyFormatter = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });

/** 6000 → "R$ 60,00" (pode conter espaço não separável, conforme Intl). */
export function formatCents(cents: number, currency = 'BRL'): string {
  const formatter =
    currency === 'BRL' ? currencyFormatter : new Intl.NumberFormat('pt-BR', { style: 'currency', currency });
  return formatter.format(cents / 100);
}

const pad = (n: number) => String(n).padStart(2, '0');

/** "29/10/2026 09:00" no fuso local do aparelho. */
export function formatDateTime(value: string | Date): string {
  const d = typeof value === 'string' ? new Date(value) : value;
  if (Number.isNaN(d.getTime())) return '—';
  return `${pad(d.getDate())}/${pad(d.getMonth() + 1)}/${d.getFullYear()} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

/** Intervalo compacto: "29/10/2026 09:00 – 11:00" (mesmo dia) ou datas completas. */
export function formatPeriod(start: string | Date, end: string | Date): string {
  const s = typeof start === 'string' ? new Date(start) : start;
  const e = typeof end === 'string' ? new Date(end) : end;
  const sameDay = s.toDateString() === e.toDateString();
  return sameDay
    ? `${formatDateTime(s)} – ${pad(e.getHours())}:${pad(e.getMinutes())}`
    : `${formatDateTime(s)} – ${formatDateTime(e)}`;
}

/** 90 → "1h30", 120 → "2h", 1440 → "1 dia", 2880 → "2 dias". */
export function formatDuration(minutes: number): string {
  if (minutes >= 1440 && minutes % 1440 === 0) {
    const days = minutes / 1440;
    return days === 1 ? '1 dia' : `${days} dias`;
  }
  const h = Math.floor(minutes / 60);
  const m = minutes % 60;
  if (h === 0) return `${m} min`;
  return m === 0 ? `${h}h` : `${h}h${pad(m)}`;
}

export const SERVICE_TYPE_LABELS: Record<ServiceType, string> = {
  WALK: 'Passeio',
  COMPANIONSHIP: 'Companhia',
  HOME_VISIT: 'Visita domiciliar',
  HOSTING: 'Hospedagem',
  TEMPORARY_CARE: 'Cuidado temporário',
};

export const PET_SIZE_LABELS: Record<PetSize, string> = {
  SMALL: 'Pequeno',
  MEDIUM: 'Médio',
  LARGE: 'Grande',
};

export const BOOKING_STATUS_LABELS: Record<BookingStatus, string> = {
  REQUESTED: 'Solicitada',
  ACCEPTED: 'Aceita',
  PAID: 'Paga',
  IN_PROGRESS: 'Em andamento',
  COMPLETED: 'Concluída',
  REJECTED: 'Recusada',
  CANCELLED: 'Cancelada',
  EXPIRED: 'Expirada',
};

/** Cores com contraste suficiente para texto branco; o rótulo é sempre exibido junto. */
export const BOOKING_STATUS_COLORS: Record<BookingStatus, string> = {
  REQUESTED: '#9A6700',
  ACCEPTED: '#1A7F37',
  PAID: '#0969DA',
  IN_PROGRESS: '#6639BA',
  COMPLETED: '#1F6FEB',
  REJECTED: '#CF222E',
  CANCELLED: '#57606A',
  EXPIRED: '#6E7781',
};
