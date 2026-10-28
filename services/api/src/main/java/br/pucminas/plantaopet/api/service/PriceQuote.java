package br.pucminas.plantaopet.api.service;

import br.pucminas.plantaopet.api.domain.ServiceType;

import java.time.Instant;

public record PriceQuote(ServiceType serviceType, Instant scheduledStart, Instant scheduledEnd,
                         long priceCents, String currency) {
}
