package br.pucminas.plantaopet.api.pricing;

import br.pucminas.plantaopet.api.domain.ServiceType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PriceCalculatorTest {

    static PriceCalculator withAllStrategies() {
        return new PriceCalculator(List.of(
                new WalkPriceStrategy(),
                new CompanionshipPriceStrategy(),
                new HomeVisitPriceStrategy(),
                new HostingPriceStrategy(),
                new TemporaryCarePriceStrategy()));
    }

    @Test
    void delegatesToStrategyOfServiceType() {
        PriceCalculator calculator = withAllStrategies();
        Instant start = Instant.parse("2026-11-02T13:00:00Z");
        Instant end = Instant.parse("2026-11-02T15:00:00Z");

        assertThat(calculator.calculate(ServiceType.WALK, start, end)).isEqualTo(6000);
        assertThat(calculator.calculate(ServiceType.COMPANIONSHIP, start, end)).isEqualTo(5000);
        assertThat(calculator.calculate(ServiceType.TEMPORARY_CARE, start, end)).isEqualTo(4000);
        assertThat(calculator.calculate(ServiceType.HOME_VISIT, start, end)).isEqualTo(4500);
        assertThat(calculator.calculate(ServiceType.HOSTING, start, end)).isEqualTo(12000);
    }

    @Test
    void failsFastWhenAServiceTypeHasNoStrategy() {
        assertThatThrownBy(() -> new PriceCalculator(List.of(new WalkPriceStrategy())))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Missing PriceStrategy");
    }

    @Test
    void failsFastOnDuplicatedStrategy() {
        assertThatThrownBy(() -> new PriceCalculator(List.of(new WalkPriceStrategy(), new WalkPriceStrategy())))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Duplicated PriceStrategy");
    }
}
