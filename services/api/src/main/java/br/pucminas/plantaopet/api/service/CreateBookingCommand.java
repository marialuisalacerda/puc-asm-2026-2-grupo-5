package br.pucminas.plantaopet.api.service;

import br.pucminas.plantaopet.api.domain.ServiceType;

import java.time.Instant;
import java.util.UUID;

/** Dados de entrada do caso de uso de criação de reserva, independentes da camada HTTP. */
public record CreateBookingCommand(
        UUID tutorId,
        UUID petId,
        UUID caregiverId,
        ServiceType serviceType,
        Instant scheduledStart,
        Instant scheduledEnd) {
}
