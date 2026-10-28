package br.pucminas.plantaopet.api.pricing;

import br.pucminas.plantaopet.api.domain.ServiceType;
import org.springframework.stereotype.Component;

import java.time.Instant;

/** Visita domiciliar: R$ 45,00 por dia de calendário (UTC) coberto pelo período. */
@Component
public class HomeVisitPriceStrategy implements PriceStrategy {

    static final long CENTS_PER_DAY = 4_500L;

    @Override
    public ServiceType serviceType() {
        return ServiceType.HOME_VISIT;
    }

    @Override
    public long calculate(Instant start, Instant end) {
        return PricingPeriods.calendarDaysUtc(start, end) * CENTS_PER_DAY;
    }
}
