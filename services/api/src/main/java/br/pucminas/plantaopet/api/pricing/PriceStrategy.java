package br.pucminas.plantaopet.api.pricing;

import br.pucminas.plantaopet.api.domain.ServiceType;

import java.time.Instant;

/**
 * Strategy Pattern: cada tipo de serviço tem sua regra de preço.
 * Implementações são beans Spring e são selecionadas pelo {@link PriceCalculator}.
 */
public interface PriceStrategy {

    /** Tipo de serviço atendido por esta estratégia. */
    ServiceType serviceType();

    /**
     * Calcula o preço, em centavos de BRL, do período [start, end).
     * Pré-condição: {@code end} é posterior a {@code start}.
     */
    long calculate(Instant start, Instant end);
}
