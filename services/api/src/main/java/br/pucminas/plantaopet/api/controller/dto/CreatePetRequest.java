package br.pucminas.plantaopet.api.controller.dto;

import br.pucminas.plantaopet.api.domain.PetSize;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreatePetRequest(
        @NotNull UUID tutorId,
        @NotBlank @Size(max = 80) String name,
        @NotBlank @Size(max = 40) String species,
        PetSize size,
        @Size(max = 500) String notes) {
}
