package br.pucminas.plantaopet.api.pricing;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

/** Funções de contagem de unidades cobráveis compartilhadas pelas estratégias. */
final class PricingPeriods {

    private static final long SECONDS_PER_HOUR = 3_600L;
    private static final long SECONDS_PER_DAY = 86_400L;

    private PricingPeriods() {
    }

    /** Horas iniciadas (arredondamento para cima), mínimo 1. */
    static long startedHours(Instant start, Instant end) {
        return startedUnits(start, end, SECONDS_PER_HOUR);
    }

    /** Blocos de 24h iniciados (arredondamento para cima), mínimo 1. */
    static long started24hBlocks(Instant start, Instant end) {
        return startedUnits(start, end, SECONDS_PER_DAY);
    }

    /**
     * Dias de calendário UTC tocados pelo período [start, end). O fim é exclusivo:
     * um período que termina exatamente à meia-noite não conta o dia seguinte.
     */
    static long calendarDaysUtc(Instant start, Instant end) {
        requireValid(start, end);
        LocalDate first = LocalDate.ofInstant(start, ZoneOffset.UTC);
        LocalDate last = LocalDate.ofInstant(end.minusNanos(1), ZoneOffset.UTC);
        return ChronoUnit.DAYS.between(first, last) + 1;
    }

    private static long startedUnits(Instant start, Instant end, long unitSeconds) {
        requireValid(start, end);
        Duration duration = Duration.between(start, end);
        long seconds = duration.getSeconds() + (duration.getNano() > 0 ? 1 : 0);
        long units = (seconds + unitSeconds - 1) / unitSeconds;
        return Math.max(1, units);
    }

    private static void requireValid(Instant start, Instant end) {
        if (start == null || end == null || !end.isAfter(start)) {
            throw new IllegalArgumentException("end must be after start");
        }
    }
}
