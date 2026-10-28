package br.pucminas.plantaopet.api.pricing;

import br.pucminas.plantaopet.api.domain.ServiceType;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Contexto do Strategy Pattern: recebe todas as {@link PriceStrategy} registradas no Spring
 * e delega o cálculo à estratégia do tipo de serviço. Falha na inicialização se algum tipo
 * não tiver estratégia ou tiver mais de uma, evitando erro só em tempo de requisição.
 */
@Component
public class PriceCalculator {

    public static final String CURRENCY = "BRL";

    private final Map<ServiceType, PriceStrategy> strategies;

    public PriceCalculator(List<PriceStrategy> strategies) {
        Map<ServiceType, PriceStrategy> byType = new EnumMap<>(ServiceType.class);
        for (PriceStrategy strategy : strategies) {
            PriceStrategy previous = byType.putIfAbsent(strategy.serviceType(), strategy);
            if (previous != null) {
                throw new IllegalStateException("Duplicated PriceStrategy for " + strategy.serviceType()
                        + ": " + previous.getClass().getSimpleName() + ", " + strategy.getClass().getSimpleName());
            }
        }
        for (ServiceType type : ServiceType.values()) {
            if (!byType.containsKey(type)) {
                throw new IllegalStateException("Missing PriceStrategy for " + type);
            }
        }
        this.strategies = byType;
    }

    /** Preço em centavos de BRL. Pré-condição: {@code end} posterior a {@code start}. */
    public long calculate(ServiceType serviceType, Instant start, Instant end) {
        return strategies.get(serviceType).calculate(start, end);
    }
}
