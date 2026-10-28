package br.pucminas.plantaopet.api.pricing;

import br.pucminas.plantaopet.api.domain.ServiceType;
import org.springframework.stereotype.Component;

import java.time.Instant;

/** Cuidado temporário: R$ 20,00 por hora iniciada (mínimo 1h). */
@Component
public class TemporaryCarePriceStrategy implements PriceStrategy {

    static final long CENTS_PER_HOUR = 2_000L;

    @Override
    public ServiceType serviceType() {
        return ServiceType.TEMPORARY_CARE;
    }

    @Override
    public long calculate(Instant start, Instant end) {
        return PricingPeriods.startedHours(start, end) * CENTS_PER_HOUR;
    }
}
