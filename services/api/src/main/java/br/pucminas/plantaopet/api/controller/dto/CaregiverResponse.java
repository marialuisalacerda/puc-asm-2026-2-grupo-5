package br.pucminas.plantaopet.api.controller.dto;

import br.pucminas.plantaopet.api.domain.Caregiver;

import java.util.UUID;

public record CaregiverResponse(UUID id, String name, String email, boolean active) {

    public static CaregiverResponse from(Caregiver caregiver) {
        return new CaregiverResponse(caregiver.getId(), caregiver.getName(), caregiver.getEmail(),
                caregiver.isActive());
    }
}
