package br.pucminas.plantaopet.api.pricing;

import br.pucminas.plantaopet.api.domain.ServiceType;
import org.springframework.stereotype.Component;

import java.time.Instant;

/** Companhia: R$ 25,00 por hora iniciada (mínimo 1h). */
@Component
public class CompanionshipPriceStrategy implements PriceStrategy {

    static final long CENTS_PER_HOUR = 2_500L;

    @Override
    public ServiceType serviceType() {
        return ServiceType.COMPANIONSHIP;
    }

    @Override
    public long calculate(Instant start, Instant end) {
        return PricingPeriods.startedHours(start, end) * CENTS_PER_HOUR;
    }
}
