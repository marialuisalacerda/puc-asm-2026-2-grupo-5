package br.pucminas.plantaopet.api.pricing;

import br.pucminas.plantaopet.api.domain.ServiceType;
import org.springframework.stereotype.Component;

import java.time.Instant;

/** Hospedagem: R$ 120,00 por diária (blocos de 24h iniciados, mínimo 1). */
@Component
public class HostingPriceStrategy implements PriceStrategy {

    static final long CENTS_PER_DAILY_RATE = 12_000L;

    @Override
    public ServiceType serviceType() {
        return ServiceType.HOSTING;
    }

    @Override
    public long calculate(Instant start, Instant end) {
        return PricingPeriods.started24hBlocks(start, end) * CENTS_PER_DAILY_RATE;
    }
}
