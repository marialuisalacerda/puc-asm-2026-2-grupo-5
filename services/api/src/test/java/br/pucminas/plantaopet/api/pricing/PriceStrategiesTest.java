package br.pucminas.plantaopet.api.pricing;

import br.pucminas.plantaopet.api.domain.ServiceType;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PriceStrategiesTest {

    private static Instant at(String iso) {
        return Instant.parse(iso);
    }

    @Nested
    class Walk {

        private final PriceStrategy strategy = new WalkPriceStrategy();

        @Test
        void servesWalk() {
            assertThat(strategy.serviceType()).isEqualTo(ServiceType.WALK);
        }

        @ParameterizedTest(name = "{0} → {1} = {2}")
        @CsvSource({
                // exemplos do contrato
                "2026-11-02T13:00:00Z, 2026-11-02T15:00:00Z, 6000",
                "2026-11-02T13:00:00Z, 2026-11-02T14:30:00Z, 6000",
                // mínimo de 1h e hora iniciada
                "2026-11-02T13:00:00Z, 2026-11-02T13:15:00Z, 3000",
                "2026-11-02T13:00:00Z, 2026-11-02T14:00:00Z, 3000",
                "2026-11-02T13:00:00Z, 2026-11-02T14:00:01Z, 6000"
        })
        void chargesThirtyReaisPerStartedHour(String start, String end, long expected) {
            assertThat(strategy.calculate(at(start), at(end))).isEqualTo(expected);
        }
    }

    @Nested
    class Companionship {

        private final PriceStrategy strategy = new CompanionshipPriceStrategy();

        @Test
        void servesCompanionship() {
            assertThat(strategy.serviceType()).isEqualTo(ServiceType.COMPANIONSHIP);
        }

        @ParameterizedTest(name = "{0} → {1} = {2}")
        @CsvSource({
                "2026-11-02T13:00:00Z, 2026-11-02T15:00:00Z, 5000",
                "2026-11-02T13:00:00Z, 2026-11-02T14:30:00Z, 5000",
                "2026-11-02T13:00:00Z, 2026-11-02T13:10:00Z, 2500"
        })
        void chargesTwentyFiveReaisPerStartedHour(String start, String end, long expected) {
            assertThat(strategy.calculate(at(start), at(end))).isEqualTo(expected);
        }
    }

    @Nested
    class TemporaryCare {

        private final PriceStrategy strategy = new TemporaryCarePriceStrategy();

        @Test
        void servesTemporaryCare() {
            assertThat(strategy.serviceType()).isEqualTo(ServiceType.TEMPORARY_CARE);
        }

        @ParameterizedTest(name = "{0} → {1} = {2}")
        @CsvSource({
                "2026-11-02T13:00:00Z, 2026-11-02T15:00:00Z, 4000",
                "2026-11-02T13:00:00Z, 2026-11-02T14:30:00Z, 4000",
                "2026-11-02T13:00:00Z, 2026-11-02T13:01:00Z, 2000"
        })
        void chargesTwentyReaisPerStartedHour(String start, String end, long expected) {
            assertThat(strategy.calculate(at(start), at(end))).isEqualTo(expected);
        }
    }

    @Nested
    class HomeVisit {

        private final PriceStrategy strategy = new HomeVisitPriceStrategy();

        @Test
        void servesHomeVisit() {
            assertThat(strategy.serviceType()).isEqualTo(ServiceType.HOME_VISIT);
        }

        @ParameterizedTest(name = "{0} → {1} = {2}")
        @CsvSource({
                // mesmo dia
                "2026-11-02T13:00:00Z, 2026-11-02T15:00:00Z, 4500",
                // atravessa a meia-noite UTC: 2 dias de calendário, mesmo com apenas 2h
                "2026-11-02T23:00:00Z, 2026-11-03T01:00:00Z, 9000",
                // três dias de calendário
                "2026-11-02T08:00:00Z, 2026-11-04T08:00:00Z, 13500",
                // fim exclusivo: terminar exatamente à meia-noite não cobra o dia seguinte
                "2026-11-02T20:00:00Z, 2026-11-03T00:00:00Z, 4500"
        })
        void chargesFortyFiveReaisPerUtcCalendarDay(String start, String end, long expected) {
            assertThat(strategy.calculate(at(start), at(end))).isEqualTo(expected);
        }
    }

    @Nested
    class Hosting {

        private final PriceStrategy strategy = new HostingPriceStrategy();

        @Test
        void servesHosting() {
            assertThat(strategy.serviceType()).isEqualTo(ServiceType.HOSTING);
        }

        @ParameterizedTest(name = "{0} → {1} = {2}")
        @CsvSource({
                // exemplo do contrato: 30h → 2 diárias
                "2026-11-02T10:00:00Z, 2026-11-03T16:00:00Z, 24000",
                // mínimo de 1 diária
                "2026-11-02T10:00:00Z, 2026-11-02T12:00:00Z, 12000",
                "2026-11-02T10:00:00Z, 2026-11-03T10:00:00Z, 12000",
                "2026-11-02T10:00:00Z, 2026-11-05T10:00:00Z, 36000"
        })
        void chargesOneHundredTwentyReaisPerStarted24h(String start, String end, long expected) {
            assertThat(strategy.calculate(at(start), at(end))).isEqualTo(expected);
        }
    }

    @Test
    void rejectsInvalidPeriod() {
        PriceStrategy strategy = new WalkPriceStrategy();
        Instant instant = at("2026-11-02T13:00:00Z");

        assertThatThrownBy(() -> strategy.calculate(instant, instant))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
