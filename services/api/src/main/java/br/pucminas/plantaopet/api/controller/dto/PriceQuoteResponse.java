package br.pucminas.plantaopet.api.controller.dto;

import br.pucminas.plantaopet.api.domain.ServiceType;
import br.pucminas.plantaopet.api.service.PriceQuote;

import java.time.Instant;

public record PriceQuoteResponse(
        ServiceType serviceType,
        Instant scheduledStart,
        Instant scheduledEnd,
        long priceCents,
        String currency) {

    public static PriceQuoteResponse from(PriceQuote quote) {
        return new PriceQuoteResponse(quote.serviceType(), quote.scheduledStart(), quote.scheduledEnd(),
                quote.priceCents(), quote.currency());
    }
}
