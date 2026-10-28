import { readConfig, parseUseMocks } from '../../config/env';
import {
  BOOKING_STATUS_LABELS,
  SERVICE_TYPE_LABELS,
  formatCents,
  formatDateTime,
  formatDuration,
  formatPeriod,
} from '../format';
import { buildPeriod, isInFuture } from '../schedule';

/** Intl usa espaço não separável entre "R$" e o valor. */
const normalize = (s: string) => s.replace(/ /g, ' ');

describe('formatCents', () => {
  it('formata centavos em BRL', () => {
    expect(normalize(formatCents(6000))).toBe('R$ 60,00');
    expect(normalize(formatCents(123456))).toBe('R$ 1.234,56');
    expect(normalize(formatCents(0))).toBe('R$ 0,00');
  });
});

describe('formatDateTime / formatPeriod', () => {
  it('usa dd/MM/aaaa HH:mm no fuso local', () => {
    expect(formatDateTime(new Date(2026, 9, 29, 9, 5))).toBe('29/10/2026 09:05');
    expect(formatDateTime('invalida')).toBe('—');
  });

  it('compacta períodos no mesmo dia', () => {
    expect(formatPeriod(new Date(2026, 9, 29, 9, 0), new Date(2026, 9, 29, 11, 0))).toBe('29/10/2026 09:00 – 11:00');
    expect(formatPeriod(new Date(2026, 9, 29, 9, 0), new Date(2026, 9, 30, 9, 0))).toBe(
      '29/10/2026 09:00 – 30/10/2026 09:00',
    );
  });
});

describe('formatDuration', () => {
  it.each([
    [30, '30 min'],
    [60, '1h'],
    [90, '1h30'],
    [1440, '1 dia'],
    [4320, '3 dias'],
  ])('%i min → %s', (minutes, expected) => {
    expect(formatDuration(minutes)).toBe(expected);
  });
});

describe('rótulos em português', () => {
  it('traduz serviços e status', () => {
    expect(SERVICE_TYPE_LABELS.HOME_VISIT).toBe('Visita domiciliar');
    expect(SERVICE_TYPE_LABELS.TEMPORARY_CARE).toBe('Cuidado temporário');
    expect(BOOKING_STATUS_LABELS.REQUESTED).toBe('Solicitada');
    expect(BOOKING_STATUS_LABELS.CANCELLED).toBe('Cancelada');
  });
});

describe('buildPeriod', () => {
  it('monta "amanhã 09:00" + 2h', () => {
    const { start, end } = buildPeriod(new Date(2026, 9, 28, 15, 30), 1, '09:00', 120);
    expect(start).toEqual(new Date(2026, 9, 29, 9, 0));
    expect(end).toEqual(new Date(2026, 9, 29, 11, 0));
  });

  it('vira o mês corretamente', () => {
    expect(buildPeriod(new Date(2026, 9, 31, 8), 1, '07:00', 60).start).toEqual(new Date(2026, 10, 1, 7, 0));
  });

  it('isInFuture compara com o relógio informado', () => {
    const now = new Date(2026, 9, 28, 12);
    expect(isInFuture(new Date(2026, 9, 28, 13), now)).toBe(true);
    expect(isInFuture(new Date(2026, 9, 28, 9), now)).toBe(false);
  });
});

describe('config (variáveis EXPO_PUBLIC_*)', () => {
  it('usa mocks e localhost por padrão', () => {
    expect(readConfig({})).toEqual({ apiUrl: 'http://localhost:8080', useMocks: true });
  });

  it('lê URL e desativa mocks com "false"', () => {
    expect(readConfig({ EXPO_PUBLIC_API_URL: 'http://10.0.2.2:8080', EXPO_PUBLIC_USE_MOCKS: 'false' })).toEqual({
      apiUrl: 'http://10.0.2.2:8080',
      useMocks: false,
    });
    expect(parseUseMocks('TRUE')).toBe(true);
    expect(parseUseMocks('0')).toBe(false);
  });
});
